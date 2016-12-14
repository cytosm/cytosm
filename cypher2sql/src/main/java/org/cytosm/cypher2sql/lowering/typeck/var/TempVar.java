package org.cytosm.cypher2sql.lowering.typeck.var;

import org.cytosm.cypher2sql.lowering.typeck.NameProvider;
import org.cytosm.cypher2sql.lowering.typeck.types.AType;
import org.cytosm.cypher2sql.lowering.typeck.types.NodeType;

/**
 * Temporary variables that might be needed during the expansion.
 * Those variable are special in the sense that they are
 * not visible in the AvailableVars data structure at any point.
 *
 */
public class TempVar extends NodeOrTempOrRelVar {

    public AType type() {
        // FIXME
        return new NodeType();
    }

    public TempVar() {
        this.uniqueName = NameProvider.getUniqueTempVarName();
    }
}
