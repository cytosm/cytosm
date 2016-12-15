**CYpher TO Sql Mapper (Cytosm)** is an open source software library for converting Cypher queries into plain old SQL on-the-fly. 

Cytosm was originally developed by researchers and engineers working on the graph analytics on a variety of databases. 

If you'd like to contribute to Cytosm, be sure to review the [contribution guidelines](CONTRIBUTING.md.

**We use [GitHub issues](https://github.com/cytosm/cytosm/issues) for tracking, requests, and bugs.**

## Installation

You can simply use Cytosm as a library in your code (a simple mapping function that given a valid Cypher 'string' returns a valid 'SQL'. 

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
