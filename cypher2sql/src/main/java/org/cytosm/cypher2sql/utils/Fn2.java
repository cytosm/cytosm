package org.cytosm.cypher2sql.utils;

/**
 */
public interface Fn2<T1, T2, R> {

    R apply(T1 arg1, T2 arg2);
}
