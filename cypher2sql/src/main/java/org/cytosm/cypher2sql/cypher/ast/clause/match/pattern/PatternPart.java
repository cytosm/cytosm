package org.cytosm.cypher2sql.cypher.ast.clause.match.pattern;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;

/**
 */
public class PatternPart extends ASTNode {

    public PatternElement element;

    public PatternPart(final PatternElement element) {
        this.element = element;
    }
}
