package org.cytosm.cypher2sql.lowering.typeck.constexpr;

import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;

/**
 */
public class ConstExprException extends Cypher2SqlException {
    ConstExprException(final String message) {
        super(message);
    }
}
