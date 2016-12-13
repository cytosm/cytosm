package org.cytosm.cypher2sql.cypher.ast.clause.match.pattern;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;
import org.cytosm.cypher2sql.cypher.ast.expression.MapExpression;
import org.cytosm.cypher2sql.cypher.ast.expression.Variable;

import java.util.List;
import java.util.Optional;

/**
 */
public class RelationshipPattern extends ASTNode {

    public enum SemanticDirection {
        OUTGOING,
        INCOMING,
        BOTH
    }

    public Optional<Variable> variable;
    public Optional<Optional<Range>> length;
    public Optional<MapExpression> properties;
    public List<RelTypeName> types;
    public SemanticDirection direction;

    public RelationshipPattern(final Variable variable, final Optional<Range> range,
                               final MapExpression properties, final List<RelTypeName> types) {
        this.variable = (variable == null) ? Optional.empty(): Optional.of(variable);
        this.length = (range == null) ? Optional.empty(): Optional.of(range);
        this.properties = (properties == null) ? Optional.empty(): Optional.of(properties);
        this.types = types;
    }
}
