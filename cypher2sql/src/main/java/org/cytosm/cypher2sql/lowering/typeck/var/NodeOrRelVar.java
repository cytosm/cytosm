package org.cytosm.cypher2sql.lowering.typeck.var;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;

/**
 * Sum types for:
 *  - {@link NodeVar}
 *  - {@link RelVar}
 *
 */
public abstract class NodeOrRelVar extends NodeOrTempOrRelVar {

    NodeOrRelVar(ASTNode node) {
        super(node);
    }
}
