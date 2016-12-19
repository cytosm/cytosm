package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;
import org.cytosm.cypher2sql.lowering.sqltree.ScopeSelect;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelect;
import org.cytosm.cypher2sql.lowering.sqltree.visitor.Walk;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprVar;
import org.cytosm.cypher2sql.lowering.typeck.types.BoolType;
import org.cytosm.cypher2sql.lowering.typeck.types.NumberType;
import org.cytosm.cypher2sql.lowering.typeck.types.StringType;
import org.cytosm.cypher2sql.lowering.typeck.var.AliasVar;
import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;
import org.cytosm.cypher2sql.lowering.typeck.constexpr.ConstExprFolder;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprWalk;

import java.util.*;

import static org.cytosm.cypher2sql.lowering.exceptions.fns.LambdaExceptionUtil.rethrowConsumer;


/**
 * This class contains all passes that replace {@link AliasVar}
 * when this is possible. Example are:
 *
 *  - Unused {@link AliasVar}s
 *  - Constant propagation (Remove associated {@link AliasVar})
 */
public class UnwrapAliasVar {

    /**
     * Unwrap AliasVar that map to constants such as String or Number.
     * It does that by evaluating them using {@link ConstExprFolder}
     * @param tree is the SQL tree to unwrap.
     */
    public static void unwrapConstants(ScopeSelect tree) throws Cypher2SqlException {
        Walk.walkSQLNode(new UnwrapConstantsVisitor(), tree);
    }

    private static class UnwrapConstantsVisitor extends Walk.BaseVisitorAndExprFolder {

        @Override
        protected ExprWalk.IdentityFolder<Cypher2SqlException> makeExprFolder(SimpleSelect context) {
            return new UnwrapConstantsFolder();
        }
    }

    private static class UnwrapConstantsFolder extends ExprWalk.IdentityFolder<Cypher2SqlException> {

        private ConstExprFolder folder = new ConstExprFolder();

        @Override
        public Expr foldVariable(ExprVar expr) throws Cypher2SqlException {
            if (expr.var instanceof AliasVar) {
                AliasVar var = (AliasVar) expr.var;
                if (var.type() instanceof NumberType ||
                        var.type() instanceof StringType ||
                        var.type() instanceof BoolType) {
                    try {
                        return ExprWalk.fold(folder, var.aliased);
                    } catch (Cypher2SqlException e) {}
                }
            }
            return super.foldVariable(expr);
        }
    }

    /**
     * Remove unused {@link AliasVar}. This pass will typically remove AliasVars that
     * have been "unwrapped" either because the properties being accessed have vanished
     * or because the constant that thehave been
     */
    public static void removeUnusedVariables(ScopeSelect tree) throws Cypher2SqlException {
        CollectUnusedVariables visitor = new CollectUnusedVariables();
        Walk.walkSQLNode(visitor, tree);
        Walk.walkSQLNode(new RemoveMatchingVariables(visitor.useCount), tree);
    }

    private static class CollectUnusedVariables extends Walk.BaseVisitorAndExprVisitor {

        final Map<ScopeSelect, Map<AliasVar, Integer>> useCount = new HashMap<>();
        private ScopeSelect currentScopeSelect;
        private int defaultCountValue = 0;

        @Override
        public void visitScopeSelect(ScopeSelect scopeSelect) throws Cypher2SqlException {
            ScopeSelect parentScope = currentScopeSelect;
            currentScopeSelect = scopeSelect;
            useCount.put(scopeSelect, new HashMap<>());
            scopeSelect.withQueries.forEach(rethrowConsumer(this::visitWithSelect));
            defaultCountValue = 1;
            this.visitSimpleSelect(scopeSelect.ret);
            defaultCountValue = 0;
            currentScopeSelect = parentScope;
        }

        @Override
        protected ExprWalk.Visitor makeExprVisitor() {
            return new CollectUnusedAliasVar(useCount.get(currentScopeSelect), defaultCountValue);
        }
    }

    private static class CollectUnusedAliasVar extends ExprWalk.BaseVisitor {

        private final Map<AliasVar, Integer> useCount;
        private final int defaultCountValue;

        CollectUnusedAliasVar(Map<AliasVar, Integer> useCount, int defaultCountValue) {
            this.useCount = useCount;
            this.defaultCountValue = defaultCountValue;
        }

        @Override
        public void visitVariable(ExprVar expr) {
            if (expr.var instanceof AliasVar) {
                useCount.put((AliasVar) expr.var, useCount.getOrDefault(expr.var, defaultCountValue) + 1);
            }
        }
    }

    private static class RemoveMatchingVariables extends Walk.BaseSQLNodeVisitor {

        private final Map<ScopeSelect, Map<AliasVar, Integer>> useCount;
        private ScopeSelect currentScopeSelect;

        RemoveMatchingVariables(final Map<ScopeSelect, Map<AliasVar, Integer>> useCount) {
            this.useCount = useCount;
        }

        @Override
        public void visitSimpleSelect(SimpleSelect simpleSelect) throws Cypher2SqlException {
            simpleSelect.exportedItems.removeIf(x -> {
                if (x instanceof ExprVar) {
                    Var v = ((ExprVar) x).var;
                    if (v instanceof AliasVar) {
                        return useCount.get(currentScopeSelect).get(v) == 1;
                    }
                }
                return false;
            });
        }

        @Override
        public void visitScopeSelect(ScopeSelect scopeSelect) throws Cypher2SqlException {
            ScopeSelect parentscope = currentScopeSelect;
            currentScopeSelect = scopeSelect;
            super.visitScopeSelect(scopeSelect);
            currentScopeSelect = parentscope;
        }
    }
}
