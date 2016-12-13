package org.cytosm.cypher2sql.lowering.exceptions;

/**
 */
public class Unreachable extends Cypher2SqlException {

    public Unreachable() {
        super("Unreachable code reached! This is a bug. Please report this issue.");
    }
}
