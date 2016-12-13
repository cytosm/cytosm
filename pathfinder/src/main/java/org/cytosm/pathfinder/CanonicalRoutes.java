package org.cytosm.pathfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cytosm.pathfinder.routeelements.ExpansionEdge;
import org.cytosm.pathfinder.routeelements.ExpansionElement;
import org.cytosm.pathfinder.routeelements.ExpansionNode;

/***
 * List of traversal possibilities.
 *
 *
 */
public class CanonicalRoutes {

    private List<List<ExpansionElement>> allPossibleRoutes = new ArrayList<>();

    private Map<ExpansionEdge, List<List<ExpansionElement>>> expansionForEachEdge = new HashMap<>();

    /***
     * Default constructor.
     *
     * @param routes list of routes
     */
    public CanonicalRoutes(List<List<ExpansionElement>> routes) {
        allPossibleRoutes = routes;
    }

    /***
     * Generates a route based on a list of edge and nodes. The routes always ends and start with a
     * node.
     *
     * @param nodesList list of nodes
     * @param edgesList list of edges
     */
    public CanonicalRoutes(List<ExpansionNode> nodesList, List<ExpansionEdge> edgesList) {

        ArrayList<ExpansionNode> nodes = new ArrayList<>();
        ArrayList<ExpansionEdge> edges = new ArrayList<>();

        nodes.addAll(nodesList);
        edges.addAll(edgesList);

        // Every relationship chain starts with a node, followed by zero or one edge. Joins the two
        // lists together in sequence.
        List<ExpansionElement> originalRoute = new ArrayList<>();

        // Generate traversal path
        ExpansionElement currentlyInsertedElement;
        while (!nodes.isEmpty()) {

            currentlyInsertedElement = nodes.get(0);
            originalRoute.add(currentlyInsertedElement);
            nodes.remove(0);

            if (!edges.isEmpty()) {

                currentlyInsertedElement = edges.get(0);
                originalRoute.add(currentlyInsertedElement);
                edges.remove(0);
            }
        }

        // pre-expand possible edge wild-cards
        expandWildCards(originalRoute);
    }

    /***
     * Solves problems like ()-[:knows*1..2]->(), by the expanding all possibilities and adding to
     * routes.
     *
     * @param originalRoute
     */
    private void expandWildCards(List<ExpansionElement> originalRoute) {

        // Expand all edges that require expansion
        int indexOfCurrentObject = 0;

        // Traverse the route looking for edges that need to be expanded.
        for (ExpansionElement edgeOrNode : originalRoute) {
            if (!edgeOrNode.isNode()) {
                ExpansionEdge edge = (ExpansionEdge) edgeOrNode;

                if (edge.isExpandable()) {
                    List<List<ExpansionElement>> edgeExpansion =
                            edge.expand((ExpansionNode) originalRoute.get(indexOfCurrentObject - 1),
                                    (ExpansionNode) originalRoute.get(indexOfCurrentObject + 1));
                    expansionForEachEdge.put(edge, edgeExpansion);
                }
            }
            indexOfCurrentObject++;
        }

        // Return all possible routes
        glueAllExpadedEdgesPossibilities(0, originalRoute);
    }

    /***
     * Generates all possible paths, using the edge expansions.
     *
     * @param currentAnalysedPosition current position being analysed in the route
     * @param originalRoute original route to be analysed
     */
    private void glueAllExpadedEdgesPossibilities(int currentAnalysedPosition, List<ExpansionElement> originalRoute) {

        // The list needs to be duplicated, to avoid changing the original route
        List<ExpansionElement> thisIterationRoute = originalRoute.stream().collect(Collectors.toList());

        List<ExpansionElement> newRouteAfterEdgeExpansion = new ArrayList<>();

        int currentRouteElement = currentAnalysedPosition;
        int routeSize = thisIterationRoute.size();

        // If the call of this function has expanded, then the results should not be added to the
        // list. Only the recursive bits that were not expanded.
        boolean hasExpanded = false;

        // Iterates on the list, searching for edges that were expanded.
        for (currentRouteElement = currentAnalysedPosition; currentRouteElement < routeSize; currentRouteElement++) {
            // current element being analysed:
            ExpansionElement edgeOrNode = thisIterationRoute.get(currentAnalysedPosition);

            if (edgeOrNode instanceof ExpansionEdge) {
                ExpansionEdge analysedEdge = (ExpansionEdge) edgeOrNode;

                /*
                 * check if this edge was expanded, and glue all possibilities on its place. The
                 * edges added on the possibilities are all clones of the original, but without the
                 * expansion variables. They are, then, different objects that will not be found in
                 * the map.
                 */
                if (expansionForEachEdge.containsKey(analysedEdge)) {
                    hasExpanded = true;

                    List<List<ExpansionElement>> substitutionPossibilities = expansionForEachEdge.get(analysedEdge);

                    // glue and expand with all possibilities:
                    for (List<ExpansionElement> expansionPath : substitutionPossibilities) {
                        newRouteAfterEdgeExpansion =
                                insertEdgeExpansionInRoute(currentAnalysedPosition, expansionPath, thisIterationRoute);

                        // updates the analysed position depending on the size of the edge expansion
                        // glued. The increment should go to the next edge
                        int analysedPositionAfterGluing = currentAnalysedPosition + expansionPath.size() - 2;

                        // Continue route gluing from there.
                        glueAllExpadedEdgesPossibilities(analysedPositionAfterGluing, newRouteAfterEdgeExpansion);
                    }

                    // There not need to expand the other nodes after, since they will be analysed
                    // later in the traversal recursion. See this as a pruning.
                    break;
                }
            }

            currentAnalysedPosition++;
        }

        // if there was no expansion in the end of this traversal, it is a complete route. Without
        // ranges or wild-cards.
        if (!hasExpanded) {
            allPossibleRoutes.add(thisIterationRoute);
        }
    }

    /***
     * Adds an expanded edge to the original route.
     *
     * @param currentAnalysedPosition
     * @param expansionPath
     * @param thisIterationRoute
     * @return
     */
    private List<ExpansionElement> insertEdgeExpansionInRoute(int currentAnalysedPosition,
            List<ExpansionElement> expansionPath, List<ExpansionElement> thisIterationRoute) {

        // clones to not change the original object
        List<ExpansionElement> gluedRoute = thisIterationRoute.stream().collect(Collectors.toList());

        // since the left node was also removed
        int insertPosition = currentAnalysedPosition - 1;

        // removes the () -- ()
        gluedRoute.remove(insertPosition);
        gluedRoute.remove(insertPosition);
        gluedRoute.remove(insertPosition);

        // insert the current edge expansion.
        for (ExpansionElement nodeOrEdge : expansionPath) {
            gluedRoute.add(insertPosition, nodeOrEdge);
            insertPosition++;
        }
        return gluedRoute;
    }

    /**
     * @return the allPossibleroutes
     */
    public List<List<ExpansionElement>> getAllPossibleRoutes() {
        return allPossibleRoutes;
    }
}
