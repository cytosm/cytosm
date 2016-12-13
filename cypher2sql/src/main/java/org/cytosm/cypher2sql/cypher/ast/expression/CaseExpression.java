package org.cytosm.cypher2sql.cypher.ast.expression;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;

/**
 */
public class CaseExpression extends Expression {

    public Optional<Expression> expression;
    public Optional<Expression> default_;
    public List<Pair<Expression, Expression>> alternatives;

    public CaseExpression(Expression expression, Expression default_,
                          List<Pair<Expression, Expression>> alternatives) {
        this.expression = (expression == null) ? Optional.empty(): Optional.of(expression);
        this.default_ = (default_ == null) ? Optional.empty(): Optional.of(default_);
        this.alternatives = alternatives;
    }
}
