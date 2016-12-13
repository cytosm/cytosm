package org.cytosm.pathfinder.enumerators;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import org.cytosm.pathfinder.routeelements.ExpansionEdge;
import org.cytosm.pathfinder.routeelements.ExpansionElement;

/***
 * Applies hashing methods based on route characteristics in order to make sure that the results are
 * unique.
 *
 *
 */
public class EnumeratorFilter {

    private static final Logger LOGGER = Logger.getLogger(EnumeratorFilter.class.getName());

    /***
     * Map with all solved route possibilities, indexed by a route aware hashing function (the
     * integers are the hash of the list and the reverse list, first the largest and then the
     * smallest). This is used to filter symmetric rules.
     */
    protected ConcurrentMap<Entry<Integer, Integer>, List<ExpansionElement>> solvedRoutesMap =
            new ConcurrentHashMap<>();

    private boolean hasVariables;

    private boolean hasDirectedEdges;


    /***
     * Default constructor.
     *
     * @param enumerator enumerator used
     */
    public EnumeratorFilter(final AbstractEnumerator enumerator) {
        hasVariables = enumerator.hasVariables;
        hasDirectedEdges = enumerator.getFollowDirectedEdges();
    }

    /***
     * Generates traversal dependent hash code. It provides different values for routes like
     * (:Person)--(:Car) and (:Car)--(:Person)
     *
     * @param route
     * @return
     */
    @SuppressWarnings("checkstyle:magicnumber")
    protected int generateHashFromRoute(final List<ExpansionElement> route) {
        int traversalHash = 0;
        // traversal sequence dependent hashcode.
        for (ExpansionElement element : route) {
            traversalHash = 31 * traversalHash + element.getEquivalentMaterializedGtop().hashCode();
        }

        return traversalHash;
    }

    /***
     * Updates the solved map information.
     *
     * @param foundMatchingRoute
     */
    protected void applyHashFiltering(List<List<ExpansionElement>> distinctList) {

        // removes duplicates:
        distinctList = distinctList.stream().distinct().collect(Collectors.toList());

        if (!hasDirectedEdges) {
            // Undirected:
            if (!hasVariables) {
                /*
                 * Remove ordering duplicates. Since (actor)--(movie) is equivalent to
                 * (:movie)--(:actor) This filter can only be applied when there are no variables,
                 * because (v:actor)--(:movie) is different from (v:movie)--(:actor)
                 */
                populateWithSymmetricHash(distinctList);
            } else {
                populateWithAsymmetricHash(distinctList);
            }
        } else {
            // Directed:
            if (!hasVariables) {
                /*
                 * Remove ordering duplicates. Since (actor)--(movie) is equivalent to
                 * (:movie)--(:actor) This filter can only be applied when there are no variables,
                 * because (v:actor)--(:movie) is different from (v:movie)--(:actor)
                 */
                populateWithDirectSymmetricHash(distinctList);
            } else {
                populateWithAsymmetricHash(distinctList);
            }
        }
    }

    /***
     * Hashing methods that guarantees the same Map key for the route and the reverse of the route,
     * in the directed scenario.
     *
     * @param distinctList
     */
    private void populateWithDirectSymmetricHash(final List<List<ExpansionElement>> distinctList) {

        for (List<ExpansionElement> route : distinctList) {

            int hash = generateHashFromRoute(route);

            List<ExpansionElement> reverseRoute = route.stream().collect(Collectors.toList());
            Collections.reverse(reverseRoute);

            // to make it symmetric, it is required to invert the edge directions:
            // (:employees)->(:orders)<-(:customers) would become
            // (:customers)<-(:orders)->(:Employee) when reversing, which is incorrect.
            reverseRoute.forEach(element -> {
                if (!element.isNode()) {
                    ExpansionEdge edge = (ExpansionEdge) element;
                    edge.reverseDirection();
                }
            });

            int reverseHash = generateHashFromRoute(reverseRoute);

            // Return to normal, before changing the directions:
            reverseRoute.forEach(element -> {
                if (!element.isNode()) {
                    ExpansionEdge edge = (ExpansionEdge) element;
                    edge.reverseDirection();
                }
            });

            int firstHash = 0;
            int secondHash = 0;

            if (reverseHash > hash) {
                firstHash = reverseHash;
                secondHash = hash;
            } else {
                firstHash = hash;
                secondHash = reverseHash;
            }

            Entry<Integer, Integer> hashEntry = new AbstractMap.SimpleEntry<>(firstHash, secondHash);

            if (solvedRoutesMap.containsKey(hashEntry)) {
                // This can be cause by relationships like (n:Employee)--[:REPORTS_TO]--(Employee)
                // In other words, self relationships
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Hash Collision detected. Are there self-relationships?");
                }
            } else {
                solvedRoutesMap.put(hashEntry, route);
            }
        }
    }

    /***
     * Generates a symmetric hash. It provides equal values for routes like (:Person)--(:Car) and
     * (:Car)--(:Person)
     *
     * @param distinctList
     */
    private void populateWithSymmetricHash(final List<List<ExpansionElement>> distinctList) {
        for (List<ExpansionElement> route : distinctList) {
            int hash = generateHashFromRoute(route);

            List<ExpansionElement> reverseRoute = route.stream().collect(Collectors.toList());
            Collections.reverse(reverseRoute);

            int reverseHash = generateHashFromRoute(reverseRoute);

            int firstHash = 0;
            int secondHash = 0;

            if (reverseHash > hash) {
                firstHash = reverseHash;
                secondHash = hash;
            } else {
                firstHash = hash;
                secondHash = reverseHash;
            }

            Entry<Integer, Integer> hashEntry = new AbstractMap.SimpleEntry<>(firstHash, secondHash);

            if (solvedRoutesMap.containsKey(hashEntry)) {
                // This can be cause by relationships like (n:Employee)--[:REPORTS_TO]--(Employee)
                // In other words, self relationships
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Hash Collision detected. Are there self-relationships?");
                }
            } else {
                solvedRoutesMap.put(hashEntry, route);
            }
        }
    }

    /***
     * Generates a asymmetric hash. It provides different values for routes like (:Person)--(:Car)
     * and (:Car)--(:Person)
     *
     * @param foundMatchingRoute
     */
    private void populateWithAsymmetricHash(final List<List<ExpansionElement>> foundMatchingRoute) {
        for (List<ExpansionElement> route : foundMatchingRoute) {
            int hash = generateHashFromRoute(route);

            Entry<Integer, Integer> hashEntry = new AbstractMap.SimpleEntry<>(hash, 0);

            if (solvedRoutesMap.containsKey(hashEntry)) {
                // This can be cause by relationships like (n:Employee)--[:REPORTS_TO]--(Employee)
                // In other words, self relationships
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Hash Collision detected. Are there self-relationships?");
                }
            } else {
                solvedRoutesMap.put(hashEntry, route);
            }
        }
    }

    /***
     * Returns the filtered results.
     *
     * @return list of results
     */
    public List<List<ExpansionElement>> collectResults() {
        return solvedRoutesMap.values().stream().collect(Collectors.toList());
    }
}
