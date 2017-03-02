package org.cytosm.pathfinder.routeelements;

import java.util.*;

/***
 * Represent an edge in the path traversal.
 *
 *
 */
public class ExpansionEdge extends ExpansionElement {

    /**
     * Otherwise is pointed towards the left.
     */
    private boolean toRight = false;

    /***
     * Minimum range. Used in query length expansion
     */
    private int minimumRange = 0;

    /***
     * Maximum range. Used in query length expansion
     */
    private int maximumRange = 0;

    /***
     * has * character. Currently is not supported, since it is an infinite depth query.
     */
    private boolean matchall = false;

    /***
     * If this edge has already been expanded, to solve wildcards.
     */
    private boolean hasBeenExpanded = false;

    private boolean directed = false;

    /**
     * Direction of the Edge.
     */
    public enum Direction {
        Right,
        Left,
        Both
    }

    /***
     * Default constructor.
     *
     * @param attributes attributes from the edge
     * @param cypherInformation additional edge information available in the cypher
     */
    public ExpansionEdge(final List<String> types, final Map<String, String> attributes,
                         final Direction direction, final OptionalLong minimumRange,
                         final OptionalLong maximumRange, final boolean matchall,
                         final Optional<String> variable) {
        super(types, attributes);

        if (!types.isEmpty()) {
            hintAvailable = true;
        }
        if (direction.equals(Direction.Right) || direction.equals(Direction.Left)) {
            directed = true;
            if (direction.equals(Direction.Right)) {
                toRight = true;
            }
            // otherwise is left
        }

        if (minimumRange.isPresent()) {
            this.minimumRange = (int) minimumRange.getAsLong();
        }

        if (maximumRange.isPresent()) {
            this.maximumRange = (int) maximumRange.getAsLong();
        }

        this.matchall = matchall;

        if (variable.isPresent()) {
            this.variable = variable.get();
        }
   }

    /***
     * Used for generating an edge with the same attributes, but with with the expansion flag
     * already set.
     *
     * @param expansionEdge original expansion edge to be cloned
     */
    public ExpansionEdge(final ExpansionEdge expansionEdge) {
        types.addAll(expansionEdge.getTypes());

        if (expansionEdge.directed) {
            directed = true;
            if (expansionEdge.isToRight()) {
                toRight = true;
            } else {
                toRight = false;
            }
        } else {
            directed = false;
        }

        variable = expansionEdge.getVariable();

        hasBeenExpanded = true;

        hintAvailable = expansionEdge.isHintAvailable();
        attributeMap = expansionEdge.getAttributeMap();
    }

    /**
     * @return the toRight
     */
    public boolean isToRight() {
        return toRight;
    }

    /**
     * @return the toLeft
     */
    public boolean isToLeft() {
        return !toRight;
    }

    /**
     * @param toRight the toRight to set
     */
    public void setToRight(final boolean toRight) {
        this.toRight = toRight;
    }

    /**
     * @return the variable
     */
    @Override
    public String getVariable() {
        return variable;
    }

    /**
     * @param variable the variable to set
     */
    public void setVariable(final String variable) {
        this.variable = variable;
    }

    /**
     * @return the minimumRange
     */
    public int getMinimumRange() {
        return minimumRange;
    }

    /**
     * @param minimumRange the minimumRange to set
     */
    public void setMinimumRange(final int minimumRange) {
        this.minimumRange = minimumRange;
    }

    /**
     * @return the maximumRange
     */
    public int getMaximumRange() {
        return maximumRange;
    }

    /**
     * @param maximumRange the maximumRange to set
     */
    public void setMaximumRange(final int maximumRange) {
        this.maximumRange = maximumRange;
    }

    /**
     * @return the matchall
     */
    public boolean isMatchall() {
        return matchall;
    }

    /**
     * @param matchall the matchall to set
     */
    public void setMatchall(final boolean matchall) {
        this.matchall = matchall;
    }

    /***
     * Checks if the edge can be expanded.
     *
     * @return true if the edge is expandable, otherwise false
     */
    public boolean isExpandable() {

        boolean isExpandable = false;

        // TODO Match all is not implemented
        if (!hasBeenExpanded) {
            if ((getMaximumRange() != 0 && getMinimumRange() != 0) || matchall) {
                isExpandable = true;
            }
        }

        return isExpandable;
    }

    /***
     * Expand node to generate a relationship chain that will match the expansion.
     *
     * @param nodeToTheLeft left node
     * @param nodeToTheRight right node
     * @return list of possible expansions for that given node - edge - node triple.
     */
    public List<List<ExpansionElement>> expand(final ExpansionNode nodeToTheLeft, final ExpansionNode nodeToTheRight) {

        // Disable expansions to not expand in the future:
        hasBeenExpanded = true;

        List<List<ExpansionElement>> possibleExpansions = new ArrayList<>();

        // TODO add star clause
        for (int currentRange = minimumRange; currentRange <= maximumRange; currentRange++) {
            List<ExpansionElement> currentExpansion = expandOnRange(currentRange, nodeToTheLeft, nodeToTheRight);
            possibleExpansions.add(currentExpansion);
        }


        return possibleExpansions;
    }

    private List<ExpansionElement> expandOnRange(final int currentRange, final ExpansionNode nodeToTheLeft,
            final ExpansionNode nodeToTheRight) {

        List<ExpansionElement> expandedRoute = new ArrayList<>(currentRange * 2 + 1);

        expandedRoute.add(nodeToTheLeft);
        expandedRoute.add(this);

        for (int expansioncounter = 1; expansioncounter < currentRange; expansioncounter++) {
            expandedRoute.add(ExpansionNode.generateAnonymousNode());
            expandedRoute.add(this.expandedClone());
        }

        expandedRoute.add(nodeToTheRight);

        return expandedRoute;
    }

    private ExpansionEdge expandedClone() {
        // This returns a complete clone of the edge, but already expanded (without wildcards) and
        // (obviously) as another object. So in the gluing, it wont use the original edge position
        // in the map.
        ExpansionEdge expandedClone = new ExpansionEdge(this);
        return expandedClone;
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
    public boolean isNode() {
        return false;
    }

    /***
     * Used in order to generate reverse hash of directed edges. Reverses the edge position.
     */
    public void reverseDirection() {
        this.toRight = !toRight;

    }
}
