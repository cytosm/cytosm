package org.cytosm.cypher2sql.lowering.typeck.constexpr;

/**
 */
public class UnknownOperation extends ConstExprException {
    public UnknownOperation(final String message) {
        super(message);
    }
}
