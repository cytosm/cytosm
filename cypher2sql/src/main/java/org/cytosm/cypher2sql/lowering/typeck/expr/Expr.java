package org.cytosm.cypher2sql.lowering.typeck.expr;

import org.cytosm.cypher2sql.lowering.rendering.RenderingContext;

/**
 * An expression that can be made of Var and constants.
 * This is a bridge between the SQL representation and the cypher one.
 * Because expression are extremely similar between the too we reuse the
 * same classes. It also makes the transformation easier as be don't have
 * to rebuild a new full expression tree and modify it in place.
 *
 * The initial construction is done using the a cypher expression and is
 * a valid SQL tree for the simple cases. They start to differ when we
 * consider cyphers Lists, MapExpression and CaseExpression.
 *
 * For those, we need to modify the expression and the SQL tree to
 * to translate the original semantic and get the same result.
 *
 */
public interface Expr {

    /**
     * Attempt to convert this expression into an equivalent SQL string.
     * This will mostly works if correct passes have been ran before hand.
     * This can fail for the following reasons:
     *
     *  * MapExpression don't have a direct equivalent in SQL but can be unwrapped
     *    most of the time. When they can't, they will be returned as part of another
     *    select. Thus they should never be present in a valid SQL expression tree.
     *
     * @param ctx provides context for the rendering.
     * @return Returns a SQL representation or throw a runtime exception if a bug is found.
     */
    String toSQLString(RenderingContext ctx);
}
