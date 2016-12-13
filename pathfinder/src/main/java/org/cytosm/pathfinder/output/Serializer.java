package org.cytosm.pathfinder.output;

import java.util.List;

import org.cytosm.pathfinder.routeelements.ExpansionElement;

/***
 * Abstract class for serialization.
 *
 *
 */
public interface Serializer {

    /***
     * serialize a list of routes, after the matching process.
     *
     * @param routes routes to be serialized
     * @return serialization result.
     */
    List<String> serialize(final List<List<ExpansionElement>> routes);
}
