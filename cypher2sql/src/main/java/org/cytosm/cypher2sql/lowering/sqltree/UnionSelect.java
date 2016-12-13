package org.cytosm.cypher2sql.lowering.sqltree;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UnionSelect represent a node that will be rendered
 * as a union of all its children.
 */
public class UnionSelect extends BaseSelect {

    /**
     * Union of all the sub select. In here
     * we can only either have a simple select
     * or a top select.
     */
    public List<SimpleOrScopeSelect> unions = new ArrayList<>();

    @Override
    public String toSQLString() {
        return unions.stream()
                .map(x -> {
                    if (x instanceof ScopeSelect) {
                        return "(" + x.toSQLString() + ")";
                    } else {
                        return x.toSQLString();
                    }
                })
                .collect(Collectors.joining("UNION ALL\n"));
    }
}
