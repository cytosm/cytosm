package org.cytosm.cypher2sql.lowering.typeck.expr;

import org.cytosm.cypher2sql.lowering.rendering.RenderingContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Compound nodes
 */
public class ExprTree {

    private ExprTree() {}

    public static abstract class LhsRhs implements Expr {
        public Expr lhs;
        public Expr rhs;

        LhsRhs(Expr lhs, Expr rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        String toSQLStringInfix(final String operator, final RenderingContext helper) {
            return "(" + this.lhs.toSQLString(helper) + " " + operator + " " + this.rhs.toSQLString(helper) + ")";
        }

        String toSQLStringFunc(final String funcName, final RenderingContext helper) {
            return " " + funcName + "(" + this.lhs.toSQLString(helper) + ", " + this.rhs.toSQLString(helper) + ") ";
        }
    }

    public static class Add extends LhsRhs { Add(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringInfix("+", helper); } }
    public static class Sub extends LhsRhs { Sub(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringInfix("-", helper); } }
    public static class Mod extends LhsRhs { Mod(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringInfix("%", helper); } }
    public static class Mul extends LhsRhs { Mul(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringInfix("*", helper); } }
    public static class Div extends LhsRhs { Div(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringInfix("/", helper); } }
    public static class Pow extends LhsRhs { Pow(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringFunc("POWER", helper); } }
    public static class Neq extends LhsRhs { Neq(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringInfix("<>", helper); } }
    public static class Eq extends LhsRhs { public Eq(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringInfix("=", helper); } }
    public static class And extends LhsRhs { public And(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringInfix("AND", helper); } }
    public static class Or extends LhsRhs { Or(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringInfix("OR", helper); } }
    public static class In extends LhsRhs { In(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringInfix("IN", helper); } }
    public static class Xor extends LhsRhs { Xor(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringInfix("XOR", helper); } }
    public static class LessThan extends LhsRhs { LessThan(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringInfix("<", helper); } }
    public static class LessThanOrEqual extends LhsRhs { LessThanOrEqual(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringInfix("<=", helper); } }
    public static class GreaterThan extends LhsRhs { GreaterThan(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringInfix(">", helper); } }
    public static class GreaterThanOrEqueal extends LhsRhs { GreaterThanOrEqueal(Expr lhs, Expr rhs) { super(lhs, rhs); } public String toSQLString(RenderingContext helper) { return toSQLStringInfix(">=", helper); } }

    public static abstract class Unary implements Expr {
        public Expr unary;

        Unary(Expr unary) {
            this.unary = unary;
        }

        String toSQLStringFunc(final String funcName, final RenderingContext helper) {
            return " " + funcName + "(" + this.unary.toSQLString(helper) + ") ";
        }

        String toSQLStringPostFix(final String funcName, final RenderingContext helper) {
            return this.unary.toSQLString(helper) + " " + funcName + " ";
        }
    }

    public static class Not extends Unary { Not(Expr unary) { super(unary); } public String toSQLString(RenderingContext helper) { return toSQLStringFunc("NOT", helper); } }
    public static class UnaryAdd extends Unary { UnaryAdd(Expr unary) { super(unary); } public String toSQLString(RenderingContext helper) { return toSQLStringFunc("+", helper); } }
    public static class IsNull extends Unary { IsNull(Expr unary) { super(unary); } public String toSQLString(RenderingContext helper) { return toSQLStringPostFix("IS NULL", helper); } }
    public static class IsNotNull extends Unary { IsNotNull(Expr unary) { super(unary); } public String toSQLString(RenderingContext helper) { return toSQLStringPostFix("IS NOT NULL", helper); } }

    public static class PropertyAccess implements Expr {
        public String propertyAccessed;
        public Expr expression;

        public PropertyAccess(final String propertyAccessed, final Expr expression) {
            this.expression = expression;
            this.propertyAccessed = propertyAccessed;
        }

        public String toSQLString(final RenderingContext helper) {
            if (expression instanceof ExprVar) {
                return helper.renderVariableForUse((ExprVar) expression, propertyAccessed);
            } else if (expression instanceof MapExpr) {
                return ((MapExpr) expression).props.get(propertyAccessed).toSQLString(helper);
            } else {
                throw new RuntimeException(
                        "Don't know how to lookup '" + propertyAccessed +
                        "' on type " + expression.getClass()
                );
            }
        }
    }

    public static class ListExpr implements Expr {

        public List<Expr> exprs = new ArrayList<>();

        @Override
        public String toSQLString(RenderingContext ctx) {
            return "(" + exprs.stream().map(e -> e.toSQLString(ctx))
                    .collect(Collectors.joining(",")) + ")";
        }
    }

    public static class MapExpr implements Expr {
        public Map<String, Expr> props = new HashMap<>();

        public String toSQLString(RenderingContext helper) {
            // TODO: For now we don't support rendering MapExpression.
            // TODO: We should support a limited form of rendering though.
            throw new RuntimeException("MapExpression should be unwrapped.");
        }
    }

    public static class CaseExpr implements Expr {
        /**
         * CASE <expr>
         * if null then this is just CASE.
         */
        public Expr caseExpr = null;

        /**
         * ELSE <defaultExpr>
         * if null then the ELSE is dropped entirely.
         */
        public Expr defaultExpr = null;

        public List<Pair<Expr, Expr>> whenExprs = new ArrayList<>();

        @Override
        public String toSQLString(RenderingContext helper) {
            String result = "CASE ";
            if (this.caseExpr != null) {
                result += this.caseExpr.toSQLString(helper) + " ";
            }
            for (Pair<Expr, Expr> when: whenExprs) {
                result += "WHEN " + when.getKey().toSQLString(helper) +
                        " THEN " + when.getValue().toSQLString(helper) + " ";
            }
            if (this.defaultExpr != null) {
                result += "ELSE " + this.defaultExpr.toSQLString(helper);
            }
            return result;
        }
    }

    /**
     * This class represents Alias that can't be referred as Var.
     * This shows up particularly within a RETURN statement where
     * all expression are wrapped withing an AliasExpr.
     */
    public static class AliasExpr implements Expr {

        /**
         * The alias.
         */
        public String alias;

        /**
         * The aliased expression.
         */
        public Expr expr;

        public AliasExpr(Expr expr, String alias) {
            this.expr = expr;
            this.alias = alias;
        }

        @Override
        public String toSQLString(RenderingContext helper) {
            return this.expr.toSQLString(helper) + " AS " + helper.renderEscapedColumnName(alias);
        }
    }
}
