package org.cytosm.common.gtop.abstraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/***
 * Abstraction Edge.
 *
 *
 */
public class AbstractionEdge extends AbstractionGraphComponent {

    /***
     * Types of source nodes that can have this edge. It can be a list of node types or the
     * classifier ALL.
     */
    protected List<String> sourceType = new ArrayList<>();

    /***
     * Types of destination nodes that can have this edge. It can be a list of node types or the
     * classifier ALL.
     */
    protected List<String> destinationType = new ArrayList<>();

    /***
     * true if the edge is directed, false otherwise.
     */
    protected boolean directed = false;

    /***
     * Default constructor.
     */
    public AbstractionEdge() {}

    /***
     * Default constructor.
     *
     * @param types types for that edge
     * @param attributes attributes of that edge.
     * @param sourceType source type of the edge
     * @param destinationType destionation type of the edge
     * @param directed is the edge directed?
     */
    public AbstractionEdge(final List<String> types, final List<String> attributes, final List<String> sourceType,
            final List<String> destinationType, final boolean directed) {
        this.types = types;
        this.attributes = attributes;
        this.sourceType = sourceType;
        this.destinationType = destinationType;
        this.directed = directed;
    }

    /**
     * @return the sourceType
     */
    public List<String> getSourceType() {
        return sourceType;
    }

    /**
     * @param sourceType the sourceType to set
     */
    public void setSourceType(final List<String> sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * @return the destinationType
     */
    public List<String> getDestinationType() {
        return destinationType;
    }

    /**
     * @param destinationType the destinationType to set
     */
    public void setDestinationType(final List<String> destinationType) {
        this.destinationType = destinationType;
    }

    /**
     * @return the directed
     */
    public boolean isDirected() {
        return directed;
    }

    /**
     * @param directed the directed to set
     */
    public void setDirected(final boolean directed) {
        this.directed = directed;
    }

    @Override
    @SuppressWarnings("checkstyle:magicnumber")
    public int hashCode() {
        // In order to produce the same hash code.
        Collections.sort(types);
        Collections.sort(attributes);
        Collections.sort(sourceType);
        Collections.sort(destinationType);

        int result = types != null ? types.hashCode() : 0;
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 31 * result + (sourceType != null ? sourceType.hashCode() : 0);
        result = 31 * result + (destinationType != null ? destinationType.hashCode() : 0);
        result = 31 * result + (directed ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        return (this == o);
    }
}
