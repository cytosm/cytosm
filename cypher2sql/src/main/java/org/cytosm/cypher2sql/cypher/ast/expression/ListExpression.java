package org.cytosm.cypher2sql.cypher.ast.expression;

import java.util.List;

/**
 */
public class ListExpression extends Expression {

    public List<Expression> exprs;

    public ListExpression(final List<Expression> exprs) {
        this.exprs = exprs;
    }
}
