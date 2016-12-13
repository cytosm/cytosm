package org.cytosm.cypher2sql.cypher.ast.expression;


/**
 */
public class Property extends Expression {

    public Expression map;
    public PropertyKeyName propertyKey;

    public Property(final Expression map, final PropertyKeyName propertyKey) {
        this.map = map;
        this.propertyKey = propertyKey;
    }
}
