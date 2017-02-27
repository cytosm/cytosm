package org.cytosm.cypher2sql.lowering.typeck.expr;

import org.cytosm.cypher2sql.lowering.typeck.var.AliasVar;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;
import org.cytosm.cypher2sql.lowering.typeck.constexpr.ConstVal;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Walk utilities for expressions.
 */
public class ExprWalk {

    // ==================================================================
    //                          VISITORs
    // ==================================================================

    // TODO(Joan): Visitors should be able to throw exceptions as well.
    public interface Visitor {

        void visitBinaryOperator(ExprTree.LhsRhs expr);
        void visitUnaryOperator(ExprTree.Unary expr);
        void visitPropertyAccess(ExprTree.PropertyAccess expr);
        void visitVariable(ExprVar expr);
        void visitListExpr(ExprTree.ListExpr expr);
        void visitMapExpr(ExprTree.MapExpr expr);
        void visitLiteral(ConstVal.Literal expr);
        void visitFn(ExprFn expr);
        void visitCaseExpr(ExprTree.CaseExpr expr);
        void visitAliasExpr(ExprTree.AliasExpr expr);
    }

    public static abstract class BaseVisitor implements Visitor {
        @Override
        public void visitBinaryOperator(ExprTree.LhsRhs expr) {
            walk(this, expr.lhs);
            walk(this, expr.rhs);
        }

        @Override
        public void visitUnaryOperator(ExprTree.Unary expr) {
            walk(this, expr.unary);
        }

        @Override
        public void visitPropertyAccess(ExprTree.PropertyAccess expr) {
            walk(this, expr.expression);
        }

        @Override
        public void visitFn(ExprFn expr) {
            expr.args.forEach(x -> walk(this, x));
        }

        @Override
        public void visitLiteral(ConstVal.Literal expr) {}

        @Override
        public void visitListExpr(ExprTree.ListExpr expr) {
            expr.exprs.forEach(x -> walk(this, x));
        }

        @Override
        public void visitMapExpr(ExprTree.MapExpr expr) {
            expr.props.entrySet().forEach(x -> walk(this, x.getValue()));
        }

        @Override
        public void visitAliasExpr(ExprTree.AliasExpr expr) {
            walk(this, expr.expr);
        }

        @Override
        public void visitCaseExpr(ExprTree.CaseExpr expr) {
            if (expr.caseExpr != null) {
                walk(this, expr.caseExpr);
            }
            for (Pair<Expr, Expr> when: expr.whenExprs) {
                walk(this, when.getLeft());
                walk(this, when.getRight());
            }
            if (expr.defaultExpr != null) {
                walk(this, expr.defaultExpr);
            }
        }

        @Override
        public void visitVariable(ExprVar expr) {
            if (expr.var instanceof AliasVar) {
                walk(this, ((AliasVar) expr.var).aliased);
            }
        }
    }

    public static void walk(Visitor visitor, Expr expr) {
        if (expr instanceof ExprTree.LhsRhs) {
            visitor.visitBinaryOperator((ExprTree.LhsRhs) expr);
        } else if (expr instanceof ExprTree.Unary) {
            visitor.visitUnaryOperator((ExprTree.Unary) expr);
        } else if (expr instanceof ExprTree.PropertyAccess) {
            visitor.visitPropertyAccess((ExprTree.PropertyAccess) expr);
        } else if (expr instanceof ExprFn) {
            visitor.visitFn((ExprFn) expr);
        } else if (expr instanceof ConstVal.Literal) {
            visitor.visitLiteral((ConstVal.Literal) expr);
        } else if (expr instanceof ExprTree.ListExpr) {
            visitor.visitListExpr((ExprTree.ListExpr) expr);
        } else if (expr instanceof ExprTree.MapExpr) {
            visitor.visitMapExpr((ExprTree.MapExpr) expr);
        } else if (expr instanceof ExprVar) {
            visitor.visitVariable((ExprVar) expr);
        } else if (expr instanceof ExprTree.CaseExpr) {
            visitor.visitCaseExpr((ExprTree.CaseExpr) expr);
        } else if (expr instanceof ExprTree.AliasExpr) {
            visitor.visitAliasExpr((ExprTree.AliasExpr) expr);
        } else {
            throw new RuntimeException("Unreachable code reached in walk.");
        }
    }

    // ==================================================================
    //                          FOLDERs
    // ==================================================================

    public interface Folder<T, E extends Throwable> {

        T foldBinaryOperator(ExprTree.LhsRhs expr) throws E;
        T foldUnaryOperator(ExprTree.Unary expr) throws E;
        T foldPropertyAccess(ExprTree.PropertyAccess expr) throws E;
        T foldVariable(ExprVar expr) throws E;
        T foldListExpr(ExprTree.ListExpr expr) throws E;
        T foldMapExpr(ExprTree.MapExpr expr) throws E;
        T foldLiteral(ConstVal.Literal expr) throws E;
        T foldFn(ExprFn expr) throws E;
        T foldCaseExpr(ExprTree.CaseExpr expr) throws E;
        T foldAliasExpr(ExprTree.AliasExpr expr) throws E;
    }

    public static class IdentityFolder<E extends Throwable> implements Folder<Expr, E> {

        @Override
        public Expr foldBinaryOperator(ExprTree.LhsRhs expr) throws E {
            if (expr instanceof ExprTree.Add) {
                return new ExprTree.Add(fold(this, expr.lhs), fold(this, expr.rhs));
            } else if (expr instanceof ExprTree.And) {
                return new ExprTree.And(fold(this, expr.lhs), fold(this, expr.rhs));
            } else if (expr instanceof ExprTree.Div) {
                return new ExprTree.Div(fold(this, expr.lhs), fold(this, expr.rhs));
            } else if (expr instanceof ExprTree.Eq) {
                return new ExprTree.Eq(fold(this, expr.lhs), fold(this, expr.rhs));
            } else if (expr instanceof ExprTree.GreaterThan) {
                return new ExprTree.GreaterThan(fold(this, expr.lhs), fold(this, expr.rhs));
            } else if (expr instanceof ExprTree.GreaterThanOrEqueal) {
                return new ExprTree.GreaterThanOrEqueal(fold(this, expr.lhs), fold(this, expr.rhs));
            } else if (expr instanceof ExprTree.In) {
                return new ExprTree.In(fold(this, expr.lhs), fold(this, expr.rhs));
            } else if (expr instanceof ExprTree.LessThan) {
                return new ExprTree.LessThan(fold(this, expr.lhs), fold(this, expr.rhs));
            } else if (expr instanceof ExprTree.LessThanOrEqual) {
                return new ExprTree.LessThanOrEqual(fold(this, expr.lhs), fold(this, expr.rhs));
            } else if (expr instanceof ExprTree.Mod) {
                return new ExprTree.Mod(fold(this, expr.lhs), fold(this, expr.rhs));
            } else if (expr instanceof ExprTree.Mul) {
                return new ExprTree.Mul(fold(this, expr.lhs), fold(this, expr.rhs));
            } else if (expr instanceof ExprTree.Neq) {
                return new ExprTree.Neq(fold(this, expr.lhs), fold(this, expr.rhs));
            } else if (expr instanceof ExprTree.Or) {
                return new ExprTree.Or(fold(this, expr.lhs), fold(this, expr.rhs));
            } else if (expr instanceof ExprTree.Pow) {
                return new ExprTree.Pow(fold(this, expr.lhs), fold(this, expr.rhs));
            } else if (expr instanceof ExprTree.Sub) {
                return new ExprTree.Sub(fold(this, expr.lhs), fold(this, expr.rhs));
            } else if (expr instanceof ExprTree.Xor) {
                return new ExprTree.Xor(fold(this, expr.lhs), fold(this, expr.rhs));
            } else {
                // With proper sum types this wouldn't be needed.
                throw new RuntimeException("Unreachable code reached!");
            }
        }

        @Override
        public Expr foldFn(ExprFn expr) throws E {
            List<Expr> args = new ArrayList<>();

            for (Expr oldArg: expr.args) {
                args.add(fold(this, oldArg));
            }
            return new ExprFn(expr.name, args);
        }

        @Override
        public Expr foldListExpr(ExprTree.ListExpr expr) throws E {
            ExprTree.ListExpr res = new ExprTree.ListExpr();

            for (Expr e: expr.exprs) {
                res.exprs.add(fold(this, e));
            }

            return res;
        }

        @Override
        public Expr foldMapExpr(ExprTree.MapExpr expr) throws E {
            ExprTree.MapExpr res = new ExprTree.MapExpr();

            for (Map.Entry<String, Expr> entry: expr.props.entrySet()) {
                res.props.put(entry.getKey(), fold(this, entry.getValue()));
            }

            return res;
        }

        @Override
        public Expr foldCaseExpr(ExprTree.CaseExpr expr) throws E {
            ExprTree.CaseExpr res = new ExprTree.CaseExpr();

            if (expr.caseExpr != null) {
                res.caseExpr = fold(this, expr.caseExpr);
            }

            for (Pair<Expr, Expr> when: expr.whenExprs) {
                Expr whenExpr = fold(this, when.getLeft());
                Expr thenExpr = fold(this, when.getRight());
                res.whenExprs.add(new ImmutablePair<>(whenExpr, thenExpr));
            }

            if (expr.defaultExpr != null) {
                res.defaultExpr = fold(this, expr.defaultExpr);
            }

            return res;
        }

        @Override
        public Expr foldLiteral(ConstVal.Literal expr) throws E {
            return expr;
        }

        @Override
        public Expr foldUnaryOperator(ExprTree.Unary expr) throws E {
            if (expr instanceof ExprTree.IsNotNull) {
                return new ExprTree.IsNotNull(fold(this, expr.unary));
            } else if (expr instanceof ExprTree.IsNull) {
                return new ExprTree.IsNull(fold(this, expr.unary));
            } else if (expr instanceof ExprTree.Not) {
                return new ExprTree.Not(fold(this, expr.unary));
            } else if (expr instanceof ExprTree.UnaryAdd) {
                return new ExprTree.UnaryAdd(fold(this, expr.unary));
            } else {
                throw new RuntimeException("Unreachable code reached!");
            }
        }

        @Override
        public Expr foldPropertyAccess(ExprTree.PropertyAccess expr) throws E {
            return new ExprTree.PropertyAccess(expr.propertyAccessed, fold(this, expr.expression));
        }

        @Override
        public Expr foldVariable(ExprVar expr) throws E {
            return new ExprVar(expr.var);
        }

        @Override
        public Expr foldAliasExpr(ExprTree.AliasExpr expr) throws E {
            return new ExprTree.AliasExpr(fold(this, expr.expr), expr.alias);
        }
    }

    public static <T, E extends Throwable> T fold(Folder<T, E> folder, Expr expr) throws E {
        if (expr == null) {
            return null;
        } else if (expr instanceof ExprTree.LhsRhs) {
            return folder.foldBinaryOperator((ExprTree.LhsRhs) expr);
        } else if (expr instanceof ExprTree.ListExpr) {
            return folder.foldListExpr((ExprTree.ListExpr) expr);
        } else if (expr instanceof ExprTree.Unary) {
            return folder.foldUnaryOperator((ExprTree.Unary) expr);
        } else if (expr instanceof ExprTree.PropertyAccess) {
            return folder.foldPropertyAccess((ExprTree.PropertyAccess) expr);
        } else if (expr instanceof ExprFn) {
            return folder.foldFn((ExprFn) expr);
        } else if (expr instanceof ConstVal.Literal) {
            return folder.foldLiteral((ConstVal.Literal) expr);
        } else if (expr instanceof ExprTree.MapExpr) {
            return folder.foldMapExpr((ExprTree.MapExpr) expr);
        } else if (expr instanceof ExprVar) {
            return folder.foldVariable((ExprVar) expr);
        } else if (expr instanceof ExprTree.CaseExpr) {
            return folder.foldCaseExpr((ExprTree.CaseExpr) expr);
        } else if (expr instanceof ExprTree.AliasExpr) {
            return folder.foldAliasExpr((ExprTree.AliasExpr) expr);
        } else {
            throw new RuntimeException("Unreahable code reached in walk.");
        }
    }
}
