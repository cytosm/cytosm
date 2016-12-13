package org.cytosm.cypher2sql.expandpaths;

import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.PatternElement;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.RelationshipChain;
import org.cytosm.cypher2sql.cypher.visitor.Walk;
import org.cytosm.cypher2sql.cypher.ast.expression.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * Visitor that collects information about variables being used
 * and provide a render form.
 */
public class VariableCollectorVisitor extends Walk.BaseExpressionVisitor {

    private final Map<String, Set<String>> variables = new HashMap<>();

    private Set<String> addVariable(final String name) {
        this.variables.putIfAbsent(name, new HashSet<>());
        return this.variables.get(name);
    }

    /**
     * Returns the list of variables found.
     * @return Returns the list of variables found.
     */
    public Map<String, Set<String>> getVars() {
        return this.variables;
    }

    /**
     * Traverse the provided limited Cypher AST. We look for all variables
     * uses and collect them.
     * @param expression is the AST to traverse.
     */
    public void collectVarInfos(final Expression expression) {
        Walk.walkExpression(this, expression);
    }

    /**
     * PatternElement are not expression but there are used in some of them such
     * as the ShortestPathExpression and PatternExpression.
     */
    private void tryOpVar(final Optional<Variable> pt) {
        if (pt.isPresent()) {
            this.collectVarInfos(pt.get());
        }
    }

    @Override
    public void visitProperty(Property a) {
        // TODO(Joan): I haven't seen any example where map() would not contain a Variable.
        // TODO(Joan): Is it because the parser needs to support legacy grammar??
        String varname = ((Variable) a.map).name;
        this.addVariable(varname).add(a.propertyKey.name);
    }

    @Override
    public void visitMapExpression(MapExpression mapExpression) {
        // FIXME
    }

    @Override
    public void visitVariable(Variable a) {
        this.addVariable(a.name);
    }

    @Override
    public void visitLiteral(Literal literal) {

    }

    @Override
    public void visitFunctionInvocation(FunctionInvocation a) {
        Iterator<Expression> iter = a.args.iterator();
        while (iter.hasNext()) {
            Walk.walkExpression(this, iter.next());
        }
    }

    @Override
    public void visitCaseExpression(CaseExpression a) {
        a.default_.ifPresent(d -> Walk.walkExpression(this, d));
        a.expression.ifPresent(d -> Walk.walkExpression(this, d));
        a.alternatives.stream().flatMap(p -> Stream.of(p.getKey(), p.getValue()))
                .forEach(al -> Walk.walkExpression(this, al));
    }

    @Override
    public void visitPatternExpression(PatternExpression a) {
        PatternElement c = a.element;
        do {
            RelationshipChain rel = (RelationshipChain) c;
            this.tryOpVar(rel.relationship.variable);
            this.tryOpVar(rel.rightNode.variable);
            c = rel.element;
        } while (c instanceof RelationshipChain);
    }
}
