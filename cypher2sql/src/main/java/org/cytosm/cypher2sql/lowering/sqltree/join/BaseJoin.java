package org.cytosm.cypher2sql.lowering.sqltree.join;

import org.cytosm.cypher2sql.lowering.rendering.RenderingContext;
import org.cytosm.cypher2sql.lowering.sqltree.from.FromItem;
import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;

/**
 * BaseJoin is a sum type for:
 *  - {@link InnerJoin}
 *  - {@link LeftJoin}
 *
 */
public abstract class BaseJoin {

    /**
     * Condition if there's any.
     */
    public Expr condition;


    /**
     * Source of the JOIN.
     */
    public FromItem joiningItem;

    public String toSQLString(RenderingContext ctx) {
        if (joiningItem == null) {
            throw new RuntimeException("Bug found! BaseJoin with joiningItem null.");
        }
        if (condition == null) {
            throw new RuntimeException("Joining condition is null! Wut?!");
        }
        String result = "";
        result += " JOIN ";
        result += joiningItem.toSQLString();
        result += " ON (" + condition.toSQLString(ctx) + ")";
        return result;
    }
}
