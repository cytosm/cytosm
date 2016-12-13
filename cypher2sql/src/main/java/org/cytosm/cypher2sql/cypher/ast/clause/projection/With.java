package org.cytosm.cypher2sql.cypher.ast.clause.projection;

import org.cytosm.cypher2sql.cypher.ast.clause.Where;

import java.util.List;
import java.util.Optional;

/**
 */
public class With extends ProjectionClause {

    public Optional<Where> where;

    public With(boolean distinct, List<ReturnItem> returnItems, OrderBy orderBy, Skip skip, Limit limit, Where where) {
        super(distinct, returnItems, orderBy, skip, limit);
        this.where = (where == null) ? Optional.empty(): Optional.of(where);
    }
}
