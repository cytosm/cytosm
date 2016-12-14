package org.cytosm.cypher2sql.lowering.sqltree;

import org.cytosm.cypher2sql.lowering.rendering.RenderingHelper;
import org.cytosm.cypher2sql.lowering.sqltree.from.FromItem;
import org.cytosm.cypher2sql.lowering.sqltree.join.BaseJoin;
import org.cytosm.cypher2sql.lowering.typeck.expr.Expr;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SimpleSelect is a node
 */
public abstract class SimpleSelect extends SimpleOrScopeSelect {

    /**
     * The exported items from this select.
     *
     * The type of the exported items can either be AliasVar
     * or PropertyAccess. Other type are more likely an error.
     */
    public List<Expr> exportedItems = new ArrayList<>();

    /**
     * The where condition on this SELECT.
     *
     * If this is null then there no condition.
     * If this is a {@link SimpleSelectWithLeftJoins}
     * then the where condition is applied on the last JOIN
     * instead.
     */
    public Expr whereCondition = null;

    /**
     * Limit
     *
     * A negative value is used to represent the absence of limit.
     */
    public long limit = -1;

    /**
     * Whether or not this select needs a DISTINCT.
     */
    public boolean isDistinct = false;

    /**
     * Skip
     *
     * A negative value is used to represent the absence of skip.
     */
    public long skip = -1;

    /**
     * Order by
     */
    public List<OrderItem> orderBy = new ArrayList<>();

    /**
     * From items. Those are only used for FROM part.
     * See in subclasses for JOIN(s).
     */
    public List<FromItem> fromItem = new ArrayList<>();

    public static class OrderItem {

        /**
         * Is this order item DESC or ASC?
         */
        public boolean descending = false;

        /**
         * Expression
         *
         * It can be anything but if the result type is an object then they can't
         * be compared between each other and the result is empty.
         */
        public Expr item;
    }

    @Override
    public String toSQLString() {
        RenderingHelper helper = createHelper();
        String result = renderExportedVariable(helper);
        result += fromItem();
        result += joins(helper);
        result += renderWhereCondition();
        result += renderOrderBy();
        result += renderLimit();
        result += renderSkip();
        return result;
    }

    public List<FromItem> dependencies() {
        List<FromItem> fromItems = new ArrayList<>(fromItem);
        fromItems.addAll(joinsFromItem());
        return fromItems;
    }

    public abstract void addJoin(BaseJoin join);
    public abstract List<BaseJoin> joinList();

    private RenderingHelper createHelper() {
        return new RenderingHelper(dependencies());
    }

    protected String renderExportedVariable(RenderingHelper helper) {
        return "SELECT " + renderDistinct() + exportedItems.stream()
                .map(x -> x.toSQLString(helper))
                .collect(Collectors.joining(", ")) +
                "\n";
    }

    private String renderSkip() {
        if (skip > 0) {
            return "SKIP " + skip + "\n";
        }
        return "";
    }

    private String renderDistinct() {
        if (isDistinct) {
            return "DISTINCT ";
        }
        return "";
    }

    private String fromItem() {
        if (fromItem.isEmpty()) {
            throw new RuntimeException("Bug found! This select doesn't select anything...");
        }
        return "FROM " + fromItem.stream().map(FromItem::toSQLString)
                .collect(Collectors.joining(", ")) + "\n";
    }

    private String renderWhereCondition() {
        if (whereCondition == null) {
            return "";
        }
        return "WHERE " + whereCondition.toSQLString(createHelper()) + "\n";
    }

    private String renderLimit() {
        if (limit > 0) {
            return "LIMIT " + limit+ "\n";
        }
        return "";
    }

    private String renderOrderBy() {
        if (orderBy.isEmpty()) {
            return "";
        }
        return "ORDER BY " + orderBy.stream()
                .map(oi -> oi.item.toSQLString(createHelper()) + (oi.descending ? " DESC": " ASC"))
                .collect(Collectors.joining(", ")) + "\n";
    }

    abstract protected String joins(RenderingHelper helper);
    abstract protected List<FromItem> joinsFromItem();
}
