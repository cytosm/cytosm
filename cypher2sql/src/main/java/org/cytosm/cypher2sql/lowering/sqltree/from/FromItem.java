package org.cytosm.cypher2sql.lowering.sqltree.from;

import org.cytosm.cypher2sql.lowering.sqltree.WithSelect;
import org.cytosm.cypher2sql.lowering.typeck.NameProvider;
import org.cytosm.cypher2sql.lowering.typeck.var.NodeOrTempOrRelVar;
import org.cytosm.cypher2sql.lowering.typeck.var.Var;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent where does the data will come from.
 * This is used by two types:
 *  - {@link org.cytosm.cypher2sql.lowering.sqltree.join.BaseJoin}
 *  - {@link org.cytosm.cypher2sql.lowering.sqltree.SimpleSelect}
 *
 * They use it to find the Select they depend on.
 *
 * The following strong invariants needs to be verified:
 *
 *  - If `source` is null, `variables` size must be exactly 1.
 *  - If `source` is not null, `variables` size must be >= 1.
 *  - If `source` is null, sourceTableName isn't.
 *  - If `source` is not null, sourceTableName is.
 *
 * <pre>source</pre> and <pre>sourceTableName</pre> would be better
 * represented with a tagged union but sadly those do not exists in Java...
 *
 */
public class FromItem {

    /**
     * The source providing data for the attached variables.
     */
    public WithSelect source = null;

    /**
     * Table name if the source is null. Null otherwise.
     */
    public String sourceTableName = null;

    /**
     * Variables that will be provided by the given source.
     * It can only be a RelVar or a NodeVar.
     */
    public List<Var> variables = new ArrayList<>();

    /**
     * Name of the source for the scope where variables are
     * used and this FromItem provides values for them.
     */
    public String sourceVariableName = NameProvider.genFromItemName();

    /**
     * Returns the table name or the origin select
     * providing data for that variable.
     * @return Returns the string that this variable should be selected from.
     */
    public String toSQLString() {
        String result;
        if (source != null) {
            result = source.subqueryName;
        } else if (variables.size() == 1 && sourceTableName != null) {
            result = sourceTableName;
        } else if (variables.size() > 1) {
            throw new RuntimeException(
                    "Bug found! A FromItem can't have more than one variable issued from the same table."
            );
        } else {
            throw new RuntimeException(
                    "No table associated with this FROM !!"
            );
        }
        if (variables.isEmpty()) {
            throw new RuntimeException(
                    "Bug found! A FromItem can't have no variable depending on it."
            );
        }
        return result + " AS " + sourceVariableName;
    }
}
