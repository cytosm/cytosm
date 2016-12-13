package org.cytosm.cypher2sql.cypher.ast;

/**
 */
public class Span {

    public int lo;
    public int hi;

    public Span(int lo, int hi) {
        this.hi = hi;
        this.lo = lo;
    }
}
