package org.cytosm.cypher2sql.lowering.rendering;

import org.cytosm.cypher2sql.lowering.sqltree.SQLNode;
import org.cytosm.cypher2sql.lowering.sqltree.from.FromItem;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprVar;
import org.cytosm.cypher2sql.lowering.typeck.var.AliasVar;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;

import java.util.List;

/**
 * The rendering context contains key information to know
 * how to render an expression properly. In particular,
 * in carries information about:
 *  - where variables come from
 *  - if we are rendering exported items
 */
public class RenderingContext {

    public enum Location {
        Export,
        FunctionArgs,
        Other,
    }

    public final Location location;

    /**
     * List of FromItem availables.
     */
    public final List<FromItem> fromItems;

    /**
     * The rendering helper that knows how to render vendor's SQL requirements.
     * TODO: Allow users to provide a custom one.
     */
    public final RenderingHelper helper = new RenderingHelper();

    public RenderingContext(final List<FromItem> fromItems, final Location location) {
        this.fromItems = fromItems;
        this.location = location;
    }

    public RenderingContext(RenderingContext original, final Location location) {
        this.fromItems = original.fromItems;
        this.location = location;
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
        return this.helper.renderEscapedColumnName(alias);
    }

    /**
     * This is again driver specific. See comment of previous function for
     * more details.
     *
     * @param literal is the string literal to render.
     * @return Returns a rendered version of the string literal.
     */
    public String renderStringLiteral(String literal) {
        return this.helper.renderStringLiteral(literal);
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
    public String renderVariableForUse(ExprVar exprVar, String propertyAccessed) {
        Var var = exprVar.var;
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
    public String renderVariableForExport(ExprVar exprVar, String propertyAccessed) {
        Var var = exprVar.var;
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
            ExprVar exprVar = (ExprVar) ((AliasVar) var).aliased;
            return getUniqueName(exprVar.var);
        } else {
            return var.uniqueName;
        }
    }
}
