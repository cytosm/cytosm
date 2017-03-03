package org.cytosm.cypher2sql.lowering;

import org.cytosm.common.gtop.GTopInterfaceImpl;
import org.cytosm.common.gtop.implementation.relational.ImplementationNode;
import org.cytosm.cypher2sql.lowering.exceptions.BugFound;
import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;
import org.cytosm.cypher2sql.lowering.exceptions.Unreachable;
import org.cytosm.cypher2sql.lowering.sqltree.*;
import org.cytosm.cypher2sql.lowering.sqltree.from.FromItem;
import org.cytosm.cypher2sql.lowering.sqltree.visitor.Walk;
import org.cytosm.cypher2sql.lowering.typeck.var.NodeVar;

import static org.cytosm.cypher2sql.lowering.exceptions.fns.LambdaExceptionUtil.rethrowIntFunction;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Expand the NodeVar using GTop. This pass assumes
 * that it is ran before the population of JOINs.
 *
 * Note:
 *    This is a port of the old JoinPostProcessor by James Brook.
 *    Most of the logic has been preserved.
 *
 */
public class ExpandNodeVarWithGtop {

    private ExpandNodeVarWithGtop() {}

    /**
     * Mutate and create a new SQL tree (the old one should no longer be used).
     * We populate all {@link FromItem} table source name where appropriate.
     * This pass is relatively simple because we make the assumption that JOINs
     * haven't been populated yet.
     *
     * @param sqltree is the tree that will be consumed.
     * @param gTopInterface is the gtop implementation.
     * @return Returns the new SQL tree.
     */
    public static ScopeSelect computeTableNamesOnFromItems(ScopeSelect sqltree, GTopInterfaceImpl gTopInterface)
            throws Cypher2SqlException
    {

        ScopeSelect newTree = (ScopeSelect) Walk.fold(new ExpandSelectInUnionAndComputeTableNames(gTopInterface), sqltree);
        return BubbleUnions.bubbleAllThoseUnions(newTree);
    }

    /**
     * The first sub-pass expand Select in Union (where appropriate) and compute all
     * table names. After this pass many FromItem are incoherent because they point
     * to older WithSelect that have been removed from the tree.
     *
     * This state is fine however because the FromItem will be update during
     * the BubbleUnions phase.
     *
     *
     *      ScopeSelect:
     *          with [
     *              WithSelect - SimpleSelect' (1),
     *              WithSelect - SimpleSelect' (2),
     *              WithSelect - SimpleSelect' (3),
     *          ]
     *          ret: SimpleSelect'
     *
     *
     * becomes:
     *
     *      ScopeSelect:
     *          with [
     *              WithSelect - SimpleSelect (1),  ┌── SimpleSelect (2.a)
     *              WithSelect - UnionSelect  (2) ──├── SimpleSelect (2.b)
     *              WithSelect - SimpleSelect (3),  └── SimpleSelect (2.c)
     *          ]
     *          ret: SimpleSelect
     *
     */
    private static class ExpandSelectInUnionAndComputeTableNames extends Walk.IdentityFolder<Cypher2SqlException> {

        final GTopInterfaceImpl gTopInterface;

        ExpandSelectInUnionAndComputeTableNames(final GTopInterfaceImpl gTopInterface) {
            this.gTopInterface = gTopInterface;
        }

        @Override
        public BaseSelect foldSimpleSelect(SimpleSelect select) throws Cypher2SqlException {

            // Iterate over each FromItem to find the ones that needs
            // a tableName.
            List<List<FromItem>> possibilities = new ArrayList<>();

            for (FromItem fromItem: select.fromItem) {

                // If the fromItem has already a source
                // we add it and continue the iteration.
                if (fromItem.source != null) {
                    possibilities.add(Collections.singletonList(fromItem));
                    continue;
                }

                // Filter variables
                List<NodeVar> vars = fromItem.variables.stream()
                        .filter(v -> v instanceof NodeVar)
                        .map(v -> (NodeVar) v)
                        .collect(Collectors.toList());

                // If we have just one variable then we might
                // generate
                if (vars.size() == 1) {
                    NodeVar var = vars.get(0);

                    // FIXME: This needs to take into account the restriction items from ImplementationNode.
                    Set<ImplementationNode> implNodes = new TreeSet<>(
                            Comparator.comparing(ImplementationNode::getTableName)
                    );

                    // Labels act as an AND. The ImplementationNodes found must have types
                    // for ALL the labels. So if we ask for 'message' and 'post', we will reject
                    // the 'Comment' table because it does not satisfies the 'post' type.
                    for (String label: var.labels) {
                        implNodes.addAll(gTopInterface.getImplementationNodesByType(label));
                    }
                    implNodes = implNodes.stream()
                            .filter(n -> var.labels.stream()
                                    .allMatch(l -> n.getTypes().stream().anyMatch(s -> s.equalsIgnoreCase(l)))
                            )
                            .collect(Collectors.toSet());

                    // If we have only one node:
                    if (implNodes.size() == 1) {
                        // Then we simply set the source for the tableName.
                        fromItem.sourceTableName = implNodes.iterator().next().getTableName();
                        possibilities.add(Collections.singletonList(fromItem));
                    } else {
                        // Otherwise transform into as many as needed FromItem
                        // that points to the correct table.
                        possibilities.add(implNodes.stream().map(node -> {
                            FromItem newFrom = new FromItem();
                            newFrom.sourceTableName = node.getTableName();
                            newFrom.variables = fromItem.variables;
                            return newFrom;
                        }).collect(Collectors.toList()));
                    }


                } else {
                    possibilities.add(Collections.singletonList(fromItem));
                }
            }

            // Compute the number of Select required:
            int numberOfQueries = possibilities.stream().map(List::size).reduce(1, (a, b) -> a * b);

            // If we just have one, then return the original Select. It was mutated through
            // the last loop.
            if (numberOfQueries == 1) {
                return select;
            }

            // Generate the new SimpleSelect using all the List of FromItems.

            // Assume we are about to generate multiple SimpleSelect.
            // In the end if there's just one we simply return it.
            List<SimpleOrScopeSelect> unions = new ArrayList<>(numberOfQueries);

            for (int i = 0; i < numberOfQueries; i++) {
                // FIXME: Interesting.. We need to propagate SKIP, LIMIT, ORDER BT to the Union or
                // FIXME: wrap everything in a ScopeSelect.
                SimpleSelect newQuery = shallowClone(select, false);

                int j = 1;
                for (List<FromItem> fromItems: possibilities) {
                    FromItem fromItem = fromItems.get((i / j) % fromItems.size());
                    newQuery.fromItem.add(fromItem);
                    j *= fromItems.size();
                }

                unions.add(newQuery);
            }

            UnionSelect res = new UnionSelect();
            res.unions = unions;
            res.varId = select.varId;
            return res;
        }
    }

    /**
     * The second sub-pass will bubble up Unions, merging them and creating
     * new ScopeSelect for each combination possible. So for instance, the following
     * SQL tree:
     *
     *      ScopeSelect:
     *          with [
     *              WithSelect - SimpleSelect (1),  ┌── SimpleSelect (2.a)
     *              WithSelect - UnionSelect  (2) ──├── SimpleSelect (2.b)
     *              WithSelect - SimpleSelect (3),  └── SimpleSelect (2.c)
     *          ]
     *          ret: SimpleSelect
     *
     * becomes:
     *
     *      ScopeSelect:
     *          with [
     *                                          ┌── ScopeSelect:
     *                                          │       with [
     *                                          │           WithSelect - SimpleSelect (1)
     *                                          │           WithSelect - SimpleSelect (2.a)
     *                                          │           WithSelect - SimpleSelect (3)
     *                                          │       ]
     *                                          │       ret: SimpleSelect
     *                                          │
     *              WithSelect - UnionSelect  ──├── ScopeSelect:
     *                                          │       with [
     *                                          │           WithSelect - SimpleSelect (1)
     *                                          │           WithSelect - SimpleSelect (2.b)
     *                                          │           WithSelect - SimpleSelect (3)
     *                                          │       ]
     *                                          │       ret: SimpleSelect
     *                                          │
     *                                          └── ScopeSelect: ...
     *          ]
     *          ret: SimpleSelect
     */
    private static class BubbleUnions {

        /**
         * Bubble all unions and returns the new tree.
         * @param oldSqlTree is the old tree that will be consumed (should not be used afterwards).
         * @return Returns the new SQL tree.
         */
        public static ScopeSelect bubbleAllThoseUnions(ScopeSelect oldSqlTree) throws Cypher2SqlException {

            // 1. Calculate the number of new ScopeSelect required.
            int numberOfNewScopeSelects = oldSqlTree.withQueries.stream()
                    .map(w -> w.subquery)
                    .map(s -> (s instanceof UnionSelect) ? ((UnionSelect) s).unions.size() : 1)
                    .reduce(1, (a, b) -> a * b);

            // Early return if there's nothing to do.
            if (numberOfNewScopeSelects == 1) {
                return oldSqlTree;
            }

            // 2. Create empty ScopeSelects and the Union containing them
            UnionSelect unionSelect = new UnionSelect();
            unionSelect.varId = oldSqlTree.varId;
            unionSelect.unions = IntStream.range(0, numberOfNewScopeSelects)
                    .<ScopeSelect>mapToObj(rethrowIntFunction(id -> partialClone(oldSqlTree, id)))
                    .collect(Collectors.toList());

            // 3. Update all froms to point to the correct new WithSelect.
            unionSelect.unions.forEach(BubbleUnions::updateAllFroms);

            // 4. Create the outer ScopeSelect
            ScopeSelect newSqlTree = new ScopeSelect();
            WithSelect wrapper = new WithSelect(unionSelect);
            NameSubqueries.nameSubquery(wrapper);
            newSqlTree.withQueries.add(wrapper);
            newSqlTree.ret = createReturnForOuterScopeSelect(oldSqlTree.ret, wrapper);

            return newSqlTree;
        }

        /**
         * We do a partial clone of the ScopeSelect. We select the appropriate Unions
         * in the each WithSelect that contains a Union. This is really similar to
         * the last part of the {@link ExpandSelectInUnionAndComputeTableNames} algorithm
         * when we create the Select of the UnionSelect. Only difference is that we build a
         * ScopeSelect instead of a Select.
         * @param oldSqlTree is the old SQL tree.
         * @param selectId is the id for this select.
         * @return Returns the new ScopeSelect where all WithSelect contains only SimpleSelect(s).
         */
        private static ScopeSelect partialClone(ScopeSelect oldSqlTree, int selectId) throws Cypher2SqlException {
            ScopeSelect partialClone = new ScopeSelect();
            partialClone.varId = oldSqlTree.varId;
            partialClone.ret = shallowClone(oldSqlTree.ret, true);

            int j = 1;
            for (WithSelect oldWith: oldSqlTree.withQueries) {

                WithSelect newWith;

                if (oldWith.subquery instanceof UnionSelect) {

                    UnionSelect nestedUnion = (UnionSelect) oldWith.subquery;
                    int n = (selectId / j) % nestedUnion.unions.size();
                    newWith = new WithSelect(shallowClone((SimpleSelect) nestedUnion.unions.get(n), true));

                    j *= nestedUnion.unions.size();
                } else if (oldWith.subquery instanceof SimpleSelect) {

                    newWith = new WithSelect(shallowClone((SimpleSelect) oldWith.subquery, true));

                    j *= 1;
                } else {

                    throw new BugFound(
                            "Can't handle '" + oldWith.subquery.getClass() + "' in partialClone of ScopeSelect"
                    );
                }

                newWith.subqueryName = oldWith.subqueryName;
                partialClone.withQueries.add(newWith);
            }
            return partialClone;
        }

        /**
         * Time to now update the FromItems. The trick is to use the subqueryName
         * field of WithSelect as a lookup mechanism.
         *
         * Don't pay attention to the misleading type parameter here. The first
         * statement cast it into a ScopeSelect.
         *
         * @param simpleOrScopeSelect is the ScopeSelect to update.
         */
        private static void updateAllFroms(SimpleOrScopeSelect simpleOrScopeSelect) {
            ScopeSelect scopeSelect = (ScopeSelect) simpleOrScopeSelect;

            Map<String, WithSelect> lookup = scopeSelect.withQueries.stream()
                    .collect(Collectors.toMap(x -> x.subqueryName, x -> x));

            for (WithSelect withSelect: scopeSelect.withQueries) {
                SimpleSelect nestedSelect = (SimpleSelect) withSelect.subquery;

                nestedSelect.fromItem = nestedSelect.fromItem.stream()
                        .map(fromItem -> shallowCloneWithLookup(fromItem, lookup)).collect(Collectors.toList());
            }

            scopeSelect.ret.fromItem = scopeSelect.ret.fromItem.stream()
                    .map(fromItem -> shallowCloneWithLookup(fromItem, lookup)).collect(Collectors.toList());
        }

        private static FromItem shallowCloneWithLookup(final FromItem old, final Map<String, WithSelect> lookup) {
            FromItem newFromItem = new FromItem();

            if (old.source != null) {
                newFromItem.source = lookup.get(old.source.subqueryName);
            }

            newFromItem.sourceTableName = old.sourceTableName;
            newFromItem.sourceVariableName = old.sourceVariableName;
            newFromItem.variables = old.variables;

            return newFromItem;
        }

        /**
         * Create a Select for the outer ScopeSelect.
         */
        private static SimpleSelect createReturnForOuterScopeSelect(SimpleSelect oldReturn, WithSelect wrappedUnion)
                throws Cypher2SqlException
        {
            // Doesn't matter which class we choose here
            SimpleSelect res = shallowClone(oldReturn, false);
            FromItem fetchFrom = new FromItem();
            fetchFrom.source = wrappedUnion;
            fetchFrom.variables = oldReturn.fromItem.stream().flatMap(fi -> fi.variables.stream())
                    .collect(Collectors.toSet()).stream().collect(Collectors.toList());
            res.fromItem.add(fetchFrom);
            return res;
        }

    }

    /**
     * This clone performs a shallow copy of the original select, thus
     * preserving all original references. This means you might do
     * further adjustements to point to the correct objects.
     *
     * @param select the original select to shallow copy.
     * @param includeFroms should be true if from also should be shallow copied.
     * @return Return a shallow copy of the object.
     */
    private static SimpleSelect shallowClone(SimpleSelect select, boolean includeFroms) throws Cypher2SqlException {
        try {
            SimpleSelect newInstance = select.getClass().newInstance();
            newInstance.varId = select.varId;
            newInstance.exportedItems = select.exportedItems;
            newInstance.isDistinct = select.isDistinct;
            newInstance.limit = select.limit;
            newInstance.skip = select.skip;
            newInstance.orderBy = select.orderBy;
            newInstance.whereCondition = select.whereCondition;
            if (includeFroms) {
                newInstance.fromItem = select.fromItem.stream().collect(Collectors.toList());
            }
            return newInstance;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new Unreachable();
    }
}
