package org.cytosm.cypher2sql.cypher.ast.clause.match.pattern;

import org.cytosm.cypher2sql.cypher.ast.expression.MapExpression;
import org.cytosm.cypher2sql.cypher.ast.expression.Variable;

import java.util.List;
import java.util.Optional;

/**
 */
public class NodePattern extends PatternElement {

    public Optional<Variable> variable;
    public List<LabelName> labels;
    public Optional<MapExpression> properties;

    public NodePattern(final Variable variable, final List<LabelName> labels,
                       final MapExpression properties) {
        this.variable = (variable == null) ? Optional.empty(): Optional.of(variable);
        this.labels = labels;
        this.properties = (properties == null) ? Optional.empty(): Optional.of(properties);
    }
}
