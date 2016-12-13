package org.cytosm.cypher2sql.lowering.typeck;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.NamedPatternPart;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.NodePattern;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.PatternPart;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.RelationshipPattern;
import org.cytosm.cypher2sql.cypher.ast.clause.projection.ReturnItem;
import org.cytosm.cypher2sql.cypher.ast.expression.Variable;

/**
 * Generates names for various part of the steps.
 *
 */
public class NameProvider {

    private NameProvider() {}

    private static int lastSourceName = 0;
    private static int lastTemporary = 0;

    public static String getName(NodePattern np) {
        if (np.variable.isPresent()) {
            return np.variable.get().name;
        } else {
            return getAnonymouseName(np);
        }
    }

    public static String getUniqueName(ASTNode n) {
        Variable var;
        if (n instanceof NodePattern) {
            NodePattern np = (NodePattern) n;
            if (np.variable.isPresent()) {
                var = np.variable.get();
            } else {
                return getAnonymouseName(np);
            }
        } else if (n instanceof NamedPatternPart) {
            var = ((NamedPatternPart) n).variable;
        } else if (n instanceof RelationshipPattern) {
            var = ((RelationshipPattern) n).variable.get();
        } else if (n instanceof ReturnItem.Aliased) {
            var = ((ReturnItem.Aliased) n).alias;
        } else {
            throw new RuntimeException("Can't give a name to this type of ASTNode: " + n.getClass().getName());
        }
        return getUniqueNameForVariable(var);
    }

    public static String genFromItemName() {
        return "__src" + lastSourceName++;
    }

    public static String getUniqueTempVarName() {
        return "__tmp" + lastTemporary++;
    }

    private static String getUniqueNameForVariable(Variable var) {
        return "__cytosm" + var.span.lo + "$" + var.span.hi;
    }

    private static String getAnonymouseName(ASTNode n) {
        return "__ano" + n.span.lo + "__";
    }
}
