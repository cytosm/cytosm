package org.cytosm.cypher2sql.cypher.constexpr;

import org.cytosm.cypher2sql.cypher.visitor.Walk;
import org.cytosm.cypher2sql.cypher.ast.expression.Binary.*;
import org.cytosm.cypher2sql.cypher.ast.expression.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;

/**
 * Map Expression visitor that collects all the properties
 * into a hashmap.
 */
public class ConstExpressionFolder implements Walk.ExpressionFolder<ConstExpressionFolder.ConstExprValue, ConstExpressionFolder.ConstExprException> {

    public static class ConstExprException extends Exception {
        ConstExprException(final String message) {
            super(message);
        }
    }

    public static class UnkownOperation extends ConstExprException {
        UnkownOperation(final String message) {
            super(message);
        }
    }

    public static class UnimplementedException extends ConstExprException {
        UnimplementedException() {
            super("Unimplemented code reached.");
        }
    }

    public static class ConversionException extends ConstExprException {
        ConversionException(final String from, final String into) {
            super("Can't convert '" + from + "' into '" + into + "'");
        }
    }

    public interface ConstExprValue {
        // Arithmetic operations
        ConstExprValue add(ConstExprValue other) throws ConstExprException;
        ConstExprValue sub(ConstExprValue other) throws ConstExprException;
        ConstExprValue mul(ConstExprValue other) throws ConstExprException;
        ConstExprValue mod(ConstExprValue other) throws ConstExprException;
        ConstExprValue div(ConstExprValue other) throws ConstExprException;
        ConstExprValue pow(ConstExprValue other) throws ConstExprException;
        // Boolean operations
        ConstExprValue and(ConstExprValue other) throws ConstExprException;
        ConstExprValue or(ConstExprValue other) throws ConstExprException;
        ConstExprValue xor(ConstExprValue other) throws ConstExprException;
        ConstExprValue gt(ConstExprValue other) throws ConstExprException;
        ConstExprValue lt(ConstExprValue other) throws ConstExprException;
        ConstExprValue lteq(ConstExprValue other) throws ConstExprException;
        ConstExprValue gteq(ConstExprValue other) throws ConstExprException;
        ConstExprValue eq(ConstExprValue other) throws ConstExprException;
        ConstExprValue neq(ConstExprValue other) throws ConstExprException;


        String asStr() throws ConstExprException;
        long asLong() throws ConstExprException;
        double asDouble() throws ConstExprException;
        Object asObject() throws ConstExprException;
        boolean asBool() throws ConstExprException;

        String className();
    }

    private static abstract class BaseConstExprValue implements ConstExprValue {

        @Override
        public ConstExprValue add(ConstExprValue other) throws ConstExprException {
            throw new UnkownOperation("Unkown add operation on '"+ this.className() + "'.");
        }

        @Override
        public ConstExprValue sub(ConstExprValue other) throws ConstExprException {
            throw new UnkownOperation("Unkown sub operation on '"+ this.className() + "'.");
        }

        @Override
        public ConstExprValue mul(ConstExprValue other) throws ConstExprException {
            throw new UnkownOperation("Unkown mul operation on '"+ this.className() + "'.");
        }

        @Override
        public ConstExprValue mod(ConstExprValue other) throws ConstExprException {
            throw new UnkownOperation("Unkown mod operation on '"+ this.className() + "'.");
        }

        @Override
        public ConstExprValue div(ConstExprValue other) throws ConstExprException {
            throw new UnkownOperation("Unkown div operation on '"+ this.className() + "'.");
        }

        @Override
        public ConstExprValue pow(ConstExprValue other) throws ConstExprException {
            throw new UnkownOperation("Unkown pow operation on '"+ this.className() + "'.");
        }

        @Override
        public ConstExprValue and(ConstExprValue other) throws ConstExprException {
            throw new UnkownOperation("Unkown and operation on '"+ this.className() + "'.");
        }

        @Override
        public ConstExprValue or(ConstExprValue other) throws ConstExprException {
            throw new UnkownOperation("Unkown or operation on '"+ this.className() + "'.");
        }

        @Override
        public ConstExprValue xor(ConstExprValue other) throws ConstExprException {
            throw new UnkownOperation("Unkown xor operation on '"+ this.className() + "'.");
        }

        @Override
        public ConstExprValue gt(ConstExprValue other) throws ConstExprException {
            throw new UnkownOperation("Unkown gt operation on '"+ this.className() + "'.");
        }

        @Override
        public ConstExprValue lt(ConstExprValue other) throws ConstExprException {
            throw new UnkownOperation("Unkown lt operation on '"+ this.className() + "'.");
        }

        @Override
        public ConstExprValue lteq(ConstExprValue other) throws ConstExprException {
            throw new UnkownOperation("Unkown lteq operation on '"+ this.className() + "'.");
        }

        @Override
        public ConstExprValue gteq(ConstExprValue other) throws ConstExprException {
            throw new UnkownOperation("Unkown gteq operation on '"+ this.className() + "'.");
        }

        @Override
        public ConstExprValue eq(ConstExprValue other) throws ConstExprException {
            throw new UnkownOperation("Unkown eq operation on '"+ this.className() + "'.");
        }

        @Override
        public ConstExprValue neq(ConstExprValue other) throws ConstExprException {
            throw new UnkownOperation("Unkown neq operation on '"+ this.className() + "'.");
        }

        @Override
        public String asStr() throws ConstExprException {
            throw new UnkownOperation("Unkown asStr operation on '"+ this.className() + "'.");
        }

        @Override
        public boolean asBool() throws ConstExprException {
            throw new UnkownOperation("Unkown asBool operation on '"+ this.className() + "'.");
        }

        @Override
        public long asLong() throws ConstExprException {
            throw new UnkownOperation("Unkown asLong operation on '"+ this.className() + "'.");
        }

        @Override
        public double asDouble() throws ConstExprException {
            throw new UnkownOperation("Unkown asDouble operation on '"+ this.className() + "'.");
        }

        @Override
        public Object asObject() throws ConstExprException {
            throw new UnkownOperation("Unkown asObject operation on '"+ this.className() + "'.");
        }
    }

    private static class ConstNull extends BaseConstExprValue implements ConstExprValue {
        @Override
        public String className() {
            return "ConstNull";
        }
    }

    private static class ConstBool extends BaseConstExprValue implements ConstExprValue {
        public boolean value;

        ConstBool(boolean value) {
            this.value = value;
        }

        @Override
        public String className() {
            return "ConstBool";
        }

        @Override
        public boolean asBool() throws ConstExprException {
            return this.value;
        }

        @Override
        public ConstExprValue eq(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value == other.asBool());
        }

        @Override
        public ConstExprValue neq(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value != other.asBool());
        }

        @Override
        public ConstExprValue and(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value && other.asBool());
        }

        @Override
        public ConstExprValue or(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value || other.asBool());
        }

    }

    private static class ConstDouble extends BaseConstExprValue implements ConstExprValue {
        public double value;

        ConstDouble(double value) {
            this.value = value;
        }

        @Override
        public String className() {
            return "ConstDouble";
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
        public Object asObject() throws ConstExprException {
            return this.value;
        }

        @Override
        public ConstExprValue add(ConstExprValue other) throws ConstExprException {
            return new ConstDouble(this.value + other.asDouble());
        }

        @Override
        public ConstExprValue sub(ConstExprValue other) throws ConstExprException {
            return new ConstDouble(this.value - other.asDouble());
        }

        @Override
        public ConstExprValue mul(ConstExprValue other) throws ConstExprException {
            return new ConstDouble(this.value * other.asDouble());
        }

        @Override
        public ConstExprValue mod(ConstExprValue other) throws ConstExprException {
            return new ConstDouble(this.value % other.asDouble());
        }

        @Override
        public ConstExprValue div(ConstExprValue other) throws ConstExprException {
            return new ConstDouble(this.value / other.asDouble());
        }

        @Override
        public ConstExprValue pow(ConstExprValue other) throws ConstExprException {
            return new ConstDouble(Math.pow(this.value, other.asDouble()));
        }

        @Override
        public ConstExprValue gt(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value > other.asDouble());
        }

        @Override
        public ConstExprValue lt(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value < other.asDouble());
        }

        @Override
        public ConstExprValue lteq(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value <= other.asDouble());
        }

        @Override
        public ConstExprValue gteq(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value >= other.asDouble());
        }

        @Override
        public ConstExprValue eq(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value == other.asDouble());
        }

        @Override
        public ConstExprValue neq(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value != other.asDouble());
        }
    }

    private static class ConstList extends BaseConstExprValue implements ConstExprValue {
        public List<ConstExprValue> value;

        ConstList(List<ConstExprValue> value) {
            this.value = value;
        }

        @Override
        public String className() {
            return "ConstList";
        }

        @Override
        public Object asObject() throws ConstExprException {
            List<Object> res = new ArrayList<>();
            for (ConstExprValue el: this.value) {
                res.add(el.asObject());
            }
            return res;
        }
    }

    private static class ConstMap extends BaseConstExprValue implements ConstExprValue {
        public Map<String, ConstExprValue> value;

        ConstMap(Map<String, ConstExprValue> value) {
            this.value = value;
        }

        @Override
        public String className() {
            return "ConstMap";
        }

        @Override
        public Object asObject() throws ConstExprException {
            Map<String, Object> res = new HashMap<>();
            for (Map.Entry<String, ConstExprValue> entry: this.value.entrySet()) {
                res.put(entry.getKey(), entry.getValue().asObject());
            }
            return res;
        }
    }

    private static class ConstLong extends BaseConstExprValue implements ConstExprValue {
        public long value;

        ConstLong(long value) {
            this.value = value;
        }

        @Override
        public String className() {
            return "ConstLong";
        }

        @Override
        public String asStr() throws ConstExprException {
            return this.value + "";
        }

        @Override
        public long asLong() throws ConstExprException {
            return this.value;
        }

        @Override
        public double asDouble() throws ConstExprException {
            return this.value;
        }

        @Override
        public Object asObject() throws ConstExprException {
            return this.value;
        }

        @Override
        public ConstExprValue add(ConstExprValue other) throws ConstExprException {
            return new ConstLong(this.value + other.asLong());
        }

        @Override
        public ConstExprValue sub(ConstExprValue other) throws ConstExprException {
            return new ConstLong(this.value - other.asLong());
        }

        @Override
        public ConstExprValue mul(ConstExprValue other) throws ConstExprException {
            return new ConstLong(this.value * other.asLong());
        }

        @Override
        public ConstExprValue mod(ConstExprValue other) throws ConstExprException {
            return new ConstLong(this.value % other.asLong());
        }

        @Override
        public ConstExprValue div(ConstExprValue other) throws ConstExprException {
            return new ConstLong(this.value / other.asLong());
        }

        @Override
        public ConstExprValue gt(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value > other.asLong());
        }

        @Override
        public ConstExprValue lt(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value < other.asLong());
        }

        @Override
        public ConstExprValue lteq(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value <= other.asLong());
        }

        @Override
        public ConstExprValue gteq(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value >= other.asLong());
        }

        @Override
        public ConstExprValue eq(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value == other.asLong());
        }

        @Override
        public ConstExprValue neq(ConstExprValue other) throws ConstExprException {
            return new ConstBool(this.value != other.asLong());
        }
    }

    private static class ConstStr extends BaseConstExprValue implements ConstExprValue {
        public String value;

        ConstStr(String value) {
            this.value = value;
        }

        @Override
        public ConstExprValue add(ConstExprValue other) throws ConstExprException {
            return new ConstStr(this.value + other.asStr());
        }

        @Override
        public String asStr() throws ConstExprException {
            return this.value;
        }

        @Override
        public long asLong() throws ConstExprException {
            try {
                return Long.parseLong(this.value);
            } catch (NumberFormatException e) {
                throw new ConversionException(this.value, "long");
            }
        }

        @Override
        public double asDouble() throws ConstExprException {
            try {
                return Double.parseDouble(this.value);
            } catch (NumberFormatException e) {
                throw new ConversionException(this.value, "double");
            }
        }

        @Override
        public Object asObject() throws ConstExprException {
            return "\"" + this.value + "\"";
        }

        @Override
        public String className() {
            return "ConstStr";
        }
    }

    private ConstExpressionFolder() {}

    /**
     * Eval the provided MapExpression and returns a Map of string and objects.
     * @param mapExpression is the MapExpression to eval.
     * @return Returns a Map with all values computed.
     */
    public static Map<String, Object> evalMapExpression(MapExpression mapExpression) {
        ConstExpressionFolder folder = new ConstExpressionFolder();
        try {
            ConstExprValue value = Walk.foldExpression(folder, mapExpression);
            if (value instanceof ConstMap) {
                return (Map<String, Object>) value.asObject();
            }
            return Collections.emptyMap();
        } catch (ConstExprException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    /**
     * Eval the given expression into a value ready to be consumed.
     * @param expression the expression to eval.
     * @return Returns the evaluated expression.
     */
    public static ConstExprValue eval(Expression expression) {
        ConstExpressionFolder folder = new ConstExpressionFolder();
        try {
            return Walk.foldExpression(folder, expression);
        } catch (ConstExprException e) {
            e.printStackTrace();
            return new ConstNull();
        }
    }

    public ConstExprValue foldProperty(final Property expression) throws ConstExprException {
        ConstExprValue val = Walk.foldExpression(this, expression.map);
        String fieldName = expression.propertyKey.name;
        if (val instanceof ConstMap) {
            return ((ConstMap) val).value.get(fieldName);
        } else {
            throw new UnkownOperation("Can't access field '" + fieldName +
                    "' on type other than MapExpression");
        }
    }
    public ConstExprValue foldIn(final In expression) throws ConstExprException {
        // FIXME
        throw new UnimplementedException();
    }
    public ConstExprValue foldNot(final Unary.Not expression) throws ConstExprException {
        return new ConstBool(!Walk.foldExpression(this, expression.lhs).asBool());
    }
    public ConstExprValue foldInvalidNotEquals(final InvalidNotEquals expression) throws ConstExprException {
        // FIXME
        throw new UnimplementedException();
    }
    public ConstExprValue foldListExpression(final ListExpression expression) throws ConstExprException {
        List<ConstExprValue> res = new ArrayList<>();
        Iterator<Expression> iter = expression.exprs.iterator();
        while (iter.hasNext()) {
            res.add(Walk.foldExpression(this, iter.next()));
        }
        return new ConstList(res);
    }
    public ConstExprValue foldMapExpression(final MapExpression expression) throws ConstExprException {

        HashMap<String, ConstExprValue> res = new HashMap<>();
        Iterator<Pair<PropertyKeyName, Expression>> iter = expression.props.iterator();
        while (iter.hasNext()) {
            // TODO: Handle the case when the property is already present.
            Pair<PropertyKeyName, Expression> prop = iter.next();
            res.put(prop.getKey().name, Walk.foldExpression(this, prop.getValue()));
        }
        return new ConstMap(res);
    }
    public ConstExprValue foldVariable(final Variable expression) throws ConstExprException {
        // FIXME
        throw new UnimplementedException();
    }
    public ConstExprValue foldFunctionInvocation(final FunctionInvocation expression) throws ConstExprException {
        // FIXME
        throw new UnimplementedException();
    }
    public ConstExprValue foldCaseExpression(final CaseExpression expression) throws ConstExprException {
        // FIXME
        throw new UnimplementedException();
    }
    public ConstExprValue foldIsNull(final Unary.IsNull expression) throws ConstExprException {
        // FIXME
        throw new UnimplementedException();
    }
    public ConstExprValue foldPatternExpression(final PatternExpression expression) throws ConstExprException {
        // FIXME
        throw new UnimplementedException();
    }
    public ConstExprValue foldUnaryAdd(final Unary.Add expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs);
    }
    // Booleans operations
    public ConstExprValue foldGreaterThan(final GreaterThan expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs).gt(Walk.foldExpression(this, expression.rhs));
    }
    public ConstExprValue foldLessThan(final LessThan expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs).lt(Walk.foldExpression(this, expression.rhs));
    }
    public ConstExprValue foldAnd(final And expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs).and(Walk.foldExpression(this, expression.rhs));
    }
    public ConstExprValue foldOr(final Or expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs).or(Walk.foldExpression(this, expression.rhs));
    }
    public ConstExprValue foldEquals(final Equals expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs).eq(Walk.foldExpression(this, expression.rhs));
    }
    public ConstExprValue foldNotEquals(final NotEquals expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs).neq(Walk.foldExpression(this, expression.rhs));
    }
    public ConstExprValue foldXor(final Xor expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs).xor(Walk.foldExpression(this, expression.rhs));
    }
    public ConstExprValue foldGreaterThanOrEqual(final GreaterThanOrEqual expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs).gteq(Walk.foldExpression(this, expression.rhs));
    }
    public ConstExprValue foldLessThanOrEqual(final LessThanOrEqual expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs).lteq(Walk.foldExpression(this, expression.rhs));
    }
    // Arithmetic operations
    public ConstExprValue foldSubtract(final Subtract expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs).sub(Walk.foldExpression(this, expression.rhs));
    }
    public ConstExprValue foldAdd(final Add expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs).add(Walk.foldExpression(this, expression.rhs));
    }
    public ConstExprValue foldDivide(final Divide expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs).div(Walk.foldExpression(this, expression.rhs));
    }
    public ConstExprValue foldMultiply(final Multiply expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs).mul(Walk.foldExpression(this, expression.rhs));
    }
    public ConstExprValue foldModulo(final Modulo expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs).mod(Walk.foldExpression(this, expression.rhs));
    }
    public ConstExprValue foldPow(final Pow expression) throws ConstExprException {
        return Walk.foldExpression(this, expression.lhs).pow(Walk.foldExpression(this, expression.rhs));
    }
    public ConstExprValue foldRegexMatch(final RegexMatch expression) throws ConstExprException {
        // FIXME
        throw new UnimplementedException();
    }
    public ConstExprValue foldStartsWith(final StartsWith expression) throws ConstExprException {
        // FIXME
        throw new UnimplementedException();
    }
    public ConstExprValue foldEndsWith(final EndsWith expression) throws ConstExprException {
        // FIXME
        throw new UnimplementedException();
    }
    public ConstExprValue foldIsNotNull(final Unary.IsNotNull expression) throws ConstExprException {
        // FIXME
        throw new UnimplementedException();
    }
    // Literals
    public ConstExprValue foldStringLiteral(final Literal.StringLiteral stringLiteral) throws ConstExprException {
        return new ConstStr(stringLiteral.value);
    }
    public ConstExprValue foldDecimalDoubleLiteral(final Literal.DecimalDouble decimalDoubleLiteral) throws ConstExprException {
        return new ConstDouble(decimalDoubleLiteral.value);
    }
    public ConstExprValue foldUnsignedDecimalIntegerLiteral(final Literal.Integer unsignedDecimalIntegerLiteral) throws ConstExprException {
        return new ConstLong(unsignedDecimalIntegerLiteral.value);
    }
}
