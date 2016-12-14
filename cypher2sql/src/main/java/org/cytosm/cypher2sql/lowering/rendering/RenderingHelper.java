package org.cytosm.cypher2sql.lowering.rendering;

import org.cytosm.cypher2sql.lowering.sqltree.SQLNode;
import org.cytosm.cypher2sql.lowering.sqltree.from.FromItem;
import org.cytosm.cypher2sql.lowering.typeck.var.AliasVar;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;

import java.util.List;

/**
 * The rendering helper provides context to render
 * variables. Without it, property access on variables
 * can't be rendered as there's information missing regarding
 * the way values from the variable can be accessed.
 *
 */
public class RenderingHelper {

    /**
     * This is driver specific. Right now a library user has no way
     * to override this. Shouldn't be too difficult though:
     *  - Add a factory of RenderingHelper parameter to {@link SQLNode#toSQLString()}
     *  - Update the code that creates RenderingHelper(s).
     * @param alias is the name to escape.
     * @return Returns an escaped name.
     */
    public String renderEscapedColumnName(String alias) {
        // FIXME: for some RDBMS brackets ('[' and ']') might be preferred.
        return "\"" + alias + "\"";
    }

    /**
     * This is again driver specific. See comment of previous function for
     * more details.
     *
     * @param literal is the string literal to render.
     * @return Returns a rendered version of the string literal.
     */
    public String renderStringLiteral(String literal) {
        // FIXME: Again, we need the factory to have this behavior driver-specific.
        return "'" + literal + "'";
    }
}
