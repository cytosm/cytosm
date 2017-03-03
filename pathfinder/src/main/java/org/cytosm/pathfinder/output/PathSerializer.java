package org.cytosm.pathfinder.output;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.cytosm.common.gtop.abstraction.AbstractionEdge;
import org.cytosm.common.gtop.abstraction.AbstractionNode;
import org.cytosm.pathfinder.routeelements.ExpansionEdge;
import org.cytosm.pathfinder.routeelements.ExpansionElement;

/***
 * Serialises an expansion element list. In this scenario, serializes to neo4j cypher. TODO Use abstract class? So we
 * can expand it to gremlin.
 *
 *
 */
public class PathSerializer implements Serializer {
    @Override
    public List<String> serialize(final List<List<ExpansionElement>> routes) {
        // ()-[:knows]->()
        List<String> paths = new ArrayList<>();
        routes.stream().forEach((expansionList) -> {
            StringBuilder sb = new StringBuilder();
            expansionList.forEach((expansion) -> {
                if (expansion.isNode()) {
                    AbstractionNode node = (AbstractionNode) expansion.getEquivalentMaterializedGtop();
                    sb.append("(");
                    if (expansion.getVariable() != null) {
                        sb.append(expansion.getVariable());
                    }
                    sb.append(":");
                    sb.append(node.getTypes().get(0));

                    addAttributeMap(sb, expansion);

                    sb.append(")");
                } else {
                    ExpansionEdge expansionEdge = (ExpansionEdge) expansion;
                    String leftEdge = "-";
                    String rightEdge = "-";
                    if (expansionEdge.isDirected()) {
                        if (expansionEdge.isToLeft()) {
                            leftEdge = "<-";
                        } else {
                            rightEdge = "->";
                        }
                    }


                    sb.append(leftEdge);
                    sb.append("[");
                    sb.append(expansion.getVariable());
                    sb.append(":");
                    AbstractionEdge edge = (AbstractionEdge) expansion.getEquivalentMaterializedGtop();
                    sb.append(edge.getTypes().get(0));

                    addAttributeMap(sb, expansion);

                    sb.append("]");
                    sb.append(rightEdge);
                }
            });
            paths.add(sb.toString());
        });

        return paths;
    }

    private void addAttributeMap(final StringBuilder sb, final ExpansionElement expansion) {
        if (!expansion.getAttributeMap().isEmpty()) {
            StringBuilder attributes = new StringBuilder();
            StringBuilder attribute = new StringBuilder();
            expansion.getAttributeMap().entrySet().forEach((entry) -> {
                if (!StringUtils.isEmpty(entry.getValue())) {
                    if (attribute.length() != 0) {
                        attribute.append(",");
                    }
                    attribute.append(entry.getKey() + ":" + entry.getValue());
                }
            });

            if (attribute.length() != 0) {
                attributes.append(" {");
                attributes.append(attribute);
                attributes.append("}");
                sb.append(attributes);
            }
        }
    }
}
