package org.cytosm.cypher2sql.lowering.sqltree;

import org.cytosm.cypher2sql.lowering.rendering.RenderingContext;
import org.cytosm.cypher2sql.lowering.rendering.RenderingHelper;
import org.cytosm.cypher2sql.lowering.sqltree.from.FromItem;
import org.cytosm.cypher2sql.lowering.sqltree.join.BaseJoin;

import java.util.Collections;
import java.util.List;

/**
 * A 'SELECT *' with no JOINs.
 */
public class SelectAll extends SimpleSelect {


    @Override
    protected String renderExportedVariable(RenderingContext ctx) {
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
    protected String joins(RenderingContext ctx) {
        return "";
    }

    @Override
    protected List<FromItem> joinsFromItem() {
        return Collections.emptyList();
    }
}
