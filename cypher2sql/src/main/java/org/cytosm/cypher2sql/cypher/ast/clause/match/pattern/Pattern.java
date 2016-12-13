package org.cytosm.cypher2sql.cypher.ast.clause.match.pattern;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;

import java.util.List;

/**
 */
public class Pattern extends ASTNode {

    public List<PatternPart> patternParts;

    public Pattern(List<PatternPart> patternParts) {
        this.patternParts = patternParts;
    }
}
