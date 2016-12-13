package org.cytosm.cypher2sql.cypher.ast.clause.match.pattern;

/**
 */
public class RelationshipChain extends PatternElement {

    public PatternElement element;
    public RelationshipPattern relationship;
    public NodePattern rightNode;

    public RelationshipChain(final PatternElement element, final RelationshipPattern relationship,
                             final NodePattern rightNode) {
        this.element = element;
        this.relationship = relationship;
        this.rightNode = rightNode;
    }
}
