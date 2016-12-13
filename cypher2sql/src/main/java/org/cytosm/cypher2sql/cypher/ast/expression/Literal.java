package org.cytosm.cypher2sql.cypher.ast.expression;

/**
 */
public abstract class Literal extends Expression {

    public String stringValue;

    Literal(final String stringValue) {
        this.stringValue = stringValue;
    }

    public static class Number extends Literal {

        Number(final String stringValue) {
            super(stringValue);
        }
    }

    public static class Integer extends Number {

        public long value;

        Integer(final String stringValue) {
            super(stringValue);
        }
    }

    public static class DecimalDouble extends Number {
        public double value;

        public DecimalDouble(final String stringValue) {
            super(stringValue);
            this.value = Double.parseDouble(stringValue);
        }
    }

    public static class HexInteger extends Integer {

        public HexInteger(final String stringValue) {
            super(stringValue);
            this.value = Long.parseLong(stringValue, 16);
        }
    }

    public static class UnsignedInteger extends Integer {

        public UnsignedInteger(final String stringValue) {
            super(stringValue);
            this.value = Long.parseLong(stringValue);
        }
    }

    public static class OctalInteger extends Integer {

        public OctalInteger(final String stringValue) {
            super(stringValue);
            this.value = Long.parseLong(stringValue, 8);
        }
    }

    public static class StringLiteral extends Literal {
        public String value;

        public StringLiteral(final String stringValue) {
            super(stringValue);
            this.value = stringValue.substring(1, stringValue.length() - 1);
        }
    }

    public static class Null extends Literal {

        public Null() {
            super("NULL");
        }
    }

    public static class True extends Literal {

        public True() {
            super("TRUE");
        }
    }

    public static class False extends Literal {

        public False() {
            super("FALSE");
        }
    }
}