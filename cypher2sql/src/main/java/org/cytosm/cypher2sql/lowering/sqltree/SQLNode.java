package org.cytosm.cypher2sql.lowering.sqltree;

import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;

/**
 * SQLNode is the base interface for all SQL nodes.
 */
public interface SQLNode {

    /**
     * Attempt to convert this sql tree into an equivalent SQL string.
     * This will mostly work for the same reason as explained in
     * {@link Expr}
     */
    String toSQLString();
}
