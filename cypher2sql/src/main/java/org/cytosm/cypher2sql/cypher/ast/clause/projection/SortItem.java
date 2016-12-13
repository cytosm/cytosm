package org.cytosm.cypher2sql.cypher.ast.clause.projection;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;
import org.cytosm.cypher2sql.cypher.ast.expression.Expression;

/**
 */
public abstract class SortItem extends ASTNode {

    public Expression expression;

    SortItem(final Expression expression) {
        this.expression = expression;
    }

    public static class Asc extends SortItem {

        public Asc(final Expression expression) {
            super(expression);
        }
    }
    public static class Desc extends SortItem {

        public Desc(final Expression expression) {
            super(expression);
        }
    }
}
