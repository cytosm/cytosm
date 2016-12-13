package org.cytosm.common.gtop.abstraction;

import java.util.List;

/***
 * Abstraction Level gTop.
 *
 */
public class AbstractionLevelGtop {

    /**
     * List of abstraction level nodes.
     */
    private List<AbstractionNode> abstractionNodes;

    /**
     * List of abstraction level edges.
     */
    private List<AbstractionEdge> abstractionEdges;

    /***
     * Default constructor.
     */
    public AbstractionLevelGtop() {}

    /***
     * creates an abstraction level gtop.
     * @param nodes abstraction nodes on it
     * @param edges abstraction edges on it
     */
    public AbstractionLevelGtop(final List<AbstractionNode> nodes, final List<AbstractionEdge> edges) {
        abstractionNodes = nodes;
        abstractionEdges = edges;
    }

    /**
     * @param nodes the nodes to set
     */
    public void setAbstractionNodes(final List<AbstractionNode> nodes) {
        abstractionNodes = nodes;
    }

    /**
     * @return the edges
     */
    public List<AbstractionEdge> getAbstractionEdges() {
        return abstractionEdges;
    }

    /**
     * @param edges the edges to set
     */
    public void setAbstractionEdges(final List<AbstractionEdge> edges) {
        abstractionEdges = edges;
    }

    /**
     * @return the nodes
     */
    public List<AbstractionNode> getAbstractionNodes() {
        return abstractionNodes;
    }

    @Override
    @SuppressWarnings("checkstyle:magicnumber")
    public int hashCode() {
        int result = abstractionNodes != null ? abstractionNodes.hashCode() : 0;
        result = 31 * result + (abstractionEdges != null ? abstractionEdges.hashCode() : 0);
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

        final AbstractionLevelGtop that = (AbstractionLevelGtop) o;

        List<AbstractionNode> thatNodes = that.getAbstractionNodes();
        List<AbstractionEdge> thatEdges = that.getAbstractionEdges();

        if (!abstractionNodes.containsAll(thatNodes) || !thatNodes.containsAll(abstractionNodes)) {
            return false;
        }
        if (!abstractionEdges.containsAll(thatEdges) || !thatEdges.containsAll(abstractionEdges)) {
            return false;
        }

        return true;
    }
}
