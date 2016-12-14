package org.cytosm.cypher2sql.lowering.typeck.var;

import org.cytosm.cypher2sql.lowering.typeck.NameProvider;
import org.cytosm.cypher2sql.cypher.ast.ASTNode;
import org.cytosm.cypher2sql.lowering.typeck.types.AType;

/**
 * A variable declaration in a node, or relationship.
 */
public abstract class Var {


    /**
     * Name of the variable in the cypher. Two different
     * instance of Var can have the same name. However because
     * the reference are different they represent different variable.
     */
    public String name;

    /**
     * Return the type of this variable.
     * @return Returns the type of this variable
     */
    public abstract AType type();

    /**
     * Unique name given to that variable. If two Var references
     * point to different instance of Var with identical names, they
     * will have a different uniqueName.
     */
    public String uniqueName;

    Var(ASTNode n) {
        this.uniqueName = NameProvider.getUniqueName(n);
    }

    // This constructor is only used by temporary variables.
    protected Var() {}
}
