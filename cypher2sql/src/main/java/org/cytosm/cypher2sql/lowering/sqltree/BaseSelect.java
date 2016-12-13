package org.cytosm.cypher2sql.lowering.sqltree;

import org.cytosm.cypher2sql.lowering.typeck.ClauseId;

/**
 * BaseSelect is a sum type for:
 *  - {@link SimpleSelect]
 *  - {@link ScopeSelect }
 *  - {@link UnionSelect}
 *
 */
public abstract class BaseSelect implements SQLNode {

    /**
     * Contains the id for the list of variables
     * being used by this select. In the case of
     * a union it is shared between all the sub-select.
     */
    public ClauseId varId;
}
