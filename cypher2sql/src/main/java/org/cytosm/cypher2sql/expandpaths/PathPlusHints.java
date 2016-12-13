package org.cytosm.cypher2sql.expandpaths;

import java.util.*;

/**
 * Contains the path from the Cypher plus any corresponding hints, for example match (a) where
 * a.name = 'test'.
 *
 * Would be a path of "a" and a single hint of "a", "name"
 *
 */
public class PathPlusHints {
    private String path = null;
    private Map<String, Set<String>> hints = new HashMap<>();

    /**
     * Constructor that just takes the path.
     *
     * @param path the path
     */
    public PathPlusHints(final String path) {
        this.path = path;
    }

    /**
     * Add a hint to this path.
     *
     * @param variableName variable that owns that hint
     * @param value value of the hint
     */
    public void addHints(final String variableName, final String attributeName) {
        Set<String> attributes;

        if (hints.containsKey(variableName)) {
            attributes = hints.get(variableName);
        } else {
            attributes = new LinkedHashSet<>();
        }

        attributes.add(attributeName);

        hints.put(variableName, attributes);
    }

    /**
     * Add multiple hints to this path.
     *
     * @param newHints hints to add
     */
    public void addHints(final Map<String, Set<String>> newHints) {
        hints.putAll(newHints);
    }

    /**
     * Get the map of hints for this path.
     *
     * @return the map of hints
     */
    public Map<String, Set<String>> getHints() {
        return hints;
    }

    /**
     * Get the map of hints for this path where
     * the hints have been converted as List of string instead of a map.
     * @return
     */
    public Map<String, List<String>> getHintsIntoList() {
        Map<String, List<String>> result = new HashMap<>();
        this.hints.entrySet().forEach(entry -> {
            List<String> value = new ArrayList<>(entry.getValue());
            result.put(entry.getKey(), value);
        });
        return result;
    }

    /**
     * The path to return.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "Paths: " + path + "Hints: " + hints;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        PathPlusHints pph = (PathPlusHints) obj;

        return getPath().equals(pph.getPath()) && getHints().equals(pph.getHints());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getPath() == null) ? 0 : getPath().hashCode());
        result = prime * result + ((getHints() == null) ? 0 : getHints().hashCode());
        return result;
    }

}
