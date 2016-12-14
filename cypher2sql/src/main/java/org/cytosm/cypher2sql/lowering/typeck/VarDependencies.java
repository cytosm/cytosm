package org.cytosm.cypher2sql.lowering.typeck;

import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprVar;
import org.cytosm.cypher2sql.lowering.typeck.rel.Relationship;
import org.cytosm.cypher2sql.lowering.typeck.var.*;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTree;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTreeBuilder;
import org.cytosm.cypher2sql.cypher.ast.*;
import org.cytosm.cypher2sql.cypher.ast.clause.*;
import org.cytosm.cypher2sql.cypher.ast.clause.match.*;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.*;
import org.cytosm.cypher2sql.cypher.ast.clause.projection.*;

import java.util.*;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Data-structure that compute and encapsulate dependencies between
 * variables defined by a particular Cypher Query.
 *
 * It provides a mapping between each clauseId and the list of possible variables.
 * Reference are shared between list when the variables is actually the same.
 * A variable's name is insufficient because WITH clauses introduce a scope and
 * can also rename variables.
 *
 * The list of Var is either new var being declared or an expression that can
 * be made of constants and other variables previously defined.
 *
 * There are many possible
 *
 * In case of dependency error, the constructor throw a RuntimeException with
 * an error message that tries to explain the source of the error.
 *
 *
 * There's room for improvements at this level.
 */
public class VarDependencies {

    /**
     * List of variables used and defined by a particular clause.
     */
    private Map<ClauseId, List<Var>> usedVariablesInClause = new HashMap<>();
    /**
     * List of variables reachable by a particular clause.
     * This map also includes the Return clause Id (which is not the case
     * of the previous map).
     */
    private Map<ClauseId, AvailableVariables> reachableVariables = new HashMap<>();
    /**
     * List of relationships that occurs at a certain clause.
     */
    private Map<ClauseId, List<Relationship>> relationships = new HashMap<>();
    /**
     * The Return does not have a clause Id, but does have a list
     * of variables expression
     */
    private List<Expr> returnExprs = new ArrayList<>();

    public VarDependencies(final Statement statement) {
        ClauseVisitor visitor = new ClauseVisitor(
                this.usedVariablesInClause, this.reachableVariables,
                this.returnExprs, this.relationships);
        visitor.visitQuery((SingleQuery) statement.query.part);
    }

    /**
     * Return the list of variables used by that clause.
     * @param clauseId is the clause id.
     * @return Returns the list of variables.
     */
    public List<Var> getUsedVars(ClauseId clauseId) {
        return Collections.unmodifiableList(this.usedVariablesInClause.get(clauseId));
    }

    /**
     * Returns the list of all variables. No variables exists beyond those ones.
     * This method only purpose is for testing. Some tests might want
     * to verify properties without caring about the visibility of variables.
     * Do NOT use this method for other purposes.
     *
     * @return Returns a list of all variables.
     */
    public Set<Var> getAllVariables() {
        return usedVariablesInClause.values()
                .stream().flatMap(Collection::stream)
                .collect(Collectors.toCollection(() -> new TreeSet<>(
                        Comparator.comparing(v -> v.uniqueName)
                )));
    }

    /**
     * Return the list of variables used by that clause and the
     * one that are indirectly used. A simple example is this one:
     *
     * Cypher:                  ClauseId:
     *
     *     MATCH (a)--(b)           0
     *     MATCH (b)--(c)           1
     *     MATCH (a)--(d)           2
     *
     *
     * In this example, `b` and `c` are indirectly used by the clause 2 because
     * `a` and `b` are connected by a relationship.
     *
     * However in this example:
     *
     * Cypher:                  ClauseId:
     *
     *     MATCH (a)--(e)           0
     *     MATCH (b)--(c)           1
     *     MATCH (a)--(d)           2
     *
     * The clause 2 does not indirectly use variable `b` and `c` because neither
     * `b` or `c` is related to `d`, `a` or `e`.
     *
     * @param clauseId is the clause id
     * @return Returns the list of variables that match the previous definition.
     */
    public List<Var> getUsedAndIndirectUsedVars(ClauseId clauseId) {
        // Start with the used variable.
        Set<Var> result = new HashSet<>(this.getUsedVars(clauseId));
        Stack<Var> stack = new Stack<>();

        List<Relationship> relsToInspect = relationships.entrySet().stream()
                .filter(a -> a.getKey().compareTo(clauseId) < 0)
                .flatMap(x -> x.getValue().stream())
                .collect(Collectors.toList());

        // Start with the used variables:
        stack.addAll(result);

        while (!stack.isEmpty()) {
            Var var = stack.pop();
            if (var instanceof NodeVar) {
                relsToInspect.stream().filter(r -> r.leftNode == var).forEach(rel -> {
                    if (!result.contains(rel.rightNode)) {
                        stack.add(rel.rightNode);
                        result.add(rel.rightNode);
                    }
                });
                relsToInspect.stream().filter(r -> r.rightNode == var).forEach(rel -> {
                    if (!result.contains(rel.leftNode)) {
                        stack.add(rel.leftNode);
                        result.add(rel.leftNode);
                    }
                });
            }
        }

        return Collections.unmodifiableList(result.stream().collect(Collectors.toList()));
    }

    /**
     * Return the list of variables reachable by that clause.
     * @param clauseId is the clause id.
     * @return Returns the list of variables.
     */
    public AvailableVariables getReachableVars(ClauseId clauseId) {
        return this.reachableVariables.get(clauseId);
    }

    /**
     * Return the list of relationships involved in that clause.
     * @param clauseId is the clause id.
     * @return Returns the list of relationships.
     */
    public List<Relationship> getRelationships(ClauseId clauseId) {
        return Collections.unmodifiableList(this.relationships.get(clauseId));
    }

    /**
     * Returns the expressions that the RETURN is made of.
     * @return Returns a list of {@link Expr}.
     */
    public List<Expr> getReturnExprs() {
        return Collections.unmodifiableList(this.returnExprs);
    }

    /**
     * We collect all the variables per Clause. Given the following cypher,
     * we have the following data after visiting the Cypher AST:
     *
     *  Cypher:                 Clause Id:   Used Variables:   Relationships:
     *
     *    MATCH (a) -- (b)         0              [a, b]        [a -- b]
     *    WITH (a)                 1              [a]           []
     *    MATCH (b)                2              [b']          []
     *    RETURN a.firstName       3              [a]          N/A
     *
     *  (The RETURN has a PropertyAccess which points to the variable 'a')
     *
     * As you can see, the clause 2, contains a list with a reference to a new variable
     * that we represent as b' here.
     * The variable reference a is shared among all the list. It means that any
     * subsequent transformation will reflect on all element that retains a reference
     * to it either directly or through a Clause Id.
     *
     * This key aspect makes reasoning easier. Modifying the variable in one place,
     * will "informs" other places of the changes without any additional transformation
     * or message passing needed.
     *
     * It also enforce the idea of a single source of Truth for variables.
     */
    private static class ClauseVisitor {

        private AvailableVariables availablesVariables = new AvailableVariables();
        private ClauseId currentClauseId;

        private Map<ClauseId, List<Relationship>> relationships;
        private Map<ClauseId, List<Var>> usedVariables;
        private Map<ClauseId, AvailableVariables> reachableVariables;
        private List<Expr> returnExprs;

        ClauseVisitor(final Map<ClauseId, List<Var>> usedVariables, final Map<ClauseId, AvailableVariables> reachableVariables,
                      final List<Expr> returnExprs, final Map<ClauseId, List<Relationship>> relationships) {
            this.reachableVariables = reachableVariables;
            this.usedVariables = usedVariables;
            this.returnExprs = returnExprs;
            this.relationships = relationships;
        }

        void visitQuery(final SingleQuery query) {
            Iterator<Clause> iter = query.clauses.iterator();
            while (iter.hasNext()) {
                Clause el = iter.next();

                if (el instanceof Match) {
                    this.visitMatch((Match) el);
                } else if (el instanceof With) {
                    this.visitWith((With) el);
                } else if (el instanceof Return) {
                    this.visitReturn((Return) el);
                }
            }
        }

        private void visitReturn(Return ret) {

            // Similar to the visitWith, except that we do not update
            // the list of availablesVariables as this is not necessary.
            Iterator<ReturnItem> iter = ret.returnItems.iterator();
            List<Var> newAvailablesVariables = this.newClauseID(ret.span, ClauseId.ClauseKind.RETURN);

            while (iter.hasNext()) {
                ReturnItem rt = iter.next();
                if (rt instanceof ReturnItem.Aliased) {
                    AliasVar var = new AliasVar((ReturnItem.Aliased) rt, availablesVariables);
                    returnExprs.add(new ExprVar(var));
                    newAvailablesVariables.add(var);
                } else if (rt instanceof ReturnItem.Unaliased) {
                    ReturnItem.Unaliased urt = (ReturnItem.Unaliased) rt;
                    returnExprs.add(
                        new ExprTree.AliasExpr(
                            ExprTreeBuilder.buildFromCypherExpression(rt.expression, availablesVariables),
                            urt.name
                        )
                    );
                }
            }

            // Make variable defined in RETURN visible to ORDER BY
            this.availablesVariables.extend(newAvailablesVariables);

            ret.orderBy.ifPresent(ob -> reachableVariables.put(new ClauseId(ob), availablesVariables));
        }

        private void visitWith(With with) {

            // Similar to the visitReturn, except that we update
            // the list of availables variables.
            Iterator<ReturnItem> iter = with.returnItems.iterator();
            List<Var> newAvailablesVariables = this.newClauseID(with.span, ClauseId.ClauseKind.WITH);

            while (iter.hasNext()) {
                ReturnItem rt = iter.next();
                this.visitReturnItemInWith(rt, newAvailablesVariables);
            }

            // Create a new list to make sure we won't by mistake change
            // the content of the With List.
            this.availablesVariables = new AvailableVariables(newAvailablesVariables);

            with.orderBy.ifPresent(ob -> reachableVariables.put(new ClauseId(ob), availablesVariables));
            with.where.ifPresent(w -> reachableVariables.put(new ClauseId(w), availablesVariables));
        }

        private void visitReturnItemInWith(final ReturnItem rt, final List<Var> variableList) {
            // In returnItem we can only alias variable or declare
            // that we use another variable.
            Var newVar;
            if (rt instanceof ReturnItem.Aliased) {
                newVar = new AliasVar((ReturnItem.Aliased) rt, availablesVariables);
            } else {
                try {
                    ExprVar exprVar = (ExprVar) ExprTreeBuilder.buildFromCypherExpression(
                        rt.expression,
                        availablesVariables
                    );
                    // If the variable is not found we get null
                    if (exprVar == null) {
                        throw new RuntimeException("Variable not found: " + rt.expression);
                    }
                    newVar = exprVar.var;
                } catch (ClassCastException e) {
                    throw new RuntimeException("Expression in WITH must be aliased.");
                }
            }
            variableList.add(newVar);
        }

        private void visitMatch(Match m) {

            Iterator<PatternPart> iterpp = m.pattern.patternParts.iterator();

            // Each pattern part will either reuse previously defined
            // variables or create new ones.
            while (iterpp.hasNext()) {
                PatternPart pp = iterpp.next();

                List<Var> newVarList = this.newClauseID(pp.span, ClauseId.ClauseKind.MATCH);

                if (pp instanceof NamedPatternPart) {
                    newVarList.add(new PathVar((NamedPatternPart) pp));
                }
                collectVariable(pp.element, newVarList);
                collectRelationships(pp.element, this.relationships.get(this.currentClauseId));
            }

            // Where contains mainly restriction on properties
            // to filter out values. Those translate naturally into
            // WHERE in SQL or in JOIN conditions.
            // The only case when this translation can't be performed
            // is if the WHERE contains a PatternExpression.
            // In that case the PatternExpression needs to be properly
            // injected as a JOIN and a condition in the SELECT.
            //
            // This is tricky:
            //   * In Cypher terms, a PatternExpression in a WHERE acts like
            //     a predicate. So in can be hidden in layers of conditionals.
            //   * We might not translate all of those into SQL. For instance:
            //
            //          MATCH (a), (b)
            //          WHERE (a.firstName == "test" OR (a)--(b)) AND
            //                (b.foo * a.op > 23 OR (a)-[:FOOBAR]-(b))
            //
            //     In that case, the RouteMatcher and the SelectVisitor needs
            //     to cooperate to expand into multiple SELECT that get unions
            //     together.
            //
            // For now we only register the where as a valid id to access variables.
            m.where.ifPresent(w -> reachableVariables.put(new ClauseId(w), availablesVariables));
        }

        private List<Var> newClauseID(final Span pos, final ClauseId.ClauseKind kind) {
            List<Var> newList = new ArrayList<>();
            ClauseId id = new ClauseId(pos.lo, kind);
            this.currentClauseId = id;
            this.reachableVariables.put(id, availablesVariables);
            this.availablesVariables = new AvailableVariables(availablesVariables);
            this.usedVariables.put(id, newList);
            if (kind != ClauseId.ClauseKind.RETURN) {
                this.relationships.put(id, new ArrayList<>());
            }
            return newList;
        }

        private void collectVariable(PatternElement pe, List<Var> foundVariables) {
            if (pe instanceof NodePattern) {
                // We always create a variable for node
                // patterns because the Relationship
                // refers to existing variables only.
                // This special case is really to accommodate
                // and make more ergonomic the use of Relationship objects.
                NodePattern np = (NodePattern) pe;
                addVariable(foundVariables, new NodeVar(np, availablesVariables));
            } else if (pe instanceof RelationshipChain) {
                RelationshipChain rl = (RelationshipChain) pe;

                collectVariable(rl.element, foundVariables);
                collectVariable(rl.rightNode, foundVariables);

                rl.relationship.variable.ifPresent(_v ->
                    addVariable(foundVariables, new RelVar(rl.relationship))
                );
            }
        }

        private void addVariable(List<Var> foundVariables, Var variable) {
            Optional<Var> var = availablesVariables.get(variable.name);
            if (var.isPresent()) {
                foundVariables.add(var.get());
            } else {
                foundVariables.add(variable);
                availablesVariables.add(variable);
            }
        }

        private void collectRelationships(final PatternElement p1, final List<Relationship> rels) {
            if (p1 instanceof RelationshipChain) {
                RelationshipChain rel = (RelationshipChain) p1;
                if (rel.element instanceof RelationshipChain) {
                    collectRelationships(rel.element, rels);
                }
                rels.add(new Relationship(rel, availablesVariables));
            }
        }
    }

}
