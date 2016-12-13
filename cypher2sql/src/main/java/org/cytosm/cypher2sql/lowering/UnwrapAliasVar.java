package org.cytosm.cypher2sql.lowering;

/**
 * This pass unwrap `AliasVar`s when they refers
 * to a NodeVar, RelVar or PathVar.
 *
 * The following is invalid in Cypher:
 *
 *      MATCH (a)
 *      WITH {a: {b: a}} AS a
 *      MATCH (a.b.a)--(b)
 *      RETURN a, b
 *
 * However the following is not:
 *
 *      MATCH (a)
 *      WITH a AS b
 *      MATCH (b)--(c)
 *      RETURN b, c
 *
 * This pass is dealing with the second
 * case.
 *
 */
public class UnwrapAliasVar {
}
