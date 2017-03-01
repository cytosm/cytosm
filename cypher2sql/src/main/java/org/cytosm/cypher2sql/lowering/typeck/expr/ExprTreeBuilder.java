package org.cytosm.cypher2sql.lowering.typeck.expr;

import org.cytosm.cypher2sql.lowering.typeck.AvailableVariables;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;
import org.cytosm.cypher2sql.cypher.visitor.Walk;
import org.cytosm.cypher2sql.lowering.typeck.constexpr.ConstVal;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.cytosm.cypher2sql.cypher.ast.expression.*;
import org.cytosm.cypher2sql.cypher.ast.expression.Binary.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 */
public class ExprTreeBuilder {

    private static final Logger LOGGER = Logger.getLogger(ExprTreeBuilder.class.getName());

    public static class ExprTreeException extends Exception {
        ExprTreeException(final String message) {
            super(message);
        }
    }

    public static class UnimplementedException extends ExprTreeException {
        UnimplementedException() { super("Unimplemented code reached!"); }
    }

    public static class UndefinedVariableException extends ExprTreeException {
        UndefinedVariableException(final String varname) {
            super("Variable '" + varname + "' is undefined.");
        }
    }

    private static class ExprFolder implements Walk.ExpressionFolder<Expr, ExprTreeException> {

        final AvailableVariables availablesVariables;


        ExprFolder(final AvailableVariables availablesVariables) {
            this.availablesVariables = availablesVariables;
        }

        // =========================================================
        //                  INTERESTING OVERRIDE
        // =========================================================

        @Override
        public Expr foldMapExpression(MapExpression expression) throws ExprTreeException {
            ExprTree.MapExpr res = new ExprTree.MapExpr();
            Iterator<Pair<PropertyKeyName, Expression>> iter = expression.props.iterator();
            while (iter.hasNext()) {
                Pair<PropertyKeyName, Expression> val = iter.next();
                if (res.props.containsKey(val.getKey().name)) {
                    LOGGER.warn("Property '" + val.getKey().name + "'" +
                            " (offset: " + val.getKey().span.lo + ")" +
                            " will be overridden.");
                }
                res.props.put(val.getKey().name, Walk.foldExpression(this, val.getValue()));
            }
            return res;
        }

        @Override
        public Expr foldVariable(Variable expression) throws ExprTreeException {
            Optional<Var> var = this.availablesVariables.get(expression.name);
            if (var.isPresent()) {
                return new ExprVar(var.get());
            } else {
                throw new UndefinedVariableException(expression.name);
            }
        }

        @Override
        public Expr foldProperty(Property expression) throws ExprTreeException {
            return new ExprTree.PropertyAccess(
                    expression.propertyKey.name,
                    Walk.foldExpression(this, expression.map));
        }

        @Override
        public Expr foldListExpression(ListExpression expression) throws ExprTreeException {
            ExprTree.ListExpr list = new ExprTree.ListExpr();

            for (Expression expr: expression.exprs) {
                list.exprs.add(Walk.foldExpression(this, expr));
            }
            return list;
        }

        @Override
        public Expr foldFunctionInvocation(FunctionInvocation expression) throws ExprTreeException {
            Iterator<Expression> iter = expression.args.iterator();
            List<Expr> args = new ArrayList<>(expression.args.size());
            while (iter.hasNext()) {
                args.add(Walk.foldExpression(this, iter.next()));
            }
            return new ExprFn(expression.functionName.name, args);
        }

        // =========================================================
        //                  BORING OVERRIDE
        // =========================================================

        @Override
        public Expr foldGreaterThan(GreaterThan expression) throws ExprTreeException {
            return new ExprTree.GreaterThan(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));
        }

        @Override
        public Expr foldLessThan(LessThan expression) throws ExprTreeException {
            return new ExprTree.LessThan(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));
        }

        @Override
        public Expr foldEquals(Equals expression) throws ExprTreeException {
            return new ExprTree.Eq(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));
        }

        @Override
        public Expr foldAnd(And expression) throws ExprTreeException {
            return new ExprTree.And(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));

        }

        @Override
        public Expr foldOr(Or expression) throws ExprTreeException {
            return new ExprTree.Or(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));

        }

        @Override
        public Expr foldIn(In expression) throws ExprTreeException {
            return new ExprTree.In(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));
        }

        @Override
        public Expr foldNot(Unary.Not expression) throws ExprTreeException {
            return new ExprTree.Not(Walk.foldExpression(this, expression.lhs));

        }

        @Override
        public Expr foldInvalidNotEquals(InvalidNotEquals expression) throws ExprTreeException {
            // FIXME: we could just accept them like if it was an Equals expression.
            throw new UnimplementedException();
        }

        @Override
        public Expr foldCaseExpression(CaseExpression expression) throws ExprTreeException {
            ExprTree.CaseExpr res = new ExprTree.CaseExpr();
            if (expression.expression.isPresent()) {
                res.caseExpr = Walk.foldExpression(this, expression.expression.get());
            }
            if (expression.default_.isPresent()) {
                res.defaultExpr = Walk.foldExpression(this, expression.default_.get());
            }
            Iterator<Pair<Expression, Expression>> iter = expression.alternatives.iterator();
            while (iter.hasNext()) {
                Pair<Expression, Expression> when = iter.next();
                Expr whenExpr = Walk.foldExpression(this, when.getKey());
                Expr thenExpr = Walk.foldExpression(this, when.getValue());
                res.whenExprs.add(new ImmutablePair<>(whenExpr, thenExpr));
            }
            return res;
        }

        @Override
        public Expr foldIsNull(Unary.IsNull expression) throws ExprTreeException {
            return new ExprTree.IsNull(Walk.foldExpression(this, expression.lhs));

        }

        @Override
        public Expr foldGreaterThanOrEqual(GreaterThanOrEqual expression) throws ExprTreeException {
            return new ExprTree.GreaterThanOrEqueal(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));

        }

        @Override
        public Expr foldLessThanOrEqual(LessThanOrEqual expression) throws ExprTreeException {
            return new ExprTree.LessThanOrEqual(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));
        }

        @Override
        public Expr foldPatternExpression(PatternExpression expression) throws ExprTreeException {
            // FIXME: We propably want to throw an exception here because we can't fold a PatternExpression
            // FIXME: into a SQL expression. It needs some pre-processing.
            throw new UnimplementedException();
        }

        @Override
        public Expr foldUnaryAdd(Unary.Add expression) throws ExprTreeException {
            return new ExprTree.UnaryAdd(Walk.foldExpression(this, expression.lhs));

        }

        @Override
        public Expr foldXor(Xor expression) throws ExprTreeException {
            return new ExprTree.Xor(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));

        }

        @Override
        public Expr foldSubtract(Subtract expression) throws ExprTreeException {
            return new ExprTree.Sub(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));

        }

        @Override
        public Expr foldAdd(Add expression) throws ExprTreeException {
            return new ExprTree.Add(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));

        }

        @Override
        public Expr foldDivide(Divide expression) throws ExprTreeException {
            return new ExprTree.Div(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));

        }

        @Override
        public Expr foldMultiply(Multiply expression) throws ExprTreeException {
            return new ExprTree.Mul(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));

        }

        @Override
        public Expr foldModulo(Modulo expression) throws ExprTreeException {
            return new ExprTree.Mod(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));

        }

        @Override
        public Expr foldPow(Pow expression) throws ExprTreeException {
            return new ExprTree.Pow(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));

        }

        @Override
        public Expr foldNotEquals(NotEquals expression) throws ExprTreeException {
            return new ExprTree.Neq(
                    Walk.foldExpression(this, expression.lhs),
                    Walk.foldExpression(this, expression.rhs));

        }

        @Override
        public Expr foldRegexMatch(RegexMatch expression) throws ExprTreeException {
            // FIXME
            throw new UnimplementedException();
        }

        @Override
        public Expr foldStartsWith(StartsWith expression) throws ExprTreeException {
            // FIXME
            throw new UnimplementedException();
        }

        @Override
        public Expr foldEndsWith(EndsWith expression) throws ExprTreeException {
            // FIXME
            throw new UnimplementedException();
        }

        @Override
        public Expr foldIsNotNull(Unary.IsNotNull expression) throws ExprTreeException {
            return new ExprTree.IsNotNull(Walk.foldExpression(this, expression.lhs));
        }

        @Override
        public Expr foldStringLiteral(Literal.StringLiteral stringLiteral) throws ExprTreeException {
            return new ConstVal.StrVal(stringLiteral.value);
        }

        @Override
        public Expr foldDecimalDoubleLiteral(Literal.DecimalDouble decimalDoubleLiteral) throws ExprTreeException {
            return new ConstVal.DoubleVal(decimalDoubleLiteral.value);
        }

        @Override
        public Expr foldUnsignedDecimalIntegerLiteral(Literal.Integer unsignedDecimalIntegerLiteral) throws ExprTreeException {
            return new ConstVal.LongVal(unsignedDecimalIntegerLiteral.value);
        }
    }

    public static Expr buildFromCypherExpression(final Expression expr, final AvailableVariables availablesVariables) {
        ExprFolder folder = new ExprFolder(availablesVariables);
        try {
            return Walk.foldExpression(folder, expr);
        } catch (ExprTreeException e) {
            e.printStackTrace();
        }
        return null;
    }
}
