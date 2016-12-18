#CYpher TO Sql Mapper (Cytosm)

[![Build Status](https://travis-ci.org/cytosm/cytosm.svg?branch=master)](https://travis-ci.org/cytosm/cytosm.svg?branch=master)

Cytosm is an open source software library for converting Cypher queries into plain old SQL on-the-fly. 

Cytosm was originally developed by researchers and engineers working on the graph analytics on a variety of databases. 

If you'd like to contribute to Cytosm, be sure to review the [contribution guidelines](CONTRIBUTING.md).

**We use [GitHub issues](https://github.com/cytosm/cytosm/issues) for tracking, requests, and bugs.**

## Installation

You can simply use Cytosm as a library in your code (a simple mapping function that given a valid Cypher 'string' returns a valid 'SQL'. 


## Overview

A Cypher string goes into several transformations in it journey through Cytsom:

* Parsing (auto generated ANTLR parsed based on OpenCypher EBNF grammar). It creates an AST to be used later on.
* PathFinder navigates the AST, given a graph topology file, in order to make Cypher queries more concrete.
* Cypher2SQL. The module where all the magic happens. 


### Path Finder

A set of simple optimisations that try to make Cypher queries more concrete (avoiding the mapper from exploring patterns in the Cypher queries that 
are logically correct, but impossible in the light of the database Tables that exist in the database).
 
This way, the mapping process is simpler and we make SQL queries more efficient.

See [PathFinder](pathfinder/README.md)

### Cypher2SQL

This module takes the concreted Cypher queries that the PathFinder module spits out and
 
 * analises dependencies between Cypher variables and tracks their scope.
 * creates an intermediate language representation of the query (something closer to SQL, but quite there yet). This is a hierarchical representation. 
 * from the hierarchy created in the previous stage, it builds a sequence of nested joins and unions in SQL to represent the graph patterns indicated in Cypher.

See [Cypher2SQL](cypher2sql/README.md)

### Common

#### Graph Topology Files (gTop)

A description of the graph hiding in your relational database. It also includes how to mapp from abstract node/edges in the graph into specific databse tables/columns. 

Find more details about [gTop](common/README.md)
 
A gTop file can be automatically discovered by the "Graph Extraction" module (to be opensourced soon).

## Benchmarks

Cytosm queries have been run on a variety of backends, obtaining quite suprising results. Please find more details in 
the sibling repo for [Cytosm benchmarking](https://github.com/Alnaimi-/database-benchmark). 


## Known Issues

- Directed relationships (see `PopulateJoins` pass for more information on the current status)
- Arbitrary hops that could be defined in the gtop (similarly to the previous dot, more detail can be found in `PopulateJoins`)
- Proper handling of the `COUNT` function (we only support limited use cases)
- `SKIP`, `LIMIT` and `ORDER BY` are not propagated appropriately on "wide" query - that is queries involving at least one UNION in the generated SQL.
- Improve the type-checker to compute correctly the type-checker of any expressions. The current version is incomplete.

The following would be nice to have:

- Pattern expressions are not supported at all (the grammar does not even includes them). So things such as:
  ```cypher
  MATCH (a), (b) WHERE NOT((a)--(b))
  ```
  are not supported
- Improve `CypherConverter` and `pathfinder` to generate AST nodes instead of using intermediary string representations.
- Improve the `pathfinder` related code to use the full information available about the variable and their type.
