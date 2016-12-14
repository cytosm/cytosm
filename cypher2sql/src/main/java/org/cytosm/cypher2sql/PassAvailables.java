package org.cytosm.cypher2sql;

import org.cytosm.common.gtop.GTopInterfaceImpl;
import org.cytosm.cypher2sql.expandpaths.ExpandCypher;
import org.cytosm.cypher2sql.lowering.*;
import org.cytosm.cypher2sql.lowering.exceptions.Cypher2SqlException;
import org.cytosm.cypher2sql.lowering.sqltree.ScopeSelect;
import org.cytosm.cypher2sql.lowering.typeck.ComputeAliasVarType;
import org.cytosm.cypher2sql.lowering.typeck.VarDependencies;
import org.cytosm.cypher2sql.cypher.ast.Statement;
import org.cytosm.cypher2sql.cypher.parser.ASTBuilder;

import static org.cytosm.cypher2sql.lowering.exceptions.fns.LambdaExceptionUtil.rethrowFunction;

import java.util.List;
import java.util.stream.Collectors;

/**
 */
public class PassAvailables {

    /**
     * Parse the cypher and returns the AST representation.
     * @param cypher is the string to parse.
     * @return Returns the AST.
     */
    public static Statement parseCypher(String cypher) {
        return ASTBuilder.parse(cypher);
    }

    /**
     * On build the query tree running the VarDependencies
     * pass and the SelectTreeBuilder pass.
     * @param cypher is the cypher to parse.
     * @return Returns the SQL tree.
     */
    public static ScopeSelect buildQueryTree(String cypher) {
        Statement st = parseCypher(cypher);
        VarDependencies vars = new VarDependencies(st);
        return SelectTreeBuilder.createQueryTree(vars, st);
    }

    /**
     * Convert the provided Cypher into SQL.
     * @param gTopInterface gtop implementation.
     * @param cypher is the cypher to convert.
     * @return Returns a SQL tree.
     */
    public static ScopeSelect cypher2sqlOnExpandedPaths(final GTopInterfaceImpl gTopInterface, String cypher)
            throws Cypher2SqlException
    {
        // First pass, convert the cypher into an AST.
        Statement st = parseCypher(cypher);

        // Extract from the AST the dependencies between variables.
        VarDependencies vars = new VarDependencies(st);

        // Compute types of variables. Some passes requires it. So we do it
        // as soon as we can.
        ComputeAliasVarType.computeAliasVarTypes(vars);

        // TODO: Add a pass that parse the Cypher and do
        // TODO: something useful with PatternExpressions.

        // Build the canonical SQL tree. At this point, only
        // the structure from the SQL tree as been created.
        // The data structure is mostly empty.
        ScopeSelect tree = SelectTreeBuilder.createQueryTree(vars, st);

        // This pass will provide sub query name for each WithSelect.
        NameSubqueries.nameSubqueries(tree);

        // We now compute all the FROM and JOIN. We do so by computing
        // dependencies between SELECTs by transposing variables
        // dependencies they use or they indirectly depend on.
        ComputeFromItems.computeFromItems(tree, vars);

        // We move restrictions on NodeVar as Where conditions
        // or JOIN conditions if there's any JOINS.
        // TODO: can this pass be moved sooner or later? Why would it need to be done here??
        // TODO: It seems that it should be run after Populate joins...
        MoveRestrictionInPattern.moveRestrictionInPatterns(tree, vars);

        // Provide a tableName where appropriate for each FromItem
        // using the provided gTop.
        tree = ExpandNodeVarWithGtop.computeTableNamesOnFromItems(tree, gTopInterface);

        // So far, FROM and JOINS are mixed and all stored within
        // the 'fromItems' property of each SimpleSelect. We move
        // the appropriate ones in separate 'joins' using the list
        // of relationships.
        // This pass also resolve table names for NodeVar using
        // the labels visible on them.
        tree = PopulateJoins.populateJoins(tree, vars, gTopInterface);

        // Compute exports add to the tree the last piece
        // of information missing in the tree.
        // After this pass, the vars object is no longer
        // necessary. Variables have been attached at appropriate locations.
        ComputeExports.computeExports(tree, vars);

        // TODO: Add a pass here that converts Return Expression returning variables
        // TODO: into a usable form.

        // This pass converts all "count" functions calls into an appropriate
        // form. This mean any ScopeSelect that leaves in the tree will
        // be turned into a "count" and everything that is above will
        // be turned into a "sum".
        TransformFunctions.convertCypherCountFn(tree, gTopInterface);

        // We can now unwrap every property access that we see in the tree.
        // This means essentially unwrapping property access on aliases.
        UnwrapPropertyAccess.unwrapPropertyAccess(tree);

        // This pass here transforms length(p) function calls into
        // the value stored on the PathVar p otherwise it does nothing.
        TransformFunctions.convertPathLength(tree);

        // Unwrap alias expr and compute constant expression
        // where possible.
        UnwrapAliasVar.unwrapConstants(tree);

        // Remove AliasVars that are no longer used.
        // This should remove variables that are MapExpressions
        // and constants on valid Cypher queries.
        UnwrapAliasVar.removeUnusedVariables(tree);

        // TODO: Add a pass here that copy the latest ORDER BY in
        // TODO: the ScopeSelect.ret SimpleSelect.

        // Finally the only PropertyAccess that we see are on variable of
        // type NodeVar, PathVar or RelVar. We mark them as used.
        ComputeExports.markPropertiesAsUsedWhenEncountered(tree);

        // Unwrap in ScopeSelect that are nested the AliasExpr
        // to only the Expr. Otherwise this break exports.
        UnwrapAliasExpr.visitAndUnwrap(tree);

        // Return the tree!
        return tree;
    }

    /**
     * Root method for calling the cypher to sql parser. It takes a GTOP implementation and cypher string and returns a
     * SQL statement.
     *
     *
     * @param gtopInterface gtop implementaton
     * @param originalCypher cypher string to convert to sql
     * @return SQL statement or empty if it doesnt parse.
     */
    public static String cypher2sql(final GTopInterfaceImpl gtopInterface, final String originalCypher)
            throws Cypher2SqlException
    {
        List<String> cyphers = ExpandCypher.expandCypher(gtopInterface, originalCypher);

        List<ScopeSelect> allQ = cyphers.stream()
                .<ScopeSelect>map(rethrowFunction(cypher -> cypher2sqlOnExpandedPaths(gtopInterface, cypher)))
                .collect(Collectors.toList());

        // Merge expanded cypher together.
        ScopeSelect query = MergeExpandedCyphers.merge(allQ);

        // TODO: ORDER BY, LIMIT and SKIP needs to be handled here
        // TODO: will be the same as the one ran in 'cypher2sqlOnExpandedPaths'

        // Last pass: Render the tree into SQL!
        return query.toSQLString();
    }

    // This class shouldn't be instantiated.
    private PassAvailables() {}
}
