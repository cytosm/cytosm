package org.cytosm.cypher2sql.lowering.typeck;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;

/**
 * Clause ID store the input position relevant
 * for that clause as well as the clause kind.
 *
 */
public class ClauseId implements Comparable<ClauseId> {

    public enum ClauseKind {
        MATCH,
        WITH,
        RETURN,
        UNKNOWN,
    }

    /**
     * Index of this ClauseId. This is how the key is computed.
     */
    public int index;
    /**
     * Kind of the Clause. This is only for debugging purpose
     * and could be removed entirely.
     */
    public ClauseKind kind;

    public ClauseId(final ASTNode astnode) {
        this.index = astnode.span.lo;
        this.kind = ClauseKind.UNKNOWN;
    }

    ClauseId(final Integer index, final ClauseKind kind) {
        this.index = index;
        this.kind = kind;
    }

    @Override
    public String toString() {
        switch (this.kind) {
            case MATCH:   return "MATCH (offset: " + index + ")";
            case WITH:    return "WITH (offset: " + index + ")";
            case RETURN:  return "RETURN (offset: " + index + ")";
            case UNKNOWN: return "UNKNOWN (offset: " + index + ")";
        }
        throw new RuntimeException("Unreachable code! Thanks static analysis...");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClauseId clauseId = (ClauseId) o;

        return index == clauseId.index;

    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public int compareTo(ClauseId o) {
        return this.index - o.index;
    }
}

