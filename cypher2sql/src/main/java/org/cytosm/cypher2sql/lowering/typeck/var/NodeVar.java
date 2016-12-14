package org.cytosm.cypher2sql.lowering.typeck.var;

import org.cytosm.cypher2sql.lowering.typeck.AvailableVariables;
import org.cytosm.cypher2sql.lowering.typeck.NameProvider;
import org.cytosm.cypher2sql.lowering.typeck.types.AType;
import org.cytosm.cypher2sql.lowering.typeck.types.NodeType;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTree;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTreeBuilder;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.NodePattern;
import org.cytosm.cypher2sql.cypher.ast.expression.MapExpression;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Variable representing a node.
 */
public class NodeVar extends NodeOrRelVar {

    /**
     * Labels of the node.
     */
    public List<String> labels;

    /**
     * Node can be restricted on some
     * properties using map expressions.
     */
    public ExprTree.MapExpr predicate;

    /**
     * This represent the future properties
     * that will be required to be transmitted between selects.
     */
    public Set<String> propertiesRequired = new HashSet<>();

    public AType type() {
        return new NodeType();
    }

    public NodeVar(NodePattern np, AvailableVariables vars) {
        super(np);
        this.name = NameProvider.getName(np);
        this.labels = np.labels
                .stream().map(l -> l.name)
                .collect(Collectors.toList());

        if (np.properties.isPresent()) {
            MapExpression mapExpression = np.properties.get();
            this.predicate = (ExprTree.MapExpr) ExprTreeBuilder.buildFromCypherExpression(
                    mapExpression, vars
            );
        }
    }
}
