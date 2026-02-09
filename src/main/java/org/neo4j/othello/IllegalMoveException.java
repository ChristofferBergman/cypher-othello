package org.neo4j.othello;

public class IllegalMoveException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private final PlayerWrapper player;
	private final Cell move;

	public IllegalMoveException(PlayerWrapper player, Cell move) {
		this(player, move, "Player " + player.getName() + " (" + player.getColor() + ") performed an invalid move.");
	}

	private IllegalMoveException(PlayerWrapper player, Cell move, String message) {
		super(message);
		
		this.player = player;
		this.move = move;
	}
	
	public PlayerWrapper getPlayer() {
		return player;
	}
	
	public Cell getInvalidMove() {
		return move;
	}
}
