#Cytosm: gTop

##Overview:

In Grapher, property graphs follows a graph topology file (gTop) - it is a set of rules where every edge or vertex from the graph can be assigned at least one edge or vertex type. Vertices or edges of the same type share the same property keys, having possibly different values for those keys. The file format supports mixed graphs (that are graphs that can contain both undirected and directed edges) and parallel edges. Through the use of the gTop, it is possible to map a GQL into the relational domain.

##Example:

An example of gTop file is presented in figure below.

<p align="center">
  <img src="../docs/static_files/gtop.png?raw=true" alt="gtop"/>
</p>

The following graph could be modeled by the gtop above.

<p align="center">
  <img src="../docs/static_files/gtopgraph.png?raw=true" alt="gtopgraph"/>
</p>

## Gtop Features

GTop enables:
 
* <b>Flexibility:</b> GQLs can run on RDBMS without implementation system details. A translation step having as input a gTop file associated with the GQL should be able to construct valid SQL for the RDBMS.
* <b>Multiple Models:</b> it describes how data stored in the RDBMS would be visualised as a property graph, classifying information as node types and edge types as long as it fits certain interpretation rules. In other words, it maps relational tuple sets into nodes and edge types.

The separation of gTop files in two layers enables flexibility in mapping. The abstraction layer describes the property graph model (as a serialized description of the first figure) while the implementation one defines the mapping mechanisms between domains. We will describe this in detail.

This layer describes how the information described in the abstraction level gTop is stored in the underlying relational system. Nodes of the same type can be found on multiple tables and edges can actually represent multiple table joins between the source and destination nodes.

Also nodes and edges of the same type can coexist in multiple tables: nodes of type Person could have been stored inside the tables <i><b>proletariat</b></i> and <i><b>bourgeoisie</b></i> in the relational system. As long as both data tables were assigned the type Person on the implementation level gTop, the GQL will be agnostic of this structure and refer to the data of both of them by the type Person. This feature can be extended to resemble a type hierarchy from object-oriented languages: nodes of type Electricity Suppliers can also be of type Company. Similarly, restrictions can be applied based on table data rules. It provides support to split a single table into several different types of nodes/edges. It would make sense for a relational system to have the information of Companies and Electricity Suppliers in the same table. An extra aspect that implementation level sums is the possibility of representing an edge as a multiple sequential join of tables.

