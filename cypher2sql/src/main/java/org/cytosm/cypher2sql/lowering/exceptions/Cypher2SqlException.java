package org.cytosm.cypher2sql.lowering.exceptions;

import org.cytosm.common.gtop.GTopInterfaceImpl;
import org.cytosm.cypher2sql.PassAvailables;

/**
 * The sum type of all exception that might occurs in
 * the {@link PassAvailables#cypher2sqlOnExpandedPaths(GTopInterfaceImpl, String)}
 * routine.
 */
public class Cypher2SqlException extends Exception {

    protected Cypher2SqlException(String message) {
        super(message);
    }
}
