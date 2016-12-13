package org.cytosm.common.gtop.implementation.relational;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/***
 * Implementation Edge. A description of the relational implementation of an edge declared in the abstraction layer.
 *
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImplementationEdge {

    /***
     * Names that this edge may be references on the abstraction level.
     */
    private List<String> synonyms = new ArrayList<>();

    /***
     * The path is a list of all the tables, that at the end would model analogous property graph's edges.
     */
    private List<TraversalPath> paths = new ArrayList<>();

    /***
     * Generates an Implementation edge.
     * @param synonyms synonyms used to refer to that edge on the abstraction level
     * @param paths traversal paths that compose that edge
     */
    public ImplementationEdge(final List<String> synonyms, final List<TraversalPath> paths) {
        this.synonyms = synonyms;
        this.paths = paths;
    }


    /**
     * Constructs an empty Implementation Edge.
     */
    public ImplementationEdge() {

        paths = new ArrayList<>();
        synonyms = new ArrayList<>();

        TraversalPath traversalPath = new TraversalPath();
        TraversalHop traversalHop = new TraversalHop();

        List<TraversalHop> traversalList = new ArrayList<>();
        traversalList.add(traversalHop);

        traversalPath.setTraversalHops(traversalList);
        traversalPath.setTraversalHops(new ArrayList<>());

        paths.add(traversalPath);
    }

    /**
     * @return the synonyms
     */
    public List<String> getSynonyms() {
        return synonyms;
    }

    /**
     * @param synonyms the synonyms to set
     */
    public void setSynonyms(final List<String> synonyms) {
        this.synonyms = synonyms;
    }

    /**
     * @return the paths
     */
    public List<TraversalPath> getPaths() {
        return paths;
    }

    /**
     * @param paths the paths to set
     */
    public void setPaths(final List<TraversalPath> paths) {
        this.paths = paths;
    }

    @Override
    @SuppressWarnings("checkstyle:magicnumber")
    public int hashCode() {
        int result = synonyms != null ? synonyms.hashCode() : 0;
        result = 31 * result + (paths != null ? paths.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ImplementationEdge that = (ImplementationEdge) o;

        List<String> thatSynonyms = that.getSynonyms();
        List<TraversalPath> thatPaths = that.getPaths();

        if (!synonyms.containsAll(thatSynonyms) || !thatSynonyms.containsAll(synonyms)) {
            return false;
        }
        if (!paths.containsAll(thatPaths) || !thatPaths.containsAll(paths)) {
            return false;
        }

        return true;
    }
}
