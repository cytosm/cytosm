package org.cytosm.cypher2sql.lowering.typeck.types;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class MapType extends ObjectType {

    public Map<String, AType> fields = new HashMap<>();
}
