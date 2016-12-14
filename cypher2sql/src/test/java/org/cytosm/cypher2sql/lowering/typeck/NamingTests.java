package org.cytosm.cypher2sql.lowering.typeck;

import org.cytosm.cypher2sql.PassAvailables;
import org.cytosm.cypher2sql.cypher.ast.SingleQuery;
import org.cytosm.cypher2sql.cypher.ast.Statement;
import org.cytosm.cypher2sql.lowering.typeck.VarDependencies;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

/**
 */
public class NamingTests extends BaseVarTests {

    @Test
    public void testGeneratedNames() throws Exception {
        String cypher = "MATCH p=(a)-[r]-(b) WITH a AS foo RETURN foo.bar";
        Statement st = PassAvailables.parseCypher(cypher);
        SingleQuery sq = (SingleQuery) st.query.part;
        VarDependencies dependencies = new VarDependencies(st);

        List<Var> vars = dependencies.getAllVariables().stream().collect(Collectors.toList());

        Assert.assertEquals(5, vars.size());
        Assert.assertEquals("__cytosm6$7", getByName(vars, "p").uniqueName);
        Assert.assertEquals("__cytosm9$10", getByName(vars, "a").uniqueName);
        Assert.assertEquals("__cytosm13$14", getByName(vars, "r").uniqueName);
        Assert.assertEquals("__cytosm17$18", getByName(vars, "b").uniqueName);
        Assert.assertEquals("__cytosm30$33", getByName(vars, "foo").uniqueName);
    }
}
