package org.cytosm.common.gtop;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.cytosm.common.gtop.abstraction.AbstractionLevelGtop;
import org.cytosm.common.gtop.implementation.relational.ImplementationLevelGtop;

/***
 * Graph Topology (.gtop) format that maps a relational database into a set of Nodes and Edges.
 *
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GTop {

    /***
     * Graph Topology version.
     */
    private String version = "1.0";

    /***
     * Abstraction Level Graph Topology: an implementation independent description of the graph
     * topology mapped underneath.
     */
    private AbstractionLevelGtop abstractionLevel = new AbstractionLevelGtop();

    /***
     * Implementation Level Graph Topology: a description of how the graph structure is mapped into
     * the relational world.
     */
    private ImplementationLevelGtop implementationLevel = new ImplementationLevelGtop();

    /***
     * Default constructor.
     */
    public GTop() {}

    /***
     * Default constructor.
     *
     * @param abstractionLevel Abstraction Level gtop to be used.
     * @param implementationLevel Implementation Level gtop to be used.
     */
    public GTop(final AbstractionLevelGtop abstractionLevel, final ImplementationLevelGtop implementationLevel) {
        this.abstractionLevel = abstractionLevel;
        this.implementationLevel = implementationLevel;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return the abstractionLevel
     */
    public AbstractionLevelGtop getAbstractionLevel() {
        return abstractionLevel;
    }

    /**
     * @return the implementationLevel
     */
    public ImplementationLevelGtop getImplementationLevel() {
        return implementationLevel;
    }
}
