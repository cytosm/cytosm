package org.cytosm.cypher2sql.cypher.ast.clause.projection;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;
import org.cytosm.cypher2sql.cypher.ast.expression.Expression;
import org.cytosm.cypher2sql.cypher.ast.expression.Variable;

/**
 */
public abstract class ReturnItem extends ASTNode {

    public Expression expression;

    public static class Aliased extends ReturnItem {
        public Variable alias;

        public String name() {
            return alias.name;
        }

        public Aliased(Expression expression, Variable alias) {
            this.expression = expression;
            this.alias = alias;
        }
    }

    public static class Unaliased extends ReturnItem {

        // The input text trimmed.
        public String name;

        public Unaliased(Expression expression, String inputText) {
            this.expression = expression;
            this.name = inputText.trim();
        }
    }
}
