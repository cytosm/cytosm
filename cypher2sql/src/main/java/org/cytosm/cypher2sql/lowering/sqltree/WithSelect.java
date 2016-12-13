package org.cytosm.cypher2sql.lowering.sqltree;

/**
 */
public class WithSelect extends BaseSelect {

    /**
     * The name given to the select below.
     */
    public String subqueryName;

    /**
     * The select wrapped inside a WITH.
     */
    public BaseSelect subquery;

    public WithSelect(BaseSelect subquery) {
        this.subquery = subquery;
        this.varId = this.subquery.varId;
    }

    @Override
    public String toSQLString() {
        return " " + subqueryName + " AS (\n" + subquery.toSQLString() + "\n)";
    }
}
