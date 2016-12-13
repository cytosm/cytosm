package org.cytosm.cypher2sql.lowering.typeck.var;

import org.cytosm.cypher2sql.lowering.rendering.RenderingHelper;
import org.cytosm.cypher2sql.lowering.typeck.AvailableVariables;
import org.cytosm.cypher2sql.lowering.typeck.types.AType;
import org.cytosm.cypher2sql.lowering.typeck.types.NodeType;
import org.cytosm.cypher2sql.lowering.typeck.types.VarType;
import org.cytosm.cypher2sql.lowering.typeck.var.expr.ConstVal;
import org.cytosm.cypher2sql.lowering.typeck.var.expr.ExprTree;
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
        if (this._type instanceof NodeType) {
            return resolve((Var) aliased).renderPropertiesWithVarContext(helper, this);
        }
        return aliased.toSQLString(helper) + " AS " + this.name;
    }

    private static NodeVar resolve(Var var) {
        if (var instanceof NodeVar) {
            return (NodeVar) var;
        }
        if (var instanceof AliasVar) {
            return resolve((Var) ((AliasVar) var).aliased);
        }
        return null;
    }
}
