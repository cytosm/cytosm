package org.cytosm.cypher2sql.cypher.ast.expression;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;

/**
 */
public class FunctionName extends ASTNode {

    public String name;

    public FunctionName(final String name) {
        this.name = name;
    }
}
