MATCH (a:Person)
MATCH (b:Person)
MATCH (a)-[:KNOWS]-(b)
RETURN a.id, b.id