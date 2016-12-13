package org.cytosm.common.gtop.implementation.relational;

import java.util.List;

import org.cytosm.common.gtop.implementation.graphmetadata.GraphMetadata;

/**
 * A relational implementation description of the graph declared in the AbstractionLevelGtop.
 *
 *
 */
public class ImplementationLevelGtop {

    /***
     * Set of restrictions imposed to the graph analysis.
     */
    private GraphMetadata graphMetadata = new GraphMetadata();

    /***
     * Set of rules that will interpret data has a node of a given type.
     */
    private List<ImplementationNode> implementationNodes;

    /***
     * Set of rules that will interpret data has an edge of a given type.
     */
    private List<ImplementationEdge> implementationEdges;

    /***
     * Default constructor.
     */
    public ImplementationLevelGtop() {};

    /***
     * Generates an implementation level gtop.
     * @param graphMetadata the implementation level graph metadata
     * @param nodes the implementation nodes
     * @param edges the implementation edges
     */
    public ImplementationLevelGtop(final GraphMetadata graphMetadata, final List<ImplementationNode> nodes,
            final List<ImplementationEdge> edges) {
        this.graphMetadata = graphMetadata;
        implementationNodes = nodes;
        implementationEdges = edges;
    }

    /**
     * @return the graphMetadata
     */
    public GraphMetadata getGraphMetadata() {
        return graphMetadata;
    }

    /**
     * @param graphMetadata the graphMetadata to set
     */
    public void setGraphMetadata(final GraphMetadata graphMetadata) {
        this.graphMetadata = graphMetadata;
    }

    /**
     * @return the nodes
     */
    public List<ImplementationNode> getImplementationNodes() {
        return implementationNodes;
    }

    /**
     * @param nodes the nodes to set
     */
    public void setImplementationNodes(final List<ImplementationNode> nodes) {
        implementationNodes = nodes;
    }

    /**
     * @return the edges
     */
    public List<ImplementationEdge> getImplementationEdges() {
        return implementationEdges;
    }

    /**
     * @param edges the edges to set
     */
    public void setImplementationEdges(final List<ImplementationEdge> edges) {
        implementationEdges = edges;
    }

    @Override
    @SuppressWarnings("checkstyle:magicnumber")
    public int hashCode() {
        int result = implementationNodes != null ? implementationNodes.hashCode() : 0;
        result = 31 * result + (implementationEdges != null ? implementationEdges.hashCode() : 0);
        result = 31 * result + (graphMetadata != null ? graphMetadata.hashCode() : 0);
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

        final ImplementationLevelGtop that = (ImplementationLevelGtop) o;

        List<ImplementationNode> thatNodes = that.getImplementationNodes();
        List<ImplementationEdge> thatEdges = that.getImplementationEdges();
        GraphMetadata thatMeta = that.getGraphMetadata();

        if (!implementationNodes.containsAll(thatNodes) || !thatNodes.containsAll(implementationNodes)) {
            return false;
        }
        if (!implementationEdges.containsAll(thatEdges) || !thatEdges.containsAll(implementationEdges)) {
            return false;
        }
        if (!graphMetadata.equals(thatMeta)) {
            return false;
        }

        return true;
    }

}
