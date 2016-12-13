package org.cytosm.cypher2sql.cypher.ast.clause.projection;

import java.util.List;

/**
 */
public class Return extends ProjectionClause {

    public Return(boolean distinct, List<ReturnItem> returnItems, OrderBy orderBy, Skip skip, Limit limit) {
        super(distinct, returnItems, orderBy, skip, limit);
    }
}
