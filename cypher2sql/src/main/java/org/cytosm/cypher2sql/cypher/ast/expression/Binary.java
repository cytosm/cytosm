package org.cytosm.cypher2sql.cypher.ast.expression;

/**
 */
public abstract class Binary extends Unary {

    public Expression rhs;

    Binary(Expression lhs, Expression rhs) {
        super(lhs);
        this.rhs = rhs;
    }

    public static class GreaterThan extends Binary {

        public GreaterThan(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class LessThan extends Binary {

        public LessThan(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class Equals extends Binary {

        public Equals(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class NotEquals extends Binary {

        public NotEquals(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class GreaterThanOrEqual extends Binary {

        public GreaterThanOrEqual(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class LessThanOrEqual extends Binary {

        public LessThanOrEqual(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class InvalidNotEquals extends Binary {

        public InvalidNotEquals(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class RegexMatch extends Binary {

        public RegexMatch(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class And extends Binary {

        public And(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class Or extends Binary {

        public Or(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class Xor extends Binary {

        public Xor(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class StartsWith extends Binary {

        public StartsWith(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class EndsWith extends Binary {

        public EndsWith(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class In extends Binary {

        public In(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class Subtract extends Binary {

        public Subtract(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class Add extends Binary {

        public Add(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class Divide extends Binary {

        public Divide(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class Multiply extends Binary {

        public Multiply(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class Modulo extends Binary {

        public Modulo(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class Pow extends Binary {

        public Pow(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
    public static class Contains extends Binary {

        public Contains(Expression lhs, Expression rhs) {
            super(lhs, rhs);
        }
    }
}
