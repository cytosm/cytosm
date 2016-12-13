profile MATCH (person:Person {id:2199023259437})-[:KNOWS]->(:Person)-[:KNOWS]->(friend:Person)-[:IS_LOCATED_IN]->(city:City)
WHERE ((toInt(substring(friend.birthday,5,2)) = 6 AND toInt(substring(friend.birthday,8,2)) >= 21)
OR (toInt(substring(friend.birthday,5,2)) = (6+1)%12 AND toInt(substring(friend.birthday,8,2)) < 22))
AND not(friend=person)
AND not((friend)-[:KNOWS]-(person))
WITH DISTINCT friend, city, person
OPTIONAL MATCH (friend:Person)<-[:HAS_CREATOR]-(post:Post)
WITH friend, city, collect(post) AS posts, person
WITH
friend,
city,
length(posts) AS postCount,
length([p IN posts WHERE (p)-[:HAS_TAG]->(:Tag)<-[:HAS_INTEREST]-(person)]) AS commonPostCount
RETURN
friend.id AS personId,
friend.firstName AS personFirstName,
friend.lastName AS personLastName,
friend.gender AS personGender,
city.name AS personCityName,
commonPostCount - (postCount - commonPostCount) AS commonInterestScore
ORDER BY commonInterestScore DESC, personId ASC
LIMIT 100
;
