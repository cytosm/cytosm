package org.cytosm.cypher2sql.cypher.ast;

/**
 */
public abstract class Union extends QueryPart {

    public class All extends Union {
        public All() {}
    }
    public class Distinct extends Union {
        public Distinct() {}
    }
}
