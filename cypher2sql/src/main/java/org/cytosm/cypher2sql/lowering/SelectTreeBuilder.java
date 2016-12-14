package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.cypher.constexpr.ConstExpressionFolder;
import org.cytosm.cypher2sql.lowering.sqltree.*;
import org.cytosm.cypher2sql.lowering.typeck.ClauseId;
import org.cytosm.cypher2sql.lowering.typeck.VarDependencies;
import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTreeBuilder;
import org.cytosm.cypher2sql.cypher.ast.*;
import org.cytosm.cypher2sql.cypher.ast.clause.*;
import org.cytosm.cypher2sql.cypher.ast.clause.projection.*;
import org.cytosm.cypher2sql.cypher.ast.clause.match.*;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.stream.Collectors;

/**
 * Select Tree Builder is in charge of converting the Cypher tree
 * into a Select tree. It won't performs any optimization and does not
 * provide a complete SQL tree. It only transform the Cypher AST into
 * the canonical SQL tree form. The latter will be modified by later passes.
 *
 */
public class SelectTreeBuilder {

    /**
     * Create the SQL tree from the Cypher AST and the analysis result from
     * previous passes.
     * @param varDependencies is the list of variables and their interactions.
     * @param st is the root of the cypher AST.
     * @return Returns the root of the SQL tree.
     */
    public static ScopeSelect createQueryTree(final VarDependencies varDependencies,
                                              final Statement st) {
        SelectFolder folder = new SelectFolder(varDependencies);
        try {
            return folder.foldSingleQuery((SingleQuery) st.query.part);
        } catch (SelectTreeException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class SelectTreeException extends Exception {
        SelectTreeException(final String message) { super(message); }
    }

    public static class InvalidExpression extends SelectTreeException {
        InvalidExpression(final String message) {
            super("Invalid expression: " + message);
        }
    }

    /**
     *      ScopeSelect:
     *          with [
     *              WithSelect - SimpleSelect' (1),
     *              WithSelect - SimpleSelect' (2),
     *              WithSelect - SimpleSelect' (3),
     *          ]
     *          ret: SimpleSelect'

     */
    private static class SelectFolder {

        private final VarDependencies varDependencies;
        // ScopeSelect is special: it is created at SingleQuery and modified by foldReturn.
        private ScopeSelect top;

        SelectFolder(final VarDependencies varDependencies) {
            this.varDependencies = varDependencies;
        }

        ScopeSelect foldSingleQuery(SingleQuery singleQuery) throws SelectTreeException {
            top = new ScopeSelect();

            // We are about to put all the clause (converted as SELECT)
            // within
            Iterator<Clause> iter = singleQuery.clauses.iterator();
            while (iter.hasNext()) {
                Clause clause = iter.next();
                if (clause instanceof Match) {
                    // Match clause gets added to the ScopeSelect.
                    top.withQueries.addAll(
                        this.foldMatch((Match) clause)
                            .stream().map(WithSelect::new)
                            .collect(Collectors.toList())
                    );
                } else if (clause instanceof With) {
                    top.withQueries.add(new WithSelect(this.foldWith((With) clause)));
                } else if (clause instanceof Return) {
                    top.ret = this.foldReturn((Return) clause);
                    return top;
                }
            }
            throw new SelectTreeException("Invalid tree: No RETURN statement.");
        }

        SimpleSelect foldWith(With with) throws SelectTreeException {
            return foldProjectionClause(with);
        }

        SimpleSelect foldReturn(Return retnode) throws SelectTreeException {
            return foldProjectionClause(retnode);
        }

        SimpleSelect foldProjectionClause(ProjectionClause projectionClause) throws SelectTreeException {
            SimpleSelect select = new SimpleSelectWithInnerJoins();

            if (projectionClause.limit.isPresent()) {
                ConstExpressionFolder.ConstExprValue val = ConstExpressionFolder.eval(
                        projectionClause.limit.get().expression);
                try {
                    select.limit = val.asLong();
                } catch (ConstExpressionFolder.ConstExprException e) {
                    throw new InvalidExpression(
                            "LIMIT can only resolve to an integer and cannot refer to variables"
                    );
                }
            }

            if (projectionClause.skip.isPresent()) {
                ConstExpressionFolder.ConstExprValue val = ConstExpressionFolder.eval(
                        projectionClause.skip.get().expression);
                try {
                    select.skip = val.asLong();
                } catch (ConstExpressionFolder.ConstExprException e) {
                    throw new InvalidExpression(
                            "SKIP can only resolve to an integer and cannot refer to variables"
                    );
                }
            }

            select.isDistinct = projectionClause.distinct;

            if (projectionClause.orderBy.isPresent()) {
                OrderBy orderBy = projectionClause.orderBy.get();
                Iterator<SortItem> sortItems = orderBy.sortItems.iterator();
                while (sortItems.hasNext()) {
                    SortItem si = sortItems.next();
                    SimpleSelect.OrderItem oi = new SimpleSelect.OrderItem();
                    if (si instanceof SortItem.Desc) {
                        oi.descending = true;
                    }
                    oi.item = ExprTreeBuilder.buildFromCypherExpression(
                            si.expression, varDependencies.getReachableVars(new ClauseId(orderBy))
                    );
                    select.orderBy.add(oi);
                }
            }

            // This is how we refer to the variable being used.
            // Those are stored within the VarDependencies data structure.
            select.varId = new ClauseId(projectionClause);

            return select;
        }

        List<SimpleSelect> foldMatch(Match match) throws SelectTreeException {

            List<SimpleSelect> result = new ArrayList<>();

            Iterator<PatternPart> iter = match.pattern.patternParts.iterator();
            while (iter.hasNext()) {
                PatternPart pp = iter.next();

                // Create the SimpleSelect node
                SimpleSelect select;
                if (match.optional) {
                    select =  new SimpleSelectWithLeftJoins();
                } else {
                    select = new SimpleSelectWithInnerJoins();
                }

                // Assign the id.
                select.varId = new ClauseId(pp);

                // Add the select.
                result.add(select);
            }

            // If there's a where it will either finish in its own SELECT
            // or finish attached to the only SELECT that was defined
            // previously.
            if (match.where.isPresent()) {
                Where where = match.where.get();

                Expr condition = ExprTreeBuilder.buildFromCypherExpression(
                    where.expression, varDependencies.getReachableVars(new ClauseId(where))
                );

                // If we have more than one select then create a new one.
                if (result.size() > 1) {
                    SimpleSelect select;
                    if (match.optional) {
                        select = new SimpleSelectWithLeftJoins();
                    } else {
                        select = new SimpleSelectWithInnerJoins();
                    }

                    select.varId = new ClauseId(where);
                    select.whereCondition = condition;
                    result.add(select);
                } else {
                    result.get(0).whereCondition = condition;
                }
            }

            return result;
        }
    }
}
