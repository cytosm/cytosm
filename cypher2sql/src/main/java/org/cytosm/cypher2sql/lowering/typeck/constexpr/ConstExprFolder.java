package org.cytosm.cypher2sql.lowering.typeck.constexpr;

import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTree;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprVar;
import org.cytosm.cypher2sql.lowering.typeck.types.MapType;
import org.cytosm.cypher2sql.lowering.typeck.var.AliasVar;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprFn;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTree.*;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprWalk;

/**
 */
public class ConstExprFolder implements ExprWalk.Folder<ConstVal.Literal, Cypher2SqlException> {


    @Override
    public ConstVal.Literal foldBinaryOperator(LhsRhs expr) throws Cypher2SqlException {
        if (expr instanceof Add) {
            return ExprWalk.fold(this, expr.lhs).add(ExprWalk.fold(this, expr.rhs));
        } else if (expr instanceof And) {
            return ExprWalk.fold(this, expr.lhs).and(ExprWalk.fold(this, expr.rhs));
        } else if (expr instanceof Div) {
            return ExprWalk.fold(this, expr.lhs).div(ExprWalk.fold(this, expr.rhs));
        } else if (expr instanceof Eq) {
            return ExprWalk.fold(this, expr.lhs).eq(ExprWalk.fold(this, expr.rhs));
        } else if (expr instanceof GreaterThan) {
            return ExprWalk.fold(this, expr.lhs).gt(ExprWalk.fold(this, expr.rhs));
        } else if (expr instanceof GreaterThanOrEqueal) {
            return ExprWalk.fold(this, expr.lhs).gteq(ExprWalk.fold(this, expr.rhs));
        } else if (expr instanceof In) {
            throw new UnknownOperation("In");
        } else if (expr instanceof LessThan) {
            return ExprWalk.fold(this, expr.lhs).lt(ExprWalk.fold(this, expr.rhs));
        } else if (expr instanceof LessThanOrEqual) {
            return ExprWalk.fold(this, expr.lhs).lteq(ExprWalk.fold(this, expr.rhs));
        } else if (expr instanceof Mod) {
            return ExprWalk.fold(this, expr.lhs).mod(ExprWalk.fold(this, expr.rhs));
        } else if (expr instanceof Mul) {
            return ExprWalk.fold(this, expr.lhs).mul(ExprWalk.fold(this, expr.rhs));
        } else if (expr instanceof Neq) {
            return ExprWalk.fold(this, expr.lhs).neq(ExprWalk.fold(this, expr.rhs));
        } else if (expr instanceof Or) {
            return ExprWalk.fold(this, expr.lhs).or(ExprWalk.fold(this, expr.rhs));
        } else if (expr instanceof Pow) {
            return ExprWalk.fold(this, expr.lhs).pow(ExprWalk.fold(this, expr.rhs));
        } else if (expr instanceof Sub) {
            return ExprWalk.fold(this, expr.lhs).sub(ExprWalk.fold(this, expr.rhs));
        } else if (expr instanceof Xor) {
            return ExprWalk.fold(this, expr.lhs).xor(ExprWalk.fold(this, expr.rhs));
        }
        throw new UnknownOperation("Unreachable code reached");
    }

    @Override
    public ConstVal.Literal foldUnaryOperator(Unary expr) throws Cypher2SqlException {
        if (expr instanceof IsNotNull) {
            throw new UnknownOperation("IS NOT NULL");
        } else if (expr instanceof IsNull) {
            throw new UnknownOperation("IS NULL");
        } else if (expr instanceof Not) {
            return new ConstVal.BoolVal(!ExprWalk.fold(this, expr.unary).asBool());
        } else if (expr instanceof UnaryAdd) {
            return ExprWalk.fold(this, expr.unary);
        }
        throw new UnknownOperation("Unreachable code reached");
    }

    @Override
    public ConstVal.Literal foldPropertyAccess(PropertyAccess expr) throws Cypher2SqlException {
        if (expr.expression instanceof MapExpr) {
            MapExpr map = (MapExpr) expr.expression;
            return ExprWalk.fold(this, map.props.get(expr.propertyAccessed));
        } else if (expr.expression instanceof ExprVar) {
            ExprVar exprVar = (ExprVar) expr.expression;
            if (exprVar.var instanceof AliasVar) {
                AliasVar var = (AliasVar) exprVar.var;
                if (var.type() instanceof MapType) {
                    var = (AliasVar) AliasVar.resolveAliasVar(var);
                    MapExpr map = (MapExpr) var.aliased;
                    return ExprWalk.fold(this, map.props.get(expr.propertyAccessed));
                }
            }
        }
        throw new UnknownOperation("Can't access property '" + expr.propertyAccessed +
                "' of '" + expr.expression.getClass() + "'");
    }

    @Override
    public ConstVal.Literal foldVariable(ExprVar expr) throws Cypher2SqlException {
        if (expr.var instanceof AliasVar) {
            return ExprWalk.fold(this, ((AliasVar) expr.var).aliased);
        }
        throw new UnknownOperation("Can't fold var.");
    }

    @Override
    public ConstVal.Literal foldListExpr(ListExpr expr) throws Cypher2SqlException {
        throw new UnknownOperation("Can't fold list expr.");
    }

    @Override
    public ConstVal.Literal foldMapExpr(MapExpr expr) throws Cypher2SqlException {
        throw new UnknownOperation("Can't fold map expr.");
    }

    @Override
    public ConstVal.Literal foldLiteral(ConstVal.Literal expr) throws Cypher2SqlException {
        return expr;
    }

    @Override
    public ConstVal.Literal foldFn(ExprFn expr) throws Cypher2SqlException {
        throw new UnknownOperation("Can't fold function.");
    }

    @Override
    public ConstVal.Literal foldCaseExpr(CaseExpr expr) throws Cypher2SqlException {
        throw new UnknownOperation("Can't fold CASE.");
    }

    @Override
    public ConstVal.Literal foldAliasExpr(AliasExpr expr) throws Cypher2SqlException {
        return ExprWalk.fold(this, expr.expr);
    }
}
