# Cytosm: Path Finder

## Overview:

"The" Path Finder is a parallelizable Regular Path Query solver, taking into consideration the graph topology in the gTop file. The Regular Path Query is the pattern/relationship-chain section on Cypher Language.

## Context:
Without a well defined topological mapping between the relational and the property graph models, certain queries can cause a combinatorial explosion of arrangements due to lack of understanding on the graph model layout underneath it. Many solutions would actually be impossible if not analysed in the light of the graph topology model. When every node and edge in the graph follows properly defined rules, the query planner can prune several impossible graph traversals before start executing the query - in a similar fashion to what the common relational systems planner does (or should do) with dead code.

## Black Box Example

The Path Finder module acts as a pre-planner in order to provide any relational system query planner with graph topology information. It trims out any unnecessary join that would either return an impossible graph traversal - by impossible graph traversal one can understand a sequence of nodes and edges that would not follow the model described in the graph topology file. The Path Finder algorithm is designed to be independently parallelizable. One of the benefits of having this pre-computation layer is that badly formatted or model invalid queries can be invalidated before even being sent to the RDBMS, reducing drastically the response time.

<p align="center">
  <img src="../common/docs/gtop.png?raw=true" alt="gtop"/>
</p>


The model described in the figure above above contains six different types of nodes and the same number of unique types of directed edges. A sample of a cypher query that could be done on top of this system is shown below. This query would return all the people that work in some company.

```
Match (a)-[:works_at]->(b)
Return a;
```

The input of the Path Finder is the Regular Path Query. In the Cypher example, it is the relationship-chain defined as:

```
(a)-[:works_at]->(b)
```

The Path Finder module is, however, not limited to a single graph query language or representation of a Regular Path Query. Node <i>a</i> and <i>b</i>, represented by <i>(a)</i> and <i>(b)</i> are what is called anonymous node. These are nodes whose type is not explicitelly defined in the query. The letters <i>a</i> and <i>b</i> are called variables and used to refer to that given node in other moments of the query.

A human reader, knowing the information on the gTop, would have correctly infered that <i>(a)</i> can only be of type Person and <i>(b)</i> of type Company since of the edge between them is labeled works at.

## Detailed Example

A more complex route path description would take to the human reader considerably more time to visualise and enumerate all the possible outcomes. Assume the following path description:

```
( )<--(m: {"passport_no"': "FD8X723"})-[*1..2 ]->(n)<--(c)
```

There are four anonymous nodes in the original query, three of them referenced with the variables <i>m</i>, <i>n</i> and <i>c</i>. The directed edge between <i>m</i> and <i>n</i> has the content <i>*1..2</i>. It is what we called of edge expansion wildcard. This means that <i>n</i> could be one or two hops away from <i>m</i>. The correct set of paths, matching the gtop description and the described query, are:

The correct solution found by Path Finder solutions are:

```
(:Pet)<-[:owns]- (:Person) -[:works_at]-> (:Company) <-[:supplies]- (:Electric_Supplier)

(:Company)<-[works_at]- (:Person) -[:works_at]-> (:Company) <-[:supplies]- (:Electric_Supplier)

(:Pet)<-[:owns]-(:Person)-[:works_at]->(:Company) -[:based_in]-> (:City)<-[:available_in]- (:Electric_Supplier)

(:Company)<-[works_at]- (:Person) -[works_at]-> (:Company) -[:based_in]-> (:City) <-[:available_in]- (:Electric_Supplier)
```

### Top Level Algorithm

The process that allows the Path Finder to enumerate all the possible routes that fit that pattern is described in the algorithm below:

<b>Input:</b> Graph path that may contain regular edge and node expressions; a gTop file<br>
<b>Output:</b> Set of graph paths without regular edge or node expressions, in accordance to gTop rules.

```
read graph path description;
read gTop file;
solves any possible node and edge hints;
replace multi-hop regular edge expressions with equivalent edge/node pairs;

While( exists non-solved graph path) {
	contextualise graph path using the gtop;
    }
}
```

The algorithm receives a gTop file and a graph route with edge wildcard expansions or anonymous nodes. Some queries may contain characteristics that will greatly reduce the possible matches from an anonymous node/edge. In this query, the anonymous node <i>(m)</i> has an attribute called "passport_no". Based on the gTop model, one can easily infer that this node can only be of type Person. Sometimes it is an attribute that is not exclusive to a single node or edge type, but still reduces the number of possible matches for that node early in the search.
In the sequence it identifies any edge wildcard expansion in the query and performs the route dilatation. Thus, the route original route is equivalent to:

```
[1]		( )<--(m: Person {"passport_no": "FD8X723"})-->(n)<--(c)

[2]		( )<--(m: Person {"passport_no": "FD8X723"})-->()-->(n)<--(c)
```

In other words, the input route is the union of the routes [1] and [2]. Due to this property, routes [1] and [2] can be contextualized in independently in parallel. In order to continue the demonstration, we are going to assume the current route being analyzed is [2].

A graph search algorithm (as Depth-First Search) associated with a path tracking structure can be used in order to solve every single graph path according to the graph topology file. This process is called contextualization, since it contextualizes a route with non-defined nodes and edges to a set of well defined node and edge sequence that follow the graph topology model.

At the end of this process, the set of solutions described above are found.

## Performance comparison:

The following results compare the planning of 4 hops away-queries with and without the Path Finder. They are based in LDBC query number 6.

### 1. Four hops with anonymous node:

```
Profile MATCH (person:Person {id:2199023259437})-[:KNOWS]->()-[:KNOWS]->()-[:KNOWS]->()-[:KNOWS]->(friend:Person),
(friend:Person)<-[:HAS_CREATOR]-(friendPost:Post)-[:HAS_TAG]->(knownTag:Tag {name:"A_Woman_and_a_Man"})
WHERE not(person=friend)
MATCH (friendPost:Post)-[:HAS_TAG]->(commonTag:Tag)
WHERE not(commonTag=knownTag)
WITH DISTINCT commonTag, knownTag, friend
```

Cypher version: CYPHER 2.3, planner: COST. <b>1678093696 total db hits</b> in <b>1769262 ms</b>.

The equivalent plan is:

<p align="center">
  <img src="docs/pathFinderPlanning/neo4j2_3_4/anonNodesPlan/plan.png?raw=true" alt="Anonymous Nodes Plan"/>
</p>

### 2. Edge Expansion:

```
Profile MATCH (person:Person {id:2199023259437})-[:KNOWS*4]->(friend:Person),
(friend:Person)<-[:HAS_CREATOR]-(friendPost:Post)-[:HAS_TAG]->(knownTag:Tag {name:"A_Woman_and_a_Man"})
WHERE not(person=friend)
MATCH (friendPost:Post)-[:HAS_TAG]->(commonTag:Tag)
WHERE not(commonTag=knownTag)
WITH DISTINCT commonTag, knownTag, friend
```

Cypher version: CYPHER 2.3, planner: COST. <b>1678093696 total db hits</b> in <b>1669792 ms</b>.

The equivalent plan is:

<p align="center">
  <img src="./docs/pathFinderPlanning/neo4j2_3_4/regEdgePlan/plan.png?raw=true" alt="Regular Edge Expression Plan"/>
</p>

### 3. Path Finder:

```
Profile MATCH (person:Person {id:2199023259437})-[:KNOWS]->(:Person)-[:KNOWS]->(:Person)-[:KNOWS]->(:Person)-[:KNOWS]->(friend:Person),
(friend:Person)<-[:HAS_CREATOR]-(friendPost:Post)-[:HAS_TAG]->(knownTag:Tag {name:"A_Woman_and_a_Man"})
WHERE not(person=friend)
MATCH (friendPost:Post)-[:HAS_TAG]->(commonTag:Tag)
WHERE not(commonTag=knownTag)
WITH DISTINCT commonTag, knownTag, friend
MATCH (commonTag:Tag)<-[:HAS_TAG]-(commonPost:Post)-[:HAS_TAG]->(knownTag:Tag)
WHERE (commonPost:Post)-[:HAS_CREATOR]->(friend:Person)
RETURN
commonTag.name AS tagName,
count(commonPost) AS postCount
ORDER BY postCount DESC, tagName ASC
LIMIT 20;
```

Cypher version: CYPHER 2.3, planner: COST. <b>829079 total db hits</b> in <b>3256 ms</b>.

The equivalent plan is:

<p align="center">
  <img src="docs/pathFinderPlanning/neo4j2_3_4/pathFinder/plan.png?raw=true" alt="Regular Edge Expression Plan"/>
</p>
