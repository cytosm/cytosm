package org.cytosm.cypher2sql.lowering.sqltree;

import org.cytosm.cypher2sql.lowering.rendering.RenderingContext;
import org.cytosm.cypher2sql.lowering.sqltree.from.FromItem;
import org.cytosm.cypher2sql.lowering.sqltree.join.BaseJoin;
import org.cytosm.cypher2sql.lowering.sqltree.join.LeftJoin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SimpleSelect that can only contains left joins.
 */
public class SimpleSelectWithLeftJoins extends SimpleSelect {

    /**
     * List of left joins.
     */
    public List<LeftJoin> joins = new ArrayList<>();

    @Override
    protected String joins(RenderingContext ctx) {
        if (joins.isEmpty()) {
            return "";
        }
        return joins.stream()
                .map(j -> "LEFT " + j.toSQLString(ctx) + "\n")
                .reduce("", String::concat);
    }

    @Override
    protected List<FromItem> joinsFromItem() {
        return joins.stream().map(x -> x.joiningItem).collect(Collectors.toList());
    }

    @Override
    public void addJoin(BaseJoin join) {
        if (join instanceof LeftJoin) {
            joins.add((LeftJoin) join);
        } else {
            throw new RuntimeException("Bug found!");
        }
    }

    @Override
    public List<BaseJoin> joinList() {
        return joins.stream().map(x -> (BaseJoin) x).collect(Collectors.toList());
    }
}
