package org.neo4j.othello;

import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Query;
import org.neo4j.driver.SessionConfig;

public class DBConnection implements AutoCloseable {
	private final Driver driver;
	private final String db;
	private final String apiKey;
	private final int width;
	private final int height;

	public DBConnection(String uri, String user, String pwd, String db, String apiKey, int width, int height) {
		driver = GraphDatabase.driver(uri, AuthTokens.basic(user, pwd), Config.defaultConfig());
		
		this.db = db;
		this.apiKey = apiKey;
		this.width = width;
		this.height = height;
	}

	@Override
	public void close() throws Exception {
		driver.close();
	}

	public void deleteAll() {
		var query = new Query(
				"""
				MATCH (n)
				DETACH DELETE n
				""");

		try (var session = driver.session(SessionConfig.forDatabase(db))) {
			session.executeWriteWithoutResult(tx -> tx.run(query).consume());
		}
	}

	public void setupGrid() {
		deleteAll();

		var query = new Query(
				"""
				UNWIND range(0,8-1) AS row
				WITH row
				UNWIND range(0,8-1) AS column
				WITH row, column
				CREATE (cell:Cell {row:row, column:column})
				CALL(cell, row, column) {
				  WHEN (row = 3 AND column = 3) OR (row = 4 AND column = 4) THEN
				    SET cell.state = "X"
				  WHEN (row = 3 AND column = 4) OR (row = 4 AND column = 3) THEN
				    SET cell.state = "O"
				}
				WITH row, column, cell
				UNWIND [[row-1, column-1, 1],[row-1, column, 2],[row-1, column+1, 3],[row, column-1, 4]] AS neighbor
				MATCH (other:Cell {row:neighbor[0], column:neighbor[1]})
				MERGE (other)-[:NEIGHBOR_OF {direction: neighbor[2]}]->(cell)
				""",
				Map.of("width", width, "height", height));

		try (var session = driver.session(SessionConfig.forDatabase(db))) {
			session.executeWriteWithoutResult(tx -> tx.run(query).consume());
		}
	}

	public Collection<Cell> getState() {
		var query = new Query(
				"""
				MATCH (cell:Cell)
				RETURN cell
				""");

		try (var session = driver.session(SessionConfig.forDatabase(db))) {
			var record = session.executeRead(tx -> tx.run(query).list());
			return new TreeSet<>(record.stream().map(r -> new Cell(r.get("cell"))).toList());
		}
	}

	public boolean isAnyMovePossible(PlayerWrapper player) throws IllegalMoveException {
		var query = new Query(
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
				RETURN COUNT(c) > 0 AS valid
				""",
				Map.of("symbol", player.getSymbol()));

		try (var session = driver.session(SessionConfig.forDatabase(db))) {
			var record = session.executeRead(tx -> tx.run(query).single());
			return record.get("valid").asBoolean();
		}
	}

	public String getWinner() throws IllegalMoveException {
		var query = new Query(
				"""
				MATCH (c:Cell)
				WHERE c.state IS NOT NULL
				WITH c.state AS state, count(*) AS cnt
				ORDER BY cnt DESC
				WITH collect({state: state, cnt: cnt}) AS rows
				RETURN
				CASE
				  WHEN size(rows) = 1 OR rows[0].cnt > rows[1].cnt
				    THEN rows[0].state
				  ELSE null
				END AS result
				""");

		try (var session = driver.session(SessionConfig.forDatabase(db))) {
			var record = session.executeRead(tx -> tx.run(query).single());
			if (record.get("result").isNull() || record.get("result").isEmpty()) {
				return null; // This indicates a draw
			}
			return record.get("result").asString();
		}
	}

	public void tickPlayer(PlayerWrapper player, int x, int y) throws IllegalMoveException {
		Cell move = requestMove(player, x, y);
		validateMove(player, move);
		makeMove(player, move);
	}

	private Cell requestMove(PlayerWrapper player, int x, int y) {
		var query = new Query(
				player.getCypherForRound(),
				Map.of("symbol", player.getSymbol(), "apiKey", apiKey, "x", x, "y", y));

		try (var session = driver.session(SessionConfig.forDatabase(db))) {
			var record = session.executeRead(tx -> tx.run(query).single());
			return new Cell(record.get("cell"));
		}
	}

	private void validateMove(PlayerWrapper player, Cell move) throws IllegalMoveException {
		var query = new Query(
				"""
				MATCH (c:Cell)
				WHERE c.row = $y AND c.column = $x
				WITH c,
				CASE $symbol
				  WHEN "X" THEN "O"
				  ELSE "X"
				END AS other
				RETURN CASE
				  WHEN c.state IS NOT NULL THEN false
				  ELSE 
				    EXISTS {
				      p=(c)-[r]-(:Cell {state: other})
				      ((:Cell {state: other})-[{direction: r.direction}]-(:Cell {state: other})){0,5}
				      (:Cell {state: other})-[{direction: r.direction}]-(:Cell {state: $symbol})
				    }
				END AS valid
				""",
				Map.of("symbol", player.getSymbol(), "x", move.getX(), "y", move.getY()));

		try (var session = driver.session(SessionConfig.forDatabase(db))) {
			var record = session.executeRead(tx -> tx.run(query).single());
			if (!record.get("valid").asBoolean()) {
				throw new IllegalMoveException(player, move);
			}
		}
	}

	private void makeMove(PlayerWrapper player, Cell move) {
		var query = new Query(
				"""
				MATCH (c:Cell)
				WHERE c.row = $y AND c.column = $x
				WITH c,
				CASE $symbol
				  WHEN "X" THEN "O"
				  ELSE "X"
				END AS other
				MATCH
				  p=(c)-[r]-(:Cell {state: other})
				  ((:Cell {state: other})-[{direction: r.direction}]-(:Cell {state: other})){0,5}
				  (:Cell {state: other})-[{direction: r.direction}]-(:Cell {state: $symbol})
				FOREACH (n IN nodes(p) | SET n.state = $symbol)
				""",
				Map.of("symbol", player.getSymbol(), "x", move.getX(), "y", move.getY()));

		try (var session = driver.session(SessionConfig.forDatabase(db))) {
			session.executeWriteWithoutResult(tx -> tx.run(query).consume());
		}
	}
}
