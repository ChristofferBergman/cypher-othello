package org.neo4j.othello;

import java.awt.Point;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

public class Game {
	private static final int HEIGHT = 8;
	private static final int WIDTH = 8;

	public static void main(String[] args) {
		////////////////////////////////////////////////////////
		// Check and parse all command line arguments
		
		if (args.length < 7) {
			System.err.println("Incorrect number of arguments");
			System.err.println("Usage: mvn exec:java -Dexec.args=\"Player_Black_ClassName Player_White_ClassName  DB_URI DB_USER DB_PWD DB_NAME API_KEY [TIME_LIMIT_S [BENCHMARK [FRAME_X FRAME_Y]]]\"");
			System.err.println("");
			return;
		}
		
		// Instantiate players
		String player1Name = args[0];
		String player2Name = args[1];
		
		Player player1 = null;
		Player player2 = null;
		
		try {
			player1 = (Player)Class.forName(Game.class.getPackageName() + "." + player1Name)
					.getConstructor().newInstance();
		}
		catch (Throwable t) {
			System.err.println("Player class name " + player1Name + " not found");
			return;
		}
		try {
			player2 = (Player)Class.forName(Game.class.getPackageName() + "." + player2Name)
					.getConstructor().newInstance();
		}
		catch (Throwable t) {
			System.err.println("Player class name " + player2Name + " not found");
			return;
		}
		
		// Read optional time limit (seconds)
		long timeLimitS = -1;
		if (args.length > 7) {
			try {
				timeLimitS = Long.parseLong(args[7]);
			} catch (Throwable t) {
				System.err.println("Incorrect format of time limit, should be seconds as integer, but was: " + args[7]);
				return;
			}
		}
		
		// Check if players should be benchmarked
		boolean benchmark = false;
		if (args.length > 8) {
			try {
				benchmark = Boolean.parseBoolean(args[8]);
			} catch (Throwable t) {
				System.err.println("Incorrect format of benchmark flag, should be true or false, but was: " + args[7]);
				return;
			}
		}
		
		// Check if a specific screen position is requested for the main game frame
		Point position = null;
		if (args.length > 10) {
			try {
				position = new Point(Integer.parseInt(args[9]), Integer.parseInt(args[10]));
			} catch (Throwable t) {
				System.err.println("Incorrect format of screen position, should be two integers, was: " + args[9] +
						" and " + args[10]);
				return;
			}
		}


		////////////////////////////////////////////////////////
		// Prepare game objects
		
		boolean hasHuman = player1 instanceof Human || player2 instanceof Human;

		PlayerWrapper[] players = new PlayerWrapper[] {
				new PlayerWrapper(
						player1Name,
						player1,
						"X"),
				new PlayerWrapper(
						player2Name,
						player2,
						"O") };

		GameFrame frame = new GameFrame(WIDTH, HEIGHT, hasHuman, position);
		frame.setVisible(true);
		

		////////////////////////////////////////////////////////
		// Main game loop

		try (DBConnection db = new DBConnection(args[2], args[3], args[4], args[5], args[6], WIDTH, HEIGHT)) {
			db.setupGrid();
			frame.update(db.getState());

			int skippedMoved = 0;
			while (frame.isVisible()) {
				for (PlayerWrapper player : players) {
					if (frame.isVisible()) {
						frame.setPlayer(player);
						if(!db.isAnyMovePossible(player)) {
							if (++skippedMoved >= 2) {
								String winningSymbol = db.getWinner();
								PlayerWrapper winner = null;
								for (PlayerWrapper p : players) {
									if (p.getSymbol().equals(winningSymbol)) {
										winner = p;
										break;
									}
								}
								if (winner == null) {
									JOptionPane.showMessageDialog(frame, "There is no winner, it's a draw", "Draw", JOptionPane.INFORMATION_MESSAGE);
									System.out.println(players[1].getName());
								} else {
									JOptionPane.showMessageDialog(frame, winner.getName() + " (" + winner.getColor() + ") won the game", "Winner", JOptionPane.INFORMATION_MESSAGE);
									System.out.println(winner.getName());
								}
								frame.dispose();
								break;
							}
							else {
								continue;
							}
						} else {
							skippedMoved = 0;
						}
						Collection<Cell> state = player.tick(db, frame, timeLimitS);
						if (state != null) {
							frame.update(state);
						}
					}
				}
			}
		} catch (IllegalMoveException e) {
			if (frame.isVisible()) {
				frame.setInvalidMove(e.getInvalidMove());
				JOptionPane.showMessageDialog(frame, e.getMessage(), "Invalid move", JOptionPane.INFORMATION_MESSAGE);
			}
			System.out.println(players[0] == e.getPlayer() ? players[1].getName() : players[0].getName());
		} catch (TimeLimitException e) {
			if (frame.isVisible()) {
				JOptionPane.showMessageDialog(frame, e.getMessage(), "Invalid move", JOptionPane.INFORMATION_MESSAGE);
			}
			System.out.println(players[0] == e.getPlayer() ? players[1].getName() : players[0].getName());
		} catch (Throwable t) {
			t.printStackTrace();
			if (frame.isVisible()) {
				JOptionPane.showMessageDialog(frame, t.getMessage(), "Failure", JOptionPane.ERROR_MESSAGE);
			}
			System.out.println("");
		} finally {
			if (benchmark) {
				debugOutputResult("Completed", players);
			}
			if (frame.isVisible()) {
				frame.dispose();
			}
		}
	}

	private static void debugOutputResult(String result, PlayerWrapper[] players) {
		System.out.println(result);
		System.out.println("Benchmarking");
		for (PlayerWrapper player : players) {
			System.out.println(" - " + player.getName());
			long totalTime = 0;
			for (long timestampNs : player.getTimestampsNs()) {
				long timestampMs = TimeUnit.NANOSECONDS.toMillis(timestampNs);
				totalTime += timestampNs;
				System.out.println("" + timestampMs + " ms");
			}
			System.out.println("Average: " + (TimeUnit.NANOSECONDS.toMillis(totalTime) /
					player.getTimestampsNs().size()) + " ms");
		}
	}
}
