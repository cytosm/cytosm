package org.cytosm.common.gtop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.cytosm.common.gtop.abstraction.AbstractionEdge;
import org.cytosm.common.gtop.abstraction.AbstractionLevelGtop;
import org.cytosm.common.gtop.abstraction.AbstractionNode;
import org.cytosm.common.gtop.implementation.relational.ImplementationEdge;
import org.cytosm.common.gtop.implementation.relational.ImplementationLevelGtop;
import org.cytosm.common.gtop.implementation.relational.ImplementationNode;

/***
 * Implementation of gTop Interface.
 *
 *
 */
public abstract class GTopInterfaceImpl implements GTopInterface {

    /**
     * Gtop that is being accessed.
     */
    protected GTop gtop = null;

    // Interface Implementation:

    // Implementation-independent:
    /**
     * @return the version
     */
    @Override
    public String getVersion() {
        return gtop.getVersion();
    }

    /**
     * @return the implementationLevel
     */
    @Override
    public ImplementationLevelGtop getImplementationLevel() {
        return gtop.getImplementationLevel();
    }

    /**
     * @return the abstractionLevel
     */
    @Override
    public AbstractionLevelGtop getAbstractionLevel() {
        return gtop.getAbstractionLevel();
    }

    /**
     * @return the abstraction nodes
     */
    @Override
    @JsonIgnore
    public List<AbstractionNode> getAbstractionNodes() {
        return gtop.getAbstractionLevel().getAbstractionNodes();
    }

    /**
     * @param nodes the abstraction nodes to set
     */
    @Override
    @JsonIgnore
    public void setAbstractionNodes(final List<AbstractionNode> nodes) {
        gtop.getAbstractionLevel().setAbstractionNodes(nodes);
    }

    /**
     * @return the edges
     */
    @Override
    @JsonIgnore
    public List<AbstractionEdge> getAbstractionEdges() {
        return gtop.getAbstractionLevel().getAbstractionEdges();
    }

    /**
     * @param edges the edges to set
     */
    @JsonIgnore
    public void setAbstractionEdges(final List<AbstractionEdge> edges) {
        gtop.getAbstractionLevel().setAbstractionEdges(edges);
    }

    // Interface-friendly methods

    /***
     * @return a deduplicated edges types list.
     */
    @Override
    @JsonIgnore
    public List<String> getAllEdgeTypes() {
        final List<String> withDuplicationsAllEdgeTypes = new ArrayList<>();

        gtop.getAbstractionLevel().getAbstractionEdges()
                .forEach(edge -> withDuplicationsAllEdgeTypes.addAll(edge.getTypes()));

        // de-duplicate:
        List<String> allEdgesTypes =
                withDuplicationsAllEdgeTypes.stream().distinct().collect(Collectors.toList());

        return allEdgesTypes;
    }

    /***
     * @return a deduplicated nodes types list.
     */
    @Override
    @JsonIgnore
    public List<String> getAllNodeTypes() {
        final List<String> withDuplucationsAllNodeTypes = new ArrayList<>();

        gtop.getAbstractionLevel().getAbstractionNodes()
                .forEach(node -> withDuplucationsAllNodeTypes.addAll(node.getTypes()));

        // de-duplicate:
        List<String> allNodesTypes =
                withDuplucationsAllNodeTypes.stream().distinct().collect(Collectors.toList());

        return allNodesTypes;
    }

    /***
     * Finds an Abstraction Edge by type.
     *
     * @param types
     * @return
     */
    @Override
    @JsonIgnore
    public List<AbstractionEdge> getAbstractionEdgesByTypes(final String types) {

        List<AbstractionEdge> edgeList = new ArrayList<>();
        String typeLower = types.toLowerCase();

        edgeList = gtop
                .getAbstractionLevel().getAbstractionEdges().stream().filter(edge -> edge.getTypes().stream()
                        .map(String::toLowerCase).collect(Collectors.toList()).contains(typeLower))
                .collect(Collectors.toList());

        return edgeList;
    }

    /**
     * Finds an abstract node by types.
     *
     * @param types
     * @return
     */
    @Override
    @JsonIgnore
    public List<AbstractionNode> getAbstractionNodesByTypes(final String types) {

        List<AbstractionNode> nodeList = new ArrayList<>();
        String typeLower = types.toLowerCase();

        nodeList = gtop
                .getAbstractionLevel().getAbstractionNodes().stream().filter(node -> node.getTypes().stream()
                        .map(String::toLowerCase).collect(Collectors.toList()).contains(typeLower))
                .collect(Collectors.toList());

        return nodeList;
    }

    /***
     * Return the abstractions for a given node implementation.
     *
     * @param node implementation node that the abstraction will be found.
     * @return
     */
    @Override
    @JsonIgnore
    public List<AbstractionNode> findNodeAbstractions(final ImplementationNode node) {
        List<AbstractionNode> abstractions = new ArrayList<>();

        // if the abstraction matches any of the implementation level types, append to list.
        abstractions = gtop.getAbstractionLevel().getAbstractionNodes().stream()
                .filter(filteredNode -> !Collections.disjoint(filteredNode.getTypes(), node.getTypes()))
                .collect(Collectors.toList());


        return abstractions;
    }

    /**
     * Get the edges that connect nodeA to nodeB, including directed and undirected.
     *
     * @param nodeA nodeA to look for
     * @param nodeB nodeB to look for
     * @return list of Abstraction Edges
     */
    @Override
    @JsonIgnore
    public List<AbstractionEdge> getAllAbstractEdgesBetweenTwoNodes(final AbstractionNode nodeA,
            final AbstractionNode nodeB) {

        List<AbstractionEdge> edgeList = new ArrayList<>();

        // from A to B:
        edgeList = getDirectedAbstractEdgesBetweenTwoNodes(nodeA, nodeB);
        // from B to A:
        edgeList.addAll(getDirectedAbstractEdgesBetweenTwoNodes(nodeB, nodeA));

        // deduplicates:
        edgeList = edgeList.stream().distinct().collect(Collectors.toList());

        return edgeList;
    }

    /**
     * Get the edges that start or end with node.
     *
     * @param types types that are going used in the search
     * @return list of Abstraction Edges
     */
    @Override
    @JsonIgnore
    public List<AbstractionEdge> getAllAbstractEdgesForNodeTypes(final List<String> types) {

        List<AbstractionEdge> edgeList = new ArrayList<>();

        if (types != null && !types.isEmpty()) {
            for (AbstractionEdge edge : gtop.getAbstractionLevel().getAbstractionEdges()) {
                if (edge.getSourceType().stream()
                        .anyMatch(type -> types.contains(type.toLowerCase()) || type.toCharArray().equals("all"))
                        || edge.getDestinationType().stream().anyMatch(
                                type -> types.contains(type.toLowerCase()) || type.toCharArray().equals("all"))) {
                    edgeList.add(edge);
                }
            }
        }

        return edgeList;
    }

    /**
     * Get the edges that connect nodeA to nodeB and are directed.
     *
     * @param sourceNode sourceNode
     * @param destinationNode destinationNode
     * @return list of Abstraction Edges
     */
    @Override
    @JsonIgnore
    public List<AbstractionEdge> getDirectedAbstractEdgesBetweenTwoNodes(final AbstractionNode sourceNode,
            final AbstractionNode destinationNode) {

        List<AbstractionEdge> edgeList = new ArrayList<>();

        if (sourceNode != null && destinationNode != null) {
            for (AbstractionEdge edge : gtop.getAbstractionLevel().getAbstractionEdges()) {
                if (edge.getSourceType().stream()
                        .anyMatch(type -> sourceNode.getTypes().contains(type.toLowerCase())
                                || type.toCharArray().equals("all"))
                        && edge.getDestinationType().stream()
                                .anyMatch(type -> destinationNode.getTypes().contains(type.toLowerCase())
                                        || type.toCharArray().equals("all"))) {
                    edgeList.add(edge);
                }
            }
        }

        return edgeList;
    }

    /**
     * Return source node types for a given edge.
     *
     * @param analyzedEdge edge to lookup
     * @return set of two nodes for this edge
     */
    @Override
    @JsonIgnore
    public List<AbstractionNode> getSourceNodesForEdge(final AbstractionEdge analyzedEdge) {

        final List<AbstractionNode> duplicatedEdgeNodes = new ArrayList<>();

        for (AbstractionEdge edge : this.getAbstractionEdges()) {
            if (edge.equals(analyzedEdge)) {
                List<String> sourceTypes = analyzedEdge.getSourceType();

                if (sourceTypes.stream().anyMatch(type -> type.toCharArray().equals("all"))) {
                    // All nodes have that edge.
                    duplicatedEdgeNodes.addAll(this.getAbstractionNodes());
                } else {
                    edge.getSourceType().stream()
                            .forEach(type -> duplicatedEdgeNodes.addAll(this.getAbstractionNodesByTypes(type)));
                }
            }
        }

        // deduplicates:
        List<AbstractionNode> edgeNodes = duplicatedEdgeNodes.stream().distinct().collect(Collectors.toList());

        return edgeNodes;
    }

    /**
     * Return destination node types for a given edge.
     *
     * @param analyzedEdge edge to lookup
     * @return list node types that this edge directs to
     */
    @Override
    @JsonIgnore
    public List<AbstractionNode> getDestinationNodesForEdge(final AbstractionEdge analyzedEdge) {
        final List<AbstractionNode> duplicatedEdgeNodes = new ArrayList<>();

        for (AbstractionEdge edge : this.getAbstractionEdges()) {
            if (edge.equals(analyzedEdge)) {
                List<String> destinationTypes = analyzedEdge.getDestinationType();

                if (destinationTypes.stream().anyMatch(type -> type.toCharArray().equals("all"))) {
                    // All nodes have that edge.
                    duplicatedEdgeNodes.addAll(this.getAbstractionNodes());
                } else {
                    edge.getDestinationType().stream()
                            .forEach(type -> duplicatedEdgeNodes.addAll(this.getAbstractionNodesByTypes(type)));
                }
            }
        }

        // deduplicates:
        List<AbstractionNode> edgeNodes = duplicatedEdgeNodes.stream().distinct().collect(Collectors.toList());

        return edgeNodes;
    }

    /**
     * Return all node types associated with a given edge.
     *
     * @param edge edge to lookup
     * @return List of nodes types associated with this edge
     */
    @Override
    @JsonIgnore
    public List<AbstractionNode> getNodesForEdge(final AbstractionEdge edge) {

        List<AbstractionNode> edgeNodes = new ArrayList<>();

        edgeNodes.addAll(getSourceNodesForEdge(edge));
        edgeNodes.addAll(getDestinationNodesForEdge(edge));

        return edgeNodes;
    }

    // Implementation - Dependent:

    /**
     * @return the implementation nodes
     */
    @Override
    @JsonIgnore
    public abstract List<ImplementationNode> getImplementationNodes();

    /**
     * @param nodes the implementation nodes to set
     */
    @Override
    @JsonIgnore
    public abstract void setImplementationNodes(final List<ImplementationNode> nodes);

    /**
     * @return the edges
     */
    @Override
    @JsonIgnore
    public abstract List<ImplementationEdge> getImplementationEdges();

    /**
     * @param edges the edges to set
     */
    @Override
    @JsonIgnore
    public abstract void setImplementationEdges(final List<ImplementationEdge> edges);

    /***
     * Finds an Implementation edge by type or table name reference.
     *
     * @param type
     * @return
     */
    @Override
    @JsonIgnore
    public abstract List<ImplementationNode> getImplementationNodesByType(final String type);

    /***
     * Finds an Implementation edge by type or table name reference.
     *
     * @param type
     * @return
     */
    @Override
    @JsonIgnore
    public abstract List<ImplementationEdge> getImplementationEdgeByType(final String type);

    /***
     * Return the implementations for a given node. The node can be represented in several tables.
     *
     * @param node abstraction node
     * @return
     */
    @Override
    @JsonIgnore
    public abstract List<ImplementationNode> findNodeImplementations(final AbstractionNode node);

    /***
     * Return the implementations for a given edge.
     *
     * @param edge
     * @return
     */
    @Override
    @JsonIgnore
    public abstract ImplementationEdge findEdgeImplementation(final AbstractionEdge edge);

}
