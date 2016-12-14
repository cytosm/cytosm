package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.lowering.exceptions.BugFound;
import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelect;
import org.cytosm.cypher2sql.lowering.sqltree.ScopeSelect;
import org.cytosm.cypher2sql.lowering.sqltree.from.FromItem;
import org.cytosm.cypher2sql.lowering.sqltree.visitor.Walk;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprVar;
import org.cytosm.cypher2sql.lowering.typeck.types.VarType;
import org.cytosm.cypher2sql.lowering.typeck.var.AliasVar;
import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTree;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprWalk;

import java.util.List;
import java.util.Optional;

/**
 * This pass unwrap property accesses where possible.
 *
 */
public class UnwrapPropertyAccess {

    /**
     * Unwrap the property accesses where possible.
     *
     * This turn things such as:
     *
     *      {b: {c: d}}.b.c
     *
     * into:
     *
     *      d
     *
     * Similar case are handled when the variable is hidden behind
     * an AliasVar.
     *
     * In case where a property access is made on an {@link AliasVar} of
     * type {@link VarType}, we mutate the FromItems affected.
     *
     * @param sqltree is the sql tree to mutate.
     */
    public static void unwrapPropertyAccess(ScopeSelect sqltree) throws Cypher2SqlException {
        Walk.walkSQLNode(new Unwrapper(), sqltree);
    }


    private static class Unwrapper extends Walk.BaseVisitorAndExprFolder {

        @Override
        protected ExprWalk.IdentityFolder<Cypher2SqlException> makeExprFolder(SimpleSelect context) {
            return new ExprFolder(context.dependencies());
        }
    }

    private static class ExprFolder extends ExprWalk.IdentityFolder<Cypher2SqlException> {

        private final List<FromItem> fromItemsContext;

        ExprFolder(final List<FromItem> fromItemsContext) {
            this.fromItemsContext = fromItemsContext;
        }

        private void addVarToFromItemProvidingAlias(AliasVar aliasVar, Var var) throws Cypher2SqlException {
            Optional<FromItem> fromItem = this.fromItemsContext.stream()
                    .filter(x -> x.variables.stream().anyMatch(v -> v == aliasVar))
                    .findAny();
            if (fromItem.isPresent()) {
                fromItem.get().variables.add(var);
            } else {
                throw new BugFound("AliasVar '" + aliasVar.name + "' is not exported by any FromItem");
            }
        }

        @Override
        public Expr foldPropertyAccess(ExprTree.PropertyAccess expr) throws Cypher2SqlException {

            if (expr.expression instanceof ExprTree.MapExpr) {
                return ((ExprTree.MapExpr) expr.expression).props.get(expr.propertyAccessed);

            } else if (expr.expression instanceof ExprVar) {
                ExprVar exprVar = (ExprVar) expr.expression;

                if (exprVar.var instanceof AliasVar) {

                    AliasVar aliasVar = (AliasVar) exprVar.var;

                    if (aliasVar.type() instanceof VarType) {
                        return new ExprTree.PropertyAccess(expr.propertyAccessed, exprVar);
                    }
                    Expr reduced = ExprWalk.fold(this, aliasVar.aliased);
                    return foldPropertyAccess(new ExprTree.PropertyAccess(expr.propertyAccessed, reduced));
                }

            } else if (expr.expression instanceof ExprTree.PropertyAccess) {
                Expr reduced = foldPropertyAccess((ExprTree.PropertyAccess) expr.expression);
                if (reduced instanceof ExprTree.MapExpr) {
                    return ((ExprTree.MapExpr) reduced).props.get(expr.propertyAccessed);
                } else {
                    return new ExprTree.PropertyAccess(expr.propertyAccessed, reduced);
                }

            }
            return super.foldPropertyAccess(expr);
        }
    }
}
