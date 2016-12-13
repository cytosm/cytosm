package org.cytosm.cypher2sql.lowering.typeck.var;

import org.cytosm.cypher2sql.lowering.rendering.RenderingHelper;
import org.cytosm.cypher2sql.lowering.typeck.AvailableVariables;
import org.cytosm.cypher2sql.lowering.typeck.types.AType;
import org.cytosm.cypher2sql.lowering.typeck.types.NodeType;
import org.cytosm.cypher2sql.lowering.typeck.types.VarType;
import org.cytosm.cypher2sql.lowering.typeck.var.expr.ExprTreeBuilder;
import org.cytosm.cypher2sql.cypher.ast.clause.projection.ReturnItem;

/**
 * A var that alias an expression which can be composed
 * of other variables or just a few constants.
 */
public class AliasVar extends Var {
    public Expr aliased;

    private AType _type;

    public AType type() {
        return this._type;
    }

    public void setVarType(AType type) {
        this._type = type;
    }

    public AliasVar(final ReturnItem.Aliased aliased, final AvailableVariables availablesVariables) {
        super(aliased);
        this.name = aliased.alias.name;
        this.aliased = ExprTreeBuilder.buildFromCypherExpression(
                aliased.expression,
                availablesVariables
        );
    }

    @Override
    public String toSQLString(RenderingHelper helper) {
        // TODO: This code is not only hacky and buggy.
        // TODO: It helped me to find the solution.
        // TODO:
        // TODO:    AliasVar, as NodeVar are defined in a particular Select.
        // TODO:    Later use, must use the uniqueName of the AliasVar when being
        // TODO:    rendered. If all property are rendered. Then we must
        // TODO:
        if (this._type instanceof NodeType) {
            return ((NodeVar) resolveAliasVar((Var) aliased)).renderPropertiesWithVarContext(helper, this);
        }
        return aliased.toSQLString(helper) + " AS " + this.name;
    }

    public static Var resolveAliasVar(Var var) {
        if (var instanceof AliasVar && var.type() instanceof VarType) {
            return resolveAliasVar((Var) ((AliasVar) var).aliased);
        }
        return var;
    }
}
