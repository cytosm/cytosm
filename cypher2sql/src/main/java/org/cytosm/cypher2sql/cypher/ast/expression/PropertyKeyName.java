package org.cytosm.cypher2sql.cypher.ast.expression;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;

/**
 */
public class PropertyKeyName extends ASTNode {

    public String name;

    public PropertyKeyName(final String name) {
        this.name = name;
    }
}
