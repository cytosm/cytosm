package org.cytosm.cypher2sql.lowering.typeck.expr;

import org.cytosm.cypher2sql.lowering.rendering.RenderingContext;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;

/**
 * This SQL node represent a variable but unlike {@link Var}
 * it is not designed to be a shared reference.
 * ExprVars know how to render Var using the {@link RenderingContext}
 * information.
 */
public class ExprVar implements Expr {

    /**
     * The wrapped variable. This is a shared reference.
     */
    public Var var;

    public ExprVar(Var var) {
        this.var = var;
    }

    @Override
    public String toSQLString(RenderingContext helper) {
        throw new RuntimeException("FIXME");
    }


//    @Override
//    public String toSQLString(RenderingHelper helper) {
//        // TODO: This code is not only hacky and buggy.
//        // TODO: It helped me to find the solution.
//        // TODO:
//        // TODO:    AliasVar, as NodeVar are defined in a particular Select.
//        // TODO:    Later use, must use the uniqueName of the AliasVar when being
//        // TODO:    rendered. If all property are rendered. Then we must
//        // TODO:
//        if (this._type instanceof NodeType) {
//            return ((NodeVar) resolveAliasVar((Var) aliased)).renderPropertiesWithVarContext(helper, this);
//        }
//        return aliased.toSQLString(helper) + " AS " + this.name;
//    }

//    @Override
//    public String toSQLString(RenderingHelper helper) {
//        return renderPropertiesWithVarContext(helper, this);
//    }
//
//    public String renderPropertiesWithVarContext(RenderingHelper helper, Var context) {
//        return propertiesRequired.stream().map(prop -> helper.renderVariableForExport(context, prop))
//                .collect(Collectors.joining(", "));
//    }

//    @Override
//    public String toSQLString(RenderingHelper _ignored) {
//        throw new RuntimeException("Temporary variable can't be rendered. Only property they contains can.");
//    }
}
