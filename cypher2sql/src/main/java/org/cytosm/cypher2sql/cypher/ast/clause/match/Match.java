package org.cytosm.cypher2sql.cypher.ast.clause.match;

import org.cytosm.cypher2sql.cypher.ast.clause.Clause;
import org.cytosm.cypher2sql.cypher.ast.clause.Where;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.Pattern;

import java.util.Optional;

/**
 */
public class Match extends Clause {

    public boolean optional;
    public Pattern pattern;
    public Optional<Where> where;

    public Match(boolean optional, Pattern pattern, Where where) {
        this.optional = optional;
        this.pattern = pattern;
        this.where = (where == null) ? Optional.empty(): Optional.of(where);
    }
}
