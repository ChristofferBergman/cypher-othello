package org.neo4j.othello;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class PlayerWrapper {
	private final String name;
	private final Player player;
	private final String symbol;
	private final ArrayList<Long> timestampsNs = new ArrayList<>();
	private boolean firstMove = true;

	public PlayerWrapper(String name, Player player, String symbol) {
		this.name = name;
		this.player = player;
		this.symbol = symbol;
	}

	public String getName() {
		return name;
	}
	
	public String toString() {
		return getName();
	}
	
	public String getColor() {
		return symbol.equals("X") ? "BLACK" : "WHITE";
	}
	
	public String getSymbol() {
		return symbol;
	}

	public ArrayList<Long> getTimestampsNs() {
		return timestampsNs;
	}

	public Collection<Cell> tick(DBConnection db, GameFrame frame, long limit)
			throws IllegalMoveException, TimeLimitException {
		try {
			int x = 0; // Only for human players
			int y = 0; // Only for human players
			if (player instanceof Human) {
				GameFrame.HumanSelection selection = frame.waitForHumanMove();
				x = selection.getX();
				y = selection.getY();
			} else {
				frame.waitForNextTick();
			}
	
			if (frame.isVisible()) {
				long beforeTick = System.nanoTime();
				db.tickPlayer(this, x, y);
				long time = System.nanoTime() - beforeTick;
				timestampsNs.add(time);
				if(!firstMove && limit > 0 && time > TimeUnit.SECONDS.toNanos(limit)) {
					throw new TimeLimitException(this, TimeUnit.NANOSECONDS.toSeconds(time), limit);
				}
				Collection<Cell> state = db.getState();
				return state;
			}
	
			return null;
		}
		finally {
			firstMove = false;
		}
	}

	public String getCypherForRound() {
		return player.getCypherForRound();
	}
}