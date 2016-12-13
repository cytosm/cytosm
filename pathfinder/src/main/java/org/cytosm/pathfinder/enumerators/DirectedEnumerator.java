package org.cytosm.pathfinder.enumerators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import org.cytosm.common.gtop.GTopInterfaceImpl;
import org.cytosm.common.gtop.abstraction.AbstractionGraphComponent;
import org.cytosm.common.gtop.abstraction.AbstractionNode;
import org.cytosm.pathfinder.routeelements.ExpansionElement;
import org.cytosm.pathfinder.routeelements.ExpansionNode;

/***
 * Enumerator for directed routes.
 *
 *
 */
public class DirectedEnumerator extends AbstractEnumerator {

    static final  Logger LOGGER = Logger.getLogger(DirectedEnumerator.class.getName());

    /***
     * key: is the source node values: List of enumerated possibilities for that segment. The
     * possibilities are @link {@link AbstractionNode} and AbstractionEdge
     */
    private Map<ExpansionNode, List<List<AbstractionGraphComponent>>> enumeratedPaths = new HashMap<>();

    /***
     * List of all @link {@link AbstractionNode} and AbstractionEdge that can be used to
     * generate the specific route.
     */
    private List<List<AbstractionGraphComponent>> possibleAbstractSequences = new ArrayList<>();

    /***
     * Default constructor.
     *
     * @param hasEdgeHints true if there are edge hints
     * @param hasNodeHints true if there are node hints
     * @param hasVariables true if there are variables
     * @param gtopInterface the gtop interface
     * @param route route to expand.
     */
    public DirectedEnumerator(final boolean hasEdgeHints, final boolean hasNodeHints, final boolean hasVariables,
            final GTopInterfaceImpl gtopInterface, final List<ExpansionElement> route) {
        super(hasEdgeHints, hasNodeHints, hasVariables, gtopInterface);

        toBeExpandedRoute = route;
    }

    @Override
    protected List<List<ExpansionElement>> findPossibleRoutes(final List<ExpansionElement> route) {

        ((ExpansionNode) route.get(0)).setSource(true);
        enumerateRoutes(route);
        possibleAbstractSequences = enumeratedPaths.values().stream().flatMap(list -> list.stream()).collect(Collectors.toList());

        // Clean up not possible paths:
        // TODO Check if required
        // validateTraversals(possibleAbstractSequences);

        // Use the sequences to generate ExpansionElements sequences
        List<List<ExpansionElement>> possibleRoutes =
                generateRoutesFromAbstractionSequence(route, possibleAbstractSequences);

        return possibleRoutes;

    }

    /***
     * Runs DFS independently on all routes. TODO make parallel.
     *
     * @param inputRoute
     */
    private void enumerateRoutes(final List<ExpansionElement> inputRoute) {
        List<AbstractionNode> absList;
        ExpansionNode source = (ExpansionNode) inputRoute.get(0);

        // if the first node has a hint, use only the possible hints
        if (source.isHintAvailable()) {
            absList = source.getMatchedGtopAbstractionEntities().stream().map(obj -> (AbstractionNode) obj)
                    .collect(Collectors.toList());
        } else {
         // otherwise run on all possibilities
            // Run DFS on all possibilities
            absList = gTopInter.getAbstractionNodes();
        }

        for (AbstractionNode sourceAbstraction : absList) {
            Stack<AbstractionGraphComponent> sequentialAbstractionStack = new Stack<>();

            sequentialAbstractionStack.add(sourceAbstraction);
            runDFS(sequentialAbstractionStack, sourceAbstraction, inputRoute, 0);
        }
    }

    /***
     * Follows directed edges.
     */
    @Override
    boolean getFollowDirectedEdges() {
        return true;
    }

    @Override
    /***
     * Reports the sequence, indexing by the source node.
     */
    protected void reportPossibleAbstractSequence(final List<ExpansionElement> originalRoute,
            final List<AbstractionGraphComponent> sequentialAbstractionChain) {
        // find source node
        ExpansionNode sourceNode = originalRoute.stream().filter(element -> element.isNode())
                .map(node -> (ExpansionNode) node).filter(node -> node.isSource()).findFirst().get();

        List<List<AbstractionGraphComponent>> listOfRoutes = enumeratedPaths.get(sourceNode);

        if (listOfRoutes == null) {
            listOfRoutes = new ArrayList<>();
        }

        // Add this route
        listOfRoutes.add(sequentialAbstractionChain);

        // update the map
        enumeratedPaths.put(sourceNode, listOfRoutes);
    }

}
