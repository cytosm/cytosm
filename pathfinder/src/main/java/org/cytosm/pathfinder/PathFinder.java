package org.cytosm.pathfinder;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.cytosm.common.gtop.GTopInterfaceImpl;
import org.cytosm.pathfinder.enumerators.DirectedEnumerator;
import org.cytosm.pathfinder.enumerators.EnumeratorDriver;
import org.cytosm.pathfinder.enumerators.UndirectedEnumerator;
import org.cytosm.pathfinder.input.CanonicalConverter;
import org.cytosm.pathfinder.routeelements.ExpansionEdge;
import org.cytosm.pathfinder.routeelements.ExpansionElement;

/***
 * Searches for routes that would match the described elements.
 *
 *
 */
public class PathFinder {

    /***
     * Format that will translate from a Graph Query Language to a List of Expansion Elements.
     */
    private CanonicalConverter inputFormat = null;

    /***
     * Route without possible edges wild cards. All possibilities are already listed here.
     */
    private CanonicalRoutes wildCardResolvedRoute = null;

    /***
     * Default constructor.
     *
     * @param converterType type of converter that will be used to interpret the input
     */
    public PathFinder(final CanonicalConverter converterType) {
        inputFormat = converterType;
    }

    /***
     * Enumerates all the possibilites of a relationship chain taking into consideration the gTop
     * file.
     *
     * @param input the relationship chain
     * @param gTopInter gTop interface
     * @return all valid routes that would match the relationship chain and the gTop file.
     */
    public CanonicalRoutes enumerate(final String input, final GTopInterfaceImpl gTopInter) {

        // Generate a list of possible paths, before consulting Gtop. More than one possibility
        // exists due to wild-card expansion.
        wildCardResolvedRoute = generateCanonicalRoutesFromInput(input);

        // Use Gtop to evaluate possibilities
        CanonicalRoutes allPossibleRoutes = evaluateEnumerationStrategy(wildCardResolvedRoute, gTopInter);

        return allPossibleRoutes;
    }

    /***
     * Enumerates all the possibilites of a relationship chain taking into consideration the gTop
     * file. Uses external hints, that are not available in the input relationship chain to narrow
     * down the search scope. <b>Goal use case:</b> When information that could greatly restrict a
     * route match is found outside the relationship chain, i.e. the return part of a Cypher
     * statement with the attribute of a declared variable on the relationship chain.
     *
     * @param input the relationship chain
     * @param gTopInter gTop interface
     * @param externalContextHint hint information that is not available in the original
     *        relationship chain.
     * @return all valid routes that would match the relationship chain and the gTop file.
     */
    public CanonicalRoutes enumerate(final String input, final GTopInterfaceImpl gTopInter,
            final Map<String, List<String>> externalContextHint) {

        // Add extra context to translation:
        inputFormat.addExternalContext(externalContextHint);

        return enumerate(input, gTopInter);
    }

    /***
     * Based on the route contents, analyse enumeration strategies.
     *
     * @param routeWithSolvedWildcards routes with any edge wildcard already resolved
     * @param gTopInter the interface for the gtop used in the analysis
     *
     * @return The possible routes that respect the input and the gtop context.
     */
    public CanonicalRoutes evaluateEnumerationStrategy(final CanonicalRoutes routeWithSolvedWildcards,
            final GTopInterfaceImpl gTopInter) {

        boolean hasNodeHints = false;
        boolean hasEdgeHints = false;
        boolean hasDirectedEdges = false;
        boolean hasVariables = false;

        /*
         * Evaluate the shortest of all possible paths. The other paths are only combinatorial
         * expansions of it.
         */
        Comparator<List<ExpansionElement>> comparator = Comparator.comparing(List<ExpansionElement>::size);

        List<ExpansionElement> shortestRoute =
                routeWithSolvedWildcards.getAllPossibleRoutes().stream().sorted(comparator).findFirst().get();

        // Verify route type.
        for (ExpansionElement element : shortestRoute) {
            if (element.isHintAvailable()) {

                if (element.isNode()) {
                    hasNodeHints = true;
                } else {
                    // It's an edge
                    hasEdgeHints = true;
                }
            }

            // Directed Edges:
            if (!element.isNode() && ((ExpansionEdge) element).isDirected()) {
                hasDirectedEdges = true;
            }

            // Check for variables. That is used in symmetric relation filtering.
            if (element.getVariable() != null && !element.getVariable().isEmpty()) {
                hasVariables = true;
            }
        }

        Class<?> enumeratorClass = null;

        if (!hasDirectedEdges) {
            enumeratorClass = UndirectedEnumerator.class;
        } else {
            enumeratorClass = DirectedEnumerator.class;
        }

        EnumeratorDriver driver = new EnumeratorDriver(enumeratorClass);

        driver.launch(hasEdgeHints, hasNodeHints, hasVariables, gTopInter, routeWithSolvedWildcards);

        List<List<ExpansionElement>> result = driver.collectResults();

        CanonicalRoutes resultRoutes = new CanonicalRoutes(result);

        return resultRoutes;
    }

    /***
     * Canonical routes are a list of all the routes possible due to wild-card expansion. No Gtop
     * expansion is done at this stage.
     *
     * @param input
     * @return
     */
    public CanonicalRoutes generateCanonicalRoutesFromInput(final String input) {
        return inputFormat.translate(input);
    }

    /**
     * @return the inputFormat
     */
    public CanonicalConverter getInputFormat() {
        return inputFormat;
    }
}
