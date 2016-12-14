package org.cytosm.cypher2sql.lowering.typeck.constexpr;

import org.cytosm.cypher2sql.lowering.rendering.RenderingContext;
import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;

/**
 * Const values.
 */
public class ConstVal {

    private ConstVal() {}

    public static abstract class Literal implements Expr {
        // Arithmetic operations
        public Literal add(Literal other) throws ConstExprException {
            throw new UnknownOperation("Unkown add operation on '"+ this.className() + "'.");
        }
        public Literal sub(Literal other) throws ConstExprException {
            throw new UnknownOperation("Unkown add operation on '"+ this.className() + "'.");
        }
        public Literal mul(Literal other) throws ConstExprException {
            throw new UnknownOperation("Unkown add operation on '"+ this.className() + "'.");
        }
        public Literal mod(Literal other) throws ConstExprException {
            throw new UnknownOperation("Unkown add operation on '"+ this.className() + "'.");
        }
        public Literal div(Literal other) throws ConstExprException {
            throw new UnknownOperation("Unkown add operation on '"+ this.className() + "'.");
        }
        public Literal pow(Literal other) throws ConstExprException {
            throw new UnknownOperation("Unkown add operation on '"+ this.className() + "'.");
        }
        // Boolean operations
        public Literal and(Literal other) throws ConstExprException {
            throw new UnknownOperation("Unkown add operation on '"+ this.className() + "'.");
        }
        public Literal or(Literal other) throws ConstExprException {
            throw new UnknownOperation("Unkown add operation on '"+ this.className() + "'.");
        }
        public Literal xor(Literal other) throws ConstExprException {
            throw new UnknownOperation("Unkown add operation on '"+ this.className() + "'.");
        }
        public Literal gt(Literal other) throws ConstExprException {
            throw new UnknownOperation("Unkown add operation on '"+ this.className() + "'.");
        }
        public Literal lt(Literal other) throws ConstExprException {
            throw new UnknownOperation("Unkown add operation on '"+ this.className() + "'.");
        }
        public Literal lteq(Literal other) throws ConstExprException {
            throw new UnknownOperation("Unkown add operation on '"+ this.className() + "'.");
        }
        public Literal gteq(Literal other) throws ConstExprException {
            throw new UnknownOperation("Unkown add operation on '"+ this.className() + "'.");
        }
        public Literal eq(Literal other) throws ConstExprException {
            throw new UnknownOperation("Unkown add operation on '"+ this.className() + "'.");
        }
        public Literal neq(Literal other) throws ConstExprException {
            throw new UnknownOperation("Unkown add operation on '"+ this.className() + "'.");
        }


        public abstract String asStr() throws ConstExprException;
        public abstract long asLong() throws ConstExprException;
        public abstract double asDouble() throws ConstExprException;
        public abstract boolean asBool() throws ConstExprException;
        abstract String className();
    }

    public static class StrVal extends Literal {
        public String value;

        public StrVal(final String value) {
            this.value = value;
        }

        @Override
        public String toSQLString(RenderingContext helper) {
            return helper.renderStringLiteral(value);
        }

        @Override
        public String className() {
            return "StrVal";
        }

        @Override
        public boolean asBool() throws ConstExprException {
            throw new ConversionException("string", "bool");
        }

        @Override
        public String asStr() throws ConstExprException {
            return this.value;
        }

        @Override
        public long asLong() throws ConstExprException {
            throw new ConversionException("string", "long");
        }

        @Override
        public double asDouble() throws ConstExprException {
            throw new ConversionException("string", "double");
        }

        @Override
        public Literal add(Literal other) throws ConstExprException {
            return new StrVal(this.value + other.asStr());
        }

    }

    public static class DoubleVal extends Literal {
        public double value;

        public DoubleVal(final double value) {
            this.value = value;
        }

        @Override
        public String toSQLString(RenderingContext _ignored) {
            return String.valueOf(value);
        }

        @Override
        public String className() {
            return "DoubleVal";
        }

        @Override
        public boolean asBool() throws ConstExprException {
            throw new ConversionException("double", "bool");
        }

        @Override
        public String asStr() throws ConstExprException {
            return this.value + "";
        }

        @Override
        public long asLong() throws ConstExprException {
            return (long) this.value;
        }

        @Override
        public double asDouble() throws ConstExprException {
            return this.value;
        }

        @Override
        public Literal add(Literal other) throws ConstExprException {
            return new DoubleVal(this.value + other.asDouble());
        }

        @Override
        public Literal sub(Literal other) throws ConstExprException {
            return new DoubleVal(this.value - other.asDouble());
        }

        @Override
        public Literal mul(Literal other) throws ConstExprException {
            return new DoubleVal(this.value * other.asDouble());
        }

        @Override
        public Literal mod(Literal other) throws ConstExprException {
            return new DoubleVal(this.value % other.asDouble());
        }

        @Override
        public Literal div(Literal other) throws ConstExprException {
            return new DoubleVal(this.value / other.asDouble());
        }

        @Override
        public Literal pow(Literal other) throws ConstExprException {
            return new DoubleVal(Math.pow(this.value, other.asDouble()));
        }

        @Override
        public Literal gt(Literal other) throws ConstExprException {
            return new BoolVal(this.value > other.asDouble());
        }

        @Override
        public Literal lt(Literal other) throws ConstExprException {
            return new BoolVal(this.value < other.asDouble());
        }

        @Override
        public Literal lteq(Literal other) throws ConstExprException {
            return new BoolVal(this.value <= other.asDouble());
        }

        @Override
        public Literal gteq(Literal other) throws ConstExprException {
            return new BoolVal(this.value >= other.asDouble());
        }

        @Override
        public Literal eq(Literal other) throws ConstExprException {
            return new BoolVal(this.value == other.asDouble());
        }

        @Override
        public Literal neq(Literal other) throws ConstExprException {
            return new BoolVal(this.value != other.asDouble());
        }

    }

    public static class LongVal extends Literal {
        public long value;

        public LongVal(final long value) {
            this.value = value;
        }

        @Override
        public String toSQLString(RenderingContext _ignored) {
            return String.valueOf(value);
        }

        @Override
        public String className() {
            return "LongVal";
        }

        @Override
        public boolean asBool() throws ConstExprException {
            throw new ConversionException("long", "bool");
        }

        @Override
        public String asStr() throws ConstExprException {
            return this.value + "";
        }

        @Override
        public long asLong() throws ConstExprException {
            return (long) this.value;
        }

        @Override
        public double asDouble() throws ConstExprException {
            return this.value;
        }

        @Override
        public Literal add(Literal other) throws ConstExprException {
            if (other instanceof DoubleVal) {
                return new DoubleVal(this.value + other.asDouble());
            } else {
                return new LongVal(this.value + other.asLong());
            }
        }

        @Override
        public Literal sub(Literal other) throws ConstExprException {
            if (other instanceof DoubleVal) {
                return new DoubleVal(this.value - other.asDouble());
            } else {
                return new LongVal(this.value - other.asLong());
            }
        }

        @Override
        public Literal mul(Literal other) throws ConstExprException {
            if (other instanceof DoubleVal) {
                return new DoubleVal(this.value * other.asDouble());
            } else {
                return new LongVal(this.value * other.asLong());
            }
        }

        @Override
        public Literal mod(Literal other) throws ConstExprException {
            if (other instanceof DoubleVal) {
                return new DoubleVal(this.value % other.asDouble());
            } else {
                return new LongVal(this.value % other.asLong());
            }
        }

        @Override
        public Literal div(Literal other) throws ConstExprException {
            if (other instanceof DoubleVal) {
                return new DoubleVal(this.value / other.asDouble());
            } else {
                return new LongVal(this.value / other.asLong());
            }
        }

        @Override
        public Literal pow(Literal other) throws ConstExprException {
            return new DoubleVal(Math.pow(this.value, other.asDouble()));
        }

        @Override
        public Literal gt(Literal other) throws ConstExprException {
            return new BoolVal(this.value > other.asLong());
        }

        @Override
        public Literal lt(Literal other) throws ConstExprException {
            return new BoolVal(this.value < other.asLong());
        }

        @Override
        public Literal lteq(Literal other) throws ConstExprException {
            return new BoolVal(this.value <= other.asLong());
        }

        @Override
        public Literal gteq(Literal other) throws ConstExprException {
            return new BoolVal(this.value >= other.asLong());
        }

        @Override
        public Literal eq(Literal other) throws ConstExprException {
            return new BoolVal(this.value == other.asLong());
        }

        @Override
        public Literal neq(Literal other) throws ConstExprException {
            return new BoolVal(this.value != other.asLong());
        }
    }

    public static class BoolVal extends Literal {
        public boolean value;

        public BoolVal(final boolean value) {
            this.value = value;
        }

        @Override
        public String toSQLString(RenderingContext _ignored) {
            return String.valueOf(value);
        }

        @Override
        public String className() {
            return "BoolVal";
        }

        @Override
        public boolean asBool() throws ConstExprException {
            return this.value;
        }

        @Override
        public String asStr() throws ConstExprException {
            return this.value + "";
        }

        @Override
        public double asDouble() throws ConstExprException {
            throw new ConversionException("bool", "double");
        }

        @Override
        public long asLong() throws ConstExprException {
            throw new ConversionException("bool", "long");
        }

        @Override
        public Literal eq(Literal other) throws ConstExprException {
            return new BoolVal(this.value == other.asBool());
        }

        @Override
        public Literal neq(Literal other) throws ConstExprException {
            return new BoolVal(this.value != other.asBool());
        }

        @Override
        public Literal and(Literal other) throws ConstExprException {
            return new BoolVal(this.value && other.asBool());
        }

        @Override
        public Literal or(Literal other) throws ConstExprException {
            return new BoolVal(this.value || other.asBool());
        }

        @Override
        public Literal xor(Literal other) throws ConstExprException {
            return new BoolVal(this.value ^ other.asBool());
        }
    }
}
