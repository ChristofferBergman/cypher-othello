package org.neo4j.othello;

public interface Player {
	/**
	 * Return a Cypher string that will make one move. The parameters passed
	 * to the Cypher query are:
	 * 
	 * $symbol: The symbol used by this player (either "X" for Black or "O" for White) [String]
	 * $apiKey: An API key for OpenAI that can be used if one would want [String]
	 * 
	 * The graph consists of a grid of cells (nodes with the label :Cell) where
	 * each cell is connected to all its neighbors (horizontally, vertically and
	 * diagonally) with relationships called [:NEIGHBOR_OF]. Those relationships
	 * have a property called direction which is an integer 1-4 for diagonal down,
	 * vertical, diagonal up and horizontal.
	 * 
	 * Each :Cell node has the following properties:
	 * row: The row of the cell (0-indexed) [int]
	 * column: The column of the cell (0-indexed) [int]
	 * state: Initially null for all cells, but will be set to $symbol when claimed by a player,
	 *        or when reversed because of a players move [String]
	 *        
	 * The query should return the cell node to claim as one record with one value called cell.
	 * 
	 * If the move breaks any rule (see Othello/Reversi rules) the player immediately loses.
	 * 
	 * The query will run as a Read only query.
	 * 
	 * The database used will be an Aura instance with all procedures and functions the you would
	 * normally find on Aura enabled.
	 * 
	 * @return A valid Cypher statement to perform one move
	 */
	String getCypherForRound();
}
