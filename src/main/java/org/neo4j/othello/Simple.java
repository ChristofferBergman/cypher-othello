package org.neo4j.othello;

public class Simple implements Player {

	@Override
	public String getCypherForRound() {
		return
			"""
			WITH
			CASE $symbol
			  WHEN "X" THEN "O"
			  ELSE "X"
			END AS other
			MATCH (c:Cell) WHERE c.state IS NULL
			AND EXISTS {
			  p=(c)-[r]-(:Cell {state: other})
			  ((:Cell {state: other})-[{direction: r.direction}]-(:Cell {state: other})){0,5}
			  (:Cell {state: other})-[{direction: r.direction}]-(:Cell {state: $symbol})
			}
			RETURN c AS cell LIMIT 1
			""";
	}

}
