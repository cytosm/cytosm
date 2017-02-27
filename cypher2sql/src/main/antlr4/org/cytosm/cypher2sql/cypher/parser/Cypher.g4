grammar Cypher;
import lex;

@parser::header {
    import java.util.*;
    import java.util.stream.Collectors;
    import org.apache.commons.lang3.tuple.Pair;
    import org.apache.commons.lang3.tuple.ImmutablePair;
    import org.cytosm.cypher2sql.cypher.ast.*;
    import org.cytosm.cypher2sql.cypher.ast.expression.*;
    import org.cytosm.cypher2sql.cypher.ast.clause.*;
    import org.cytosm.cypher2sql.cypher.ast.clause.match.*;
    import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.*;
    import org.cytosm.cypher2sql.cypher.ast.clause.projection.*;
    import static org.cytosm.cypher2sql.cypher.parser.SpanUtil.makeSpan;
}

// High level part

cypher returns [Statement res]: sp? st=statement (sp? ';')? EOF {
    $res = $st.res;
    $st.res.span = makeSpan($st.start, $st.stop);
};

statement returns [Statement res]: q=query {
    $res = new Statement($q.res);
    $q.res.span = makeSpan($q.start, $q.stop);
};

query returns [Query res]: rg=regularQuery {
    $res = new Query($rg.res);
    $rg.res.span = makeSpan($rg.start, $rg.stop);
};

// TODO(Jaan): Add support for Unions.
regularQuery returns [QueryPart res]: sq=singleQuery (sp? union)* { $res = $sq.res; };

singleQuery returns [SingleQuery res]: cls+=clause (sp? cls+=clause)* {
    Span span = new Span(
        $cls.get(0).getStart().getStartIndex(),
        $cls.get($cls.size() - 1).getStop().getStopIndex()
    );
    $cls.forEach(cl -> cl.res.span = makeSpan(cl.start, cl.stop));
    $res = new SingleQuery($cls.stream().map(cl -> cl.res).collect(Collectors.toList()));
    $res.span = span;
};

union: (UNION sp ALL sp? singleQuery) | (UNION sp? singleQuery);

clause returns [Clause res]: m=match   { $res = $m.res; }
                           | w=with    { $res = $w.res; }
                           | r=return_ { $res = $r.res; };

match returns [Match res] locals [boolean opt=false, Where wh=null]:
    (OPTIONAL { $opt=true; } sp)? MATCH sp? pt=pattern (sp? w=where { $wh = $w.res; })? {
        $pt.res.span = makeSpan($pt.start, $pt.stop);
        $res = new Match($opt, $pt.res, $wh);
    };

with returns [With res] locals [boolean distinct=false, Where wh=null]:
    WITH sp (DISTINCT { $distinct = true; } sp)? rb=returnBody (sp? w=where { $wh = $w.res; })? {
        $res = new With($distinct, $rb.ritems, $rb.rorder, $rb.rskip, $rb.rlimit, $wh);
    };

return_ returns [Return res] locals [boolean distinct=false]:
    RETURN sp (DISTINCT { $distinct = true; } sp)? rb=returnBody {
        $res = new Return($distinct, $rb.ritems, $rb.rorder, $rb.rskip, $rb.rlimit);
    };

where returns [Where res]: w=where_nospan { $res = $w.res; $res.span = makeSpan($w.start, $w.stop); };

where_nospan returns [Where res]: WHERE sp e=expression { $res = new Where($e.res); };

returnBody returns [List<ReturnItem> ritems, OrderBy rorder = null, Limit rlimit = null, Skip rskip = null]:
    ri=returnItems { $ritems = $ri.res; }
    (sp o=order { $rorder = $o.res; $rorder.span = makeSpan($o.start, $o.stop); })?
    (sp s=skip  { $rskip  = $s.res; $rskip.span  = makeSpan($s.start, $s.stop); })?
    (sp l=limit { $rlimit = $l.res; $rlimit.span = makeSpan($l.start, $l.stop); })?;

returnItems returns [List<ReturnItem> res]: ris+=returnItem (sp? ',' sp? ris+=returnItem)* {
    $ris.forEach(ri -> ri.res.span = makeSpan(ri.start, ri.stop));
    $res = $ris.stream().map(ri -> ri.res).collect(Collectors.toList());
};

returnItem returns [ReturnItem res]:
          ( e=expression sp AS sp v=variable ) { $res = new ReturnItem.Aliased($e.res, $v.res); }
          | e=expression                       { $res = new ReturnItem.Unaliased($e.res, $e.text);   };

order returns [OrderBy res]: ORDER sp BY sp sis+=sortItem (sp? ',' sp? sis+=sortItem)* {
    $sis.forEach(si -> si.res.span = makeSpan(si.start, si.stop));
    $res = new OrderBy($sis.stream().map(si -> si.res).collect(Collectors.toList()));
};

skip returns [Skip res]: SKIP_ sp e=expression { $res = new Skip($e.res); };

limit returns [Limit res]: LIMIT sp e=expression { $res = new Limit($e.res); };

sortItem returns [SortItem res]: (e=expression sp? DESC) { $res = new SortItem.Desc($e.res); }
                               | (e=expression sp? ASC?) { $res = new SortItem.Asc($e.res); };

pattern returns [Pattern res]: pp+=patternPart (sp? ',' sp? pp+=patternPart)* {
    $pp.forEach(pp -> pp.res.span = makeSpan(pp.start, pp.stop));
    $res = new Pattern($pp.stream().map(p -> p.res).collect(Collectors.toList()));
};

patternPart returns [PatternPart res]:
    (v=variable sp? '=' sp?) pe=patternElement  { $res = new NamedPatternPart($pe.res, $v.res);
                                                  $pe.res.span = makeSpan($pe.start, $pe.stop); }
   | pe=patternElement                          { $res = new PatternPart($pe.res);
                                                  $pe.res.span = makeSpan($pe.start, $pe.stop); };

patternElement returns [PatternElement res]
   : (np=nodePattern (sp? pc+=patternElementChain)*) {
        $np.res.span = makeSpan($np.start, $np.stop);
        $pc.forEach(rc -> rc.res.span = makeSpan(rc.start, rc.stop));
        $res = $pc.stream().map(rc -> (PatternElement) rc.res).reduce($np.res, (leftpe, rc) -> {
            ((RelationshipChain) rc).element = leftpe;
            return rc;
        });
     }
   | ( '(' pe=patternElement ')' ) { $res = $pe.res; };

nodePattern returns [NodePattern res] locals [Variable v, List<LabelName> ls = Collections.emptyList(), MapExpression p = null]
    : '(' sp? (var=variable sp?   { $v = $var.res; })?
              (nls=nodeLabels sp? { $ls = $nls.res; })?
              (ps=props sp?       { $p = $ps.res; })?
      ')' { $res = new NodePattern($v, $ls, $p); };

nodeLabels returns [List<LabelName> res]: nls+=nodeLabel (sp? nls+=nodeLabel)* {
    $res = $nls.stream().map(nl -> nl.res).collect(Collectors.toList());
};

nodeLabel returns [LabelName res]: ':' lb=labelName { $res = $lb.res; };

patternElementChain returns [RelationshipChain res]
    : rp=relationshipPattern sp? rn=nodePattern {
        $rn.res.span = makeSpan($rn.start, $rn.stop);
        $rp.res.span = makeSpan($rp.start, $rp.stop);
        $res = new RelationshipChain(null, $rp.res, $rn.res);
    };

relationshipPattern
   returns [RelationshipPattern res = new RelationshipPattern(null, null, null, Collections.emptyList())]
   : ('<' sp? '-' sp? (d=relationshipDetail { $res = $d.res; } sp?)? '-' sp? '>') { $res.direction = RelationshipPattern.SemanticDirection.BOTH; }
   | ('<' sp? '-' sp? (d=relationshipDetail { $res = $d.res; } sp?)? '-')        { $res.direction = RelationshipPattern.SemanticDirection.INCOMING; }
   | ('-' sp? (d=relationshipDetail { $res = $d.res; } sp?)? '-' sp? '>')        { $res.direction = RelationshipPattern.SemanticDirection.OUTGOING; }
   | ('-' sp? (d=relationshipDetail { $res = $d.res; } sp?)? '-')               { $res.direction = RelationshipPattern.SemanticDirection.BOTH; };

relationshipDetail returns [RelationshipPattern res]
     locals [Variable v = null, List<RelTypeName> types = null, Optional<Range> r = null, MapExpression p = null]:
    '[' sp? (var=variable  { $v = $var.res; })? sp? (rt=relationTypes { $types = $rt.res; })?
        sp? ('*' rag=range { $r = $rag.res; })? sp? (ps=props         { $p = $ps.res; })? sp?
    ']' {
        $types = ($types == null) ? Collections.emptyList(): $types;
        $res = new RelationshipPattern($v, $r, $p, $types);
    };

range returns [Optional<Range> res = Optional.empty()]
    : sp? (ilit=integerLit sp? { $res = Optional.of(new Range($ilit.res, $ilit.res)); })?
         ('..' sp? (ilit=integerLit sp? {
            $res = Optional.of($res.orElseGet(() -> new Range(null, null)));
            $res.get().upper = ($ilit.res == null) ? Optional.empty(): Optional.of($ilit.res);
            })?
         )?;

props returns [MapExpression res]: m=mapLiteral { $res = $m.res; };

relationTypes returns [List<RelTypeName> res]
    : ':' rts+=relTypeName (sp? '|' ':'? sp? rts+=relTypeName)* {
        $res = $rts.stream().map(rt -> rt.res).collect(Collectors.toList());
    };

relTypeName returns [RelTypeName res]: s=symbolicName {
    $res = new RelTypeName($s.res);
    $res.span = makeSpan($s.start, $s.stop);
};

labelName returns [LabelName res]: s=symbolicName {
    $res = new LabelName($s.res);
    $res.span = makeSpan($s.start, $s.stop);
};

variable returns [Variable res]: s=symbolicName {
    $res = new Variable($s.res);
    $res.span = makeSpan($s.start, $s.stop);
};


// Expressions

mapLiteral returns [MapExpression res]: m=mapLiteral_nospan { $res = $m.res; $res.span = makeSpan($m.start, $m.stop); };

mapLiteral_nospan returns [MapExpression res]:
    '{' sp? ( kvs+=mapKeyValuePair ( ',' sp? kvs+=mapKeyValuePair )* )? '}' {
        $res = new MapExpression($kvs.stream().map(kv -> kv.res).collect(Collectors.toList()));
    };

mapKeyValuePair returns [Pair<PropertyKeyName, Expression> res]
    : p=propertyKeyName sp? ':' sp? e=expression sp? {
        $res = new ImmutablePair($p.res, $e.res);
    };

listLiteral returns [ListExpression res]: l=listLit_nospan { $res = $l.res; $res.span = makeSpan($l.start, $l.stop); };

listLit_nospan returns [ListExpression res]:
    '[' sp? (vs+=expression ( ',' sp? vs+=expression )* )? ']' {
        $res = new ListExpression($vs.stream().map(v -> v.res).collect(Collectors.toList()));
    };

expression returns [Expression res]: e=expression12 { $res = $e.res; $res.span = makeSpan($e.start, $e.stop); };

expression12 returns [Expression res]: l=expression11 (sp OR sp oths+=expression11)* {
    $l.res.span = makeSpan($l.start, $l.stop);
    $oths.forEach(b -> b.res.span = makeSpan(b.start, b.stop));
    $res = $oths.stream().map(b -> (Expression) new Binary.Or(null, b.res)).reduce($l.res, (lhs, rhs) -> {
        ((Binary) rhs).lhs = lhs;
        return rhs;
    });
};

expression11 returns [Expression res]: l=expression10 (sp XOR sp oths+=expression10)* {
    $l.res.span = makeSpan($l.start, $l.stop);
    $oths.forEach(b -> b.res.span = makeSpan(b.start, b.stop));
    $res = $oths.stream().map(b -> (Expression) new Binary.Xor(null, b.res)).reduce($l.res, (lhs, rhs) -> {
        ((Binary) rhs).lhs = lhs;
        return rhs;
    });
};

expression10 returns [Expression res]: l=expression9 (sp AND sp oths+=expression9)* {
    $l.res.span = makeSpan($l.start, $l.stop);
    $oths.forEach(b -> b.res.span = makeSpan(b.start, b.stop));
    $res = $oths.stream().map(b -> (Expression) new Binary.And(null, b.res)).reduce($l.res, (lhs, rhs) -> {
        ((Binary) rhs).lhs = lhs;
        return rhs;
    });
};

expression9 returns [Expression res]: (sp nt+=NOT sp)* e=expression8 {
    $e.res.span = makeSpan($e.start, $e.stop);
    $res = $nt.stream().map(_i -> (Expression) null).reduce($e.res, (lhs, _i) -> new Unary.Not(lhs));
};

expression8 returns [Expression res]: l=expression7 (sp? oths+=partialComparisonExpression)* {
    $l.res.span = makeSpan($l.start, $l.stop);
    $oths.forEach(b -> b.res.span = makeSpan(b.start, b.stop));
    $res = $oths.stream().map(b -> (Expression) b.res).reduce($l.res, (lhs, rhs) -> {
        ((Binary) rhs).lhs = lhs;
        return rhs;
    });
};

expression7 returns [Expression res]: l=expression6 (sp? oths+=partialArithmeticExpressionWithLowPrecedence)* {
    $l.res.span = makeSpan($l.start, $l.stop);
    $oths.forEach(b -> b.res.span = makeSpan(b.start, b.stop));
    $res = $oths.stream().map(b -> (Expression) b.res).reduce($l.res, (lhs, rhs) -> {
        ((Binary) rhs).lhs = lhs;
        return rhs;
    });
};

expression6 returns [Expression res]: l=expression5 (sp? oths+=partialArithmeticExpressionWithHighPrecedence)* {
    $l.res.span = makeSpan($l.start, $l.stop);
    $oths.forEach(b -> b.res.span = makeSpan(b.start, b.stop));
    $res = $oths.stream().map(b -> (Expression) b.res).reduce($l.res, (lhs, rhs) -> {
        ((Binary) rhs).lhs = lhs;
        return rhs;
    });
};

expression5 returns [Expression res]: l=expression4 (sp? oths+=partialArithmeticPow)* {
    $l.res.span = makeSpan($l.start, $l.stop);
    $oths.forEach(b -> b.res.span = makeSpan(b.start, b.stop));
    $res = $oths.stream().map(b -> (Expression) b.res).reduce($l.res, (lhs, rhs) -> {
        ((Binary) rhs).lhs = lhs;
        return rhs;
    });
};

expression4 returns [Expression res]: (us+=unaryOp sp?)* e=expression3 {
    $e.res.span = makeSpan($e.start, $e.stop);
    $res = $us.stream().map(u -> (Expression) u.res).reduce($e.res, (lhs, u) -> {
        ((Unary) u).lhs = lhs;
        return u;
    });
};

unaryOp returns [Unary res]: '+' { $res = new Unary.Add(null); } | '-' { $res = new Unary.Subtract(null); };

expression3 returns [Expression res]: l=expression2 (sp? oths+=expression1)* {
    $l.res.span = makeSpan($l.start, $l.stop);
    $res = $oths.stream().map(u -> (Expression) u.res).reduce($l.res, (lhs, u) -> {
        ((Unary) u).lhs = lhs;
        return u;
    });
};

expression2 returns [Expression res]
    : a=atom (lks+=propertyLookup)* {
        $lks.forEach(p -> p.res.span = makeSpan(p.start, p.stop));
        $res = $lks.stream().map(p -> (Expression) p.res).reduce($a.res, (lhs, rhs) -> {
            ((Property) rhs).map = lhs;
            return rhs;
        });
    };

expression1 returns [Unary res]
    : '[' expression ']'                    { $res = null; } // TODO
    | '[' expression? '..' expression? ']'  { $res = null; } // TODO
    | e=expression0                         { $res = $e.res; $res.span = makeSpan($e.start, $e.stop); }
    | sp IS sp NULL                         { $res = new Unary.IsNull(null); }
    | sp IS sp NOT sp NULL                  { $res = new Unary.IsNotNull(null); };


expression0 returns [Binary res]
    : sp? '=~' sp? e=expression2            { $res = new Binary.RegexMatch(null, $e.res); }
    | sp IN sp? e=expression2               { $res = new Binary.In(null, $e.res); }
    | sp STARTS sp WITH sp? e=expression2   { $res = new Binary.StartsWith(null, $e.res); }
    | sp ENDS sp WITH sp? e=expression2     { $res = new Binary.EndsWith(null, $e.res); }
    | sp CONTAINS sp? e=expression2         { $res = new Binary.Contains(null, $e.res); };


propertyLookup returns [Property res]: sp? '.' sp? p=propertyKeyName {
    $res = new Property(null, $p.res);
};

partialArithmeticExpressionWithLowPrecedence returns [Binary res]
    : '+' sp? e=expression6     { $res = new Binary.Add(null, $e.res); }
    | '-' sp? e=expression6     { $res = new Binary.Subtract(null, $e.res); };

partialArithmeticExpressionWithHighPrecedence returns [Binary res]
    : '*' sp? e=expression5     { $res = new Binary.Multiply(null, $e.res); }
    | '/' sp? e=expression5     { $res = new Binary.Divide(null, $e.res); }
    | '%' sp? e=expression5     { $res = new Binary.Modulo(null, $e.res); };

partialArithmeticPow returns [Binary res]
    : '^' sp? e=expression4     { $res = new Binary.Pow(null, $e.res); };

partialComparisonExpression returns [Binary res]
    : ('=' sp? e=expression7)   { $res = new Binary.Equals(null, $e.res); }
    | ('<>' sp? e=expression7)  { $res = new Binary.NotEquals(null, $e.res); }
    | ('!=' sp? e=expression7)  { $res = new Binary.InvalidNotEquals(null, $e.res); }
    | ('<' sp? e=expression7)   { $res = new Binary.LessThan(null, $e.res); }
    | ('>' sp? e=expression7)   { $res = new Binary.GreaterThan(null, $e.res); }
    | ('<=' sp? e=expression7)  { $res = new Binary.GreaterThanOrEqual(null, $e.res); }
    | ('>=' sp? e=expression7)  { $res = new Binary.LessThanOrEqual(null, $e.res); };

atom returns [Expression res]
    : n=numberLit { $res = $n.res; }
    | s=StringLit { $res = new Literal.StringLiteral($s.text); }
    | TRUE        { $res = new Literal.True(); }
    | FALSE       { $res = new Literal.False(); }
    | NULL        { $res = new Literal.Null(); }
    | (COUNT '(' '*' ')')       { $res = null;   } // TODO
    | m=mapLiteral              { $res = $m.res; }
    | l=listLiteral             { $res = $l.res; }
    | p=parenthesizedExpression { $res = $p.res; }
    | f=functionInvocation      { $res = $f.res; }
    | v=variable                { $res = $v.res; };

parenthesizedExpression returns [Expression res]: '(' sp? e=expression sp? ')' { $res = $e.res; };

functionInvocation returns [FunctionInvocation res]
    : fn=functionName sp? '(' sp? DISTINCT? ( args+=expression ( ',' sp? args+=expression )* )? sp? ')' {
        $res = new FunctionInvocation($fn.res, $DISTINCT != null,
            $args.stream().map(arg -> arg.res).collect(Collectors.toList())
        );
    };

functionName returns [FunctionName res]: s=symbolicName {
    $res = new FunctionName($s.res);
    $res.span = makeSpan($s.start, $s.stop);
};

propertyKeyName returns [PropertyKeyName res]: s=symbolicName {
    $res = new PropertyKeyName($s.res);
    $res.span = makeSpan($s.start, $s.stop);
};

numberLit returns [Literal.Number res]
    : d=doubleLit  { $res = $d.res; }
    | i=integerLit { $res = $i.res; };

integerLit returns [Literal.Integer res]: i=integerLit_nospan {
    $res = $i.res;
    $res.span = makeSpan($i.start, $i.stop);
};

integerLit_nospan returns [Literal.Integer res]
    : i=HexInteger      { $res = new Literal.HexInteger($i.text); }
    | i=OctalInteger    { $res = new Literal.OctalInteger($i.text); }
    | i=DecimalInteger  { $res = new Literal.UnsignedInteger($i.text); };


doubleLit returns [Literal.DecimalDouble res]
         : e=real { $res = new Literal.DecimalDouble($e.text); $res.span = makeSpan($e.start, $e.stop); };

real
    : DigitString '.' Digit* Exponent?
    | '.' DigitString Exponent?
    | DigitString Exponent?;



// Common
symbolicName returns [String res]: s=symbols  { $res = $s.text; };
symbols: UnescapedSymbolicName
       | EscapedSymbolicName
       | UNION
       | ALL
       | OPTIONAL
       | MATCH
       | WHERE
       | AS
       | WITH
       | DISTINCT
       | RETURN
       | ORDER
       | BY
       | SKIP_
       | LIMIT
       | DESC
       | ASC
       | OR
       | XOR
       | AND
       | NOT
       | IN
       | STARTS
       | ENDS
       | CONTAINS
       | IS
       | NULL
       | TRUE
       | FALSE
       | COUNT
       | FILTER
       | EXTRACT;

sp: (WHITESPACE)+;