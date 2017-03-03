package org.cytosm.pathfinder.routeelements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 * Represents a node in the path traversal.
 *
 *
 */
public class ExpansionNode extends ExpansionElement {

    /***
     * Used for Directed Node Expansion.
     */
    private boolean isSource = false;

    /***
     * When it is source, shows the position is was found on source searching.
     */
    private int sourcePosition = 0;

    /***
     * Only source nodes are never leaves.
     */
    protected boolean hasBeenLeaf = false;
    protected boolean inMultipleSegments = false;

    /***
     * Default constructor.
     *
     * @param types node types.
     * @param attributes attributes of the node
     * @param argVariable variables used to refer to given node
     */
    public ExpansionNode(List<String> types, Map<String, String> attributes, String argVariable) {
        super(types, attributes);
        variable = argVariable;
    }

    @Override
    public boolean isNode() {
        return true;
    }

    /***
     * Used for generating a node with the same attributes. but with only one possible gtop
     *
     * @param expansionNode original node
     */
    public ExpansionNode(ExpansionNode expansionNode) {
        types.addAll(expansionNode.getTypes());
        attributeMap = expansionNode.getAttributeMap();
        variable = expansionNode.getVariable();
    }

    /***
     * Generates an anonymous node.
     *
     * @return the anonymous node
     */
    public static ExpansionNode generateAnonymousNode() {
        List<String> types = new ArrayList<>();
        Map<String, String> attributes = new HashMap<>();
        String argVariable = null;

        return new ExpansionNode(types, attributes, argVariable);
    }

    /***
     * Claims that element to a defined segment.
     */
    public void claimElementToSegment() {
        if (!this.hasBeenLeaf) {
            this.hasBeenLeaf = true;
        } else {
            // belongs to two segments
            inMultipleSegments = true;
        }
    }

    /**
     * @return the inMultipleSegments
     */
    public boolean isInMultipleSegments() {
        return inMultipleSegments;
    }

    /**
     * @return the hasBeenLeaf
     */
    public boolean hasBeenLeaf() {
        return hasBeenLeaf;
    }

    /**
     * @return the isSource
     */
    public boolean isSource() {
        return isSource;
    }

    /**
     * @param setSource the isSource to set
     */
    public void setSource(boolean setSource) {
        this.isSource = setSource;
    }

    /**
     * @return the sourcePosition
     */
    public int getSourcePosition() {
        return sourcePosition;
    }

    /**
     * @param sourcePosition the sourcePosition to set
     */
    public void setSourcePosition(int sourcePosition) {
        this.sourcePosition = sourcePosition;
    }

}
