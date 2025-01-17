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
package org.neo4j.internal.cypher.acceptance

import org.neo4j.cypher.{ExecutionEngineFunSuite, NewPlannerTestSupport, QueryStatisticsTestSupport}
import org.neo4j.graphdb.Node

class SetAcceptanceTest extends ExecutionEngineFunSuite with QueryStatisticsTestSupport with NewPlannerTestSupport {

  test("set node property to null will remove existing property") {
    // given
    val node = createNode("property" -> 12)

    // when
    val result = updateWithBothPlanners("MATCH (n) SET n.property = null RETURN count(*)")

    // then
    assertStats(result, propertiesSet = 1)
    node should not(haveProperty("property"))
  }

  test("set relationship property to null will remove existing property") {
    // given
    val relationship = relate(createNode(), createNode(), "property" -> 12)

    // when
    val result = updateWithBothPlanners("MATCH ()-[r]->() SET r.property = null RETURN count(*)")

    // then
    assertStats(result, propertiesSet = 1)
    relationship should not(haveProperty("property"))
  }

  test("set a property") {
    // given
    val a = createNode("name" -> "Andres")

    // when
    val result = updateWithBothPlanners("match (n) where n.name = 'Andres' set n.name = 'Michael'")

    // then
    assertStats(result, propertiesSet = 1)
    a should haveProperty("name").withValue("Michael")
  }

  test("set a property to an expression") {
    // given
    val a = createNode("name" -> "Andres")

    // when
    val result = updateWithBothPlanners("match (n) where n.name = 'Andres' set n.name = n.name + ' was here' return count(*)")

    // then
    assertStats(result, propertiesSet = 1)
    a should haveProperty("name").withValue("Andres was here")
  }

  test("set property for null removes the property") {
    // given
    val n = createNode("name" -> "Michael", "age" -> 35)

    // when
    val result = updateWithBothPlanners("match (n) where n.name = 'Michael' set n.name = null return n.age")

    // then
    assertStats(result, propertiesSet = 1)
    n should not(haveProperty("name"))
  }

  test("mark nodes in path") {
    // given
    val a = createNode()
    val b = createNode()
    val c = createNode()
    relate(a, b)
    relate(b, c)

    // when
    val q = "MATCH p=(a)-->(b)-->(c) WHERE id(a) = 0 AND id(c) = 2 WITH p FOREACH(n in nodes(p) | SET n.marked = true)"

    executeWithRulePlanner(q)

    // then
    a should haveProperty("marked").withValue(true)
    b should haveProperty("marked").withValue(true)
    c should haveProperty("marked").withValue(true)
  }

  test("should be able to add label to node") {
    // given
    createNode()

    // when
    val result = updateWithBothPlanners("match (n) where id(n) = 0 set n:FOO return n")

    // then
    val createdNode = result.columnAs[Node]("n").next()
    createdNode should haveLabels("FOO")
    assertStats(result, labelsAdded = 1)
  }

  test("extract on arrays") {
    // given
    createNode()

    // when
    val result = updateWithBothPlanners( "match (n) where id(n) = 0 set n.x=[1,2,3] return extract (i in n.x | i/2.0) as x")

    // then
    result.toList should equal(List(Map("x" -> List(0.5, 1.0, 1.5))))
  }

  test("concatenate to a collection") {
    // given

    // when
    val result = executeScalarWithAllPlanners[Array[Long]]("create (a {foo:[1,2,3]}) set a.foo = a.foo + [4,5] return a.foo")

    // then
    result.toList should equal(List(1, 2, 3, 4, 5))
  }

  test("concatenate to a collection in reverse") {
    // given

    // when
    val result = executeScalarWithAllPlanners[Array[Long]]("create (a {foo:[3,4,5]}) set a.foo = [1,2] + a.foo return a.foo")

    // then
    result.toList should equal(List(1, 2, 3, 4, 5))
  }

  test("overwrites values when using +=") {
    // given
    val a = createNode("foo"->"A", "bar"->"B")

    // when
    val result = updateWithBothPlanners("MATCH (n {foo:'A'}) SET n += {bar:'C'} RETURN count(*)")

    // then
    a should haveProperty("foo").withValue("A")
    a should haveProperty("bar").withValue("C")
  }

  test("old values are kept when using +=") {
    // given
    val a = createNode("foo"->"A")

    // when
    val result = updateWithBothPlanners("MATCH (n {foo:'A'}) SET n += {bar:'B'} RETURN count(*)")

    // then
    assertStats(result, propertiesSet = 1)
    a should haveProperty("foo").withValue("A")
    a should haveProperty("bar").withValue("B")
  }

  test("explicit null values in map removes old values") {
    // given
    val a = createNode("foo"->"A", "bar"->"B")

    // when
    val result = updateWithBothPlanners("MATCH (n {foo:'A'}) SET n += {foo:null} RETURN count(*)")

    // then
    assertStats(result, propertiesSet = 1)
    a should not(haveProperty("foo"))
    a should haveProperty("bar").withValue("B")
  }

  test("set += works well inside foreach") {
    // given
    val a = createNode("a"->"A")
    val b = createNode("b"->"B")
    val c = createNode("c"->"C")

    // when
    val result = executeWithRulePlanner("MATCH (n) WITH collect(n) as nodes FOREACH(x IN nodes | SET x += {x:'X'})")

    // then
    a should haveProperty("a").withValue("A")
    b should haveProperty("b").withValue("B")
    c should haveProperty("c").withValue("C")
    a should haveProperty("x").withValue("X")
    b should haveProperty("x").withValue("X")
    c should haveProperty("x").withValue("X")
  }

  test("non-existing values in an exact property map are removed with set =") {
    // given
    val a = createNode("foo"->"A", "bar"->"B")

    // when
    val result = updateWithBothPlanners("MATCH (n {foo:'A'}) SET n = {foo:'B', baz:'C'} RETURN count(*)")

    assertStats(result, propertiesSet = 3)
    // then
    a should not(haveProperty("bar"))
    a should haveProperty("foo").withValue("B")
    a should haveProperty("baz").withValue("C")
  }

  test("set = works well inside foreach") {
    // given
    val a = createNode("a"->"A")
    val b = createNode("b"->"B")
    val c = createNode("c"->"C")

    // when
    executeWithRulePlanner("MATCH (n) WITH collect(n) as nodes FOREACH(x IN nodes | SET x = {a:'D', x:'X'})")

    // then
    a should haveProperty("a").withValue("D")
    b should haveProperty("a").withValue("D")
    c should haveProperty("a").withValue("D")
    b should not(haveProperty("b"))
    c should not(haveProperty("c"))
    a should haveProperty("x").withValue("X")
    b should haveProperty("x").withValue("X")
    c should haveProperty("x").withValue("X")
  }
}
