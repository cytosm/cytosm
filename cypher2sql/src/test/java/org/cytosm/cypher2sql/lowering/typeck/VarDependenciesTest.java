package org.cytosm.cypher2sql.lowering.typeck;

import org.cytosm.cypher2sql.PassAvailables;
import org.cytosm.cypher2sql.cypher.ast.clause.projection.With;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprVar;
import org.cytosm.cypher2sql.lowering.typeck.rel.Relationship;
import org.cytosm.cypher2sql.lowering.typeck.var.AliasVar;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;
import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;
import org.cytosm.cypher2sql.lowering.typeck.expr.ExprTree;
import org.junit.Assert;
import org.junit.Test;
import org.cytosm.cypher2sql.cypher.ast.*;
import org.cytosm.cypher2sql.cypher.ast.clause.*;
import org.cytosm.cypher2sql.cypher.ast.clause.match.*;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.*;


import java.util.*;
import java.util.stream.Collectors;

/**
 */
public class VarDependenciesTest extends BaseVarTests {

    /**
     * Simple example where we make sure that the variable
     * reference is the same between the MATCH that has declared it
     * and the RETURN statement.
     */
    @Test
    public void testVariablesMatchReturn() {
        String cypher = "MATCH (a) RETURN a.firstName";
        Statement st = PassAvailables.parseCypher(cypher);
        SingleQuery sq = (SingleQuery) st.query.part;
        VarDependencies dependencies = new VarDependencies(st);
        ClauseId id = this.genClauseForASTNode(
                this.getPatternPart(sq.clauses.iterator().next()).next()
        );
        List<Expr> ret = dependencies.getReturnExprs();
        ExprTree.PropertyAccess retProp = (ExprTree.PropertyAccess) ((ExprTree.AliasExpr) ret.get(0)).expr;

        List<Var> matchVars = dependencies.getUsedVars(id);
        Assert.assertEquals(matchVars.size(), 1);
        Assert.assertEquals(matchVars.get(0).name, "a");
        Assert.assertSame(matchVars.get(0), ((ExprVar) retProp.expression).var);
        Assert.assertEquals(retProp.propertyAccessed, "firstName");
    }

    @Test
    public void testReachableVariablesMatchMatchWithMatchReturn() {
        String cypher = "" +
                "MATCH (a)--(b)\n" +     // match0
                "MATCH (b)\n" +          // match1
                "WITH (a)\n" +           // with2
                "MATCH (b)\n" +          // match3
                "RETURN a.firstName";    // ret
        Statement st = PassAvailables.parseCypher(cypher);
        SingleQuery sq = (SingleQuery) st.query.part;
        VarDependencies dependencies = new VarDependencies(st);
        Iterator<Clause> iter = sq.clauses.iterator();
        AvailableVariables match0 = dependencies.getReachableVars(this.genClauseForASTNode(this.getPatternPart(iter.next()).next()));
        AvailableVariables match1 = dependencies.getReachableVars(this.genClauseForASTNode(this.getPatternPart(iter.next()).next()));
        AvailableVariables with2 = dependencies.getReachableVars(this.genClauseForASTNode(iter.next()));
        AvailableVariables match3 = dependencies.getReachableVars(this.genClauseForASTNode(this.getPatternPart(iter.next()).next()));
        AvailableVariables ret = dependencies.getReachableVars(this.genClauseForASTNode(iter.next()));

        Assert.assertTrue(match0.isEmpty());
        Assert.assertTrue(match1.get("a").isPresent());
        Assert.assertTrue(match1.get("b").isPresent());
        Assert.assertTrue(with2.get("a").isPresent());
        Assert.assertTrue(with2.get("b").isPresent());
        Assert.assertTrue(match3.get("a").isPresent());
        Assert.assertFalse(match3.get("b").isPresent());
        Assert.assertTrue(ret.get("a").isPresent());
        Assert.assertTrue(ret.get("b").isPresent());
    }

    @Test
    public void testUsedVariablesMatchMatchWithMatchReturn() {
        String cypher = "" +
                "MATCH (a)--(b)\n" +     // match0
                "MATCH (b)\n" +          // match1
                "WITH (a)\n" +           // with2
                "MATCH (b)\n" +          // match3
                "RETURN a.firstName";    // ret
        Statement st = PassAvailables.parseCypher(cypher);
        SingleQuery sq = (SingleQuery) st.query.part;
        VarDependencies dependencies = new VarDependencies(st);
        Iterator<Clause> iter = sq.clauses.iterator();
        List<Var> match0 = dependencies.getUsedVars(this.genClauseForASTNode(this.getPatternPart(iter.next()).next()));
        List<Var> match1 = dependencies.getUsedVars(this.genClauseForASTNode(this.getPatternPart(iter.next()).next()));
        List<Var> with2 = dependencies.getUsedVars(this.genClauseForASTNode(iter.next()));
        List<Var> match3 = dependencies.getUsedVars(this.genClauseForASTNode(this.getPatternPart(iter.next()).next()));
        List<Expr> ret = dependencies.getReturnExprs();
        ExprTree.PropertyAccess retProp = (ExprTree.PropertyAccess) ((ExprTree.AliasExpr) ret.get(0)).expr;

        // Make sure we have collected something.
        Assert.assertEquals(match0.size(), 2);
        // Make sure nothing is null.
        Assert.assertNotSame(getByName(match0, "b"), null);
        Assert.assertNotSame(getByName(match0, "a"), null);
        Assert.assertNotSame(getByName(match3, "b"), null);
        // Make sure that references are the same where they should be...
        Assert.assertSame(getByName(match0, "b"), getByName(match1, "b"));
        Assert.assertSame(getByName(match0, "a"), getByName(with2, "a"));
        Assert.assertSame(getByName(match0, "a"), ((ExprVar) retProp.expression).var);
        // ...and not the same where they should not be.
        Assert.assertNotSame(getByName(match0, "b"), getByName(match3, "b"));
    }

    @Test
    public void testVariablesHiddenInMapExpression1() {
        String cypher = "" +
                "MATCH (a:Person)\n" +
                "WITH a, {b: {c: \"test\", d: a.firstName}} AS b\n" +
                "MATCH (c:Person) WHERE c.firstName = b.b.c\n" +
                "RETURN a, b, c";
        Statement st = PassAvailables.parseCypher(cypher);
        SingleQuery sq = (SingleQuery) st.query.part;
        VarDependencies dependencies = new VarDependencies(st);
        Iterator<Clause> iter = sq.clauses.iterator();
        List<Var> match0 = dependencies.getUsedVars(this.genClauseForASTNode(this.getPatternPart(iter.next()).next()));
        List<Var> with1 = dependencies.getUsedVars(this.genClauseForASTNode(iter.next()));
        List<Var> match2 = dependencies.getUsedVars(this.genClauseForASTNode(this.getPatternPart(iter.next()).next()));
        List<Expr> ret = dependencies.getReturnExprs();

        // Check the MapExpression
        Assert.assertSame(getByName(match0, "a"), getByName(with1, "a"));
        AliasVar b = (AliasVar) getByName(with1, "b");
        Assert.assertSame(getByName(match0, "a") ,
                ((ExprVar) ((ExprTree.PropertyAccess) ((ExprTree.MapExpr)((ExprTree.MapExpr) b.aliased)
                        .props.get("b"))
                        .props.get("d"))
                        .expression).var);
        Assert.assertSame(getByName(with1, "b"), ((ExprVar) ((ExprTree.AliasExpr) ret.get(1)).expr).var);
    }

    @Test
    public void testVariablesMatchCommaReturn() {
        String cypher = "" +
                "MATCH (a), (b)\n" +
                "RETURN a.firstName";
        Statement st = PassAvailables.parseCypher(cypher);
        SingleQuery sq = (SingleQuery) st.query.part;
        VarDependencies dependencies = new VarDependencies(st);
        Match match = ((Match) sq.clauses.iterator().next());
        Iterator<PatternPart> iter = this.getPatternPart(match);
        List<Var> match0 = dependencies.getUsedVars(this.genClauseForASTNode(iter.next()));
        List<Var> comma1 = dependencies.getUsedVars(this.genClauseForASTNode(iter.next()));

        // Make sure we have collected something.
        Assert.assertEquals(match0.size(), 1);
        Assert.assertEquals(comma1.size(), 1);
        Assert.assertEquals(match0.get(0).name, "a");
        Assert.assertEquals(comma1.get(0).name, "b");
    }

    @Test
    public void testRelationships() {
        String cypher = "" +
                "MATCH (a)--(b)\n" +
                "RETURN 42";
        Statement st = PassAvailables.parseCypher(cypher);
        SingleQuery sq = (SingleQuery) st.query.part;
        VarDependencies dependencies = new VarDependencies(st);
        Match match = ((Match) sq.clauses.iterator().next());
        Iterator<PatternPart> iter = this.getPatternPart(match);
        List<Relationship> match0 = dependencies.getRelationships(this.genClauseForASTNode(iter.next()));
        Assert.assertEquals(match0.size(), 1);
        Assert.assertEquals(match0.get(0).leftNode.name, "a");
        Assert.assertEquals(match0.get(0).rightNode.name, "b");
    }

    @Test
    public void testRelationshipsPathOrder() {
        String cypher = "" +
                "MATCH (a)--(b)--(c)\n" +
                "RETURN 42";
        Statement st = PassAvailables.parseCypher(cypher);
        SingleQuery sq = (SingleQuery) st.query.part;
        VarDependencies dependencies = new VarDependencies(st);
        Match match = ((Match) sq.clauses.iterator().next());
        Iterator<PatternPart> iter = this.getPatternPart(match);
        List<Relationship> match0 = dependencies.getRelationships(this.genClauseForASTNode(iter.next()));

        // Make sure the path has the correct order
        Assert.assertEquals(match0.size(), 2);
        Assert.assertEquals(match0.get(0).leftNode.name, "a");
        Assert.assertEquals(match0.get(0).rightNode.name, "b");
        Assert.assertEquals(match0.get(1).leftNode.name, "b");
        Assert.assertEquals(match0.get(1).rightNode.name, "c");
        // Make sure the reference to variables are the same
        Assert.assertSame(match0.get(0).rightNode, match0.get(1).leftNode);
    }

    @Test
    public void testGetUsedAndIndirectUsedVars1() {
        String cypher = "" +
                "MATCH (a)--(b)\n" +
                "MATCH (b)--(c)\n" +
                "MATCH (a)--(d)\n" +
                "RETURN 42";
        Statement st = PassAvailables.parseCypher(cypher);
        SingleQuery sq = (SingleQuery) st.query.part;
        VarDependencies dependencies = new VarDependencies(st);

        Iterator<Clause> clauses = sq.clauses.iterator();
        List<Var> match0 = dependencies.getUsedAndIndirectUsedVars(
                this.genClauseForASTNode(this.getPatternPart(clauses.next()).next()));
        List<Var> match1 = dependencies.getUsedAndIndirectUsedVars(
                this.genClauseForASTNode(this.getPatternPart(clauses.next()).next()));
        List<Var> match2 = dependencies.getUsedAndIndirectUsedVars(
                this.genClauseForASTNode(this.getPatternPart(clauses.next()).next()));

        Assert.assertEquals(match0.size(), 2);
        Assert.assertEquals(match1.size(), 3);
        Assert.assertEquals(match2.size(), 4);
    }

    @Test
    public void testGetUsedAndIndirectUsedVars2() {
        String cypher = "" +
                "MATCH (a)--(e)\n" +
                "MATCH (b)--(c)\n" +
                "MATCH (a)--(d)\n" +
                "RETURN 42";
        Statement st = PassAvailables.parseCypher(cypher);
        SingleQuery sq = (SingleQuery) st.query.part;
        VarDependencies dependencies = new VarDependencies(st);

        Iterator<Clause> clauses = sq.clauses.iterator();
        List<Var> match0 = dependencies.getUsedAndIndirectUsedVars(
                this.genClauseForASTNode(this.getPatternPart(clauses.next()).next()));
        List<Var> match1 = dependencies.getUsedAndIndirectUsedVars(
                this.genClauseForASTNode(this.getPatternPart(clauses.next()).next()));
        List<Var> match2 = dependencies.getUsedAndIndirectUsedVars(
                this.genClauseForASTNode(this.getPatternPart(clauses.next()).next()));

        Assert.assertEquals(match0.size(), 2);
        Assert.assertEquals(match1.size(), 2);
        Assert.assertEquals(match2.size(), 3);
    }

    @Test
    public void testVariableDefinedInReturn() {
        String cypher = "" +
                "MATCH (a)\n" +
                "RETURN a AS b ORDER BY b";
        Statement st = PassAvailables.parseCypher(cypher);
        SingleQuery sq = (SingleQuery) st.query.part;
        VarDependencies deps = new VarDependencies(st);
        AvailableVariables ret = deps.getReachableVars(new ClauseId(sq.clauses.get(1)));
        List<Var> allVars = deps.getAllVariables().stream().collect(Collectors.toList());
        Assert.assertFalse(ret.get("b").isPresent());
        Assert.assertNotNull(getByName(allVars, "b"));
    }

    @Test
    public void testReachableVarsInOrderBy() {
        String cypher = "" +
                "MATCH (a)\n" +
                "WITH a.firstName AS foo ORDER BY foo\n" +
                "RETURN foo";
        Statement st = PassAvailables.parseCypher(cypher);
        SingleQuery sq = (SingleQuery) st.query.part;
        VarDependencies deps = new VarDependencies(st);
        With with = (With) sq.clauses.get(1);
        AvailableVariables orderBy = deps.getReachableVars(new ClauseId(with.orderBy.get()));
        Assert.assertTrue(orderBy.get("foo").isPresent());
    }
}