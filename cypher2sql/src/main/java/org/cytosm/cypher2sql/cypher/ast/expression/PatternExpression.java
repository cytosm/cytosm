package org.cytosm.cypher2sql.cypher.ast.expression;

import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.RelationshipChain;

/**
 */
public class PatternExpression extends Expression {

    public RelationshipChain element;

    public PatternExpression(final RelationshipChain element) {
        this.element = element;
    }
}
