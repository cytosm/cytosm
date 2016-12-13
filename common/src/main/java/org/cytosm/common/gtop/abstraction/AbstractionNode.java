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
        synonyms = new ArrayList<String>();
        attributes = new ArrayList<String>();
    }

    /**
     * Generates an abstraction node using arguments.
     *
     * @param synonyms synonyms used for the node
     * @param attributes attributes of that node
     */
    public AbstractionNode(final List<String> synonyms, final List<String> attributes) {
        this.synonyms = synonyms;
        this.attributes = attributes;
    }

    @Override
    @SuppressWarnings("checkstyle:magicnumber")
    public int hashCode() {
        // In order to produce the same hash-code.
        Collections.sort(synonyms);
        Collections.sort(attributes);

        int result = synonyms != null ? synonyms.hashCode() : 0;
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        return (this.hashCode() == o.hashCode());
    }

}
