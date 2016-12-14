package org.cytosm.cypher2sql.lowering.typeck.rel;

import org.cytosm.cypher2sql.lowering.typeck.AvailableVariables;
import org.cytosm.cypher2sql.lowering.typeck.NameProvider;
import org.cytosm.cypher2sql.lowering.typeck.var.NodeVar;
import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;
import org.cytosm.cypher2sql.lowering.typeck.var.AliasVar;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTreeBuilder;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.NodePattern;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.RelationshipChain;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.RelationshipPattern.SemanticDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represent a relationship not necessarily named, in contrast with
 * RelVar that represent only relationship that are named and thus are
 * part of the variables set.
 *
 */
public class Relationship {

    public enum Direction {
        LEFT,
        RIGHT,
        BOTH
    }

    /**
     * The leftNode of this relationship. Any kind
     * of {@link Var} can be set here. However, the only valid
     * one that will be accepted by all passes are:
     *  - {@link NodeVar}
     *  - {@link AliasVar} that resolve to a {@link NodeVar}.
     *
     * All other types are incorrect.
     */
    public Var leftNode;

    /**
     * The rightNode of this relationship.
     */
    public Var rightNode;
    public List<String> labels = new ArrayList<>();
    public Direction direction;
    public Expr properties;

    public Relationship(final RelationshipChain rel, final AvailableVariables availablesVariables) {
        this.rightNode = availablesVariables.get(NameProvider.getName(rel.rightNode)).get();
        if (rel.element instanceof NodePattern) {
            this.leftNode = availablesVariables.get(NameProvider.getName((NodePattern) rel.element)).get();
        } else {
            RelationshipChain left = (RelationshipChain) rel.element;
            this.leftNode = (NodeVar) availablesVariables.get(NameProvider.getName(left.rightNode)).get();
        }
        if (rel.relationship.direction.equals(SemanticDirection.BOTH)) {
            this.direction = Direction.BOTH;
        } else if (rel.relationship.direction.equals(SemanticDirection.INCOMING)) {
            this.direction = Direction.LEFT;
        } else if (rel.relationship.direction.equals(SemanticDirection.OUTGOING)) {
            this.direction = Direction.RIGHT;
        }

        if (rel.relationship.properties.isPresent()) {
            this.properties = ExprTreeBuilder.buildFromCypherExpression(
                    rel.relationship.properties.get(), availablesVariables
            );
        }

        labels = rel.relationship.types.stream().map(x -> x.name).collect(Collectors.toList());
    }


}
