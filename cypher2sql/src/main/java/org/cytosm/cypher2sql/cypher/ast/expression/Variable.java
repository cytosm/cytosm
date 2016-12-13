package org.cytosm.cypher2sql.cypher.ast.expression;

/**
 */
public class Variable extends Expression {

    public String name;

    public Variable(final String name) {
        this.name = name;
    }
}
