/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.docgen.refcard

import org.neo4j.cypher.QueryStatisticsTestSupport
import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.internal.compiler.v3_0.executionplan.InternalExecutionResult

class PatternsTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("ROOT KNOWS A", "A:Person:Swedish KNOWS B", "B KNOWS C", "C KNOWS ROOT")
  val title = "Patterns"
  val css = "general c2-2 c3-2 c6-2"
  override val linkId = "introduction-pattern"

  override def assert(name: String, result: InternalExecutionResult) {
    name match {
      case "returns-one" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
      case "related" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
      case "empty" =>
        assert(result.toList.size === 0)
      case "create" =>
        assertStats(result, nodesCreated = 1, relationshipsCreated = 1, propertiesSet = 1)
        assert(result.toList.size === 1)
    }
  }

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=aname" =>
        Map("value" -> "Bob")
      case "parameters=alice" =>
        Map("value" -> "Alice")
      case "" =>
        Map()
    }

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("value" -> 10, "name" -> "Alice"),
    "B" -> Map("value" -> 20),
    "C" -> Map("value" -> 30))

  def text = """
###assertion=related
MATCH

(n)-->(m)

WHERE id(n) = %A% AND id(m) = %B%

RETURN n,m###

A relationship from `n` to `m` exists.

###assertion=related
MATCH

(n:Person)

RETURN n###

Matches nodes with the label `Person`.


###assertion=related
MATCH

(n:Person:Swedish)

RETURN n###

Matches nodes which have both `Person` and `Swedish` labels.

###assertion=related parameters=alice
MATCH

(n:Person {name: {value}})

RETURN n###

Matches nodes with the declared properties.

###assertion=related
MATCH

(n:Person)-->(m)

RETURN n,m###

Node `n` labeled `Person` has a relationship to `m`.

###assertion=related
MATCH

(n)--(m)

WHERE id(n) = %A% AND id(m) = %B%

RETURN n,m###

A relationship in any direction between `n` and `m`.

###assertion=related
MATCH

(m)<-[:KNOWS]-(n)

WHERE id(n) = %A% AND id(m) = %B%

RETURN n,m###

A relationship from `n` to `m` of type `KNOWS` exists.

###assertion=related
MATCH

(n)-[:KNOWS|LOVES]->(m)

WHERE id(n) = %A% AND id(m) = %B%

RETURN n,m###

A relationship from `n` to `m` of type `KNOWS` or `LOVES` exists.

###assertion=related
MATCH

(n)-[r]->(m)

WHERE id(n) = %A% AND id(m) = %B%

RETURN r###

Bind an identifier to the relationship.

###assertion=related
MATCH

(n)-[*1..5]->(m)

WHERE id(n) = %A% AND id(m) = %B%

RETURN n,m###

Variable length paths.

###assertion=related
MATCH

(n)-[*]->(m)

WHERE id(n) = %A% AND id(m) = %B%

RETURN n,m###

Any depth.
See the performance tips.

###assertion=create parameters=aname
MATCH (n)  WHERE id(n) = %A%
CREATE UNIQUE

(n)-[:KNOWS]->(m {property: {value}})

RETURN m###

Match or set properties in `MATCH`, `CREATE`, `CREATE UNIQUE` or `MERGE` clauses.

###assertion=empty
MATCH p =

shortestPath((n1:Person)-[*..6]-(n2:Person))

WHERE n1.name = "Alice"
RETURN p###

Find a single shortest path.

###assertion=empty
MATCH p =

allShortestPaths((n1:Person)-[*..6]->(n2:Person))

WHERE n1.name = "Alice"
RETURN p###

Find all shortest paths.

###assertion=returns-one parameters=alice
MATCH (n:Person {name: {value}})
RETURN

size((n)-->()-->())

AS fof###

Count the number of rows matching the pattern expression.
"""
}
/* confirm this, then add.
###assertion=empty parameters=alice
MATCH

()-[r {name: {value}}]-()

RETURN r###

Matches relationships with the declared properties.

*/
