package org.cytosm.cypher2sql.lowering;

import org.cytosm.common.gtop.GTopInterfaceImpl;
import org.cytosm.common.gtop.implementation.relational.ImplementationNode;
import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;
import org.cytosm.cypher2sql.lowering.sqltree.ScopeSelect;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelect;
import org.cytosm.cypher2sql.lowering.sqltree.visitor.Walk;
import org.cytosm.cypher2sql.lowering.typeck.expr.*;
import org.cytosm.cypher2sql.lowering.typeck.types.PathType;
import org.cytosm.cypher2sql.lowering.typeck.var.*;
import org.cytosm.cypher2sql.lowering.typeck.constexpr.ConstVal;

import static org.cytosm.cypher2sql.lowering.exceptions.fns.LambdaExceptionUtil.rethrowFunction;
import static org.cytosm.cypher2sql.lowering.exceptions.fns.LambdaExceptionUtil.rethrowConsumer;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class collect passes that transform functions
 * found into SQL equivalent. It contains passes that
 * will process individually each functions name. Some
 * will only affect the expression tree where they are
 * located, others needs to be ran conjointly to others
 * at specific point in the translation steps.
 *
 * For more details on each specific pass look at their
 * documentation.
 *
 */
public class TransformFunctions {

    /**
     * Look for all function COUNT uses and turn them as appropriate
     * into either a
     * @param tree is the SQL tree where COUNT will be updated.
     * @param gTopInterface is the implementation gTop.
     */
    public static void convertCypherCountFn(ScopeSelect tree, GTopInterfaceImpl gTopInterface)
            throws Cypher2SqlException
    {
        HashMap<ScopeSelect, Boolean> scopeIsLeaf = new HashMap<>();
        Walk.walkSQLNode(new ScopeSelectIsLeaf(scopeIsLeaf), tree);
        Walk.walkSQLNode(new CountVisitor(scopeIsLeaf, gTopInterface), tree);
    }

    private static class ScopeSelectIsLeaf extends Walk.BaseSQLNodeVisitor {

        private final HashMap<ScopeSelect, Boolean> scopeIsLeaf;
        private ScopeSelect parentScope;

        ScopeSelectIsLeaf(HashMap<ScopeSelect, Boolean> scopeIsLeaf) {
            this.scopeIsLeaf = scopeIsLeaf;
        }

        @Override
        public void visitScopeSelect(ScopeSelect scopeSelect) throws Cypher2SqlException {
            ScopeSelect parentScope = this.parentScope;
            this.switchScopeSelect(scopeSelect);
            super.visitScopeSelect(scopeSelect);
            this.parentScope = parentScope;
        }

        private void switchScopeSelect(ScopeSelect newScope) {
            if (parentScope != null) {
                this.scopeIsLeaf.put(parentScope, false);
            }
            this.scopeIsLeaf.put(newScope, true);
            this.parentScope = newScope;
        }
    }

    private static class CountVisitor extends Walk.BaseSQLNodeVisitor {

        private static final String COUNT = "count";
        private final HashMap<ScopeSelect, Boolean> scopeIsLeaf;
        private final GTopInterfaceImpl gtop;

        CountVisitor(HashMap<ScopeSelect, Boolean> scopeIsLeaf, GTopInterfaceImpl gtop) {
            this.scopeIsLeaf = scopeIsLeaf;
            this.gtop = gtop;
        }

        @Override
        public void visitSimpleSelect(SimpleSelect simpleSelect) throws Cypher2SqlException {
            // In a simple select that does not represent a Cypher RETURN
            // (only the ScopeSelect "ret" can and it is not captured by that pass)
            // we always go from the cypher name count to the SQL count.

            NameFnExpr namer = new NameFnExpr(gtop, COUNT, ExprFn.Name.COUNT);
            simpleSelect.exportedItems.forEach(e -> ExprWalk.walk(namer, e));
        }

        @Override
        public void visitScopeSelect(ScopeSelect scopeSelect) throws Cypher2SqlException {
            NameFnExpr namer;
            if (this.scopeIsLeaf.get(scopeSelect)) {
                namer = new NameFnExpr(gtop, COUNT, ExprFn.Name.COUNT);
            } else {
                namer = new NameFnExpr(gtop, COUNT, ExprFn.Name.SUM);
            }
            scopeSelect.ret.exportedItems.forEach(e -> ExprWalk.walk(namer, e));
            scopeSelect.withQueries.forEach(rethrowConsumer(this::visitWithSelect));
        }
    }

    private static class NameFnExpr extends ExprWalk.BaseVisitor {

        private final String fnCypherName;
        private final ExprFn.Name fnName;
        private final GTopInterfaceImpl gtop;

        NameFnExpr(GTopInterfaceImpl gtop, String fnCypherName, ExprFn.Name fnName) {
            this.gtop = gtop;
            this.fnCypherName = fnCypherName;
            this.fnName = fnName;
        }

        @Override
        public void visitFn(ExprFn expr) {
            if (expr.cypherName.equalsIgnoreCase(fnCypherName)) {
                expr.name = fnName;
            }
            // FIXME: Is is always a correct way of folding the argument?
            expr.args = expr.args.stream()
                    .map(x -> {
                        if (x instanceof ExprVar) {
                            return new ExprTree.PropertyAccess(getIdForVar(((ExprVar) x).var), x);
                        }
                        return x;
                    }).collect(Collectors.toList());
        }

        private String getIdForVar(Var var) {
            var = AliasVar.resolveAliasVar(var);
            if (var instanceof NodeVar) {
                NodeVar nodeVar = (NodeVar) var;
                List<ImplementationNode> nodes = gtop.getImplementationNodesByType(nodeVar.labels.get(0));
                // FIXME: We should make sure that we always have *exactly* one node returned here.
                return nodes.get(0).getId().get(0).getColumnName();
            }
            throw new RuntimeException(
                    "Attempt to access the ID column on a variable" +
                    "that is not a Node."
            );
        }
    }


    /**
     * Transform all uses of length(p) into the length of the path.
     *
     * @param tree is the tree that where expression will be changed.
     * @throws Cypher2SqlException is thrown if an error is encountered
     */
    public static void convertPathLength(ScopeSelect tree) throws Cypher2SqlException {
        Walk.walkSQLNode(new PathLengthVisitor(), tree);
    }

    private static class PathLengthVisitor extends Walk.BaseSQLNodeVisitor {

        @Override
        public void visitSimpleSelect(SimpleSelect simpleSelect) throws Cypher2SqlException {
            PathLengthFolder folder = new PathLengthFolder();
            simpleSelect.exportedItems = simpleSelect.exportedItems.stream()
                    .<Expr>map(rethrowFunction(e -> ExprWalk.<Expr, Cypher2SqlException>fold(folder, e)))
                    .collect(Collectors.toList());

            simpleSelect.whereCondition = ExprWalk.fold(folder, simpleSelect.whereCondition);
        }
    }

    private static class PathLengthFolder extends ExprWalk.IdentityFolder<Cypher2SqlException> {

        private static final String LENGTH = "length";

        @Override
        public Expr foldFn(ExprFn expr) throws Cypher2SqlException {
            if (expr.cypherName.equalsIgnoreCase(LENGTH)) {
                if (expr.args.size() == 1) {
                    Expr arg = expr.args.get(0);
                    if (arg instanceof ExprVar && ((ExprVar) arg).var.type() instanceof PathType) {
                        PathVar pathVar = (PathVar) AliasVar.resolveAliasVar(((ExprVar) arg).var);
                        return new ConstVal.LongVal(pathVar.length);
                    }
                }
            }
            return expr;
        }


    }
}
