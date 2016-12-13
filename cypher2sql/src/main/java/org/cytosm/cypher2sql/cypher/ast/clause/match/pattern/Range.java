package org.cytosm.cypher2sql.cypher.ast.clause.match.pattern;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;
import org.cytosm.cypher2sql.cypher.ast.expression.Literal;

import java.util.Optional;

/**
 */
public class Range extends ASTNode {
    public Optional<Literal.Integer> lower;
    public Optional<Literal.Integer> upper;

    public Range(Literal.Integer lower, Literal.Integer upper) {
        this.lower = (lower == null) ? Optional.empty(): Optional.of(lower);
        this.upper = (upper == null) ? Optional.empty(): Optional.of(upper);
    }
}
