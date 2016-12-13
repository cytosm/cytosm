package org.cytosm.cypher2sql.cypher.ast.clause.match.pattern;

import org.cytosm.cypher2sql.cypher.ast.expression.Variable;

/**
 */
public class NamedPatternPart extends PatternPart {

    public Variable variable;

    public NamedPatternPart(final PatternElement element, final Variable var) {
        super(element);
        this.variable = var;
    }
}
