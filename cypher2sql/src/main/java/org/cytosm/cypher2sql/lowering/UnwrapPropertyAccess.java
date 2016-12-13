package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelect;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelectWithInnerJoins;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelectWithLeftJoins;
import org.cytosm.cypher2sql.lowering.sqltree.ScopeSelect;
import org.cytosm.cypher2sql.lowering.sqltree.visitor.Walk;
import org.cytosm.cypher2sql.lowering.typeck.types.VarType;
import org.cytosm.cypher2sql.lowering.typeck.var.AliasVar;
import org.cytosm.cypher2sql.lowering.typeck.var.Expr;
import org.cytosm.cypher2sql.lowering.typeck.var.expr.ExprTree;
import org.cytosm.cypher2sql.lowering.typeck.var.expr.ExprWalk;

import java.util.ArrayList;
import java.util.List;

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
     * @param sqltree
     */
    public static void unwrapPropertyAccess(ScopeSelect sqltree) throws Cypher2SqlException {
        Walk.walkSQLNode(new Unwrapper(), sqltree);
    }


    private static class Unwrapper extends Walk.BaseSQLNodeVisitor {

        @Override
        public void visitSimpleSelect(SimpleSelect simpleSelect) {
            ExprFolder solver = new ExprFolder();

            if (simpleSelect.whereCondition != null) {
                try {
                    simpleSelect.whereCondition = ExprWalk.fold(solver, simpleSelect.whereCondition);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            List<Expr> newValues = new ArrayList<>();

            for (Expr expr: simpleSelect.exportedItems) {
                try {
                    newValues.add(ExprWalk.fold(solver, expr));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            simpleSelect.exportedItems = newValues;

            if (simpleSelect instanceof SimpleSelectWithInnerJoins) {
                ((SimpleSelectWithInnerJoins) simpleSelect).joins
                        .forEach(join -> {
                            try {
                                join.condition = ExprWalk.fold(solver, join.condition);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
            } else {
                ((SimpleSelectWithLeftJoins) simpleSelect).joins
                        .forEach(join -> {
                            try {
                                join.condition = ExprWalk.fold(solver, join.condition);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
            }
        }
    }

    private static class ExprFolder extends ExprWalk.IdentityFolder<Exception> {

        @Override
        public Expr foldPropertyAccess(ExprTree.PropertyAccess expr) throws Exception {

            if (expr.expression instanceof ExprTree.MapExpr) {
                return ((ExprTree.MapExpr) expr.expression).props.get(expr.propertyAccessed);

            } else if (expr.expression instanceof AliasVar) {
                if (((AliasVar) expr.expression).type() instanceof VarType) {
                    return expr;
                } else {
                    Expr reduced = ExprWalk.fold(this, ((AliasVar) expr.expression).aliased);
                    return foldPropertyAccess(new ExprTree.PropertyAccess(expr.propertyAccessed, reduced));
                }

            } else if (expr.expression instanceof ExprTree.PropertyAccess) {
                Expr reduced = foldPropertyAccess((ExprTree.PropertyAccess) expr.expression);
                if (reduced instanceof ExprTree.MapExpr) {
                    return ((ExprTree.MapExpr) reduced).props.get(expr.propertyAccessed);
                } else {
                    return new ExprTree.PropertyAccess(expr.propertyAccessed, reduced);
                }

            } else {
                return super.foldPropertyAccess(expr);
            }
        }
    }
}
