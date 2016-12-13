package org.cytosm.cypher2sql.cypher.ast.expression;

import java.util.List;

/**
 */
public abstract class Many extends Expression {

    public List<Expression> exprs;

    Many(final List<Expression> exprs) {
        this.exprs = exprs;
    }

    public static class Ands extends Many {

        public Ands(final List<Expression> exprs) {
            super(exprs);
        }
    }
    public static class Ors extends Many {

        public Ors(final List<Expression> exprs) {
            super(exprs);
        }
    }
    public static class Collection extends Many {

        public Collection(final List<Expression> exprs) {
            super(exprs);
        }
    }
}
