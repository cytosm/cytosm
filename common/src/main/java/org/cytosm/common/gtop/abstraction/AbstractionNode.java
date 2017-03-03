package org.cytosm.common.gtop.abstraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/***
 * Abstraction Node.
 *
 */
public class AbstractionNode extends AbstractionGraphComponent {

    /**
     * Generates an empty abstraction node.
     */
    public AbstractionNode() {
        types = new ArrayList<String>();
        attributes = new ArrayList<String>();
    }

    /**
     * Generates an abstraction node using arguments.
     *
     * @param types Nodes types used for the node
     * @param attributes attributes of that node
     */
    public AbstractionNode(final List<String> types, final List<String> attributes) {
        this.types = types;
        this.attributes = attributes;
    }

    @Override
    @SuppressWarnings("checkstyle:magicnumber")
    public int hashCode() {
        // In order to produce the same hash-code.
        Collections.sort(types);
        Collections.sort(attributes);

        int result = types != null ? types.hashCode() : 0;
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        return (this.hashCode() == o.hashCode());
    }

}
