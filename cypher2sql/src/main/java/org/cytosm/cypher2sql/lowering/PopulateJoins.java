package org.cytosm.cypher2sql.lowering;

import org.cytosm.common.gtop.GTopInterfaceImpl;
import org.cytosm.common.gtop.implementation.relational.ImplementationEdge;
import org.cytosm.common.gtop.implementation.relational.TraversalHop;
import org.cytosm.cypher2sql.lowering.exceptions.BugFound;
import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;
import org.cytosm.cypher2sql.lowering.exceptions.Unreachable;
import org.cytosm.cypher2sql.lowering.sqltree.*;
import org.cytosm.cypher2sql.lowering.sqltree.from.FromItem;
import org.cytosm.cypher2sql.lowering.sqltree.join.BaseJoin;
import org.cytosm.cypher2sql.lowering.sqltree.join.InnerJoin;
import org.cytosm.cypher2sql.lowering.sqltree.join.LeftJoin;
import org.cytosm.cypher2sql.lowering.sqltree.visitor.Walk;
import org.cytosm.cypher2sql.lowering.typeck.VarDependencies;
import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprVar;
import org.cytosm.cypher2sql.lowering.typeck.rel.Relationship;
import org.cytosm.cypher2sql.lowering.typeck.var.*;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTree;

import static org.cytosm.cypher2sql.lowering.exceptions.fns.LambdaExceptionUtil.rethrowConsumer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This pass moves FromItem that needs to be JOINs because
 * of a Relationship to their appropriate place.
 *
 * Note:
 *    This is a port of the old JoinPostProcessor by James Brook.
 *    Most of the logic has been preserved.
 *
 */
public class PopulateJoins {

    /**
     * Populate the SQL tree with JOIN elements based
     * ont the relationships from the VarDependencies.
     *
     * Based on the {@link Relationship}
     * provided by {@param vars} we pull from each {@link SimpleSelect} the
     * @param sqltree is the tree where JOIN will be populated.
     * @param vars contains all the information about variable in the original cypher.
     * @param gTopInterface is the gtop implementation.
     */
    public static ScopeSelect populateJoins(ScopeSelect sqltree, VarDependencies vars, GTopInterfaceImpl gTopInterface)
            throws Cypher2SqlException
    {

        Walk.walkSQLNode(new ExpandRelsAsJoins(vars, gTopInterface), sqltree);
        return sqltree;
    }

    /**
     * Expanding relationships as JOINs is fairly difficult. Many
     * aspect needs to be taken into account:
     *
     *
     *   - If the direction is set to BOTH, then a Union might be required
     *     if the JOIN table support directionality.
     *
     *
     *   - If a FromItem provides more than one variable, and two of those
     *     variables are related. Then we apply the relation condition expression
     *     on the JOIN of the joining table.
     *
     *     If not, then we need to split the condition on two JOINs, one for
     *     the joining table (if there's one) and another one for one of the
     *     variable.
     *
     *     The following example highlights the discussion:
     *
     *          MATCH (a:Person)-[:KNOWS]-(b:Person)-[:KNOWS]-(c:Person)
     *          MATCH (a)-[:KNOWS]-(c)
     *
     *     In the above example `a` knows `b` and `c`, `c` knows `a` and `b`.
     *     The above would be expanded into something like:
     *
     *          WITH __sub0 AS (
     *
     *              SELECT ..
     *              FROM Person AS a
     *              JOIN person_knows_person AS pkp1 ON (pkp.person_id = a.id)
     *              JOIN Person AS b ON (pkp.other_person_id = b.id)
     *              ...
     *
     *          ), __sub1 AS (
     *
     *              SELECT ..
     *              FROM __sub0 AS a_and_c
     *              JOIN person_knows_person AS pkp ON (pkp.person_id = a_and_c.a_id AND
     *                                                  pkp.other_person_id = a_and_c.c_id)
     *
     *          )
     *          ...
     *
     *     As we can see, the first SELECT (__sub0) will have the joining condition
     *     split on two JOINs whereas the second case will have them on only one.
     *
     */
    private static class ExpandRelsAsJoins extends Walk.BaseSQLNodeVisitor {

        private final VarDependencies vars;
        private final GTopInterfaceImpl gTopInterface;

        ExpandRelsAsJoins(final VarDependencies vars, GTopInterfaceImpl gTopInterface) {
            this.vars = vars;
            this.gTopInterface = gTopInterface;
        }

        @Override
        public void visitSimpleSelect(SimpleSelect simpleSelect) throws Cypher2SqlException {
            List<Relationship> rels = vars.getRelationships(simpleSelect.varId);

            List<FromItem> fromItems = simpleSelect.fromItem.stream().collect(Collectors.toList());
            for (Relationship rel: rels) {

                FromItem leftNodeFi = fromItems.stream()
                        .filter(fi -> fi.variables.stream().anyMatch(var -> var == rel.leftNode))
                        .findAny().get();
                FromItem rightNodeFi = fromItems.stream()
                        .filter(fi -> fi.variables.stream().anyMatch(var -> var == rel.rightNode))
                        .findAny().get();

                String leftNodeOriginTableName = getOriginTableName(rel.leftNode, leftNodeFi);
                String rightNodeOriginTableName = getOriginTableName(rel.rightNode, rightNodeFi);
                Var leftNode = rel.leftNode;
                Var rightNode = rel.rightNode;

                List<ImplementationEdge> edges = rel.labels.stream()
                        .flatMap(l -> gTopInterface.getImplementationEdgeByType(l).stream())
                        .filter(edge -> {
                            TraversalHop hop = edge.getPaths().get(0).getTraversalHops().get(0);
                            return (hop.getSourceTableName().equals(leftNodeOriginTableName) &&
                                    hop.getDestinationTableName().equals(rightNodeOriginTableName)) ||
                                   (hop.getDestinationTableName().equals(leftNodeOriginTableName) &&
                                    hop.getSourceTableName().equals(rightNodeOriginTableName));
                        })
                        .collect(Collectors.toList());


                if (edges.size() > 1) {
                    throw new BugFound("More than one source!! This is a bug.");
                } else if (edges.isEmpty()) {
                    throw new BugFound("No edge found between: '" + leftNodeOriginTableName + "' and '"
                        + rightNodeOriginTableName + "' for label(s) [" +
                            rel.labels.stream().map(l -> "'" + l + "'").collect(Collectors.joining(", "))
                        + "]"
                    );
                }

                ImplementationEdge edge = edges.get(0);

                // FIXME: Manage arbitrary hops.
                //
                // FIXME: This code totally ignore directions...
                //
                TraversalHop traversalHop = edge.getPaths().get(0).getTraversalHops().get(0);

                // First we create the Join itself and a TempVar
                // (the tempVar is special is that it is allowed to be created there.
                BaseJoin join = createJoin(simpleSelect);
                TempVar joinVar = new TempVar();

                // Then we create the different part of the Join.
                FromItem joiningFrom = new FromItem();
                joiningFrom.sourceTableName = traversalHop.getJoinTableName();
                joiningFrom.variables.add(joinVar);
                join.joiningItem = joiningFrom;

                // Condition on source:
                Var source = getMatch(traversalHop.getSourceTableName(),
                        leftNode, leftNodeOriginTableName,
                        rightNode, rightNodeOriginTableName);
                Expr conditionOnSource = new ExprTree.Eq(
                        new ExprTree.PropertyAccess(traversalHop.getSourceTableColumn(), new ExprVar(source)),
                        new ExprTree.PropertyAccess(traversalHop.getJoinTableSourceColumn(), new ExprVar(joinVar))
                );

                // Condition on destination:
                Var destination = getMatch(traversalHop.getDestinationTableName(),
                        leftNode, leftNodeOriginTableName,
                        rightNode, rightNodeOriginTableName);
                // FIXME: If the two are equals then we should have a union instead.
                if (destination == source) {
                    destination = (destination == leftNode) ? rightNode : leftNode;
                }
                Expr conditionOnDestination = new ExprTree.Eq(
                        new ExprTree.PropertyAccess(traversalHop.getDestinationTableColumn(), new ExprVar(destination)),
                        new ExprTree.PropertyAccess(traversalHop.getJoinTableDestinationColumn(), new ExprVar(joinVar))
                );

                // TODO(Joan): Write a test for this.
                // Add the join. This position is very important
                // because the join visibility needs to be present *BEFORE*
                // the additional JOIN that might appears.
                simpleSelect.addJoin(join);

                // We convert the TraversalHop into an Expr
                // that will be applied either on the JOIN created
                // or on two JOIN (also created).
                if (leftNodeFi == rightNodeFi) {

                    join.condition = new ExprTree.And(conditionOnSource, conditionOnDestination);

                } else {

                    BaseJoin rightNodeJoin = createJoin(simpleSelect);
                    rightNodeJoin.joiningItem = rightNodeFi;

                    // FIXME: We might remove a From that would need to stay.
                    // FIXME: For instance with:
                    // FIXME:       MATCH (a), (b) MATCH (a)--(c)--(b)
                    // FIXME: we will remove the FromItem when encountering b
                    // FIXME: whereas it should be fixed because it has the same
                    // FIXME: origin than the remaining fromItem.
                    // FIXME: this is the kind of things that the check
                    // FIXME: in the previous if was supposed to solve.
                    // FIXME: It doesn't...
                    simpleSelect.fromItem.remove(rightNodeFi); // Will only remove if not already done.

                    if (destination == rel.rightNode) {
                        join.condition = conditionOnSource;
                        rightNodeJoin.condition = conditionOnDestination;
                    } else {
                        join.condition = conditionOnDestination;
                        rightNodeJoin.condition = conditionOnSource;
                    }


                    // Additional join.
                    simpleSelect.addJoin(rightNodeJoin);
                }
            }
        }

        @Override
        public void visitScopeSelect(ScopeSelect scopeSelect) throws Cypher2SqlException {
            scopeSelect.withQueries.forEach(rethrowConsumer(this::visitWithSelect));
        }

        private static BaseJoin createJoin(SimpleSelect select) throws Cypher2SqlException {
            if (select instanceof SimpleSelectWithInnerJoins) {
                return new InnerJoin();
            } else if (select instanceof SimpleSelectWithLeftJoins) {
                return new LeftJoin();
            } else {
                throw new Unreachable();
            }
        }

        // FIXME: this is plain wrong, don't cover the need for a Union.
        // FIXME: this happens if and only if leftNodeOriginTableName is equal to rightNodeOriginTableName.
        private static Var getMatch(String tableName,
                                  Var leftNode, String leftNodeOriginTableName,
                                  Var rightNode, String rightNodeOriginTableName) {
            if (tableName.equals(leftNodeOriginTableName)) {
                return leftNode;
            }
            if (tableName.equals(rightNodeOriginTableName)) {
                return rightNode;
            }
            // FIXME transform into check exception.
            throw new RuntimeException("Couldn't find source between '" +
                    leftNodeOriginTableName + "' and '" + rightNodeOriginTableName + "' for " +
                    tableName
            );
        }

        private static String getOriginTableName(Var var, FromItem sourceForVar) throws Cypher2SqlException {
            return getOrigin(var, sourceForVar).sourceTableName;
        }

        private static FromItem getOrigin(Var var, FromItem origin) throws Cypher2SqlException {
            if (origin.sourceTableName != null) {
                return origin;
            } else {
                // We only expect to see a source of type SimpleSelect
                // Unions should have bubbled up and be hidden.
                if (origin.source.subquery instanceof SimpleSelect) {
                    SimpleSelect source = (SimpleSelect) origin.source.subquery;

                    Optional<FromItem> newSourceForVar = source.dependencies().stream()
                            .filter(varProvider -> varProvider.variables.stream().anyMatch(v -> v == var))
                            .findAny();

                    Var resolvedVar = AliasVar.resolveAliasVar(var);

                    if (newSourceForVar.isPresent()) {
                        return getOrigin(var, newSourceForVar.get());
                    } else if (resolvedVar != var) {
                        return getOrigin(resolvedVar, origin);
                    } else {
                        throw new BugFound("Variable came from nowhere!! -> '" + var.name + "'");
                    }
                } else {
                    throw new BugFound("Pass is ran too late. We shouldn't see any Union here.");
                }
            }
        }
    }
}
