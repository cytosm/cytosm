package org.cytosm.cypher2sql.cypher.ast;

import org.cytosm.cypher2sql.cypher.ast.clause.Clause;

import java.util.List;

/**
 */
public class SingleQuery extends QueryPart {

    public List<Clause> clauses;

    public SingleQuery(final List<Clause> clauses) {
        this.clauses = clauses;
    }
}
