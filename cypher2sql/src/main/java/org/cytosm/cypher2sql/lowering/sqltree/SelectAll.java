package org.cytosm.cypher2sql.lowering.sqltree;

import org.cytosm.cypher2sql.lowering.rendering.RenderingHelper;
import org.cytosm.cypher2sql.lowering.sqltree.from.FromItem;
import org.cytosm.cypher2sql.lowering.sqltree.join.BaseJoin;

import java.util.Collections;
import java.util.List;

/**
 */
public class SelectAll extends SimpleSelect {


    @Override
    protected String renderExportedVariable(RenderingHelper helper) {
        return "SELECT * \n";
    }

    @Override
    public void addJoin(BaseJoin join) {
        throw new RuntimeException("Can't add a join to a SELECT *");
    }

    @Override
    public List<BaseJoin> joinList() {
        return Collections.emptyList();
    }

    @Override
    protected String joins(RenderingHelper helper) {
        return "";
    }

    @Override
    protected List<FromItem> joinsFromItem() {
        return Collections.emptyList();
    }
}
