package org.cytosm.cypher2sql.cypher.ast.clause.match.pattern;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;

/**
 */
public class RelTypeName extends ASTNode {

    public String name;

    public RelTypeName(final String name) {
        this.name = name;
    }
}
