package org.cytosm.cypher2sql.expandpaths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cytosm.pathfinder.PathFinder;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import org.cytosm.common.gtop.GTopInterfaceImpl;
import org.cytosm.pathfinder.CanonicalRoutes;
import org.cytosm.pathfinder.output.PathSerializer;
import org.cytosm.pathfinder.output.Serializer;

/**
 * This class expands a cypher query using the GTOP into multiple cypher queries, removing any anonymous nodes/edges and
 * replacing with multiple queries.
 *
 *
 *
 * <pre>
 * {@code
 *  Cypher - match (:Order)-[] -(n)-[]-(b) return n,b;
 * 
 *  Step one n become Employee, Product, Customer (as that is what Order is related to)
 * 
 *  1. (:Order)-[:SOLD] -(:Employee)-[]-(b)
 *  2. (:Order)-[:PRODUCT] -(:Product)-[]-(b)
 *  3. (:Order)-[:PURCHASED] -(:Customer)-[]-(b)
 * 
 *  Step two for each value of b it then expands again - i.e. for
 *  1. (Employee) it becomes Employee and Order
 *  2. (Product) it becomes Category & Supplier & Order
 *  3. Customer there is only Order
 * 
 *  1. (:Order)-[:SOLD] -(:Employee)-[:REPORTS_TO]-(Employee)
 *  2. (:Order)-[:SOLD] -(:Employee)-[:SOLD]-(Order)
 *  3. (:Order)-[:PRODUCT] -(:Product)-[:PART_OF]-(Category)
 *  4. (:Order)-[:PRODUCT] -(:Product)-[:SUPPLIES]-(Supplier)
 *  5. (:Order)-[:PRODUCT] -(:Product)-[:PRODUCT]-(Order)
 *  6. (:Order)-[:PURCHASED] -(:Customer)-[:PURCHASED]-(Order)
 * 
 * }
 * </pre>
 *
 *
 *
 */
public final class ExpandCypher {

    private ExpandCypher() {}

    private static final Logger LOGGER = Logger.getLogger(ExpandCypher.class.getName());

    /**
     * Takes a cypher query and gTop, splits the cypher into paths and for each path expands using the
     * {@link CanonicalRoutes}.
     *
     * From these multiple paths all the combinations of queries are generated and multiple cyphers are returned.
     *
     * @param gtopInterface gTop file to use for cypher expansion
     * @param queryStr Cypher query to expand
     * @return List of cypher queries this query have become having been expanded
     */
    public static List<String> expandCypher(final GTopInterfaceImpl gtopInterface, final String queryStr) {
        String query = queryStr.trim();

        ExtractPath extractPath = new ExtractPath();
        List<PathPlusHints> paths = extractPath.split(query);
        Serializer serializer = new PathSerializer();
        PathFinder matcher = new PathFinder(new CypherConverter());


        List<List<String>> querySegments = new ArrayList<>();
        int last = 0;
        for (PathPlusHints pathAndHints : paths) {
            if (pathAndHints.getPath().toLowerCase().startsWith("with")) {
                List<String> item = new ArrayList<>();
                item.add(" " + pathAndHints.getPath());
                querySegments.add(item);
                last += pathAndHints.getPath().length();
            } else {
                String bit = query.substring(last, query.indexOf(pathAndHints.getPath(), last));
                last = query.indexOf(pathAndHints.getPath()) + (pathAndHints.getPath().length());

                List<String> queryList = new ArrayList<>();
                queryList.add(bit);
                querySegments.add(queryList);
                matcher.getInputFormat().addExternalContext(pathAndHints.getHintsIntoList());
                CanonicalRoutes expandedPaths = matcher.enumerate(pathAndHints.getPath(), gtopInterface);
                List<String> items = serializer.serialize(expandedPaths.getAllPossibleRoutes());
                if (items.size() == 0) {
                    System.out.println(pathAndHints.getPath());
                }
                querySegments.add(items);
            }
        }
        if (last < query.length()) {
            List<String> queryList = new ArrayList<>();
            queryList.add(query.substring(last, query.length()));
            querySegments.add(queryList);
        }

        List<String> newQueries = new ArrayList<>();
        computeAllPaths(newQueries, "", querySegments, 0);

        List<String> validQueries = new ArrayList<>();
        for (String qry : newQueries) {
            if (queryValid(qry)) {
                validQueries.add(qry);
            } else {
                LOGGER.info("Rejecting query as it isn't valid with this gTop:" + qry);
            }
        }
        //        int count = 0;
        //        for (String sql : validQueries) {
        //            System.out.println(count + " --> " + sql);
        //            count++;
        //        }

        return validQueries;
    }

    /**
     * Takes a query, parses out all the types, and if they aren't consistent it rejects it.
     *
     * @param query the query to validate
     */
    // FIXME: This code is plain wrong.
    public static boolean queryValid(final String query) {
        Map<String, String> mappings = new HashMap<>();
        Pattern pattern = Pattern.compile("\\((.*?)\\)");
        Matcher matcher = pattern.matcher(query);
        while (matcher.find()) {
            if (matcher.group(0).contains(":")) {
                StringBuffer sb = new StringBuffer();
                sb.append(matcher.group(0));
                sb.reverse();
                int pos = sb.toString().lastIndexOf(":");
                String variable = sb.toString().substring(pos + 1, sb.toString().indexOf("(", pos));
                if (variable.contains("=")) {
                    variable = variable.substring(0, variable.indexOf("="));
                }
                if (variable.contains(" ")) {
                    variable = variable.substring(0, variable.indexOf(" "));
                }
                int i = matcher.group(0).indexOf(":");

                //                System.out.println(matcher.group(0));
                //                StringBuffer s = new StringBuffer(variable);
                //                s.reverse();
                //                System.out.println(s);

                String type = matcher.group(0).trim().substring(i + 1, matcher.group(0).length() - 1);
                // -1 as there is a ) at the end
                if (!StringUtils.isEmpty(variable)) {
                    String existingType = mappings.get(variable);
                    if (existingType == null) {
                        mappings.put(variable, type);
                    } else {
                        if (!existingType.startsWith(type) && !type.startsWith(existingType)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }


    /**
     * Expands all the paths into multiple queries.
     *
     * It does this by iterating through every query added every other query to it.
     *
     * @param newQueries newQueries we have created
     * @param current the current query we are processing
     * @param queryList list of queries to expand
     * @param index index though the queryList
     */
    private static void computeAllPaths(final List<String> newQueries, final String current,
            final List<List<String>> queryList, final int index) {
        if (index >= queryList.size()) {
            newQueries.add(current);
            return;
        }
        if (!queryValid(current)) {
            return;
        }
        List<String> currentList = queryList.get(index);

        for (String query : currentList) {
            String part = current + " " + query;
            computeAllPaths(newQueries, part, queryList, index + 1);
        }
    }

}
