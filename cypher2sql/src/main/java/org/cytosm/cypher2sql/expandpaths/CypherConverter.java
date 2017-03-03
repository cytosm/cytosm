package org.cytosm.cypher2sql.expandpaths;

import java.util.*;
import java.util.stream.Collectors;

import org.cytosm.cypher2sql.cypher.parser.ASTBuilder;
import org.cytosm.cypher2sql.cypher.constexpr.ConstExpressionFolder;
import org.cytosm.pathfinder.input.CanonicalConverter;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.cytosm.pathfinder.CanonicalRoutes;
import org.cytosm.pathfinder.routeelements.ExpansionEdge;
import org.cytosm.pathfinder.routeelements.ExpansionElement;
import org.cytosm.pathfinder.routeelements.ExpansionNode;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.RelationshipPattern.SemanticDirection;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.*;
import org.cytosm.cypher2sql.cypher.ast.expression.*;

import java.util.Iterator;

/***
 * Converts from Cypher to {@link CanonicalRoutes}.
 *
 *
 */
public class CypherConverter extends CanonicalConverter {

    private static final Log LOG = LogFactory.getLog(CanonicalConverter.class);

    /***
     * Default constructor.
     */
    public CypherConverter() {}

    @Override
    public CanonicalRoutes translate(final String inputInformation) {



        CanonicalRoutes abstractionPathWithoutWildcards;

        abstractionPathWithoutWildcards = parseCypher(inputInformation);

        return abstractionPathWithoutWildcards;
    }

    /***
     * Parses a relationship chain information from Cypher to the Expansion Element format.
     *
     * @param relationshipchain
     * @return
     */
    private CanonicalRoutes parseCypher(final String relationshipchain) {

        Pattern pattern = ASTBuilder.parsePattern(relationshipchain);
        List<ExpansionElement> all = extractElements(pattern);

        // Add external context to information, if available:
        if (!MapUtils.isEmpty(externalContext)) {
            List<ExpansionElement> elementList;

            // Only elements that can be referenced on the outside are of interest. They require
            // variables for this.
            elementList = all.stream().filter(element -> !StringUtils.isEmpty(element.getVariable()))
                    .collect(Collectors.toList());

            for (Map.Entry<String, List<String>> entry : externalContext.entrySet()) {

                final String variableReference = entry.getKey();
                Optional<ExpansionElement> elementReferencedByVariable = elementList.stream()
                        .filter(element -> element.getVariable().equals(variableReference)).findAny();

                if (!elementReferencedByVariable.isPresent()) {
                    LOG.error("External context presented does not match any element in the relationship chain");
                    // TODO What is the expected system behaviour in this situation?
                } else {
                    // add attribute to element
                    ExpansionElement element = elementReferencedByVariable.get();

                    // Update the element attribute on all the attributes declared on external
                    // context. Variable "n" may have attributes "age" and "citizenship"
                    for (String attributeName : entry.getValue()) {
                        element.updateAttributeMap(attributeName);
                    }
                }
            }
        }

        return new CanonicalRoutes(extractNodes(all), extractEdges(all));
    }

    /***
     * Extract edge information from Cypher.
     */
    private List<ExpansionEdge> extractEdges(final List<ExpansionElement> relationshipchain) {

        return relationshipchain.stream().filter(x -> x instanceof ExpansionEdge)
                .map(x -> (ExpansionEdge) x)
                .collect(Collectors.toList());
    }

    /**
     * Converts an edge string into an expansion edge, taking into account the variables /
     * properties on the edge.
     *
     * @param edge
     * @return
     */
    private static ExpansionEdge convertToAbstractionEdge(RelationshipPattern edge) {

        Map<String, String> attributes = new HashMap<>();
        ExpansionEdge.Direction direction;
        OptionalLong maximumRange = OptionalLong.empty();
        OptionalLong minimumRange = OptionalLong.empty();
        Optional<String> variable = Optional.empty();
        boolean matchall = false;

        // This code is missing the point really. It does not support the all
        // class of MapExpression that can exist. For example:
        // {id: {b: "test"}}
        if (edge.properties.isPresent()) {
            MapExpression expr = edge.properties.get();
            Map<String, Object> res = ConstExpressionFolder.evalMapExpression(expr);

            // With a MapExpression such as the one given previously, we
            // would collect id as a string.
            res.forEach((key, val) -> attributes.put(key, val.toString()));
        }

        // check if directed
        if (edge.direction.equals(SemanticDirection.OUTGOING)) {
            direction = ExpansionEdge.Direction.Right;
        } else if (edge.direction.equals(SemanticDirection.INCOMING)) {
            direction = ExpansionEdge.Direction.Left;
        } else if (edge.direction.equals(SemanticDirection.BOTH)) {
            direction = ExpansionEdge.Direction.Both;
        } else {
            // This code is unreachable with the Neo4j 3.0 parser.
            // We keep the check for two reasons:
            //  * If the AST change, then we will get a clear error message
            //    instead of a silent failure.
            //  * For the migration to the new parser
            throw new RuntimeException("Unhandled SemanticDirection type.");
        }

        // Types
        List<String> types = edge.types.stream().map(t -> t.name).collect(Collectors.toList());

        // Variable
        if (edge.variable.isPresent()) {
            variable = Optional.of(edge.variable.get().name);
        }

        // Range
        if (edge.length.isPresent()) {
            Optional<Range> optRange = edge.length.get();
            if (optRange.isPresent()) {
                Range range = optRange.get();
                if (range.upper.isPresent()) {
                    maximumRange = OptionalLong.of(range.upper.get().value);
                }
                if (range.lower.isPresent()) {
                    minimumRange = OptionalLong.of(range.lower.get().value);
                }
            } else {
                // Wildcard
                matchall = true;
            }
        }

        return new ExpansionEdge(types, attributes, direction,
                minimumRange, maximumRange, matchall, variable);
    }

    private List<ExpansionElement> extractElements(final Pattern relationshipChain) {
        List<ExpansionElement> res = new ArrayList<>(5);
        Iterator<PatternPart> iter = relationshipChain.patternParts.iterator();

        while (iter.hasNext()) {
            PatternPart pp = iter.next();
            extractElementsRec(pp.element, res);
        }

        return res;
    }

    private void extractElementsRec(final PatternElement pe, final List<ExpansionElement> result) {
        if (pe instanceof NodePattern) {
            result.add(convertToAbstractionNode((NodePattern) pe));
        } else {
            RelationshipChain rc = (RelationshipChain) pe;
            extractElementsRec(rc.element, result);
            result.add(convertToAbstractionEdge(rc.relationship));
            extractElementsRec(rc.rightNode, result);
        }

    }

    /***
     * Extract node information from cypher.
     */
    private List<ExpansionNode> extractNodes(final List<ExpansionElement> relationshipchain) {
        return relationshipchain.stream().filter(x -> x instanceof ExpansionNode)
                .map(x -> (ExpansionNode) x)
                .collect(Collectors.toList());
    }

    private static ExpansionNode convertToAbstractionNode(NodePattern node) {


        List<String> typesList = new ArrayList<>();
        Map<String, String> attributes = new HashMap<>();

        // This code is missing the point really. It does not support the all
        // class of MapExpression that can exist. For example:
        // {id: {b: "test"}}
        if (node.properties.isPresent()) {
            MapExpression expr = (MapExpression) node.properties.get();
            Map<String, Object> res = ConstExpressionFolder.evalMapExpression(expr);

            // With a MapExpression such as the one given previously, we
            // would collect id as a string.
            res.forEach((key, val) -> attributes.put(key, val.toString()));
        }

        Iterator<LabelName> iter = node.labels.iterator();
        while (iter.hasNext()) {
            typesList.add(iter.next().name);
        }

        String variable = "";

        if (node.variable.isPresent()) {
            variable = node.variable.get().name;
        }

        return new ExpansionNode(typesList, attributes, variable);
    }
}
