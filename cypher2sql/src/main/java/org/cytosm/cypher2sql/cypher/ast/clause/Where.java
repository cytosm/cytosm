package org.cytosm.cypher2sql.cypher.ast.clause;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;
import org.cytosm.cypher2sql.cypher.ast.expression.Expression;

/**
 */
public class Where extends ASTNode {

    public Expression expression;

    public Where(Expression expression) {
        this.expression = expression;
    }
}
