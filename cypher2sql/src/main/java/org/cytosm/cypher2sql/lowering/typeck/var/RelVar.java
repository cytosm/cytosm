package org.cytosm.cypher2sql.lowering.typeck.var;

import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.RelationshipPattern;
import org.cytosm.cypher2sql.lowering.typeck.types.AType;
import org.cytosm.cypher2sql.lowering.typeck.types.RelType;

/**
 * A var that represent a relationship.
 */
public class RelVar extends NodeOrRelVar {

    public AType type() {
        return new RelType();
    }

    public RelVar(final RelationshipPattern rel) {
        super(rel);
        this.name = rel.variable.get().name;
    }
}
