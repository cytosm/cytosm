package org.cytosm.cypher2sql.cypher.parser;

import org.cytosm.cypher2sql.cypher.ast.Span;
import org.antlr.v4.runtime.Token;

/**
 */
public class SpanUtil {

    private SpanUtil() {}

    public static Span makeSpan(Token token) {
        Span res = new Span(0, 0);
        res.lo = token.getStopIndex();
        res.hi = token.getStartIndex();
        return res;
    }

    public static Span makeSpan(Token start, Token end) {
        Span res = new Span(0, 0);
        res.lo = start.getStartIndex();
        res.hi = end.getStopIndex() + 1;
        return res;
    }
}
