package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.lowering.sqltree.*;
import org.cytosm.cypher2sql.lowering.sqltree.from.FromItem;
import org.cytosm.cypher2sql.lowering.typeck.var.TempVar;

import java.util.List;

/**
 * This pass transform many Cyphers that have been converted into
 * ScopeSelect into one ScopeSelect tree. It union them together essentially.
 *
 */
public class MergeExpandedCyphers {


    /**
     * Merge the provided queries into one.
     * @param queries is the list of query to merge.
     * @return Returns the SQL tree resulting of the merge.
     */
    public static ScopeSelect merge(List<ScopeSelect> queries) {
        if (queries.size() == 1) {
            return queries.get(0);
        } else {
            // Create the result.
            ScopeSelect result = new ScopeSelect();

            // Create the union
            UnionSelect union = new UnionSelect();
            union.unions.addAll(queries);

            // Wrap the union
            WithSelect withSelect = new WithSelect(union);
            NameSubqueries.nameSubquery(withSelect);
            result.withQueries.add(withSelect);

            // Create the blind return.
            result.ret = new SelectAll();
            FromItem fromItem = new FromItem();
            fromItem.source = withSelect;
            fromItem.variables.add(new TempVar()); // Adding a fake variable.
            result.ret.fromItem.add(fromItem);

            return result;
        }
    }
}
