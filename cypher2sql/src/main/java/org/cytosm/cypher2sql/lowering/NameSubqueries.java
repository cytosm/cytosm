package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;
import org.cytosm.cypher2sql.lowering.exceptions.Unreachable;
import org.cytosm.cypher2sql.lowering.sqltree.*;

/**
 */
public class NameSubqueries {

    private static final String SUBQUERY_CONST = "SUB_";
    private static long subquery_count = 0;

    /**
     * Give a name to each sub query.
     * @param sqltree is the root of the SQL tree.
     */
    public static void nameSubqueries(ScopeSelect sqltree) throws Cypher2SqlException {
        subquery_count = nameSubqueries(sqltree, subquery_count);
    }

    /**
     * Give a name to a provided With sub query.
     * @param withSelect is the sub query to name.
     */
    public static void nameSubquery(WithSelect withSelect) {
        withSelect.subqueryName = SUBQUERY_CONST + subquery_count++;
    }

    private static long nameSubqueries(BaseSelect sqltree, long count) throws Cypher2SqlException {
        if (sqltree instanceof WithSelect) {
            ((WithSelect) sqltree).subqueryName = SUBQUERY_CONST + count++;
            return nameSubqueries(((WithSelect) sqltree).subquery, count);
        } else if (sqltree instanceof UnionSelect) {
            for (SimpleOrScopeSelect child: ((UnionSelect) sqltree).unions) {
                count = nameSubqueries(child, count);
            }
            return count;
        } else if (sqltree instanceof SimpleSelect) {
            return count;
        } else if (sqltree instanceof ScopeSelect) {
            for (WithSelect child: ((ScopeSelect) sqltree).withQueries) {
                count = nameSubqueries(child, count);
            }
            return count;
        } else {
            throw new Unreachable();
        }
    }
}
