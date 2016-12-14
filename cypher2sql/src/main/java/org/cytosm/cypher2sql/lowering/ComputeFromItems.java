package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelect;
import org.cytosm.cypher2sql.lowering.sqltree.ScopeSelect;
import org.cytosm.cypher2sql.lowering.sqltree.WithSelect;
import org.cytosm.cypher2sql.lowering.sqltree.from.FromItem;
import org.cytosm.cypher2sql.lowering.sqltree.visitor.Walk;
import org.cytosm.cypher2sql.lowering.typeck.VarDependencies;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprVar;
import org.cytosm.cypher2sql.lowering.typeck.var.AliasVar;
import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;
import org.cytosm.cypher2sql.lowering.typeck.var.NodeOrRelVar;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprWalk;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class compute all FromItem. For more details look
 * at the documentation of the `computeFromItems` function
 * below.
 *
 */
public class ComputeFromItems {

    /**
     * Compute all FROM items. This pass put directly all
     * the computed FromItem on the SimpleSelect themselves.
     *
     * In order to get the JOIN you need to have a look at
     * {@link PopulateJoins} which moves the appropriates
     * {@link FromItem}s from the "fromItem" field to the respective
     * "joins" field of {@link org.cytosm.cypher2sql.lowering.sqltree.SimpleSelectWithLeftJoins}
     * and {@link org.cytosm.cypher2sql.lowering.sqltree.SimpleSelectWithInnerJoins}.
     *
     * This pass assumes that we can't have any nested ScopeSelect or UnionSelect
     * at this point.
     *
     * @param sqltree is the root of the SQL tree.
     * @param vars is the var dependencies analysis ran previously.
     */
    public static void computeFromItems(ScopeSelect sqltree, VarDependencies vars) throws Cypher2SqlException {
        SQLTreeVisitor visitor = new SQLTreeVisitor(vars);
        Walk.walkSQLNode(visitor, sqltree);
    }

    private static class SQLTreeVisitor extends Walk.BaseSQLNodeVisitor {

        final VarDependencies vars;

        /**
         * Map variable unique names to WithSelect
         * that modify/defines them.
         */
        final Map<String, WithSelect> whereToGetTheVar = new HashMap<>();

        SQLTreeVisitor(final VarDependencies vars) {
            this.vars = vars;
        }

        @Override
        public void visitWithSelect(WithSelect withSelect) throws Cypher2SqlException {
            // We can't have any UnionSelect or ScopeSelect at this point.
            // So this cast won't fail.
            SimpleSelect simpleSelect = (SimpleSelect) withSelect.subquery;
            List<Var> usedVars = vars.getUsedAndIndirectUsedVars(simpleSelect.varId);
            List<FromItem> fromItems = new ArrayList<>();

            // Iterate over used variables to compute where they might come from.
            // In this loop we only check if they come from another WithSelect.
            usedVars.forEach(var -> {
                FromItem fromItem = new FromItem();
                fromItem.variables.add(var);

                // The var will be fetched from another SELECT.
                if (whereToGetTheVar.containsKey(var.uniqueName)) {
                    WithSelect source = whereToGetTheVar.get(var.uniqueName);
                    fromItem.source = source;
                    Optional<FromItem> existingFromItem = fromItems.stream()
                            .filter(x -> x.source == source)
                            .findFirst();
                    if (existingFromItem.isPresent()) {
                        existingFromItem.get().variables.add(var);
                    } else {
                        fromItems.add(fromItem);
                    }
                }
            });

            // A variable might come from different place if it is an AliasVar.
            // For this reason we process them separately.
            usedVars.stream()
                    .filter(x -> x instanceof NodeOrRelVar && !whereToGetTheVar.containsKey(x.uniqueName))
                    .forEach(var -> {

                FromItem fromItem = new FromItem();
                fromItem.variables.add(var);
                // The var will be fetched from the table source it comes from.
                fromItems.add(fromItem);
            });

            // Now time to process AliasVar. An AliasVar might be an aggregate
            // of multiple vars for instance:
            //
            //      MATCH (a)
            //      MATCH (b)
            //      WITH {e: a, f: b} AS d
            //      ...
            //
            // In the above example, `d` is an aggregate of multiple Vars. As such,
            // when encountering d for the firstTime we will fetch d from the two
            // `WithSelect` it comes from.
            // Later on, we will only fetch it from where it comes from.
            //
            usedVars.stream()
                    .filter(x -> x instanceof AliasVar && !whereToGetTheVar.containsKey(x.uniqueName))
                    .map(v -> (AliasVar) v)
                    .forEach(aliasVar -> {

                // Visit the expression and collect information about all encountered
                // variable. The FromItem they come from will
                Expr expr = aliasVar.aliased;
                ExprWalk.BaseVisitor visitor = new ExprWalk.BaseVisitor() {
                    @Override
                    public void visitVariable(ExprVar expr) {

                        // Search for the FromItem that provide the variable var
                        FromItem fromItem = new FromItem();
                        fromItem.variables.add(expr.var);

                        WithSelect source = whereToGetTheVar.get(expr.var.uniqueName);
                        fromItem.source = source;
                        Optional<FromItem> existingFromItem = fromItems.stream()
                                .filter(x -> x.source == source)
                                .findFirst();
                        if (existingFromItem.isPresent()) {
                            existingFromItem.get().variables.add(expr.var);
                        } else {
                            fromItems.add(fromItem);
                        }
                    }
                };

                ExprWalk.walk(visitor, expr);
            });

            // Last step: Mark all variable as coming from
            // this WithSelect now:
            usedVars.forEach(var -> whereToGetTheVar.put(var.uniqueName, withSelect));

            // All FromItems are added to the Select fromItem.
            // Note: Some of them will probably be moved later
            //       by the PopulateJoins pass.
            simpleSelect.fromItem.addAll(fromItems);
        }

        @Override
        public void visitScopeSelect(ScopeSelect scopeSelect) throws Cypher2SqlException {
            super.visitScopeSelect(scopeSelect);
            // This algorithm is fairly simple.
            // We collect all the "root" for each dependency subgraph
            // in the from item of the With.

            // This first pass collect all reachable SimpleSelect.
            List<Pair<WithSelect, Boolean>> list = scopeSelect.withQueries
                    .stream().map(x -> new MutablePair<>(x, false))
                    .collect(Collectors.toList());

            // Mark all reachable dependent.
            for (Pair<WithSelect, Boolean> el: list) {
                // Mark all the dependent of el as reached.
                SimpleSelect s = (SimpleSelect) el.getKey().subquery;
                for (FromItem fi: s.fromItem) {
                    Optional<Pair<WithSelect, Boolean>> dependent = list.stream()
                            .filter(x -> x.getKey() == fi.source).findFirst();

                    dependent.ifPresent(d -> d.setValue(true));
                }
            }

            // Collect only the roots
            list.stream().filter(x -> !x.getValue()).forEach(x -> {

                // The FromItem for this select
                // can only provide information about the variable
                // that it is using.
                // (Does this contains indirect variable use?)
                // If not -> then there is a bug in VarDependencies.
                FromItem fromItem = new FromItem();
                fromItem.source = x.getKey();
                fromItem.variables = vars.getUsedAndIndirectUsedVars(x.getKey().varId).stream()
                        .collect(Collectors.toList());
                scopeSelect.ret.fromItem.add(fromItem);
            });
        }
    }
}
