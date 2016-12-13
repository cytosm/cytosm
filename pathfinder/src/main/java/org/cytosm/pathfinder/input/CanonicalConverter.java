package org.cytosm.pathfinder.input;

import java.util.List;
import java.util.Map;

import org.cytosm.pathfinder.CanonicalRoutes;

/***
 * Abstract Class to define how input format interpreter will translate to Expansion Element format.
 *
 *
 */
public abstract class CanonicalConverter {

    /***
     * External context to input information that may produce hints to narrow down the search. The
     * key is the variable name used to refer the edge or the node in the external context. The
     * value is the attribute that should be added.
     */
    protected Map<String, List<String>> externalContext;

    /***
     * Translates from a Domain Specific Language (DSL) to Expansion Element route.
     *
     * @param inputInformation original input route to be matched.
     * @return an expansion element route
     */
    public abstract CanonicalRoutes translate(String inputInformation);

    /***
     * Adds external context to the canonical Converter. The external context will be added to the
     * internal relationship chains hints.
     *
     * @param externalContextHint external information that is relevant to the route analysis and is
     *        not contained in it.
     */
    public void addExternalContext(Map<String, List<String>> externalContextHint) {
        externalContext = externalContextHint;
    }
}
