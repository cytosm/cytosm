package org.cytosm.pathfinder.enumerators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.cytosm.common.gtop.GTopInterfaceImpl;
import org.cytosm.common.gtop.abstraction.AbstractionEdge;
import org.cytosm.common.gtop.abstraction.AbstractionGraphComponent;
import org.cytosm.common.gtop.abstraction.AbstractionNode;
import org.cytosm.pathfinder.exceptions.PathDescriptionException;
import org.cytosm.pathfinder.exceptions.PathFinderException;
import org.cytosm.pathfinder.routeelements.ExpansionEdge;
import org.cytosm.pathfinder.routeelements.ExpansionElement;
import org.cytosm.pathfinder.routeelements.ExpansionNode;

/***
 * Abstract class that hold most of the methods used by directed and undirected enumerators.
 *
 *
 */
public abstract class AbstractEnumerator {

    /***
     * Route to be expanded by this enumerator.
     */
    protected List<ExpansionElement> toBeExpandedRoute;

    /***
     * If hints that limit the number of possible edges are available.
     */
    protected boolean hasEdgeHints;

    /***
     * If hints that limit the number of possible nodes are available.
     */
    protected boolean hasNodeHints;

    /***
     * If any of the expansion elements has variables.
     */
    protected boolean hasVariables;

    /***
     * GTop file used when enumerating route.
     */
    protected GTopInterfaceImpl gTopInter;

    /***
     * If the implementation class follows or not directed edges.
     *
     * @return
     */
    abstract boolean getFollowDirectedEdges();

    /***
     * Final product of the enumeration process.
     */
    private List<List<ExpansionElement>> allSolutions = new ArrayList<>();

    /***
     * Default constructor.
     *
     * @param hasEdgeHints true if there are hints on the edge
     * @param hasNodeHints true if there are hints on the node
     * @param hasVariables true if there are vaiables
     * @param gImp the gtop interface used.
     */
    public AbstractEnumerator(final boolean hasEdgeHints, final boolean hasNodeHints, final boolean hasVariables,
            final GTopInterfaceImpl gImp) {
        this.hasEdgeHints = hasEdgeHints;
        this.hasNodeHints = hasNodeHints;
        this.hasVariables = hasVariables;
        gTopInter = gImp;
    }

    /***
     * Retrieve all solutions from the enumerator.
     *
     * @return all solutions
     */
    public List<List<ExpansionElement>> getAllSolutions() {
        return allSolutions;
    }

    /***
     * Resolves the hints and applies the algorithm for that case.
     *
     * @param route applied the gtop context on a route
     */
    public void findMatches(final List<ExpansionElement> route) {
        try {
            resolveHints(route);

            allSolutions = findPossibleRoutes(route);
        } catch (PathFinderException e) {
            e.printStackTrace();
        }
    }

    /***
     * Main method to be implemented by the classes that inherit from this class.
     *
     * @param route
     * @return
     */
    protected abstract List<List<ExpansionElement>> findPossibleRoutes(List<ExpansionElement> route);

    /***
     * Use the hints available in the nodes and edges to search gtop candidates matching those
     * criteria.
     *
     * @param route
     * @throws PathDescriptionException
     */
    protected void resolveHints(final List<ExpansionElement> route) throws PathDescriptionException {

        List<ExpansionElement> edgeWithHintsList = new ArrayList<>();
        List<ExpansionElement> nodeWithHintList = new ArrayList<>();

        if (hasEdgeHints) {
            edgeWithHintsList = route.stream().filter(element -> !element.isNode() && element.isHintAvailable())
                    .collect(Collectors.toList());
        }
        if (hasNodeHints) {
            nodeWithHintList = route.stream().filter(element -> element.isNode() && element.isHintAvailable())
                    .collect(Collectors.toList());
        }

        solveElementHint(edgeWithHintsList);
        solveElementHint(nodeWithHintList);
    }

    /***
     * Insert matching gtop equivalent on Edge or Node.
     *
     * @param elementList
     * @throws PathDescriptionException
     */
    private void solveElementHint(final List<ExpansionElement> elementList) throws PathDescriptionException {

        for (ExpansionElement element : elementList) {

            /*
             * Only apply if not processed yet. The list can contain several references to the same
             * object.
             */
            if (element.getMatchedGtopAbstractionEntities().isEmpty()) {
                if (!element.getTypes().isEmpty()) {
                    solveTypesHint(element);
                }

                // there are also attribute hints:
                if (!element.getAttributeMap().isEmpty()) {
                    solveAttributesHint(element);
                }
            }
        }
    }

    /***
     * Solves matching Gtop candidates based on attributes, not on labels/types.
     *
     * @param element
     * @throws PathDescriptionException
     */
    private void solveAttributesHint(final ExpansionElement element) throws PathDescriptionException {

        /***
         * Should results were found when looking for types, the attributes need to match what
         * was found
         */
        boolean matchWithPreviousResult = !element.getMatchedGtopAbstractionEntities().isEmpty();

        // There are attributes to evaluate
        if (!element.getAttributeMap().isEmpty()) {
            // TODO Extract method that applies to nodes and edges:
            // the abstract edge needs to contain all the attributes from the canonical edge
            if (!element.isNode()) {

                List<AbstractionEdge> matchingEdge = gTopInter.getAbstractionEdges().stream()
                        .filter(absEdge -> AbstractEnumerator.findMatchingAbstractionFilter(element, absEdge))
                        .collect(Collectors.toList());

                /*
                 * The Input provided as a hint that doesn't exist on Gtop. No Match will be found!
                 */
                if (matchingEdge.isEmpty()) {
                    throw new PathDescriptionException(
                            element.getTypes().toString() + element.getAttributeMap().keySet().toString());
                } else {
                    // The types do not match the attribute list!
                    if (matchWithPreviousResult
                            && !element.getMatchedGtopAbstractionEntities().containsAll(matchingEdge)) {

                        // if there is an intersection, make sure that the intersection will not
                        // reduce the number
                        // of already matched labels. Explicitly informing a label and then proving
                        // an attribute that
                        // does not belong to that label is a cypher syntax error.

                        // In other words: Make sure that attributes set is a superset of labels.

                        boolean congruentAttributesWithLabels =
                                matchingEdge.containsAll(element.getMatchedGtopAbstractionEntities());

                        if (!congruentAttributesWithLabels) {
                            // String str = matchingEdge.stream().map(list ->
                            // list.getTypes()).map(Object::toString)
                            // .collect(Collectors.joining(", "));

                            throw new PathDescriptionException(
                                    element.getTypes().toString() + element.getAttributeMap().keySet().toString());
                        }
                        // Otherwise just don't add anything - keeps the original match.

                    } else {
                        // add to list:
                        element.getMatchedGtopAbstractionEntities().addAll(matchingEdge);
                    }
                }
            } else {
                // It is a node:
                List<AbstractionNode> matchingNode = gTopInter.getAbstractionNodes().stream()
                        .filter(absNode -> AbstractEnumerator.findMatchingAbstractionFilter(element, absNode))
                        .collect(Collectors.toList());

                /*
                 * The Input provided as a hint that doesn't exist on Gtop. No Match will be found!
                 */
                if (matchingNode.isEmpty()) {

                    throw new PathDescriptionException(
                            element.getTypes().toString() + element.getAttributeMap().keySet().toString());
                } else {

                    // The types do not match the attribute list!
                    if (matchWithPreviousResult
                            && !element.getMatchedGtopAbstractionEntities().containsAll(matchingNode)) {

                        // if there is an intersection, make sure that the intersection will not
                        // reduce the number
                        // of already matched labels. Explicitly informing a label and then proving
                        // an attribute that
                        // does not belong to that label is a cypher syntax error.

                        // In other words: Make sure that attributes set is a superset of labels.

                        boolean congruentAttributesWithLabels =
                                matchingNode.containsAll(element.getMatchedGtopAbstractionEntities());

                        if (!congruentAttributesWithLabels) {
                            throw new PathDescriptionException(
                                    element.getTypes().toString() + element.getAttributeMap().keySet().toString());
                        }
                        // Otherwise just don't add anything - keeps the original match.
                    } else {
                        // add to list - there are no labels to retrict it.
                        element.getMatchedGtopAbstractionEntities().addAll(matchingNode);
                    }
                }

            }

        }
    }

    /***
     * Filters gtop edges and nodes that contain all described attributes.
     *
     * @param element
     * @return
     */
    private static boolean findMatchingAbstractionFilter(final ExpansionElement expansionElement,
            final AbstractionGraphComponent element) {
        // TODO Find a more elegant way to solve this. Probably there should be a abstract
        // AbstractElement class. This requires, however, a modification in the gtop implementation.

        if (element instanceof AbstractionEdge) {
            AbstractionEdge absEdge = (AbstractionEdge) element;

            // Abstraction edge has no attributes
            if (absEdge.getAttributes() == null || absEdge.getAttributes().isEmpty()) {
                return false;
            }

            for (String attribute : expansionElement.getAttributeMap().keySet()) {
                List<String> abstractAttributeList = absEdge.getAttributes().stream()
                        .map(String::toLowerCase).collect(Collectors.toList());

                if (!abstractAttributeList.contains(attribute.toLowerCase())) {
                    // If the abstraction edge does not have one of the described
                    // attributes, it is not that abstraction edge.
                    return false;
                }
            }
            return true;
        } else {
            AbstractionNode absNode = (AbstractionNode) element;

            // Abstraction edge has no attributes
            if (absNode.getAttributes() == null || absNode.getAttributes().isEmpty()) {
                return false;
            }

            List<String> abstractAttributeList = absNode.getAttributes().stream()
                    .map(String::toLowerCase).collect(Collectors.toList());

            for (String attribute : expansionElement.getAttributeMap().keySet()) {

                if (!abstractAttributeList.contains(attribute.toLowerCase())) {
                    // If the abstraction edge does not have one of the described
                    // attributes, it is not the abstraction edge that we're looking for.
                    return false;
                }
            }
            return true;
        }
    }


    /***
     * Search for Gtop Elements that matches the types.
     *
     * @param element
     * @return if an element was found
     * @throws PathDescriptionException
     */
    private void solveTypesHint(final ExpansionElement element) throws PathDescriptionException {

        List<AbstractionEdge> foundEquivalentEdge = null;
        List<AbstractionNode> foundEquivalentNode = null;

        for (String type : element.getTypes()) {

            if (!element.isNode()) {
                // It's an edge
                foundEquivalentEdge = gTopInter.getAbstractionEdgesByTypes(type);
                if (foundEquivalentEdge != null) {
                    element.addMatchedGtopAbstractionEntities(
                            foundEquivalentEdge.stream().map(x -> x).collect(Collectors.toList()));
                }
            } else {
                // It's a node:
                foundEquivalentNode = gTopInter.getAbstractionNodesByTypes(type);

                if (foundEquivalentNode != null) {
                    element.addMatchedGtopAbstractionEntities(
                            foundEquivalentNode.stream().map(x -> x).collect(Collectors.toList()));
                }
            }

            // wrong type given by the user on query time.
            if (foundEquivalentEdge == null && foundEquivalentNode == null) {
                throw new PathDescriptionException(
                        element.getTypes().toString() + element.getAttributeMap().keySet().toString());
            }
        }
    }

    /***
     * Runs a modified DFS algorithm that keeps track of the path.
     *
     * @param sequentialAbstractionChain
     * @param currentNode
     * @param originalRoute
     * @param analysingIndex
     */
    protected void runDFS(final List<AbstractionGraphComponent> sequentialAbstractionChain,
            final AbstractionNode currentNode, final List<ExpansionElement> originalRoute, final int analysingIndex) {

        // Path has not finished, continue DFS:
        if (analysingIndex < originalRoute.size() - 1) {

            List<AbstractionEdge> edgesToTraverse;

            // if there is a Gtop hint on the edge, see what edge match the hint:
            edgesToTraverse = findTraversableEdges(currentNode, originalRoute, analysingIndex, gTopInter);

            // traverse:
            for (AbstractionEdge edge : edgesToTraverse) {
                // Check all possible gtop nodes that are traversed from it:

                List<AbstractionNode> nextNodeAbstractions;

                nextNodeAbstractions = findTraversableNodes(currentNode, originalRoute, analysingIndex, edge);

                for (AbstractionNode node : nextNodeAbstractions) {

                    // clone AbstractionChain:
                    List<AbstractionGraphComponent> sequentialAbstractionChainClone =
                            sequentialAbstractionChain.stream().collect(Collectors.toList());

                    sequentialAbstractionChainClone.add(edge);
                    sequentialAbstractionChainClone.add(node);

                    runDFS(sequentialAbstractionChainClone, node, originalRoute, analysingIndex + 2);
                }
            }

        } else {
            reportPossibleAbstractSequence(originalRoute, sequentialAbstractionChain);
        }
    }


    /***
     * Reports a sequence of Abstract Nodes and Abstract Edges that match the route criteria.
     *
     * @param originalRoute
     * @param sequentialAbstractionChain
     */
    protected abstract void reportPossibleAbstractSequence(List<ExpansionElement> originalRoute,
            List<AbstractionGraphComponent> sequentialAbstractionChain);

    /***
     * Intersection of all possible nodes described by the gtop and a possible type defined on query
     * language level.
     *
     * @param originalRoute
     * @param analysingIndex
     * @param possibleEdgesOnGtop
     * @param edge
     * @param nextNodeAbstractions
     * @return
     */
    private List<AbstractionNode> findTraversableNodes(final AbstractionNode currentNode,
            final List<ExpansionElement> originalRoute, final int analysingIndex, final AbstractionEdge edge) {

        List<AbstractionNode> nextNodeAbstractions;

        ExpansionNode nextTraversedNode = (ExpansionNode) originalRoute.get(analysingIndex + 2);

        /*
         * Redundant path are possible (:person) -- (:car) may return (:car) -- (:person). This is
         * filtered in the validTraversal method
         */
        List<AbstractionNode> nextNodeOnGtop = gTopInter.getNodesForEdge(edge);
        if (nextTraversedNode.isHintAvailable()) {
            List<AbstractionNode> hintNodes = nextTraversedNode.getMatchedGtopAbstractionEntities().stream()
                    .map(obj -> (AbstractionNode) obj).collect(Collectors.toList());

            // Intersection of both lists
            nextNodeAbstractions = nextNodeOnGtop.stream().filter(hintNodes::contains).collect(Collectors.toList());

        } else {
            nextNodeAbstractions = nextNodeOnGtop;
        }

        /*
         * Remove nodes that are of the same type as the one being analysed in this traversal step,
         * unless it is a self-relationship. In this case, both source and destinations contains
         * that node
         */
        if (!selfRelationshipsAllowed(currentNode, edge, nextNodeAbstractions)) {
            nextNodeAbstractions.remove(currentNode);
        }

        // remove duplicates
        nextNodeAbstractions = nextNodeAbstractions.stream().distinct().collect(Collectors.toList());

        return nextNodeAbstractions;
    }


    /***
     * Checks if this edge contains a self relationship.
     *
     * @param currentNode
     * @param edge
     * @param nextNodeAbstractions
     * @return
     */
    private boolean selfRelationshipsAllowed(final AbstractionNode currentNode, final AbstractionEdge edge,
            final List<AbstractionNode> nextNodeAbstractions) {
        List<String> sourceTypes =
                edge.getSourceType().stream().map(type -> type.toLowerCase()).collect(Collectors.toList());
        List<String> destinationTypes =
                edge.getDestinationType().stream().map(type -> type.toLowerCase()).collect(Collectors.toList());
        List<String> currentNodeTypes =
                currentNode.getTypes().stream().map(type -> type.toLowerCase()).collect(Collectors.toList());

        boolean inBothLists = false;

        if (sourceTypes.stream().anyMatch(node -> currentNodeTypes.contains(node))
                && destinationTypes.stream().anyMatch(node -> currentNodeTypes.contains(node))) {
            inBothLists = true;
        }

        return inBothLists;
    }

    /***
     * Intersection of all possible edges described by the gtop and a possible type defined on query
     * language level.
     *
     * @param currentNode
     * @param originalRoute
     * @param analysingIndex
     * @param gtopInter
     * @return
     */
    private List<AbstractionEdge> findTraversableEdges(final AbstractionNode currentNode,
            final List<ExpansionElement> originalRoute, final int analysingIndex, final GTopInterfaceImpl gtopInter) {

        final ExpansionEdge routeEdge = (ExpansionEdge) originalRoute.get(analysingIndex + 1);

        List<AbstractionEdge> traversedEdges;

        // get abstract edges for given node.
        List<AbstractionEdge> possibleEdgesOnGtop =
                gtopInter.getAllAbstractEdgesForNodeTypes(currentNode.getTypes());

        // If next edge is undirected, add only undirected possibilities
        if (!routeEdge.isDirected()) {
            possibleEdgesOnGtop =
                    possibleEdgesOnGtop.stream().collect(Collectors.toList());
        } else {
            // Otherwise, consider only directed possibilities
            possibleEdgesOnGtop =
                    possibleEdgesOnGtop.stream().filter(AbstractionEdge::isDirected).collect(Collectors.toList());
        }

        if (routeEdge.isHintAvailable()) {
            List<AbstractionEdge> hintEdges = routeEdge.getMatchedGtopAbstractionEntities().stream()
                    .map(obj -> (AbstractionEdge) obj).collect(Collectors.toList());

            // Intersection of both lists
            traversedEdges = possibleEdgesOnGtop.stream().filter(hintEdges::contains).collect(Collectors.toList());
        } else {
            traversedEdges = possibleEdgesOnGtop;
        }

        // filter if directed, to have only source that match current node, of destination that
        // matches current node.
        if (routeEdge.isDirected()) {

            traversedEdges = traversedEdges.stream().filter(edge -> {

                if (routeEdge.isToRight()) {
                    // Then the current edge is pointing out of the current node
                    return !Collections.disjoint(edge.getSourceType(), currentNode.getTypes());
                } else {
                    // then the current edge pointing in the current node
                    return !Collections.disjoint(edge.getDestinationType(), currentNode.getTypes());
                }
            }).collect(Collectors.toList());
        }

        return traversedEdges;
    }

    /***
     * Generates a route from based in the abstract Node/Edge sequences and the original routes.
     *
     * @param originalRoute
     * @return
     */
    protected List<List<ExpansionElement>> generateRoutesFromAbstractionSequence(
            final List<ExpansionElement> originalRoute,
            final List<List<AbstractionGraphComponent>> possibleAbstractSequences) {


        // use only the abstract sequences that match that route size:
        List<List<AbstractionGraphComponent>> possibleAbstractSequencesForThisRoute = possibleAbstractSequences.stream()
                .filter(route -> route.size() == originalRoute.size()).collect(Collectors.toList());

        List<List<ExpansionElement>> possibleRoutes = new ArrayList<>();

        for (List<AbstractionGraphComponent> abstractSequence : possibleAbstractSequencesForThisRoute) {
            // clone the route
            List<ExpansionElement> materializedRoute = cloneRoute(originalRoute);

            // populates the route sequentially with equivalent AbstractionItem
            int abstractionItem = 0;
            for (ExpansionElement element : materializedRoute) {
                element.setEquivalentMaterializedGtop(abstractSequence.get(abstractionItem));
                abstractionItem++;
            }

            possibleRoutes.add(materializedRoute);
        }

        return possibleRoutes;
    }


    /***
     * Clones the original route, with new instances.
     *
     * @param route
     * @return
     */
    private List<ExpansionElement> cloneRoute(final List<ExpansionElement> route) {

        List<ExpansionElement> clonedRoute = new ArrayList<>();

        for (ExpansionElement element : route) {
            if (element.isNode()) {
                clonedRoute.add(new ExpansionNode((ExpansionNode) element));
            } else {
                clonedRoute.add(new ExpansionEdge((ExpansionEdge) element));
            }
        }
        return clonedRoute;
    }

    /***
     * Runs the object.
     */
    public void run() {
        this.findMatches(toBeExpandedRoute);
    }
}
