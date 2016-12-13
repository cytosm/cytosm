package org.cytosm.cypher2sql.cypher.ast.expression;


/**
 */
public abstract class Unary extends Expression {

    public Expression lhs;

    Unary(final Expression unary) {
        this.lhs = unary;
    }

    public static class Not extends Unary {
        public Not(Expression unary) {
            super(unary);
        }
    }
    public static class Add extends Unary {
        public Add(Expression unary) {
            super(unary);
        }
    }
    public static class Subtract extends Unary {
        public Subtract(Expression unary) {
            super(unary);
        }
    }
    public static class IsNull extends Unary {
        public IsNull(Expression unary) {
            super(unary);
        }
    }
    public static class IsNotNull extends Unary {
        public IsNotNull(Expression unary) {
            super(unary);
        }
    }
}
