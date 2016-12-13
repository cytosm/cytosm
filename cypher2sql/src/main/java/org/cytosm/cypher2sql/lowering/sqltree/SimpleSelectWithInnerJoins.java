package org.cytosm.cypher2sql.lowering.sqltree;

import org.cytosm.cypher2sql.lowering.rendering.RenderingHelper;
import org.cytosm.cypher2sql.lowering.sqltree.from.FromItem;
import org.cytosm.cypher2sql.lowering.sqltree.join.BaseJoin;
import org.cytosm.cypher2sql.lowering.sqltree.join.InnerJoin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SimpleSelect that can only contains inner joins.
 */
public class SimpleSelectWithInnerJoins extends SimpleSelect {

    /**
     * List of inner joins.
     */
    public List<InnerJoin> joins = new ArrayList<>();

    @Override
    protected String joins(RenderingHelper helper) {
        if (joins.isEmpty()) {
            return "";
        }
        return joins.stream()
                .map(j -> j.toSQLString(helper) + "\n")
                .reduce("", String::concat);
    }

    @Override
    protected List<FromItem> joinsFromItem() {
        return joins.stream().map(x -> x.joiningItem).collect(Collectors.toList());
    }

    @Override
    public void addJoin(BaseJoin join) {
        if (join instanceof InnerJoin) {
            joins.add((InnerJoin) join);
        } else {
            throw new RuntimeException("Bug found!");
        }
    }

    @Override
    public List<BaseJoin> joinList() {
        return joins.stream().map(x -> (BaseJoin) x).collect(Collectors.toList());
    }
}
