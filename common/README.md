# Cytosm: gTop

## Overview:

In Cytosm, property graphs follows a graph topology file (gTop) - it is a set of rules where every edge or vertex from the graph can be assigned at least one edge or vertex type. Vertices or edges of the same type share the same property keys, having possibly different values for those keys. The file format supports mixed graphs (that are graphs that can contain both undirected and directed edges) and parallel edges. Through the use of the gTop, it is possible to map a GQL into the relational domain.

## Example:

An example of gTop file is presented in figure below.

<p align="center">
  <img src="./docs/gtop.png" alt="gtop"/>
</p>

The following graph could be modeled by the gtop above.

<p align="center">
  <img src="./docs/gtopgraph.png" alt="gtopgraph"/>
</p>

## Gtop Features

GTop enables:

* <b>Flexibility:</b> GQLs can run on RDBMS without implementation system details. A translation step having as input a gTop file associated with the GQL should be able to construct valid SQL for the RDBMS.
* <b>Multiple Models:</b> it describes how data stored in the RDBMS would be visualised as a property graph, classifying information as node types and edge types as long as it fits certain interpretation rules. In other words, it maps relational tuple sets into nodes and edge types.

The separation of gTop files in two layers enables flexibility in mapping. The <strong>abstraction layer</strong> describes the property graph model (as a serialized description of the first figure) while the implementation one defines the mapping mechanisms between domains. We will describe this in detail.

This layer describes how the information described in the abstraction level gTop is stored in the underlying relational system. Nodes of the same type can be found on multiple tables and edges can actually represent multiple table joins between the source and destination nodes.

Also nodes and edges of the same type can coexist in multiple tables: nodes of type Person could have been stored inside the tables <i><b>proletariat</b></i> and <i><b>bourgeoisie</b></i> in the relational system. As long as both data tables were assigned the type Person on the implementation level gTop, the GQL will be agnostic of this structure and refer to the data of both of them by the type Person. The types model also allows Electricity Suppliers can to be also of type Company.

Similarly, restrictions can be applied based on table data rules. It provides support to split a single table into several different types of nodes/edges. It would make sense for a relational system to have the information of Companies and Electricity Suppliers in the same table. An extra aspect that implementation level sums is the possibility of representing an edge as a multiple sequential join of tables.

## Getting started

Before start writing your own gTop file, you should first model your RDBMS data as a property graph.

### Defining a Property Graph model to your relational dataset
To start this task, you need to be aware of your RDBMS schema.
As an example, assume the RDBMS tables below. The content of these tables are available [here](docs/movies.db), in a <i>.db</i> file.

<strong>Person Table</strong>

| id | name               | born |
|----|--------------------|------|
| 1  | Keanu Reeves       | 1964 |
| 2  | Carrie-Anne Moss   | 1967 |
| 3  | Laurence Fishburne | 1961 |
| 4  | Hugo Weaving       | 1960 |
| 5  | Andy Wachowski     | 1967 |
| 6  | Lana Wachowski     | 1965 |
| 7  | Joel Silver        | 1952 |
| 8  | Charlize Theron    | 1975 |
| 9  | Al Pacino          | 1940 |
| 10 | Taylor Hackford    | 1944 |


<strong>Movie Table</strong>

|id|title|released|tagline|
|--- |--- |--- |--- |
|1|The Matrix|1999|Welcome to the Real World|
|2|The Devil's Advocate|1997|Evil has its winning ways|
|3|Monster|2003|The first female serial killer of America|

<strong>Acted_in table</strong>

|role|person_id|movie_id|
|--- |--- |--- |
|Neo|1|1|
|Trinity|2|1|
|Morpheus|3|1|
|Agent Smith|4|1|
|Kevin Lomax|1|2|
|Mary Ann Lomax|8|2|
|John Milton|9|2|
|Aileen|8|3|

<strong>Produced table</strong>

|person_id|movie_id|
|--- |--- |
|7|1|
|8|3|

<strong>Directed Table</strong>

|person_id|movie_id|
|--- |--- |
|5|1|
|6|1|
|10|2|

All these tables above can be modeled as the following property graph:

<p align="center">
  <img src="./docs/actorMovieDiagram.png" alt="actorMovieDiagram"/>
</p>

Someone a bit experienced in graph modeling would classify this model as a directed bipartite graph.

Keep in mind that <strong>nothing</strong> in the RDBMS data itself defines that the graph is directed. It is just how me, the person writing this documentation, decided to model it.

Also, bear in mind that a single RDBMS system may be modeled in a plethora of different property graphs.
This is just one of the many models that could have been done from the relational data above.

Now that you have a model, it's time to start writing the gTop file.

### Writing your own gTop file

The root level of the gTop file is split into tree main attributes:

```
{
    version	:	1.0
    abstractionLevel		{...}
    implementationLevel		{...}
}
```

### Versioning

The first attribute, called <strong>version</strong>, is just a future-friendly feature. In the future, other additions to gTop may use a new version there. Let's proceed to the <strong>abstraction level</strong>.

### Abstraction level

The <strong>abstraction level</strong> is a simple serialized description of the model you done on your property graph. No information from the relational data at all is used here.


Let's go for an example of the Abstraction Level gTop. The bipartite property-graph defined above would be translated to:

```json
{
"abstractionLevel" : {
    "abstractionNodes" : [ {
      "types" : [ "movie" ],
      "attributes" : [ "id", "title", "released", "tagline" ]
    }, {
      "types" : [ "person" ],
      "attributes" : [ "id", "name", "born" ]
    } ],
    "abstractionEdges" : [ {
      "types" : [ "directed" ],
      "attributes" : [ ],
      "sourceType" : [ "person" ],
      "destinationType" : [ "movie" ],
      "directed" : true
    }, {
      "types" : [ "acted_in" ],
      "attributes" : [ "role" ],
      "sourceType" : [ "person" ],
      "destinationType" : [ "movie" ],
      "directed" : true
    }, {
      "types" : [ "produced" ],
      "attributes" : [ ],
      "sourceType" : [ "person" ],
      "destinationType" : [ "movie" ],
      "directed" : true
    } ]
  }
}
```

There are two nodes types - called <strong>movie</strong> and <strong>person</strong>.

The <strong>types</strong> labels a given node type - and the same type can have multiple labels. An important aspect to mention
The attributes (on the property graph sense of the word) of a node type are also listed. On the Abstraction level, it is not important to define how an attribute is implemented (e.g. if it is an Integer, Text, etc).
On the edge side, one can see that "acted_in" is a directed edge, with one attribute called "role". It also connects Person nodes towards Movie nodes.

### Implementation level

The implementation level describes how the property graph is mapped into your storage system.

Technically speaking, on the Java coding side, the implementation level is actually an interface - that can be implemented (in the Java meaning of it) by other classes.

Currently only the Relational implementation is available, but this mapping is not limited to the relational domain.

The property-graph, serialized in the <strong>abstraction level</strong> above, associated with the RDBMS schema, shown on the top, has the following <strong>implementation level</strong>:

```json
{
"implementationLevel" : {
    "graphMetadata" : {
      "backendSystem" : "RELATIONAL"
    },
    "implementationNodes" : [ {
      "types" : [ "movie" ],
      "tableName" : "movie",
      "id" : [ {
        "columnName" : "id",
        "datatype" : "INTEGER",
        "concatenationPosition" : 1
      } ],
      "attributes" : [ {
        "columnName" : "id",
        "dataType" : "INTEGER",
        "abstractionLevelName" : "id"
      }, {
        "columnName" : "title",
        "dataType" : "VARCHAR(100)",
        "abstractionLevelName" : "title"
      }, {
        "columnName" : "released",
        "dataType" : "INTEGER",
        "abstractionLevelName" : "released"
      }, {
        "columnName" : "tagline",
        "dataType" : "VARCHAR(100)",
        "abstractionLevelName" : "tagline"
      } ],
      "restrictions" : [ ]
    }, {
      "types" : [ "person" ],
      "tableName" : "person",
      "id" : [ {
        "columnName" : "id",
        "datatype" : "INTEGER",
        "concatenationPosition" : 1
      } ],
      "attributes" : [ {
        "columnName" : "id",
        "dataType" : "INTEGER",
        "abstractionLevelName" : "id"
      }, {
        "columnName" : "name",
        "dataType" : "VARCHAR(100)",
        "abstractionLevelName" : "name"
      }, {
        "columnName" : "born",
        "dataType" : "INTEGER",
        "abstractionLevelName" : "born"
      } ],
      "restrictions" : [ ]
    } ],
    "implementationEdges" : [ {
      "types" : [ "person_id_directed_person_id" ],
      "paths" : [ {
        "traversalHops" : [ {
          "sourceTableName" : "person",
          "sourceTableColumn" : "id",
          "joinTableSourceColumn" : "person_id",
          "joinTableName" : "directed",
          "joinTableDestinationColumn" : "movie_id",
          "destinationTableColumn" : "id",
          "destinationTableName" : "movie",
          "attributes" : [ ]
        } ]
      } ]
    }, {
      "types" : [ "person_id_acted_in_person_id" ],
      "paths" : [ {
        "traversalHops" : [ {
          "sourceTableName" : "person",
          "sourceTableColumn" : "id",
          "joinTableSourceColumn" : "person_id",
          "joinTableName" : "acted_in",
          "joinTableDestinationColumn" : "movie_id",
          "destinationTableColumn" : "id",
          "destinationTableName" : "movie",
          "attributes" : [ {
            "columnName" : "role",
            "dataType" : "varchar(100)",
            "abstractionLevelName" : "role"
          } ]
        } ]
      } ]
    }, {
      "types" : [ "person_id_produced_person_id" ],
      "paths" : [ {
        "traversalHops" : [ {
          "sourceTableName" : "person",
          "sourceTableColumn" : "id",
          "joinTableSourceColumn" : "person_id",
          "joinTableName" : "produced",
          "joinTableDestinationColumn" : "movie_id",
          "destinationTableColumn" : "id",
          "destinationTableName" : "movie",
          "attributes" : [ ]
        } ]
      } ]
    } ]
  }
}
```
#### Graph Metadata  
The GraphMetaData contains information describing the data stored in the backend system. The field <strong>backendSystem</strong> defines which type of system the abstraction level gTop is going to be mapped to. Currently only <strong>Relational</strong> backends are supported, but more may be added in the future.

#### Implementation Node
The mapping of every node is done in the <strong>Implementation Node</strong> attribute. For a bit more information let's use the example below:

```json
{
         "types" : [ "person" ],
         "tableName" : "person",
         "id" : [ {
           "columnName" : "id",
           "datatype" : "INTEGER",
           "concatenationPosition" : 1
         } ],
         "attributes" : [ {
           "columnName" : "id",
           "dataType" : "INTEGER",
           "abstractionLevelName" : "id"
         }, {
           "columnName" : "name",
           "dataType" : "VARCHAR(100)",
           "abstractionLevelName" : "name"
         }, {
           "columnName" : "born",
           "dataType" : "INTEGER",
           "abstractionLevelName" : "born"
         } ],
         "restrictions" : [ ]
 }
 ```

ImplementationNode JSON attribute defines how to extract nodes of type <strong>"Person"</strong> from a RDBMS. There may be many sources of nodes of type "Person" in the system. The GQL (OpenCypher) to SQL compiler should take care of retrieving all the nodes from the relational system.

In this example, nodes of type "person" are stored in the table also called "person". Cytosm assumes that every node may (but is not mandatory to) have an id. In this scenario, there is an id of type "INTEGER", from the table column called "id". In some special cases, UIDs may come from multiple columns, this is solved by the "concatenationPosition" that concatenates multiple columns together in order to generate an ID. An example would be to concatenate the strings from the column "Name" and "Born", in order to generate a UID. In this case, that section would look like this:

```json
{
"id" : [ {
           "columnName" : "name",
           "datatype" : "VARCHAR(100)",
           "concatenationPosition" : 1
         },
         {
           "columnName" : "born",
           "datatype" : "INTEGER",
           "concatenationPosition" : 2
         }]
}
```

Nodes of type person have three attributes, named in the relational schema as "id", "name" and "born". In the abstraction level, these properties were also called with those names. "Name" is a VARCHAR(100) and the other two attributes are Integers.

On the edge implementation side, there's the following example:

```json
{
         "types" : [ "acted_in" ],
         "paths" : [ {
           "traversalHops" : [ {
             "sourceTableName" : "person",
             "sourceTableColumn" : "id",
             "joinTableSourceColumn" : "person_id",
             "joinTableName" : "acted_in",
             "joinTableDestinationColumn" : "movie_id",
             "destinationTableColumn" : "id",
             "destinationTableName" : "movie",
             "attributes" : [ {
               "columnName" : "role",
               "dataType" : "varchar(100)",
               "abstractionLevelName" : "role"
             } ]
           } ]
         } ]
}
```

Although the edge "acted_in" has only one path, the gTop mapping allows for a multitude of paths during the traversal. This means that the same edge leading one node type to another can be represented by different sequences of table joins - e.g. the rows for the same edge can come from two different tables.

In parallel to that, each path can actually by a long sequence of joins, not only a single JOIN-table. This is the idea behind the <strong>traversalHops</strong> attribute.

Graph traversals, on the relation domain, are represented by Joins. Having that in mind, edges are classified on two categories:

* <strong>Edges without join tables:</strong> Usually happens between nodes of the same type, because this means that the edge information is contained in the same table row as as the node information. An example would be a table table represents a company organogram. Source could be the current row and the destination would be id of the manager, that would be another row in the same table.
* <strong>Edge with one or more join tables:</strong> In order to generalize the property graph edge modeling, an edge may actually represent more than two join operations - e.g. the edge is actually many tables that should be joined together in the process. This is the reason why there is a <strong>TraversalHop</strong>. The compiler should traverse this traversal hops sequentially in order to represent a traversal of a given edge.

In both scenarios, the <strong>SourceTableColumn</strong> and <strong>DestinationTableColumn</strong> are the columns that will be used in the Join operation in order to represent a graph traversal.

Finally, some edges may (but again don't have to) contain attributes. In this example, the edge "acted_in" has the attribute role, that is a VARCHAR(100), stored in the column "role" of the table called "acted_in".
