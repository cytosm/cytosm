package org.cytosm.cypher2sql.lowering.typeck;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;
import org.cytosm.cypher2sql.cypher.ast.clause.Clause;
import org.cytosm.cypher2sql.cypher.ast.clause.match.Match;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.PatternPart;
import org.cytosm.cypher2sql.lowering.typeck.ClauseId;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;

import java.util.Iterator;
import java.util.List;

/**
 */
public class BaseVarTests {

    protected ClauseId genClauseForASTNode(ASTNode node) {
        return new ClauseId(node);
    }

    protected Var getByName(List<Var> l, String name) {
        return l.stream().filter(x -> x.name.equals(name)).findFirst().orElseGet(() -> null);
    }

    protected Iterator<PatternPart> getPatternPart(Clause cl) {
        return ((Match) cl).pattern.patternParts.iterator();
    }

}
