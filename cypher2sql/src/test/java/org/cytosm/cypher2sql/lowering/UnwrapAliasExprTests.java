package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.PassAvailables;
import org.cytosm.cypher2sql.lowering.sqltree.ScopeSelect;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleOrScopeSelect;
import org.cytosm.cypher2sql.lowering.sqltree.UnionSelect;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTree;
import org.junit.Assert;
import org.junit.Test;

/**
 */
public class UnwrapAliasExprTests extends BaseLDBCTests {

    @Test
    public void testThatNoAliasExprIsPresentOnNestedScopeSelect() throws Exception {
        String cypher = "" +
                "MATCH (a:Message)\n" +
                "RETURN a.length";
        ScopeSelect tree = PassAvailables.cypher2sqlOnExpandedPaths(getGTopInterface(), cypher);

        Assert.assertEquals(tree.ret.exportedItems.size(), 1);
        Assert.assertTrue(tree.ret.exportedItems.get(0) instanceof ExprTree.AliasExpr);

        UnionSelect unionSelect = (UnionSelect) tree.withQueries.get(0).subquery;

        Assert.assertEquals(unionSelect.unions.size(), 2);

        for (SimpleOrScopeSelect select: unionSelect.unions) {
            ScopeSelect nestedScopeSelect = (ScopeSelect) select;
            Assert.assertFalse(nestedScopeSelect.ret.exportedItems.get(0) instanceof ExprTree.AliasExpr);
        }
    }
}
