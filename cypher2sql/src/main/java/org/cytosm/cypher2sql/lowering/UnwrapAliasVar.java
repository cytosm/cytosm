package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;
import org.cytosm.cypher2sql.lowering.sqltree.ScopeSelect;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelect;
import org.cytosm.cypher2sql.lowering.sqltree.visitor.Walk;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprVar;
import org.cytosm.cypher2sql.lowering.typeck.types.BoolType;
import org.cytosm.cypher2sql.lowering.typeck.types.NumberType;
import org.cytosm.cypher2sql.lowering.typeck.types.StringType;
import org.cytosm.cypher2sql.lowering.typeck.var.AliasVar;
import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;
import org.cytosm.cypher2sql.lowering.typeck.constexpr.ConstExprFolder;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprWalk;


/**
 * This pass unwrap `AliasVar`s when they refers
 * to a NodeVar, RelVar or PathVar.
 *
 * The following is invalid in Cypher:
 *
 *      MATCH (a)
 *      WITH {a: {b: a}} AS a
 *      MATCH (a.b.a)--(b)
 *      RETURN a, b
 *
 * However the following is not:
 *
 *      MATCH (a)
 *      WITH a AS b
 *      MATCH (b)--(c)
 *      RETURN b, c
 *
 * This pass is dealing with the second
 * case.
 *
 */
public class UnwrapAliasVar {

    /**
     * Unwrap AliasVar that map to constants such as String or Number.
     * It does that by evaluating them using {@link ConstExprFolder}
     * @param tree is the SQL tree to unwrap.
     */
    public static void unwrapConstants(ScopeSelect tree) throws Cypher2SqlException {
        Walk.walkSQLNode(new UnwrapConstantsVisitor(), tree);
    }

    private static class UnwrapConstantsVisitor extends Walk.BaseVisitorAndExprFolder {

        @Override
        protected ExprWalk.IdentityFolder<Cypher2SqlException> makeExprFolder(SimpleSelect context) {
            return new UnwrapConstantsFolder();
        }
    }

    private static class UnwrapConstantsFolder extends ExprWalk.IdentityFolder<Cypher2SqlException> {

        private ConstExprFolder folder = new ConstExprFolder();

        @Override
        public Expr foldVariable(ExprVar expr) throws Cypher2SqlException {
            if (expr.var instanceof AliasVar) {
                AliasVar var = (AliasVar) expr.var;
                if (var.type() instanceof NumberType ||
                        var.type() instanceof StringType ||
                        var.type() instanceof BoolType) {
                    try {
                        return ExprWalk.fold(folder, var.aliased);
                    } catch (Cypher2SqlException e) {}
                }
            }
            return super.foldVariable(expr);
        }
    }
}
