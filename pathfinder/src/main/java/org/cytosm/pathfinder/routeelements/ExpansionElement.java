package org.cytosm.pathfinder.routeelements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytosm.common.gtop.abstraction.AbstractionGraphComponent;

/***
 * Abstract class used as base class for ExpansionNode and ExpansionEdge.
 *
 *
 */
public abstract class ExpansionElement {

    protected Map<String, String> attributeMap = new HashMap<>();
    protected List<String> types = new ArrayList<>();

    /***
     * possible gtop abstract entities that match the hints.
     */
    protected Set<AbstractionGraphComponent> matchedGtopAbstractionEntitities = new HashSet<>();

    /***
     * resolved gtop solution for the element.
     */
    protected AbstractionGraphComponent equivalentMaterializedGtop;

    /***
     * variable name used in Cypher.
     */
    protected String variable = "";

    /***
     * A hint is an information, provided by the user when writing the query, that may narrow down
     * gtop possibilities.
     */
    protected boolean hintAvailable = false;

    /***
     * Default constructor, used for cloning.
     */
    public ExpansionElement() {}

    /***
     * Default constructor.
     * 
     * @param argTypes types of the Expansion element
     * @param attributes Attributes of the expansion element
     */
    public ExpansionElement(List<String> argTypes, Map<String, String> attributes) {
        attributeMap = attributes;
        this.types.addAll(argTypes);

        /***
         * There are some hints provided by the user that restricts gtop possibilities
         */
        if (!types.isEmpty() || !attributes.isEmpty()) {
            hintAvailable = true;
        }
    }

    /**
     * @return the variableName
     */
    public String getVariable() {
        return variable;
    }

    /**
     * @return the attributeMap
     */
    public Map<String, String> getAttributeMap() {
        return attributeMap;
    }

    /***
     * Updates the attribute Map after the creation of the element. The value that is given to the
     * attribute is not relevant for gTop search.
     *
     * @param attribute attribute
     */
    public void updateAttributeMap(String attribute) {
        // Otherwise the external context may overwrite attribute information retrieved from the
        // relationship chain.
        if (!attributeMap.containsKey(attribute)) {
            attributeMap.put(attribute, "");
        }

        // Hint was added
        hintAvailable = true;
    }

    /**
     * @param attributeMap the attributeMap to set
     */
    public void setAttributeMap(Map<String, String> attributeMap) {
        this.attributeMap = attributeMap;
    }

    /**
     * @return the types
     */
    public List<String> getTypes() {
        return types;
    }

    /**
     * @param types the types to set
     */
    public void setTypes(List<String> types) {
        this.types = types;
    }

    /***
     * Returns an abstract edge or node.
     *
     * @return matchedGtopAbstractionEntitities
     */
    public Set<AbstractionGraphComponent> getMatchedGtopAbstractionEntities() {
        return matchedGtopAbstractionEntitities;
    }

    /**
     * @param matchedGtopAbstractionEntities the matchedGtopAbstractionEntities to set
     */
    public void addMatchedGtopAbstractionEntities(List<AbstractionGraphComponent> matchedGtopAbstractionEntities) {
        this.matchedGtopAbstractionEntitities.addAll(matchedGtopAbstractionEntities);
    }

    /**
     * @return the hintAvailable
     */
    public boolean isHintAvailable() {
        return hintAvailable;
    }

    /**
     * @return the isNode. True if instance of ExpansionNode. False is instance of ExpansionEdge.
     */
    public abstract boolean isNode();

    /**
     * @return the equivalentMaterializedGtop
     */
    public Object getEquivalentMaterializedGtop() {
        return equivalentMaterializedGtop;
    }

    /**
     * @param equivalentMaterializedGtop the equivalentMaterializedGtop to set
     */
    public void setEquivalentMaterializedGtop(AbstractionGraphComponent equivalentMaterializedGtop) {
        this.equivalentMaterializedGtop = equivalentMaterializedGtop;
    }
}
