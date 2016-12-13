package org.cytosm.cypher2sql.lowering.typeck.var.constexpr;

/**
 */
public class ConversionException extends ConstExprException {

    ConversionException(final String from, final String into) {
        super("Can't convert '" + from + "' into '" + into + "'");
    }
}
