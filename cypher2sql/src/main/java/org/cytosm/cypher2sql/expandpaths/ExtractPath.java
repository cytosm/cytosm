package org.cytosm.cypher2sql.expandpaths;

import java.util.*;

import org.cytosm.cypher2sql.cypher.parser.ASTBuilder;
import org.apache.log4j.Logger;

import org.cytosm.cypher2sql.cypher.ast.*;
import org.cytosm.cypher2sql.cypher.ast.clause.*;
import org.cytosm.cypher2sql.cypher.ast.clause.match.*;
import org.cytosm.cypher2sql.cypher.ast.clause.projection.*;

/**
 * Splits the cypher text into a list of path elements, the WITH parts are also in there as a
 * separate item. The matches are split on commas
 *
 * <pre>
 * {@code
 * For example:
 * MATCH (n) --> (p), (p2) --> (q) should return (n)-->(p) and (p2)-->(q) MATCH (n) --> (p) WITH p
 * MATCH (p2) --> (q) should return (n)-->(p), WITH p, (p2)-->(q)
 * MATCH (n) --> (p) RETURN (p2) --> (q) should return (n)-->(p), RETURN (p2)-->(q)
 *  }
 * </pre>
 */

public class ExtractPath {
    private static final Logger LOGGER = Logger.getLogger(ExtractPath.class.getName());

    /**
     * Split the query based on the various cypher elements: MATCH, RETURN, WITH.
     *
     * @param query query to split onto paths plus hints
     * @return the paths and hints for this query
     */
    public List<PathPlusHints> split(final String query) {

        Statement ast = ASTBuilder.parse(query);
        // After this statements all column fields will be the offset after.
        Iterator<Clause> clauses = ((SingleQuery) ast.query.part).clauses.iterator();

        List<PathPlusHints> matches = new ArrayList<>();

        while (clauses.hasNext()) {
            Clause clause = clauses.next();
            if (clause instanceof Match || clause instanceof With) {

                // Get the optional where
                Optional<Where> whereOpt;
                if (clause instanceof Match) {
                    whereOpt = ((Match) clause).where;
                } else {
                    whereOpt = ((With) clause).where;
                }

                // Collect all the path defined by the Match.
                List<PathPlusHints> pathsSpecificToMatchClause = new ArrayList<>();
                PathPlusHintsVisitors.parse(clause, pathsSpecificToMatchClause, query);

                // If there's a where attached to this Match, collect all the variables
                // being used along with the properties declared.
                if (whereOpt.isPresent()) {
                    Where where = whereOpt.get();

                    VariableCollectorVisitor exprVisitor = new VariableCollectorVisitor();
                    exprVisitor.collectVarInfos(where.expression);

                    Map<String, Set<String>> hints = exprVisitor.getVars();
                    pathsSpecificToMatchClause.forEach(p -> p.addHints(hints));
                }

                // Finally add the PathPlusHints to the list of collected things.
                matches.addAll(pathsSpecificToMatchClause);
            } else if (clause instanceof Return) {

                Iterator<ReturnItem> iterRetItems = ((Return) clause).returnItems.iterator();
                VariableCollectorVisitor exprVisitor = new VariableCollectorVisitor();
                while (iterRetItems.hasNext()) {
                    exprVisitor.collectVarInfos(iterRetItems.next().expression);
                }
            }
        }

        return matches;
    }

    /***
     * Merges all the hints from the return clause to all path hints extracted from other clauses
     * 
     * @param matches hints found on clause analysis
     * @param returnHints hints found due to return statement analysis
     */
    private void addReturnHintsToAllPaths(final List<PathPlusHints> matches,
            final Map<String, Set<String>> returnHints) {

        for (PathPlusHints pathAndHint : matches) {
            final Map<String, Set<String>> hintsOnPath = pathAndHint.getHints();

            returnHints.entrySet().forEach(variableHint -> {

                Set<String> hintsOnThatVariable = null;
                if (hintsOnPath.containsKey(variableHint.getKey())) {
                    hintsOnThatVariable = hintsOnPath.get(variableHint.getKey());
                } else {
                    hintsOnThatVariable = new LinkedHashSet<>();
                }

                hintsOnThatVariable.addAll(variableHint.getValue());

                hintsOnPath.put(variableHint.getKey(), hintsOnThatVariable);
            });
        }
    }
}
