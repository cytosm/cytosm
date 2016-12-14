package org.cytosm.cypher2sql.lowering.typeck.expr;

import org.cytosm.cypher2sql.lowering.rendering.RenderingContext;
import org.cytosm.cypher2sql.lowering.sqltree.from.FromItem;
import org.cytosm.cypher2sql.lowering.typeck.types.NodeType;
import org.cytosm.cypher2sql.lowering.typeck.var.AliasVar;
import org.cytosm.cypher2sql.lowering.typeck.var.NodeVar;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public String toSQLString(RenderingContext ctx) {
        if (ctx.location.equals(RenderingContext.Location.Export)) {
            if (var.type() instanceof NodeType) {
                NodeVar resolvedVar = (NodeVar) AliasVar.resolveAliasVar(var);
                return this.renderPropertiesWithVarContext(
                    ctx,
                    resolvedVar.propertiesRequired.stream()
                );
            } else {
                return ctx.renderVariableForExport(this);
            }
        } else if (ctx.location.equals(RenderingContext.Location.Other)) {
            return ctx.renderVariableForUse(this);
        }
        throw new RuntimeException("FIXME");
    }

    private String renderPropertiesWithVarContext(RenderingContext ctx, Stream<String> props) {
        return props.map(prop -> ctx.renderVariableForExport(this, prop))
                .collect(Collectors.joining(", "));
    }
}
