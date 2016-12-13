package org.cytosm.cypher2sql.cypher.ast.clause.projection;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;
import org.cytosm.cypher2sql.cypher.ast.expression.Expression;

/**
 */
public class Limit extends ASTNode {

    public Expression expression;

    public Limit(final Expression expression) {
        this.expression = expression;
    }
}
