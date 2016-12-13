package org.cytosm.cypher2sql.lowering.typeck.var.expr;

import org.cytosm.cypher2sql.lowering.rendering.RenderingHelper;
import org.cytosm.cypher2sql.lowering.typeck.var.Expr;

/**
 * Const values.
 */
public class ConstVal {

    private ConstVal() {}

    public static abstract class Literal implements Expr {}

    public static class StrVal extends Literal {
        public String value;

        StrVal(final String value) {
            this.value = value;
        }

        @Override
        public String toSQLString(RenderingHelper helper) {
            return helper.renderStringLiteral(value);
        }
    }

    public static class DoubleVal extends Literal {
        public double value;

        DoubleVal(final double value) {
            this.value = value;
        }

        @Override
        public String toSQLString(RenderingHelper _ignored) {
            return String.valueOf(value);
        }
    }

    public static class LongVal extends Literal {
        public long value;

        LongVal(final long value) {
            this.value = value;
        }

        @Override
        public String toSQLString(RenderingHelper _ignored) {
            return String.valueOf(value);
        }
    }
}
