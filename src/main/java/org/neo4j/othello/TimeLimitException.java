package org.neo4j.othello;

public class TimeLimitException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private final PlayerWrapper player;
	private final long time;

	public TimeLimitException(PlayerWrapper player, long time, long limit) {
		this(player, time, "Player " + player.getName() + " (" + player.getColor() + ") took more than " + time +
				" s, which is above limit of " + limit + " s");
	}

	private TimeLimitException(PlayerWrapper player, long time, String message) {
		super(message);
		
		this.player = player;
		this.time = time;
	}
	
	public PlayerWrapper getPlayer() {
		return player;
	}
	
	public long getActualTime() {
		return time;
	}
}
