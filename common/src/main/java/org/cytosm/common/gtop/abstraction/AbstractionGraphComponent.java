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
     * List of types used, on the graph query language, to reference edges of this edge type.
     */
    protected List<String> types = new ArrayList<>();

    /***
     * List of of attributes, that an edge of this edge type, can be queried on.
     */
    protected List<String> attributes = new ArrayList<>();

    /**
     * @return the types
     */
    public List<String> getTypes() {
        return types;
    }

    /**
     * @param types the types to set
     */
    public void setTypes(final List<String> types) {
        this.types = types;
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
