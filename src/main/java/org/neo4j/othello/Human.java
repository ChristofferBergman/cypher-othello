package org.neo4j.othello;

public class Human implements Player {

	@Override
	public String getCypherForRound() {
		return  """
				MATCH (cell:Cell)
				WHERE cell.row = $y AND cell.column = $x
				RETURN cell
				""";
	}

}
