package org.cytosm.cypher2sql.cypher.visitor;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.cytosm.cypher2sql.cypher.ast.*;
import org.cytosm.cypher2sql.cypher.ast.clause.*;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.*;
import org.cytosm.cypher2sql.cypher.ast.clause.projection.*;
import org.cytosm.cypher2sql.cypher.ast.clause.match.*;
import org.cytosm.cypher2sql.cypher.ast.expression.*;
import org.cytosm.cypher2sql.cypher.ast.expression.Binary.*;

import java.util.*;

/**
 * Helpers to write visitors and folders on the AST.
 */
public class Walk {
    private static final Logger LOGGER = Logger.getLogger(Walk.class.getName());

    // ==================================================================
    //                          VISITORs
    // ==================================================================

    public interface ObjectVisitor {
        void visitOptional(final Optional<?> some);
        void visitList(final List<?> list);
        void visitPair(final Pair<?, ?> scalaTuple2);
    }

    public interface LiteralVisitor extends ObjectVisitor {
        void visitStringLiteral(final Literal.StringLiteral stringLiteral);
        void visitDecimalDoubleLiteral(final Literal.DecimalDouble decimalDoubleLiteral);
        void visitUnsignedIntegerLiteral(final Literal.Integer unsignedDecimalIntegerLiteral);
    }

    public interface ExpressionVisitor extends ObjectVisitor {
        void visitGreaterThan(final GreaterThan greaterThan);
        void visitLessThan(final LessThan lessThan);
        void visitEquals(final Equals equals);
        void visitAnd(final And and);
        void visitOr(final Or or);
        void visitProperty(final Property property);
        void visitIn(final In in);
        void visitListExpression(final ListExpression collection);
        void visitNot(final Unary.Not not);
        void visitInvalidNotEquals(final InvalidNotEquals invalidNotEquals);
        void visitMapExpression(final MapExpression mapExpression);
        void visitVariable(final Variable variable);
        void visitLiteral(final Literal literal);
        void visitFunctionInvocation(final FunctionInvocation functionInvocation);
        void visitCaseExpression(final CaseExpression caseExpression);
        void visitIsNull(final Unary.IsNull isNull);
        void visitGreaterThanOrEqual(final GreaterThanOrEqual greaterThanOrEqual);
        void visitLessThanOrEqual(final LessThanOrEqual lessThanOrEqual);
        void visitPatternExpression(final PatternExpression patternExpression);
        void visitUnaryAdd(Unary.Add expression);
        void visitXor(Xor expression);
        void visitSubtract(Subtract expression);
        void visitAdd(Add expression);
        void visitDivide(Divide expression);
        void visitMultiply(Multiply expression);
        void visitModulo(Modulo expression);
        void visitPow(Pow expression);
        void visitNotEquals(NotEquals expression);
        void visitRegexMatch(RegexMatch expression);
        void visitStartsWith(StartsWith expression);
        void visitEndsWith(EndsWith expression);
        void visitIsNotNull(Unary.IsNotNull expression);
    }

    public interface ReturnItemVisitor extends ObjectVisitor {
        void visitUnaliasedReturnItem(final ReturnItem.Unaliased unaliasedReturnItem);
        void visitAliasedReturnItem(final ReturnItem.Aliased aliasedReturnItem);
    }

    public interface UnionVisitor {
        void visitUnionDistinct(final Union.Distinct obj);
        void visitUnionAll(final Union.All obj);
    }

    public interface RootVisitor extends ObjectVisitor {
        void visitUnion(final Union union);
        void visitQuery(final Query query);
        void visitSingleQuery(final SingleQuery singleQuery);
        void visitMatch(final Match match);
        void visitPattern(final Pattern pattern);
        void visitReturn(final Return ret);
        void visitNodePattern(final NodePattern nodePattern);
        void visitPropertyKeyName(final PropertyKeyName propertyKeyName);
        void visitRelationshipChain(final RelationshipChain relationshipChain);
        void visitOrderBy(final OrderBy orderBy);
        void visitLabelName(final LabelName labelName);
        void visitRelationshipPattern(final RelationshipPattern relationshipPattern);
        void visitRelTypeName(final RelTypeName relTypeName);
        void visitWhere(final Where where);
        void visitAscSortItem(final SortItem.Asc ascSortItem);
        void visitDescSortItem(final SortItem.Desc descSortItem);
        void visitRange(final Range range);
        void visitReturnItem(final ReturnItem returnItem);
        void visitLimit(final Limit limit);
        void visitWith(final With with);
        void visitExpression(final Expression expression);
        void visitLiteral(final Literal literal);
    }

    /**
     * Walk a supposed Cypher AST, but where the object might
     * not be an ASTNode such as a list of ASTNode or an <pre>Option<ASTNode></pre>
     * @param visitor is the visitor to use.
     * @param obj the object to inspect.
     */
    public static void walk(final RootVisitor visitor, final Object obj) {
        if (obj instanceof ASTNode) {
            walkASTNode(visitor, (ASTNode) obj);
        } else {
            walkOtherObject(visitor, obj);
        }
    }

    public static void walkUnion(final UnionVisitor visitor, final Union obj) {
        if (obj instanceof Union.Distinct) {
            visitor.visitUnionDistinct((Union.Distinct) obj);
        } else if (obj instanceof Union.All) {
            visitor.visitUnionAll((Union.All) obj);
        }
    }

    /**
     * Pattern match the passed ASTNode but stops at Expression and Literal.
     * This reduce the boilerplate to write a Visitor that is only interested
     * in some bits.
     * @param visitor is the visitor that will be called.
     * @param obj is the node to inspect.
     */
    public static void walkASTNode(final RootVisitor visitor, final ASTNode obj) {

        if (obj instanceof SingleQuery) {
            visitor.visitSingleQuery((SingleQuery) obj);
        } else if (obj instanceof Union) {
            visitor.visitUnion((Union) obj);
        } else if (obj instanceof Literal) {
            visitor.visitLiteral((Literal) obj);
        } else if (obj instanceof Match) {
            visitor.visitMatch((Match) obj);
        } else if (obj instanceof Return) {
            visitor.visitReturn((Return) obj);
        } else if (obj instanceof Pattern) {
            visitor.visitPattern((Pattern) obj);
        } else if (obj instanceof NodePattern) {
            visitor.visitNodePattern((NodePattern) obj);
        } else if (obj instanceof Expression) {
            visitor.visitExpression((Expression) obj);
        } else if (obj instanceof PropertyKeyName) {
            visitor.visitPropertyKeyName((PropertyKeyName) obj);
        } else if (obj instanceof RelationshipChain) {
            visitor.visitRelationshipChain((RelationshipChain) obj);
        } else if (obj instanceof LabelName) {
            visitor.visitLabelName((LabelName) obj);
        } else if (obj instanceof RelationshipPattern) {
            visitor.visitRelationshipPattern((RelationshipPattern) obj);
        } else if (obj instanceof OrderBy) {
            visitor.visitOrderBy((OrderBy) obj);
        } else if (obj instanceof RelTypeName) {
            visitor.visitRelTypeName((RelTypeName) obj);
        } else if (obj instanceof Where) {
            visitor.visitWhere((Where) obj);
        } else if (obj instanceof Query) {
            visitor.visitQuery((Query) obj);
        } else if (obj instanceof SortItem.Asc) {
            visitor.visitAscSortItem((SortItem.Asc) obj);
        } else if (obj instanceof SortItem.Desc) {
            visitor.visitDescSortItem((SortItem.Desc) obj);
        } else if (obj instanceof Range) {
            visitor.visitRange((Range) obj);
        } else if (obj instanceof ReturnItem) {
            visitor.visitReturnItem((ReturnItem) obj);
        } else if (obj instanceof Limit) {
            visitor.visitLimit((Limit) obj);
        } else if (obj instanceof With) {
            visitor.visitWith((With) obj);
        } else {
            LOGGER.warn("UNKNOWN --> " + obj);
        }
    }

    /**
     * Pattern match the passed Expression but stops at Literal(s).
     * This reduce the boilerplate to write a Visitor that is only interested
     * in Expressions.
     * @param visitor is the visitor that will be called.
     * @param expression is the node to inspect.
     */
    public static void walkExpression(ExpressionVisitor visitor, Expression expression) {
        //  Literal includes:
        //    │
        //    ├─ String Literal
        //    │
        //    ├─ Number Literal
        //    │  ├─ IntegerLiteral
        //    │  │  ├─ ...
        //    │  │  ├─ SignedHexIntegerLiteral       (might not be supported in sql)
        //    │  │  ├─ SignedDecimalIntegerLiteral
        //    │  │  ├─ UnsigedDecimalIntegerLiteral
        //    │  │  └─ SignedOctalIntegerLiteral     (might not be supported in sql)
        //    │  └─ DoubleLiteral
        //    │     └─ DecimalDoubleLiteral
        //    │
        //    ├─ Boolean Literal
        //    │  ├─ False
        //    │  └─ True
        //    │
        //    └─ Null

        if (expression instanceof Literal) {
            visitor.visitLiteral((Literal) expression);
        } else if (expression instanceof PatternExpression) {
            visitor.visitPatternExpression((PatternExpression) expression);
        } else if (expression instanceof Property) {
            visitor.visitProperty((Property) expression);
        } else if (expression instanceof Variable) {
            visitor.visitVariable((Variable) expression);

        // N-ary operators
        } else if (expression instanceof CaseExpression) {
            visitor.visitCaseExpression((CaseExpression) expression);
        } else if (expression instanceof FunctionInvocation) {
            visitor.visitFunctionInvocation((FunctionInvocation) expression);
        } else if (expression instanceof ListExpression) {
            visitor.visitListExpression((ListExpression) expression);
        } else if (expression instanceof MapExpression) {
            visitor.visitMapExpression((MapExpression) expression);

        // Binary operators
        } else if (expression instanceof Xor) {
            visitor.visitXor((Xor) expression);
        } else if (expression instanceof And) {
            visitor.visitAnd((And) expression);
        } else if (expression instanceof Or) {
            visitor.visitOr((Or) expression);
        } else if (expression instanceof Subtract) {
            visitor.visitSubtract((Subtract) expression);
        } else if (expression instanceof Add) {
            visitor.visitAdd((Add) expression);
        } else if (expression instanceof Divide) {
            visitor.visitDivide((Divide) expression);
        } else if (expression instanceof Multiply) {
            visitor.visitMultiply((Multiply) expression);
        } else if (expression instanceof Modulo) {
            visitor.visitModulo((Modulo) expression);
        } else if (expression instanceof Pow) {
            visitor.visitPow((Pow) expression);
        } else if (expression instanceof Equals) {
            visitor.visitEquals((Equals) expression);
        } else if (expression instanceof InvalidNotEquals) {
            visitor.visitInvalidNotEquals((InvalidNotEquals) expression);
        } else if (expression instanceof NotEquals) {
            visitor.visitNotEquals((NotEquals) expression);
        } else if (expression instanceof LessThan) {
            visitor.visitLessThan((LessThan) expression);
        } else if (expression instanceof LessThanOrEqual) {
            visitor.visitLessThanOrEqual((LessThanOrEqual) expression);
        } else if (expression instanceof GreaterThan) {
            visitor.visitGreaterThan((GreaterThan) expression);
        } else if (expression instanceof GreaterThanOrEqual) {
            visitor.visitGreaterThanOrEqual((GreaterThanOrEqual) expression);
        } else if (expression instanceof RegexMatch) {
            visitor.visitRegexMatch((RegexMatch) expression);
        } else if (expression instanceof StartsWith) {
            visitor.visitStartsWith((StartsWith) expression);
        } else if (expression instanceof EndsWith) {
            visitor.visitEndsWith((EndsWith) expression);
        } else if (expression instanceof In) {
            visitor.visitIn((In) expression);

            // Unary operators
        } else if (expression instanceof Unary.Not) {
            visitor.visitNot((Unary.Not) expression);
        } else if (expression instanceof Unary.IsNull) {
            visitor.visitIsNull((Unary.IsNull) expression);
        } else if (expression instanceof Unary.IsNotNull) {
            visitor.visitIsNotNull((Unary.IsNotNull) expression);
        } else if (expression instanceof Unary.Add) {
            visitor.visitUnaryAdd((Unary.Add) expression);

        } else {
            LOGGER.warn("UNKNOWN ASTNode --> " + expression);
        }
    }

    /**
     * Pattern match the passed Literal.
     * @param visitor is the visitor that will be called.
     * @param obj is the literal to inspect.
     */
    public static void walkLiteral(LiteralVisitor visitor, Literal obj) {
        if (obj instanceof Literal.StringLiteral) {
            visitor.visitStringLiteral((Literal.StringLiteral) obj);
        } else if (obj instanceof Literal.Integer) {
            visitor.visitUnsignedIntegerLiteral((Literal.Integer) obj);
        } else if (obj instanceof Literal.DecimalDouble) {
            visitor.visitDecimalDoubleLiteral((Literal.DecimalDouble) obj);
        } else {
            LOGGER.warn("UNKNOWN Literal --> " + obj);
        }
    }

    public static void walkReturnItem(ReturnItemVisitor visitor, ReturnItem obj) {
        if (obj instanceof ReturnItem.Aliased) {
            visitor.visitAliasedReturnItem((ReturnItem.Aliased) obj);
        } else if (obj instanceof ReturnItem.Unaliased) {
            visitor.visitUnaliasedReturnItem((ReturnItem.Unaliased) obj);
        }
    }

    private static void walkOtherObject(ObjectVisitor visitor, Object obj) {
        if (obj instanceof Pair) {
            visitor.visitPair((Pair<?, ?>) obj);
        } else if (obj instanceof List) {
            visitor.visitList((List<?>) obj);
        } else if (obj instanceof Optional) {
            visitor.visitOptional((Optional<?>) obj);
        } else {
            LOGGER.warn("UNKNOWN Object --> " + obj);
        }
    }

    /**
     * A base root visitor that dispatch on all nodes by default.
     */
    public abstract static class BaseRootVisitor implements RootVisitor {

        @Override
        public void visitUnion(final Union union) {}

        @Override
        public void visitPair(final Pair<?, ?> pair) {
            Walk.walk(this, pair.getLeft());
            Walk.walk(this, pair.getValue());
        }

        @Override
        public void visitRange(final Range range) {}

        @Override
        public void visitOptional(final Optional<?> opt) {
            if (opt.isPresent()) {
                Walk.walk(this, opt.get());
            }
        }

        @Override
        public void visitRelTypeName(final RelTypeName relTypeName) {}

        @Override
        public void visitLiteral(final Literal stringLiteral) {}

        @Override
        public void visitLabelName(final LabelName labelName) {}

        @Override
        public void visitAscSortItem(final SortItem.Asc ascSortItem) {}

        @Override
        public void visitDescSortItem(final SortItem.Desc descSortItem) {}

        @Override
        public void visitRelationshipPattern(final RelationshipPattern relationshipPattern) {
            Walk.walk(this, relationshipPattern.length);
            Walk.walk(this, relationshipPattern.properties);
            Walk.walk(this, relationshipPattern.variable);
            Walk.walk(this, relationshipPattern.types);
        }

        @Override
        public void visitWhere(final Where where) {
            Walk.walk(this, where.expression);
        }

        @Override
        public void visitOrderBy(final OrderBy orderBy) {
            Walk.walk(this, orderBy.sortItems);
        }

        @Override
        public void visitLimit(final Limit limit) {
            Walk.walk(this, limit.expression);
        }

        @Override
        public void visitWith(final With with) {
            Walk.walk(this, with.returnItems);
            Walk.walk(this, with.where);
            Walk.walk(this, with.orderBy);
            Walk.walk(this, with.skip);
            Walk.walk(this, with.limit);
        }

        @Override
        public void visitQuery(final Query query) {
            Walk.walk(this, query.part);
        }

        @Override
        public void visitSingleQuery(final SingleQuery singleQuery) {
            Walk.walk(this, singleQuery.clauses);
        }

        @Override
        public void visitList(final List<?> list) {
            list.forEach(obj -> Walk.walk(this, obj));
        }

        @Override
        public void visitMatch(final Match match) {
            Walk.walk(this, match.pattern);
            Walk.walk(this, match.where);
        }

        @Override
        public void visitPattern(final Pattern pattern) {
            Walk.walk(this, pattern.patternParts);
        }

        @Override
        public void visitReturn(final Return ret) {
            Walk.walk(this, ret.returnItems);
            Walk.walk(this, ret.orderBy);
            Walk.walk(this, ret.skip);
            Walk.walk(this, ret.limit);
        }

        @Override
        public void visitReturnItem(ReturnItem returnItem) {
            Walk.walk(this, returnItem.expression);
        }

        @Override
        public void visitNodePattern(final NodePattern nodePattern) {
            Walk.walk(this, nodePattern.variable);
            Walk.walk(this, nodePattern.labels);
            Walk.walk(this, nodePattern.properties);
        }

        @Override
        public void visitPropertyKeyName(final PropertyKeyName propertyKeyName) {}

        @Override
        public void visitRelationshipChain(final RelationshipChain relationshipChain) {
            Walk.walk(this, relationshipChain.element);
            Walk.walk(this, relationshipChain.relationship);
            Walk.walk(this, relationshipChain.rightNode);
        }

    }

    /**
     * A base expression visitor that does dispatching on operators
     * that don't carry much information. Very useful to write simple visitor
     * that collect some specific information such as the list of variables
     * being used.
     */
    public abstract static class BaseExpressionVisitor implements ExpressionVisitor {

        @Override
        public void visitOptional(Optional<?> opt) {
            if (opt.isPresent()) {
                Walk.walkExpression(this, (Expression) opt.get());
            }
        }

        @Override
        public void visitPair(Pair<?, ?> pair) {
            Walk.walkExpression(this, (Expression) pair.getLeft());
            Walk.walkExpression(this, (Expression) pair.getRight());
        }

        @Override
        public void visitList(List<?> list) {
            Iterator<?> iter = list.iterator();
            while (iter.hasNext()) {
                Walk.walkExpression(this, (Expression) iter.next());
            }
        }

        @Override
        public void visitGreaterThan(GreaterThan expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitLessThan(LessThan expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitEquals(Equals expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitAnd(And expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitOr(Or expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitXor(Xor expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitSubtract(Subtract expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitAdd(Add expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitDivide(Divide expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitMultiply(Multiply expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitModulo(Modulo expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitPow(Pow expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitNotEquals(NotEquals expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitGreaterThanOrEqual(GreaterThanOrEqual expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitLessThanOrEqual(LessThanOrEqual expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitInvalidNotEquals(InvalidNotEquals expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitRegexMatch(RegexMatch expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitStartsWith(StartsWith expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitEndsWith(EndsWith expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitIn(In expression) {
            Walk.walkExpression(this, expression.lhs);
            Walk.walkExpression(this, expression.rhs);
        }

        @Override
        public void visitNot(Unary.Not expression) {
            Walk.walkExpression(this, expression.lhs);
        }

        @Override
        public void visitUnaryAdd(Unary.Add expression) {
            Walk.walkExpression(this, expression.lhs);
        }

        @Override
        public void visitIsNull(Unary.IsNull expression) {
            Walk.walkExpression(this, expression.lhs);
        }

        @Override
        public void visitIsNotNull(Unary.IsNotNull expression) {
            Walk.walkExpression(this, expression.lhs);
        }

        @Override
        public void visitListExpression(ListExpression expression) {
            Iterator<Expression> iter = expression.exprs.iterator();
            while (iter.hasNext()) {
                Walk.walkExpression(this, iter.next());
            }
        }
    }

    // ==================================================================
    //                          FOLDERs
    // ==================================================================

    public interface ClauseFolder<T, E extends Throwable> {
        T foldMatch(final Match match) throws E;
        T foldReturn(final Return ret) throws E;
        T foldWith(final With with) throws E;
    }

    public interface ExpressionFolder<T, E extends Throwable> {
        T foldGreaterThan(final GreaterThan expression) throws E;
        T foldLessThan(final LessThan expression) throws E;
        T foldEquals(final Equals expression) throws E;
        T foldAnd(final And  expression) throws E;
        T foldOr(final Or  expression) throws E;
        T foldProperty(final Property  expression) throws E;
        T foldIn(final In  expression) throws E;
        T foldListExpression(final ListExpression expression) throws E;
        T foldNot(final Unary.Not  expression) throws E;
        T foldInvalidNotEquals(final InvalidNotEquals  expression) throws E;
        T foldMapExpression(final MapExpression  expression) throws E;
        T foldVariable(final Variable  expression) throws E;
        T foldFunctionInvocation(final FunctionInvocation  expression) throws E;
        T foldCaseExpression(final CaseExpression  expression) throws E;
        T foldIsNull(final Unary.IsNull  expression) throws E;
        T foldGreaterThanOrEqual(final GreaterThanOrEqual  expression) throws E;
        T foldLessThanOrEqual(final LessThanOrEqual  expression) throws E;
        T foldPatternExpression(final PatternExpression  expression) throws E;
        T foldUnaryAdd(Unary.Add  expression) throws E;
        T foldXor(Xor expression) throws E;
        T foldSubtract(Subtract  expression) throws E;
        T foldAdd(Add  expression) throws E;
        T foldDivide(Divide  expression) throws E;
        T foldMultiply(Multiply  expression) throws E;
        T foldModulo(Modulo  expression) throws E;
        T foldPow(Pow  expression) throws E;
        T foldNotEquals(NotEquals  expression) throws E;
        T foldRegexMatch(RegexMatch  expression) throws E;
        T foldStartsWith(StartsWith  expression) throws E;
        T foldEndsWith(EndsWith  expression) throws E;
        T foldIsNotNull(Unary.IsNotNull  expression) throws E;
        T foldStringLiteral(final Literal.StringLiteral stringLiteral) throws E;
        T foldDecimalDoubleLiteral(final Literal.DecimalDouble decimalDoubleLiteral) throws E;
        T foldUnsignedDecimalIntegerLiteral(final Literal.Integer unsignedDecimalIntegerLiteral) throws E;
    }

    /**
     * Fold the given Cypher tree using the provided folder.
     * @param folder is the folder that will be used.
     * @param obj is the node to fold.
     */
    public static <T, E extends Throwable> T foldClause(ClauseFolder<T, E> folder, Clause obj) throws E {
        if (obj instanceof Match) {
            return folder.foldMatch((Match) obj);
        } else if (obj instanceof Return) {
            return folder.foldReturn((Return) obj);
        } else if (obj instanceof With) {
            return folder.foldWith((With) obj);
        } else {
            throw new RuntimeException("UNKNOWN --> " + obj);
        }
    }

    /**
     * Fold the given expression using the provided folder.
     * @param folder is the folder that will be used.
     * @param expression is the node to fold.
     */
    public static <T, E extends Throwable> T foldExpression(ExpressionFolder<T, E> folder, Object expression) throws E {
        //  Literal includes:
        //    │
        //    ├─ String Literal
        //    │
        //    ├─ Number Literal
        //    │  ├─ IntegerLiteral
        //    │  │  ├─ ...
        //    │  │  ├─ SignedHexIntegerLiteral       (might not be supported in sql)
        //    │  │  ├─ SignedDecimalIntegerLiteral
        //    │  │  ├─ UnsigedDecimalIntegerLiteral
        //    │  │  └─ SignedOctalIntegerLiteral     (might not be supported in sql)
        //    │  └─ DoubleLiteral
        //    │     └─ DecimalDoubleLiteral
        //    │
        //    ├─ Boolean Literal
        //    │  ├─ False
        //    │  └─ True
        //    │
        //    └─ Null

        if (expression instanceof Literal.StringLiteral) {
            return folder.foldStringLiteral((Literal.StringLiteral) expression);
        } else if (expression instanceof Literal.DecimalDouble) {
            return folder.foldDecimalDoubleLiteral((Literal.DecimalDouble) expression);
        } else if (expression instanceof Literal.Integer) {
            return folder.foldUnsignedDecimalIntegerLiteral((Literal.Integer) expression);

        } else if (expression instanceof PatternExpression) {
            return folder.foldPatternExpression((PatternExpression) expression);
        } else if (expression instanceof Property) {
            return folder.foldProperty((Property) expression);
        } else if (expression instanceof Variable) {
            return folder.foldVariable((Variable) expression);

            // N-ary operators
        } else if (expression instanceof CaseExpression) {
            return folder.foldCaseExpression((CaseExpression) expression);
        } else if (expression instanceof FunctionInvocation) {
            return folder.foldFunctionInvocation((FunctionInvocation) expression);
        } else if (expression instanceof ListExpression) {
            return folder.foldListExpression((ListExpression) expression);
        } else if (expression instanceof MapExpression) {
            return folder.foldMapExpression((MapExpression) expression);

            // Binary operators
        } else if (expression instanceof Xor) {
            return folder.foldXor((Xor) expression);
        } else if (expression instanceof And) {
            return folder.foldAnd((And) expression);
        } else if (expression instanceof Or) {
            return folder.foldOr((Or) expression);
        } else if (expression instanceof Subtract) {
            return folder.foldSubtract((Subtract) expression);
        } else if (expression instanceof Add) {
            return folder.foldAdd((Add) expression);
        } else if (expression instanceof Divide) {
            return folder.foldDivide((Divide) expression);
        } else if (expression instanceof Multiply) {
            return folder.foldMultiply((Multiply) expression);
        } else if (expression instanceof Modulo) {
            return folder.foldModulo((Modulo) expression);
        } else if (expression instanceof Pow) {
            return folder.foldPow((Pow) expression);
        } else if (expression instanceof Equals) {
            return folder.foldEquals((Equals) expression);
        } else if (expression instanceof InvalidNotEquals) {
            return folder.foldInvalidNotEquals((InvalidNotEquals) expression);
        } else if (expression instanceof NotEquals) {
            return folder.foldNotEquals((NotEquals) expression);
        } else if (expression instanceof LessThan) {
            return folder.foldLessThan((LessThan) expression);
        } else if (expression instanceof LessThanOrEqual) {
            return folder.foldLessThanOrEqual((LessThanOrEqual) expression);
        } else if (expression instanceof GreaterThan) {
            return folder.foldGreaterThan((GreaterThan) expression);
        } else if (expression instanceof GreaterThanOrEqual) {
            return folder.foldGreaterThanOrEqual((GreaterThanOrEqual) expression);
        } else if (expression instanceof RegexMatch) {
            return folder.foldRegexMatch((RegexMatch) expression);
        } else if (expression instanceof StartsWith) {
            return folder.foldStartsWith((StartsWith) expression);
        } else if (expression instanceof EndsWith) {
            return folder.foldEndsWith((EndsWith) expression);
        } else if (expression instanceof In) {
            return folder.foldIn((In) expression);

            // Unary operators
        } else if (expression instanceof Unary.Not) {
            return folder.foldNot((Unary.Not) expression);
        } else if (expression instanceof Unary.IsNull) {
            return folder.foldIsNull((Unary.IsNull) expression);
        } else if (expression instanceof Unary.IsNotNull) {
            return folder.foldIsNotNull((Unary.IsNotNull) expression);
        } else if (expression instanceof Unary.Add) {
            return folder.foldUnaryAdd((Unary.Add) expression);

        } else {
            throw new RuntimeException("UNKNOWN ASTNode --> " + expression);
        }
    }

}
