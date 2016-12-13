package org.cytosm.cypher2sql.cypher.ast.clause.projection;

import org.cytosm.cypher2sql.cypher.ast.clause.Clause;

import java.util.List;
import java.util.Optional;

/**
 */
public abstract class ProjectionClause extends Clause {

    public boolean distinct;
    public List<ReturnItem> returnItems;
    public Optional<OrderBy> orderBy;
    public Optional<Skip> skip;
    public Optional<Limit> limit;

    public ProjectionClause(boolean distinct, List<ReturnItem> returnItems,
                            OrderBy orderBy, Skip skip, Limit limit) {
        this.distinct = distinct;
        this.returnItems = returnItems;
        this.orderBy = (orderBy == null) ? Optional.empty(): Optional.of(orderBy);
        this.skip = (skip == null) ? Optional.empty(): Optional.of(skip);
        this.limit = (limit == null) ? Optional.empty(): Optional.of(limit);
    }
}
