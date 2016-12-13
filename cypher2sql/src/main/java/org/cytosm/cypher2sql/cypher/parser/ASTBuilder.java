package org.cytosm.cypher2sql.cypher.parser;

import org.cytosm.cypher2sql.cypher.ast.Statement;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.Pattern;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 */
public class ASTBuilder {

    /**
     * Parse the provided Cypher string and converts it into an AST.
     * @param cypher is the cypher to parse.
     * @return Returns the generated AST.
     */
    public static Statement parse(String cypher) {
        ANTLRInputStream input = new ANTLRInputStream(cypher);
        CypherLexer lexer = new CypherLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CypherParser parser = new CypherParser(tokens);
        return parser.cypher().res;
    }

    /**
     * Parse the provided pattern string and converts it into an AST.
     * @param pattern is the pattern to parse.
     * @return Returns the generated AST.
     */
    public static Pattern parsePattern(String pattern) {
        ANTLRInputStream input = new ANTLRInputStream(pattern);
        CypherLexer lexer = new CypherLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CypherParser parser = new CypherParser(tokens);
        return parser.pattern().res;
    }
}
