package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.PassAvailables;
import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelect;
import org.cytosm.cypher2sql.lowering.sqltree.ScopeSelect;
import org.cytosm.cypher2sql.lowering.typeck.VarDependencies;
import org.cytosm.cypher2sql.lowering.typeck.constexpr.ConstVal;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTree;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.cytosm.cypher2sql.cypher.ast.Statement;

/**
 */
public class MoveRestrictionTests {

    @Test
    public void testMovePredicateAsWhereClauseShouldWork() throws Cypher2SqlException {
        String cypher = "MATCH (a:Person {id: 0}) RETURN a.firstName";
        Statement query = PassAvailables.parseCypher(cypher);
        VarDependencies vars = new VarDependencies(query);
        ScopeSelect tree = SelectTreeBuilder.createQueryTree(vars, query);
        NameSubqueries.nameSubqueries(tree);
        MoveRestrictionInPattern.moveRestrictionInPatterns(tree, vars);

        // Assertions:
        SimpleSelect s = (SimpleSelect) tree.withQueries.get(0).subquery;
        Assert.assertNotNull(s.whereCondition);
        ExprTree.Eq eq = (ExprTree.Eq) s.whereCondition;
        Assert.assertEquals(((ConstVal.LongVal) eq.rhs).value, 0);
        Assert.assertEquals(((ExprTree.PropertyAccess) eq.lhs).propertyAccessed, "id");
    }

    @Test
    public void testMoveTwoPredicateAsWhereClauseShouldWork() throws Cypher2SqlException {
        String cypher = "MATCH (a:Person {id: 0})-[:KNOWS]-(b:Person {foo: 30})\n" +
                "RETURN a.firstName";
        Statement query = PassAvailables.parseCypher(cypher);
        VarDependencies vars = new VarDependencies(query);
        ScopeSelect tree = SelectTreeBuilder.createQueryTree(vars, query);
        NameSubqueries.nameSubqueries(tree);
        MoveRestrictionInPattern.moveRestrictionInPatterns(tree, vars);

        // Assertions:
        SimpleSelect s = (SimpleSelect) tree.withQueries.get(0).subquery;
        Assert.assertNotNull(s.whereCondition);
        ExprTree.And and = (ExprTree.And) s.whereCondition;
        ExprTree.Eq eq1 = (ExprTree.Eq) and.lhs;
        ExprTree.Eq eq2 = (ExprTree.Eq) and.rhs;
        Assert.assertEquals(((ConstVal.LongVal) eq1.rhs).value, 0);
        Assert.assertEquals(((ExprTree.PropertyAccess) eq1.lhs).propertyAccessed, "id");
        Assert.assertEquals(((ConstVal.LongVal) eq2.rhs).value, 30);
        Assert.assertEquals(((ExprTree.PropertyAccess) eq2.lhs).propertyAccessed, "foo");
    }

    @Test
    public void testMovePredicateEverywhere() throws Cypher2SqlException {
        String cypher = "MATCH (a:Person {id: 0})-[:KNOWS]-(b:Person)\n" +
                "MATCH (b)-[:KNOWS]-(c:Person {foo:23}) RETURN a.firstName";
        Statement query = PassAvailables.parseCypher(cypher);
        VarDependencies vars = new VarDependencies(query);
        ScopeSelect tree = SelectTreeBuilder.createQueryTree(vars, query);
        NameSubqueries.nameSubqueries(tree);
        MoveRestrictionInPattern.moveRestrictionInPatterns(tree, vars);

        // Assertions:
        SimpleSelect s = (SimpleSelect) tree.withQueries.get(0).subquery;
        Assert.assertNotNull(s.whereCondition);
        ExprTree.Eq eq = (ExprTree.Eq) s.whereCondition;
        Assert.assertEquals(((ConstVal.LongVal) eq.rhs).value, 0);
        Assert.assertEquals(((ExprTree.PropertyAccess) eq.lhs).propertyAccessed, "id");
        SimpleSelect s2 = (SimpleSelect) tree.withQueries.get(1).subquery;
        Assert.assertNotNull(s2.whereCondition);
        ExprTree.Eq eq2 = (ExprTree.Eq) s2.whereCondition;
        Assert.assertEquals(((ConstVal.LongVal) eq2.rhs).value, 23);
        Assert.assertEquals(((ExprTree.PropertyAccess) eq2.lhs).propertyAccessed, "foo");
    }

    @Test
    public void testDontAssertTwice() throws Cypher2SqlException {
        String cypher = "MATCH (a:Person {id: 0})-[:KNOWS]-(b:Person)\n" +
                "MATCH (a)-[:KNOWS]-(c:Person) " +
                "RETURN a.firstName";
        Statement query = PassAvailables.parseCypher(cypher);
        VarDependencies vars = new VarDependencies(query);
        ScopeSelect tree = SelectTreeBuilder.createQueryTree(vars, query);
        NameSubqueries.nameSubqueries(tree);
        MoveRestrictionInPattern.moveRestrictionInPatterns(tree, vars);

        // Assertions:
        SimpleSelect s = (SimpleSelect) tree.withQueries.get(0).subquery;
        Assert.assertNotNull(s.whereCondition);
        ExprTree.Eq eq = (ExprTree.Eq) s.whereCondition;
        Assert.assertEquals(((ConstVal.LongVal) eq.rhs).value, 0);
        Assert.assertEquals(((ExprTree.PropertyAccess) eq.lhs).propertyAccessed, "id");
        SimpleSelect s2 = (SimpleSelect) tree.withQueries.get(1).subquery;
        Assert.assertNull(s2.whereCondition);
    }

    @Test
    @Ignore
    public void testMoveWorksAcrossNonOptionalMatches() throws Cypher2SqlException {
        String cypher = "MATCH (a:Person {firstName: 'Richard'}) " +
                "MATCH (a {id:1099511636050}) " +
                "RETURN a.firstName";
        Statement query = PassAvailables.parseCypher(cypher);
        VarDependencies vars = new VarDependencies(query);
        ScopeSelect tree = SelectTreeBuilder.createQueryTree(vars, query);
        NameSubqueries.nameSubqueries(tree);
        MoveRestrictionInPattern.moveRestrictionInPatterns(tree, vars);

        // Assertions
        SimpleSelect s = (SimpleSelect) tree.withQueries.get(0).subquery;
        Assert.assertNotNull(s.whereCondition);
        ExprTree.And and = (ExprTree.And) s.whereCondition;
        ExprTree.Eq eq1 = (ExprTree.Eq) and.lhs;
        ExprTree.Eq eq2 = (ExprTree.Eq) and.rhs;
        Assert.assertEquals(((ConstVal.StrVal) eq1.rhs).value, "Richard");
        Assert.assertEquals(((ExprTree.PropertyAccess) eq1.lhs).propertyAccessed, "firstName");
        Assert.assertEquals(((ConstVal.LongVal) eq2.rhs).value, 1099511636050L);
        Assert.assertEquals(((ExprTree.PropertyAccess) eq2.lhs).propertyAccessed, "id");
        SimpleSelect s2 = (SimpleSelect) tree.withQueries.get(1).subquery;
        Assert.assertNull(s2.whereCondition);
    }

    @Test
    @Ignore
    public void testMoveDoesNotBreakOptionalMatches() throws Cypher2SqlException {
        String cypher = "MATCH (a:Person {firstName: 'Richard'}) " +
                "OPTIONAL MATCH (a {id:1099511636050}) " +
                "RETURN a.firstName";
        Statement query = PassAvailables.parseCypher(cypher);
        VarDependencies vars = new VarDependencies(query);
        ScopeSelect tree = SelectTreeBuilder.createQueryTree(vars, query);
        NameSubqueries.nameSubqueries(tree);
        MoveRestrictionInPattern.moveRestrictionInPatterns(tree, vars);

        // Assertions
        SimpleSelect s = (SimpleSelect) tree.withQueries.get(0).subquery;
        Assert.assertNotNull(s.whereCondition);
        ExprTree.Eq eq1 = (ExprTree.Eq) s.whereCondition;
        Assert.assertEquals(((ConstVal.StrVal) eq1.rhs).value, "Richard");
        Assert.assertEquals(((ExprTree.PropertyAccess) eq1.lhs).propertyAccessed, "firstName");

        SimpleSelect s2 = (SimpleSelect) tree.withQueries.get(1).subquery;
        Assert.assertNotNull(s2.whereCondition);
        ExprTree.Eq eq2 = (ExprTree.Eq) s2.whereCondition;
        Assert.assertEquals(((ConstVal.LongVal) eq2.rhs).value, 1099511636050L);
        Assert.assertEquals(((ExprTree.PropertyAccess) eq2.lhs).propertyAccessed, "id");
    }
}
