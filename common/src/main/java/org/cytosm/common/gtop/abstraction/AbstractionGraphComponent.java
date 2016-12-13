package org.cytosm.common.gtop.abstraction;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract Class to represent abstract components in a graph, like {@link AbstractionNode} and {@link AbstractionEdge}.
 *
 *
 */
public abstract class AbstractionGraphComponent {

    /***
     * List of synonyms used, on the graph query language, to reference edges of this edge type.
     */
    protected List<String> synonyms = new ArrayList<>();

    /***
     * List of of attributes, that an edge of this edge type, can be queried on.
     */
    protected List<String> attributes = new ArrayList<>();

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
     * @return the attributes
     */
    public List<String> getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(final List<String> attributes) {
        this.attributes = attributes;
    }
}
