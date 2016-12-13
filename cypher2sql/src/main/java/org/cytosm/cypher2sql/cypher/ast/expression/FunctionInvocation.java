package org.cytosm.cypher2sql.cypher.ast.expression;

import java.util.List;

/**
 */
public class FunctionInvocation extends Expression {

    public FunctionName functionName;
    public boolean distinct;
    public List<Expression> args;

    public FunctionInvocation(final FunctionName functionName,
                              boolean distinct, List<Expression> args) {
        this.distinct = distinct;
        this.args = args;
        this.functionName = functionName;
    }
}
