package org.cytosm.cypher2sql.cypher.ast.expression;

import java.util.List;

/**
 */
public class ListExpression extends Expression {

    public List<Expression> elts;

    public ListExpression(final List<Expression> elts) {
        this.elts = elts;
    }
}
