package org.cytosm.pathfinder.enumerators;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.cytosm.common.gtop.GTopInterfaceImpl;
import org.cytosm.common.gtop.abstraction.AbstractionEdge;
import org.cytosm.common.gtop.abstraction.AbstractionGraphComponent;
import org.cytosm.common.gtop.abstraction.AbstractionNode;
import org.cytosm.pathfinder.routeelements.ExpansionElement;
import org.cytosm.pathfinder.routeelements.ExpansionNode;

/***
 * Enumerator for undirected routes.
 *
 *
 */
public class UndirectedEnumerator extends AbstractEnumerator {

    /***
     * List of possible traversals. It is a list of {@link AbstractionNode} and
     * {@link AbstractionEdge}
     */
    protected List<List<AbstractionGraphComponent>> possibleAbstractSequences = new ArrayList<>();

    /***
     * Default constructor.
     *
     * @param hasEdgeHints true if there are edge hints
     * @param hasNodeHints true if there are node hints
     * @param hasVariables true if there are variables
     * @param gtopInterface the gtop interface
     */
    public UndirectedEnumerator(boolean hasEdgeHints, boolean hasNodeHints, boolean hasVariables,
            GTopInterfaceImpl gtopInterface) {
        super(hasEdgeHints, hasNodeHints, hasVariables, gtopInterface);
    }

    /***
     * Default constructor.
     *
     * @param hasEdgeHints true if there are edge hints
     * @param hasNodeHints true if there are node hints
     * @param hasVariables true if there are variables
     * @param gtopInterface the gtop interface
     * @param route route to expand.
     */
    public UndirectedEnumerator(boolean hasEdgeHints, boolean hasNodeHints, boolean hasVariables,
            GTopInterfaceImpl gtopInterface, List<ExpansionElement> route) {
        super(hasEdgeHints, hasNodeHints, hasVariables, gtopInterface);
        toBeExpandedRoute = route;
    }

    @Override
    protected List<List<ExpansionElement>> findPossibleRoutes(List<ExpansionElement> route) {

        List<AbstractionNode> absList;
        ExpansionNode source = (ExpansionNode) route.get(0);

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
            runDFS(sequentialAbstractionStack, sourceAbstraction, route, 0);
        }

        // Clean up not possible paths:
        // TODO Check if still required.
        validateTraversals(route);

        // Use the abstract sequences to generate ExpansionElements sequences
        List<List<ExpansionElement>> possibleRoutes =
                generateRoutesFromAbstractionSequence(route, possibleAbstractSequences);

        return possibleRoutes;
    }

    /***
     * Checks if the traversal are valid. TODO Check if this method is still required. Maybe
     * asymmetric and symmetric hashing solved this.
     *
     * @param route
     */
    private void validateTraversals(List<ExpansionElement> route) {
        // clean up any empty sequence:
        // TODO Check if this actually happens:
        possibleAbstractSequences = possibleAbstractSequences.stream()
                .filter(sequence -> sequence.size() == route.size()).collect(Collectors.toList());

        // Make sure the traversal are valid on gtop perpective
        possibleAbstractSequences = possibleAbstractSequences.stream().filter(sequence -> validGtopSequence(sequence))
                .collect(Collectors.toList());
    }

    /***
     * Verifies if the sequence if valid in gtop terms.
     *
     * @param sequence
     * @return
     */
    private boolean validGtopSequence(List<AbstractionGraphComponent> sequence) {
        boolean validSequence = true;

        int startIndex = 2;

        // Sliding window in the format () -- ()
        while (startIndex < sequence.size()) {

            AbstractionNode leftNode = (AbstractionNode) sequence.get(startIndex - 2);
            AbstractionEdge edge = (AbstractionEdge) sequence.get(startIndex - 1);
            AbstractionNode rightNode = (AbstractionNode) sequence.get(startIndex);

            // if there is no combination (left)--(right) or (right)--(left), it is invalid.
            if (!((gTopInter.getSourceNodesForEdge(edge).contains(leftNode)
                    && gTopInter.getDestinationNodesForEdge(edge).contains(rightNode))
                    || (gTopInter.getSourceNodesForEdge(edge).contains(rightNode)
                            && gTopInter.getDestinationNodesForEdge(edge).contains(leftNode)))) {
                validSequence = false;
                break;
            }

            // Slide the window
            startIndex = startIndex + 2;
        }

        return validSequence;

    }

    /***
     * Do not follow directed edges.
     */
    @Override
    boolean getFollowDirectedEdges() {
        return false;
    }

    /***
     * Add any possible abstract sequence to a list of abstract sequences. TODO How would this
     * perform with concurrent access (parallelStream)?
     */
    @Override
    protected void reportPossibleAbstractSequence(List<ExpansionElement> originalRoute,
            List<AbstractionGraphComponent> sequentialAbstractionChain) {

        possibleAbstractSequences.add(sequentialAbstractionChain);

    }
}
