package org.cytosm.cypher2sql.lowering.exceptions;

/**
 * Throw this exception when you are dealing with unimplemented code.
 *
 */
public class Unimplemented extends Cypher2SqlException {

    public Unimplemented() {
        super("Unimplemented code reached.");
    }
}
