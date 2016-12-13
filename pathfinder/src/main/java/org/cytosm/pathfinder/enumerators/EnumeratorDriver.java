package org.cytosm.pathfinder.enumerators;

import java.util.ArrayList;
import java.util.List;

import org.cytosm.common.gtop.GTopInterfaceImpl;
import org.cytosm.pathfinder.CanonicalRoutes;
import org.cytosm.pathfinder.routeelements.ExpansionElement;

/***
 * Drives the enumeration process. This class was created in order to make sure that one enumerator
 * object would only have to handle one route. This simplified the parallellization process.
 *
 *
 */
public class EnumeratorDriver {

    /***
     * Should the number of possible routes, after wild-card expansion, be greater than this number,
     * parallel streams will be used.
     */
    protected static final int PARALLEL_THRESHOLD = 100;

    /***
     * Class used by the enumerator.
     */
    @SuppressWarnings("rawtypes")
    private Class enumeratorType;

    /***
     * List of enumerator instances.
     */
    private List<AbstractEnumerator> enumerators = new ArrayList<>();

    /***
     * The solutions provided by all enumerators.
     */
    private final List<List<ExpansionElement>> allSolutions = new ArrayList<>();

    /***
     * Default constructor.
     *
     * @param enumeratorType type of enumerator used on the driver
     */
    public EnumeratorDriver(final Class<?> enumeratorType) {
        this.enumeratorType = enumeratorType;
    }

    /***
     * Launches the enumeration process.
     *
     * @param hasEdgeHints true if there are edge hints
     * @param hasNodeHints true if there are node hints
     * @param hasVariables true if there are variables
     * @param gTopInter the gtop interface
     * @param wildCardResolvedRoute the route with all possible edge expansions already solved.
     */
    public void launch(final boolean hasEdgeHints, final boolean hasNodeHints, final boolean hasVariables,
            final GTopInterfaceImpl gTopInter, final CanonicalRoutes wildCardResolvedRoute) {

        for (List<ExpansionElement> route : wildCardResolvedRoute.getAllPossibleRoutes()) {
            // TODO Use getInstance?
            if (enumeratorType == DirectedEnumerator.class) {
                enumerators.add(new DirectedEnumerator(hasEdgeHints, hasNodeHints, hasVariables, gTopInter, route));
            } else if (enumeratorType == UndirectedEnumerator.class) {
                enumerators.add(new UndirectedEnumerator(hasEdgeHints, hasNodeHints, hasVariables, gTopInter, route));
            }
        }

        if (wildCardResolvedRoute.getAllPossibleRoutes().size() > PARALLEL_THRESHOLD) {
            runEnumerationInParallel();
        } else {
            runEnumerationSequentially();
        }

        enumerators.forEach(enumerator -> allSolutions.addAll(enumerator.getAllSolutions()));
    }

    /***
     * Runs the enumeration sequentially.
     */
    private void runEnumerationSequentially() {
        enumerators.forEach(AbstractEnumerator::run);
    }

    /***
     * runs the enumeration in parallel.
     */
    private void runEnumerationInParallel() {
        enumerators.parallelStream().forEach(AbstractEnumerator::run);
    }

    /***
     * Collects the results of all the enumerators.
     *
     * @return all the enumerated routes
     */
    public List<List<ExpansionElement>> collectResults() {
        enumerators.forEach(enumerator -> allSolutions.addAll(enumerator.getAllSolutions()));

        EnumeratorFilter filter = new EnumeratorFilter(enumerators.get(0));

        // removes duplicates
        filter.applyHashFiltering(allSolutions);

        return filter.collectResults();
    }
}
