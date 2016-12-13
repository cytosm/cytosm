package org.cytosm.cypher2sql.lowering.typeck.var;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;

/**
 */
public abstract class NodeOrTempOrRelVar extends Var {

    NodeOrTempOrRelVar(ASTNode node) {
        super(node);
    }

    protected NodeOrTempOrRelVar() {}
}
