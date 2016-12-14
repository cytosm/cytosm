package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.PassAvailables;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelect;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelectWithInnerJoins;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelectWithLeftJoins;
import org.cytosm.cypher2sql.lowering.sqltree.ScopeSelect;
import org.junit.Assert;
import org.junit.Test;

/**
 */
public class SelectTreeBuilderTests {

    @Test
    public void testStructure() {
        String cypher = "" +
                "MATCH (a)\n" +
                "MATCH (a)\n" +
                "RETURN a.firstName;";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assert.assertEquals(tree.ret.limit, -1);
        Assert.assertEquals(tree.ret.skip, -1);
        Assert.assertTrue(tree.ret.orderBy.isEmpty());
        Assert.assertEquals(tree.ret.whereCondition, null);
        Assert.assertEquals(tree.withQueries.size(), 2);
        Assert.assertEquals(tree.withQueries.get(0).varId, tree.withQueries.get(0).subquery.varId);
        Assert.assertEquals(tree.withQueries.get(1).varId, tree.withQueries.get(1).subquery.varId);
        Assert.assertNotNull(tree.withQueries.get(0).subquery.varId);
        Assert.assertNotNull(tree.withQueries.get(1).subquery.varId);
        Assert.assertNotNull(tree.ret.varId);
    }

    @Test
    public void testSkipRet() {
        String cypher = "" +
                "MATCH (a) RETURN a.firstName SKIP 2 + 4*10";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assert.assertEquals(tree.ret.skip, 42);
    }

    @Test
    public void testSkipWith() {
        String cypher = "" +
                "MATCH (a) WITH a SKIP 2 + 4*10 RETURN a";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assert.assertEquals(((SimpleSelect) tree.withQueries.get(1).subquery).skip, 42);
    }

    @Test
    public void testLimitRet() {
        String cypher = "" +
                "MATCH (a) RETURN a.firstName LIMIT 2 + 4*10";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assert.assertEquals(tree.ret.limit, 42);
    }

    @Test
    public void testLimitWith() {
        String cypher = "" +
                "MATCH (a) WITH a.firstName AS afirstName LIMIT 2 + 4*10 RETURN 50";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assert.assertEquals(((SimpleSelect) tree.withQueries.get(1).subquery).limit, 42);
    }

    @Test
    public void testOrderByASC() {
        String cypher = "MATCH (a) RETURN a.firstName ORDER BY a.firstName ASC";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assert.assertEquals(tree.ret.orderBy.get(0).descending, false);
    }

    @Test
    public void testOrderByDESC() {
        String cypher = "MATCH (a) RETURN a.firstName ORDER BY a.firstName DESC";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assert.assertEquals(tree.ret.orderBy.get(0).descending, true);
    }

    @Test
    public void testOptionalMatchNormalMatch() {
        String cypher = "" +
                "MATCH (a:Person {id: 0})\n" +
                "OPTIONAL MATCH (a)-[:KNOWS]-(b:Person)\n" +
                "RETURN a.firstName, b.firstName";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assert.assertEquals(tree.withQueries.size(), 2);
        Assert.assertTrue(tree.withQueries.get(0).subquery instanceof SimpleSelectWithInnerJoins);
        Assert.assertTrue(tree.withQueries.get(1).subquery instanceof SimpleSelectWithLeftJoins);
    }
}
