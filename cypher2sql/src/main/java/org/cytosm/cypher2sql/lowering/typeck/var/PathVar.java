package org.cytosm.cypher2sql.lowering.typeck.var;

import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.NamedPatternPart;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.NodePattern;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.PatternElement;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.RelationshipChain;
import org.cytosm.cypher2sql.lowering.typeck.types.AType;
import org.cytosm.cypher2sql.lowering.typeck.types.PathType;

/**
 * Represent a Path variable. The only information
 * we have (that is always known statically thanks to GTop) is
 * the length of the Path.
 * Note that theoretically we can have dynamic length but cypher2sqlOnExpandedPaths does
 * not support them.
 */
public class PathVar extends Var {
    public int length;

    public AType type() {
        return new PathType();
    }

    public PathVar(NamedPatternPart pp) {
        super(pp);
        this.name = pp.variable.name;
        this.length = recComputeLength(pp.element);
    }

    private int recComputeLength(PatternElement rl) {
        if (rl instanceof NodePattern) {
            return 1;
        }
        return 1 + recComputeLength(((RelationshipChain) rl).element);
    }
}
