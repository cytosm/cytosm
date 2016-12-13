package org.cytosm.cypher2sql.cypher.ast.clause.match.pattern;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;

/**
 */
public class LabelName extends ASTNode {

    public String name;

    public LabelName(final String name) {
        this.name = name;
    }
}
