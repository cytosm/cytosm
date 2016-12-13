package org.cytosm.cypher2sql.cypher.ast.clause.projection;

import org.cytosm.cypher2sql.cypher.ast.ASTNode;

import java.util.List;

/**
 */
public class OrderBy extends ASTNode {

    public List<SortItem> sortItems;

    public OrderBy(List<SortItem> sortItems) {
        this.sortItems = sortItems;
    }
}
