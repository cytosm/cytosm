package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.lowering.sqltree.*;
import org.cytosm.cypher2sql.lowering.typeck.VarDependencies;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprVar;
import org.cytosm.cypher2sql.lowering.typeck.var.NodeVar;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;
import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTree;

import java.util.List;
import java.util.Map;

/**
 */
public class MoveRestrictionInPattern {


    /**
     * This pass find restriction in patterns and move them in WHERE
     * clause where they will be rendered appropriately.
     * @param sqltree is the root of the SQL tree.
     * @param vars is the list of vars.
     */
    public static void moveRestrictionInPatterns(ScopeSelect sqltree, VarDependencies vars) {

        for (WithSelect w: sqltree.withQueries) {
            dispatch(w, vars);
        }

        // While tempting to do the following, this would be invalid
        // because restriction can't appears in return expressions.
        // It can only appears in a RETURN where clause.
        // moveRestrictionInPatterns(sqltree.ret, vars);
    }

    private static void dispatch(BaseSelect select, VarDependencies vars) {
        if (select instanceof SimpleSelect) {
            moveRestrictionInPatterns((SimpleSelect) select, vars);
        } else if (select instanceof ScopeSelect) {
            moveRestrictionInPatterns((ScopeSelect) select, vars);
        } else if (select instanceof UnionSelect) {
            for (SimpleOrScopeSelect s: ((UnionSelect) select).unions) {
                dispatch(s, vars);
            }
        } else if (select instanceof WithSelect) {
            dispatch(((WithSelect) select).subquery, vars);
        }
    }

    /**
     * This routine move restriction in patterns as where
     * clause as SQL simply ignore those restriction.
     * @param select is the select of the SQL tree.
     * @param vars is the list of vars.
     */
    private static void moveRestrictionInPatterns(SimpleSelect select, VarDependencies vars) {
        List<Var> usedVars = vars.getUsedVars(select.varId);

        Expr whereCondition = select.whereCondition;
        for (Var var: usedVars) {
            if (var instanceof NodeVar) {
                if (((NodeVar) var).predicate != null) {
                    whereCondition = mergePredicate(whereCondition, (NodeVar) var);
                    // Make sure we will never move the predicate again
                    // if the variable is used later.
                    ((NodeVar) var).predicate = null;
                }
            }
        }
        select.whereCondition = whereCondition;
    }

    private static Expr mergePredicate(Expr whereCondition, NodeVar var) {
        for (Map.Entry<String, Expr> entry: var.predicate.props.entrySet()) {
            ExprTree.PropertyAccess prop = new ExprTree.PropertyAccess(
                    entry.getKey(), new ExprVar(var)
            );
            whereCondition = and(whereCondition,
                    new ExprTree.Eq(prop, entry.getValue())
            );
        }
        return whereCondition;
    }

    private static Expr and(Expr prev, Expr next) {
        if (prev == null) {
            return next;
        }
        return new ExprTree.And(prev, next);
    }
}
