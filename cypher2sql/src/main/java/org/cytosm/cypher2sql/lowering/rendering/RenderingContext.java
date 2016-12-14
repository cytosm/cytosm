package org.cytosm.cypher2sql.lowering.rendering;

import org.cytosm.cypher2sql.lowering.sqltree.SQLNode;
import org.cytosm.cypher2sql.lowering.sqltree.from.FromItem;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprVar;
import org.cytosm.cypher2sql.lowering.typeck.var.AliasVar;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;

import java.util.List;
import java.util.Optional;

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
     * @param exprVar is the variable.
     * @param propertyAccessed is the property that will be accessed.
     * @return Returns the string
     */
    public String renderVariableForUse(ExprVar exprVar, String propertyAccessed) {
        Var var = exprVar.var;
        FromItem fromItem = getSource(var).get();
        if (fromItem.source == null) {
            return fromItem.sourceVariableName + "." + propertyAccessed;
        } else {
            return fromItem.sourceVariableName + "." + var.uniqueName + "_" + propertyAccessed;
        }
    }

    /**
     * Render a variable that is used as part of a larger expression.
     * This function assumes that the variable is of type Number or String.
     * @param exprVar is the variable.
     * @return Returns the string representation of the variable.
     */
    public String renderVariableForUse(ExprVar exprVar) {
        Var var = exprVar.var;
        Optional<FromItem> fromItem = getSource(var);
        if (fromItem.isPresent()) {
            return fromItem.get().sourceVariableName + "." + var.uniqueName;
        } else {
            return var.uniqueName;
        }
    }

    /**
     * Render a variable in an export. This assume that the type of the variable
     * is a String or Number.
     * @param exprVar is the variable to render.
     * @return Returns the string representation of the variable.
     */
    public String renderVariableForExport(ExprVar exprVar) {
        Var var = exprVar.var;
        Optional<FromItem> src = getSource(var);
        if (src.isPresent()) {
            return src.get().sourceVariableName + "." + var.uniqueName;
        } else if (var instanceof AliasVar) {
            return ((AliasVar) var).aliased.toSQLString(this) + " AS " + var.uniqueName;
        } else {
            return var.uniqueName;
        }
    }

    /**
     * Returns the source for a particular variable
     * @param var is the variable.
     * @return Returns the source if there's any.
     */
    public Optional<FromItem> getSource(Var var) {
        return this.fromItems.stream()
                .filter(x -> x.variables.stream().anyMatch(v -> v == var))
                .findAny();
    }

    /**
     * Render the provided property access on the given variable when the variable
     * is exported for further use.
     *
     * This can throw an error if the variable shouldn't be available
     * in this context. However this should only happen if there's a bug
     * in {@link org.cytosm.cypher2sql.lowering.typeck.VarDependencies}
     *
     * @param exprVar is the variable.
     * @param propertyAccessed is the property that will be accessed.
     * @return Returns the string
     */
    public String renderVariableForExport(ExprVar exprVar, String propertyAccessed) {
        Var var = exprVar.var;
        Optional<FromItem> src = getSource(var);

        if (src.isPresent()) {
            FromItem fromItem = src.get();
            if (fromItem.source == null) {
                return fromItem.sourceVariableName + "." + propertyAccessed + " AS " +
                        var.uniqueName + "_" + propertyAccessed;
            } else {
                return fromItem.sourceVariableName + "." + var.uniqueName + "_" + propertyAccessed;
            }
        } else if (var instanceof AliasVar) {
            AliasVar aliasVar = (AliasVar) var;
            var = AliasVar.resolveAliasVar(aliasVar);
            src = getSource(var);
            if (src.isPresent()) {
                FromItem fromItem = src.get();
                if (fromItem.source == null) {
                    return fromItem.sourceVariableName + "." + propertyAccessed + " AS " +
                            aliasVar.uniqueName + "_" + propertyAccessed;
                } else {
                    return fromItem.sourceVariableName + "." + var.uniqueName + "_" + propertyAccessed + " AS " +
                            aliasVar.uniqueName + "_" + propertyAccessed;
                }
            }
        }
        throw new RuntimeException("Can't render a Var that comes from nowhere!");
    }
}
