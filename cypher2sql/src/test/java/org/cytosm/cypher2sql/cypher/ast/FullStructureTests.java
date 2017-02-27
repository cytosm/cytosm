package org.cytosm.cypher2sql.cypher.ast;

import org.cytosm.cypher2sql.cypher.ast.SingleQuery;
import org.cytosm.cypher2sql.cypher.ast.Statement;
import org.cytosm.cypher2sql.cypher.ast.clause.Clause;
import org.cytosm.cypher2sql.cypher.ast.clause.match.Match;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.NodePattern;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.RelationshipChain;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.RelationshipPattern;
import org.cytosm.cypher2sql.cypher.ast.clause.projection.Return;
import org.cytosm.cypher2sql.cypher.ast.clause.projection.ReturnItem;
import org.cytosm.cypher2sql.cypher.ast.clause.projection.With;
import org.cytosm.cypher2sql.cypher.ast.expression.*;
import org.cytosm.cypher2sql.cypher.parser.ASTBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

/**
 * Those tests take a long time to write but
 * they check the entire structure to make sure everything is correct.
 */
public class FullStructureTests {

    // This test would be hard to debug but it test all the features
    // together and so might be more likely to catch any bug introduced.
    @Test
    public void testFullExampleWithoutRelationships() {
        String cypher = "MATCH (a:Person {id: 'test'}) WHERE a.x > (12 + 4 / 1)\n" +
                "WITH a AS foobar RETURN 234, foobar.test ORDER BY {} SKIP 23 LIMIT 42";
        Statement st = ASTBuilder.parse(cypher);
        List<Clause> clauses = ((SingleQuery) st.query.part).clauses;
        Assert.assertEquals(3, clauses.size());
        Assert.assertTrue(clauses.get(0) instanceof Match);
        Assert.assertTrue(clauses.get(1) instanceof With);
        Assert.assertTrue(clauses.get(2) instanceof Return);

        Match m = (Match) clauses.get(0);
        With w = (With) clauses.get(1);
        Return r = (Return) clauses.get(2);

        // Match
        Assert.assertEquals(m.optional, false);
        Assert.assertNotNull(m.pattern);
        Assert.assertTrue(m.where.isPresent());

        Assert.assertEquals(m.pattern.patternParts.size(), 1);
        Assert.assertTrue(m.pattern.patternParts.get(0).element instanceof NodePattern);
        NodePattern np = (NodePattern) m.pattern.patternParts.get(0).element;
        Assert.assertEquals(np.labels.size(), 1);
        Assert.assertEquals(np.labels.get(0).name, "Person");
        Assert.assertTrue(np.variable.isPresent());
        Assert.assertEquals(np.variable.get().name, "a");

        Assert.assertTrue(np.properties.isPresent());
        MapExpression props = np.properties.get();

        Assert.assertEquals(props.props.get(0).getKey().name, "id");
        Assert.assertEquals(((Literal.StringLiteral) props.props.get(0).getValue()).value, "test");

        Binary.GreaterThan whereExpr = (Binary.GreaterThan) m.where.get().expression;
        Property lhs = (Property)  whereExpr.lhs;
        Assert.assertEquals(lhs.propertyKey.name, "x");
        Assert.assertEquals(((Variable) lhs.map).name, "a");

        Binary.Add rhs = (Binary.Add) whereExpr.rhs;
        Assert.assertEquals(((Literal.Integer) rhs.lhs).value, 12L);
        Assert.assertEquals(((Literal.Integer) ((Binary.Divide) rhs.rhs).lhs).value, 4L);
        Assert.assertEquals(((Literal.Integer) ((Binary.Divide) rhs.rhs).rhs).value, 1L);

        // With
        Assert.assertEquals(w.returnItems.size(), 1);
        Assert.assertTrue(w.returnItems.get(0) instanceof ReturnItem.Aliased);
        Assert.assertEquals(((ReturnItem.Aliased) w.returnItems.get(0)).alias.name, "foobar");
        Assert.assertEquals(((Variable) w.returnItems.get(0).expression).name, "a");

        // Return
        Assert.assertEquals(r.returnItems.size(), 2);
        Assert.assertTrue(r.returnItems.get(0) instanceof ReturnItem.Unaliased);
        Assert.assertTrue(r.returnItems.get(1) instanceof ReturnItem.Unaliased);
        Assert.assertEquals(((ReturnItem.Unaliased) r.returnItems.get(1)).name, "foobar.test");
        Assert.assertTrue(r.orderBy.isPresent());
        Assert.assertEquals(r.orderBy.get().sortItems.size(), 1);
        Assert.assertTrue(r.skip.isPresent());
        Assert.assertEquals(((Literal.Integer) r.skip.get().expression).value, 23);
        Assert.assertTrue(r.limit.isPresent());
        Assert.assertEquals(((Literal.Integer) r.limit.get().expression).value, 42);
    }

    @Test
    public void testRelationshipChains1() {
        String cypher = "MATCH (a)-[r:TEST]-(c)";
        Statement st = ASTBuilder.parse(cypher);
        List<Clause> clauses = ((SingleQuery) st.query.part).clauses;
        Match m = (Match) clauses.get(0);

        Assert.assertTrue(m.pattern.patternParts.get(0).element instanceof RelationshipChain);

        RelationshipChain rc = (RelationshipChain) m.pattern.patternParts.get(0).element;
        Assert.assertTrue(rc.element instanceof NodePattern);
        Assert.assertTrue(((NodePattern) rc.element).variable.isPresent());
        Assert.assertTrue(rc.rightNode.variable.isPresent());
        Assert.assertEquals(((NodePattern) rc.element).variable.get().name, "a");
        Assert.assertEquals(rc.rightNode.variable.get().name, "c");
        Assert.assertEquals(rc.relationship.direction, RelationshipPattern.SemanticDirection.BOTH);
        Assert.assertEquals(rc.relationship.length, Optional.empty());
        Assert.assertEquals(rc.relationship.types.size(), 1);
        Assert.assertEquals(rc.relationship.types.get(0).name, "TEST");
    }

    @Test
    public void testListExpressions() {
        String cypher = "MATCH (a) WHERE a.lastName IN ['foo','bar'] RETURN a.firstName";
        Statement st = ASTBuilder.parse(cypher);
        List<Clause> clauses = ((SingleQuery) st.query.part).clauses;
        Match m = (Match) clauses.get(0);

        Assert.assertTrue(m.where.isPresent());
        Assert.assertTrue(m.where.get().expression instanceof Binary.In);

        Binary.In in = (Binary.In) m.where.get().expression;

        Assert.assertTrue(in.rhs instanceof ListExpression);

        ListExpression list = (ListExpression) in.rhs;

        Assert.assertEquals(2, list.exprs.size());
        Assert.assertEquals("foo", ((Literal.StringLiteral) list.exprs.get(0)).value);
        Assert.assertEquals("bar", ((Literal.StringLiteral) list.exprs.get(1)).value);
    }
}