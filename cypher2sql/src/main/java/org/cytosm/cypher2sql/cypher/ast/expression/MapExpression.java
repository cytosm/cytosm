package org.cytosm.cypher2sql.cypher.ast.expression;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 */
public class MapExpression extends Expression {

    public List<Pair<PropertyKeyName, Expression>> props;

    public MapExpression(final List<Pair<PropertyKeyName, Expression>> props) {
        this.props = props;
    }
}
