package org.cytosm.cypher2sql.lowering.sqltree.visitor;

import static org.cytosm.cypher2sql.lowering.exceptions.fns.LambdaExceptionUtil.rethrowConsumer;
import static org.cytosm.cypher2sql.lowering.exceptions.fns.LambdaExceptionUtil.rethrowFunction;

import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;
import org.cytosm.cypher2sql.lowering.sqltree.*;
import org.cytosm.cypher2sql.lowering.sqltree.join.InnerJoin;
import org.cytosm.cypher2sql.lowering.sqltree.join.LeftJoin;
import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprWalk;

import java.util.stream.Collectors;

/**
 * Walking utilities.
 */
public class Walk {

    // ==================================================================
    //                          VISITORs
    // ==================================================================

    public interface SQLNodeVisitor<E extends Throwable> {
        void visitLeftJoin(LeftJoin leftJoin) throws E;
        void visitInnerJoin(InnerJoin innerJoin) throws E;
        void visitSimpleSelect(SimpleSelect simpleSelect) throws E;
        void visitScopeSelect(ScopeSelect scopeSelect) throws E;
        void visitUnionSelect(UnionSelect unionSelect) throws E;
        void visitWithSelect(WithSelect withSelect) throws E;
    }

    public static class BaseSQLNodeVisitor implements SQLNodeVisitor<Cypher2SqlException> {

        @Override
        public void visitLeftJoin(LeftJoin leftJoin) throws Cypher2SqlException {}

        @Override
        public void visitInnerJoin(InnerJoin innerJoin) throws Cypher2SqlException {}

        @Override
        public void visitSimpleSelect(SimpleSelect simpleSelect) throws Cypher2SqlException {
            if (simpleSelect instanceof SimpleSelectWithInnerJoins) {
                for (InnerJoin innerJoin: ((SimpleSelectWithInnerJoins) simpleSelect).joins) {
                    visitInnerJoin(innerJoin);
                }
            } else {
                for (LeftJoin leftJoin: ((SimpleSelectWithLeftJoins) simpleSelect).joins) {
                    visitLeftJoin(leftJoin);
                }
            }
        }

        @Override
        public void visitScopeSelect(ScopeSelect scopeSelect) throws Cypher2SqlException {
            scopeSelect.withQueries.forEach(rethrowConsumer(this::visitWithSelect));
            walkSQLNode(this, scopeSelect.ret);
        }

        @Override
        public void visitUnionSelect(UnionSelect unionSelect) throws Cypher2SqlException {
            for (SimpleOrScopeSelect s: unionSelect.unions) {
                walkSQLNode(this, s);
            }
        }

        @Override
        public void visitWithSelect(WithSelect withSelect) throws Cypher2SqlException {
            walkSQLNode(this, withSelect.subquery);
        }
    }

    public static abstract class BaseVisitorAndExprVisitor implements SQLNodeVisitor<Cypher2SqlException> {

        protected abstract ExprWalk.Visitor makeExprVisitor();

        @Override
        public void visitLeftJoin(LeftJoin leftJoin) throws Cypher2SqlException {}

        @Override
        public void visitInnerJoin(InnerJoin innerJoin) throws Cypher2SqlException {}

        @Override
        public void visitSimpleSelect(SimpleSelect simpleSelect) throws Cypher2SqlException {
            ExprWalk.Visitor visitor = makeExprVisitor();
            simpleSelect.exportedItems.forEach(rethrowConsumer(e -> ExprWalk.walk(visitor, e)));

            if (simpleSelect.whereCondition != null) {
                ExprWalk.walk(visitor, simpleSelect.whereCondition);
            }
            simpleSelect.orderBy.forEach(rethrowConsumer(
                    oi -> ExprWalk.walk(visitor, oi.item)
            ));

            if (simpleSelect instanceof SimpleSelectWithInnerJoins) {
                ((SimpleSelectWithInnerJoins) simpleSelect).joins.forEach(
                        rethrowConsumer(j -> ExprWalk.walk(visitor, j.condition))
                );
            } else {
                ((SimpleSelectWithLeftJoins) simpleSelect).joins.forEach(
                        rethrowConsumer(j -> ExprWalk.walk(visitor, j.condition))
                );
            }
        }

        @Override
        public void visitScopeSelect(ScopeSelect scopeSelect) throws Cypher2SqlException {
            scopeSelect.withQueries.forEach(rethrowConsumer(this::visitWithSelect));
            walkSQLNode(this, scopeSelect.ret);
        }

        @Override
        public void visitUnionSelect(UnionSelect unionSelect) throws Cypher2SqlException {
            for (SimpleOrScopeSelect s: unionSelect.unions) {
                walkSQLNode(this, s);
            }
        }

        @Override
        public void visitWithSelect(WithSelect withSelect) throws Cypher2SqlException {
            walkSQLNode(this, withSelect.subquery);
        }
    }

    public static abstract class BaseVisitorAndExprFolder implements SQLNodeVisitor<Cypher2SqlException>
    {

        protected abstract ExprWalk.IdentityFolder<Cypher2SqlException> makeExprFolder(SimpleSelect context);

        @Override
        public void visitLeftJoin(LeftJoin leftJoin) throws Cypher2SqlException {}

        @Override
        public void visitInnerJoin(InnerJoin innerJoin) throws Cypher2SqlException {}

        @Override
        public void visitSimpleSelect(SimpleSelect simpleSelect) throws Cypher2SqlException {
            ExprWalk.IdentityFolder<Cypher2SqlException> folder = makeExprFolder(simpleSelect);
            simpleSelect.exportedItems = simpleSelect.exportedItems.stream()
                    .<Expr>map(rethrowFunction(
                            e -> ExprWalk.<Expr, Cypher2SqlException>fold(folder, e)
                    )).collect(Collectors.toList());

            if (simpleSelect.whereCondition != null) {
                simpleSelect.whereCondition = ExprWalk.fold(folder, simpleSelect.whereCondition);
            }
            simpleSelect.orderBy.forEach(rethrowConsumer(
                oi -> oi.item = ExprWalk.<Expr, Cypher2SqlException>fold(folder, oi.item)
            ));

            if (simpleSelect instanceof SimpleSelectWithInnerJoins) {
                ((SimpleSelectWithInnerJoins) simpleSelect).joins.forEach(
                        rethrowConsumer(j -> j.condition = ExprWalk.fold(folder, j.condition))
                );
            } else {
                ((SimpleSelectWithLeftJoins) simpleSelect).joins.forEach(
                        rethrowConsumer(j -> j.condition = ExprWalk.fold(folder, j.condition))
                );
            }
        }

        @Override
        public void visitScopeSelect(ScopeSelect scopeSelect) throws Cypher2SqlException {
            scopeSelect.withQueries.forEach(rethrowConsumer(this::visitWithSelect));
            walkSQLNode(this, scopeSelect.ret);
        }

        @Override
        public void visitUnionSelect(UnionSelect unionSelect) throws Cypher2SqlException {
            for (SimpleOrScopeSelect s: unionSelect.unions) {
                walkSQLNode(this, s);
            }
        }

        @Override
        public void visitWithSelect(WithSelect withSelect) throws Cypher2SqlException {
            walkSQLNode(this, withSelect.subquery);
        }
    }

    public static <E extends Throwable> void walkSQLNode(final SQLNodeVisitor<E> visitor, final SQLNode node) throws E {
        if (node instanceof InnerJoin) {
            visitor.visitInnerJoin((InnerJoin) node);
        } else if (node instanceof LeftJoin) {
            visitor.visitLeftJoin((LeftJoin) node);
        } else if (node instanceof SimpleSelect) {
            visitor.visitSimpleSelect((SimpleSelect) node);
        } else if (node instanceof ScopeSelect) {
            visitor.visitScopeSelect((ScopeSelect) node);
        } else if (node instanceof UnionSelect) {
            visitor.visitUnionSelect((UnionSelect) node);
        } else if (node instanceof WithSelect) {
            visitor.visitWithSelect((WithSelect) node);
        }
    }

    // ==================================================================
    //                          FOLDERs
    // ==================================================================


    public interface Folder<T, E extends Throwable> {

        T foldSimpleSelect(SimpleSelect select) throws E;
        T foldScopeSelect(ScopeSelect scopeSelect) throws E;
        T foldUnionSelect(UnionSelect unionSelect) throws E;
        T foldWithSelect(WithSelect withSelect) throws E;
    }

    public static class IdentityFolder<E extends Throwable> implements Folder<BaseSelect, E> {
        @Override
        public BaseSelect foldSimpleSelect(SimpleSelect select) throws E {
            return select;
        }

        @Override
        public BaseSelect foldScopeSelect(ScopeSelect scopeSelect) throws E {
            ScopeSelect result = new ScopeSelect();
            for (WithSelect withSelect: scopeSelect.withQueries) {
                result.withQueries.add((WithSelect) fold(this, withSelect));
            }
            result.ret = (SimpleSelect) fold(this, scopeSelect.ret);
            return result;
        }

        @Override
        public BaseSelect foldUnionSelect(UnionSelect unionSelect) throws E {
            UnionSelect result = new UnionSelect();
            for (SimpleOrScopeSelect child: unionSelect.unions) {
                result.unions.add((SimpleOrScopeSelect) fold(this, child));
            }
            return result;
        }

        @Override
        public BaseSelect foldWithSelect(WithSelect withSelect) throws E {
            WithSelect result = new WithSelect(fold(this, withSelect.subquery));
            result.subqueryName = withSelect.subqueryName;
            return result;
        }
    }

    public static <T, E extends Throwable> T fold(Folder<T, E> folder, SQLNode node) throws E {
        if (node instanceof SimpleSelect) {
            return folder.foldSimpleSelect((SimpleSelect) node);
        } else if (node instanceof ScopeSelect) {
            return folder.foldScopeSelect((ScopeSelect) node);
        } else if (node instanceof UnionSelect) {
            return folder.foldUnionSelect((UnionSelect) node);
        } else if (node instanceof WithSelect) {
            return folder.foldWithSelect((WithSelect) node);
        } else {
            throw new RuntimeException("Unreachable code reached");
        }
    }
}
