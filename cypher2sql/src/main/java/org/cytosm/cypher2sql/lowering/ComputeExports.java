package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;
import org.cytosm.cypher2sql.lowering.sqltree.*;
import org.cytosm.cypher2sql.lowering.sqltree.join.BaseJoin;
import org.cytosm.cypher2sql.lowering.sqltree.visitor.Walk;
import org.cytosm.cypher2sql.lowering.typeck.VarDependencies;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprVar;
import org.cytosm.cypher2sql.lowering.typeck.var.AliasVar;
import org.cytosm.cypher2sql.lowering.typeck.var.NodeVar;
import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTree;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprWalk;

import static org.cytosm.cypher2sql.lowering.exceptions.fns.LambdaExceptionUtil.rethrowConsumer;

import java.util.stream.Collectors;

/**
 * This pass compute exports for each SELECT.
 */
public class ComputeExports {

    /**
     * Compute the exported properties for each SELECT.
     * @param sqltree is the root of the SQL tree.
     * @param vars is the data structure tracking var dependencies.
     */
    public static void computeExports(ScopeSelect sqltree, VarDependencies vars) throws Cypher2SqlException {
        Walk.walkSQLNode(new MutateExportsInSelects(vars), sqltree);
    }

    /**
     * Mark the property use found as required for exports.
     * This is a sub-pass of ComputeExports. This pass requires
     * that Properties have been unwrapped first to work correctly.
     *
     * @param sqltree is the SQL tree.
     */
    public static void markPropertiesAsUsedWhenEncountered(ScopeSelect sqltree) throws Cypher2SqlException {
        Walk.walkSQLNode(new MarkPropertyUsed(), sqltree);
    }


    private static class MarkPropertyUsed extends Walk.BaseSQLNodeVisitor {

        @Override
        public void visitSimpleSelect(SimpleSelect simpleSelect) throws Cypher2SqlException {

            // Collect requirements on where clause:
            if (simpleSelect.whereCondition != null) {
                ExprWalk.walk(new PropertyUseCollector(), simpleSelect.whereCondition);
            }

            // Collect requirements on JOINS:
            for (BaseJoin dep: simpleSelect.joinList()) {
                if (dep.condition != null) {
                    ExprWalk.walk(new PropertyUseCollector(), dep.condition);
                }
            }

            // Collect requirements on ORDER BY
            for (SimpleSelect.OrderItem oi: simpleSelect.orderBy) {
                ExprWalk.walk(new PropertyUseCollector(), oi.item);
            }

            // Mark exported items as required.
            for (Expr expr: simpleSelect.exportedItems) {
                ExprWalk.walk(new PropertyUseCollector(), expr);
            }
        }
    }

    private static class MutateExportsInSelects extends Walk.BaseSQLNodeVisitor {
        private final VarDependencies vars;

        MutateExportsInSelects(final VarDependencies vars) {
            this.vars = vars;
        }

        @Override
        public void visitSimpleSelect(SimpleSelect simpleSelect) throws Cypher2SqlException {
            simpleSelect.exportedItems = vars.getUsedAndIndirectUsedVars(simpleSelect.varId)
                    .stream().map(ExprVar::new).collect(Collectors.toList());
        }

        @Override
        public void visitScopeSelect(ScopeSelect scopeSelect) throws Cypher2SqlException {
            scopeSelect.withQueries.forEach(rethrowConsumer(this::visitWithSelect));

            scopeSelect.ret.exportedItems = vars.getReturnExprs();
        }
    }

    /**
     * Mutate the variable encountered
     * to add mark properties as used.
     */
    private static class PropertyUseCollector extends ExprWalk.BaseVisitor {

        @Override
        public void visitPropertyAccess(ExprTree.PropertyAccess expr) {
            if (expr.expression instanceof ExprVar) {
                ExprVar exprVar = (ExprVar) expr.expression;
                if (exprVar.var instanceof NodeVar) {
                    NodeVar node = (NodeVar) exprVar.var;
                    node.propertiesRequired.add(expr.propertyAccessed);
                    return;
                } else if (exprVar.var instanceof AliasVar) {
                    AliasVar alias = (AliasVar) exprVar.var;
                    visitPropertyAccess(new ExprTree.PropertyAccess(expr.propertyAccessed, alias.aliased));
                    return;
                }
            }
            super.visitPropertyAccess(expr);
        }
    }
}
