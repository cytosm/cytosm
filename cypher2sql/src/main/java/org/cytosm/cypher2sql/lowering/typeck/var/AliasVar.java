package org.cytosm.cypher2sql.lowering.typeck.var;

import org.cytosm.cypher2sql.lowering.typeck.AvailableVariables;
import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprVar;
import org.cytosm.cypher2sql.lowering.typeck.types.AType;
import org.cytosm.cypher2sql.lowering.typeck.types.VarType;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTreeBuilder;
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

    public static Var resolveAliasVar(Var var) {
        if (var instanceof AliasVar && var.type() instanceof VarType) {
            ExprVar exprVar = (ExprVar) ((AliasVar) var).aliased;
            return resolveAliasVar(exprVar.var);
        }
        return var;
    }
}
