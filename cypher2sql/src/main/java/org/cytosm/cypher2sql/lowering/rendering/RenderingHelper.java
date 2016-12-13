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
     * List of FromItem availables.
     */
    private final List<FromItem> fromItems;

    public RenderingHelper(List<FromItem> fromItems) {
        this.fromItems = fromItems;
    }

    /**
     * Render the provided property access on the given variable when the variable
     * is used somewhere.
     *
     * This can throw an error if the variable shouldn't be available
     * in this context. However this should only happen if there's a bug
     * in {@link org.cytosm.cypher2sql.lowering.typeck.VarDependencies}
     *
     * @param var is the variable.
     * @param propertyAccessed is the property that will be accessed.
     * @return Returns the string
     */
    public String renderVariableForUse(Var var, String propertyAccessed) {
        String uniqueName = getUniqueName(var);
        FromItem fromItem = this.fromItems.stream()
                .filter(x -> x.variables.stream().anyMatch(v -> v == var))
                .findFirst().get();
        if (fromItem.source == null) {
            return fromItem.sourceVariableName + "." + propertyAccessed;
        } else {
            return fromItem.sourceVariableName + "." + uniqueName + "_" + propertyAccessed;
        }
    }

    /**
     * Render the provided property access on the given variable when the variable
     * is exported for further use.
     *
     * This can throw an error if the variable shouldn't be available
     * in this context. However this should only happen if there's a bug
     * in {@link org.cytosm.cypher2sql.lowering.typeck.VarDependencies}
     *
     * @param var is the variable.
     * @param propertyAccessed is the property that will be accessed.
     * @return Returns the string
     */
    public String renderVariableForExport(Var var, String propertyAccessed) {
        String uniqueName = getUniqueName(var);
        FromItem fromItem = this.fromItems.stream()
                .filter(x -> x.variables.stream().anyMatch(v -> v == var))
                .findAny().get();
        if (fromItem.source == null) {
            return fromItem.sourceVariableName + "." + propertyAccessed + " AS " + uniqueName + "_" + propertyAccessed;
        } else {
            return fromItem.sourceVariableName + "." + uniqueName + "_" + propertyAccessed;
        }
    }

    /**
     * Resolve the unique name that will be used when rendering.
     * @param var is the var to get unique name.
     * @return Returns the appropriate unique name.
     */
    private String getUniqueName(Var var) {
        if (var instanceof AliasVar) {
            return getUniqueName((Var) ((AliasVar) var).aliased);
        } else {
            return var.uniqueName;
        }
    }

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
