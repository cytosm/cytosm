package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;
import org.cytosm.cypher2sql.lowering.sqltree.ScopeSelect;
import org.cytosm.cypher2sql.lowering.sqltree.visitor.Walk;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTree;

import java.util.stream.Collectors;

import static org.cytosm.cypher2sql.lowering.exceptions.fns.LambdaExceptionUtil.rethrowConsumer;

/**
 * This pass unwrap alias expr on nested ScopeSelect
 * to avoid having broken exports. This is required because
 * AliasExpr are only expected to be kept on the outer most
 * ScopeSelect.
 *
 */
public class UnwrapAliasExpr {

    /**
     * Visit the tree for nested ScopeSelect and unwrap AliasExpr
     * encountered in ReturnExprs.
     * @param sqltree is the SQL tree.
     */
    public static void visitAndUnwrap(ScopeSelect sqltree) throws Cypher2SqlException {
        sqltree.withQueries.forEach(rethrowConsumer(wq -> Walk.walkSQLNode(new Visitor(), wq)));
    }

    private static class Visitor extends Walk.BaseSQLNodeVisitor {

        @Override
        public void visitScopeSelect(ScopeSelect scopeSelect) throws Cypher2SqlException {
            scopeSelect.ret.exportedItems = scopeSelect.ret.exportedItems.stream().map(e -> {
                if (e instanceof ExprTree.AliasExpr) {
                    return ((ExprTree.AliasExpr) e).expr;
                } else {
                    return e;
                }
            }).collect(Collectors.toList());
        }
    }
}
