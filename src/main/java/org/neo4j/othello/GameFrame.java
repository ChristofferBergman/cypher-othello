package org.neo4j.othello;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class GameFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	private HumanSelection humanSelection = new HumanSelection();
	private final CellComponent[] cellComponents;
	private final JLabel turn = new JLabel("     ", SwingConstants.CENTER);
	private final JTextField delay = new JTextField("1");
	private final JButton tick = new JButton("Tick");
	private final JToggleButton autoTick = new JToggleButton("Auto tick");
	private long delayTime = 0;
	private Cell[] currentCells = null;
	private int width;

	public GameFrame(int width, int height, boolean hasHuman, Point position) {
		super("Five in a row");

		this.width = width;

		setLayout(new BorderLayout(5, 5));

		cellComponents = new CellComponent[width*height];

		JPanel gridPanel = new JPanel(new GridLayout(height, width));
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				cellComponents[y*width + x] = new CellComponent();
				gridPanel.add(cellComponents[y*width + x]);
				cellComponents[y*width + x].setEnabled(hasHuman);
				if (hasHuman) {
					final int xx = x;
					final int yy = y;
					cellComponents[y*width + x].addActionListener(event -> {
						humanSelection.setSelection(xx, yy);
					});
				}
			}
		}

		JPanel controlPanel = new JPanel(new GridLayout(2, 2, 5, 5));
		controlPanel.add(new JLabel("Delay in ms"));
		controlPanel.add(tick);
		controlPanel.add(delay);
		controlPanel.add(autoTick);
		
		add(turn, BorderLayout.NORTH);

		add(gridPanel, BorderLayout.CENTER);

		if (!hasHuman) {
			delayTime = -1;
			add(controlPanel, BorderLayout.SOUTH);
		}

		autoTick.addActionListener(event -> {
			synchronized (GameFrame.this) {
				delay.setEnabled(!autoTick.isSelected());
				if (autoTick.isSelected()) {
					try {
						delayTime = Long.parseLong(delay.getText());
					} catch (Exception e) {
						delayTime = 500L;
						delay.setText(Long.toString(delayTime));
					}
					notifyAll();
				} else {
					delayTime = -1;
				}
			}
		});

		tick.addActionListener(event -> {
			synchronized(GameFrame.this) {
				notifyAll();
			}
		});

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dispose();
				synchronized(GameFrame.this) {
					GameFrame.this.notifyAll();
				}
				synchronized(humanSelection) {
					humanSelection.notifyAll();
				}
			}
		});
		
		if (position == null) {
			pack();
			setLocationRelativeTo(null);
		} else {
			setUndecorated(true);
			pack();
			setLocation(position);
			setAlwaysOnTop(true);
		}
	}
	
	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);

		autoTick.requestFocus();
	}
	
	public void setPlayer(PlayerWrapper player) {
		turn.setText(player.getColor());
	}

	public void update(Collection<Cell> c) {
		currentCells = c.toArray(new Cell[0]);
		for (int i = 0; i < currentCells.length && i < cellComponents.length; i++) {
			cellComponents[i].update(currentCells[i]);
		}
	}

	public Cell[] getCurrentCells() {
		return currentCells;
	}

	public synchronized void waitForNextTick() {
		try {
			if (delayTime == 0) {
				return;
			}
			else if (delayTime < 0) {
				wait();
			} else {
				wait(delayTime);
			}
		}
		catch (InterruptedException e) {
			// Never mind
		}
	}

	public HumanSelection waitForHumanMove() {
		synchronized (humanSelection) {
			try {
				humanSelection.wait();
			}
			catch (InterruptedException e) {
				// Never mind
			}
			return humanSelection;
		}
	}

	public void setInvalidMove(Cell cell) {
		try {
			SwingUtilities.invokeAndWait(() -> {
				cellComponents[cell.getRow()*width + cell.getColumn()].setText("-");
			});
		} catch (Exception e) {
			// Ignore
		}
	}

	private class CellComponent extends JButton {
		private static final long serialVersionUID = 1L;

		public CellComponent() {
			setHorizontalAlignment(CENTER);
			setVerticalAlignment(CENTER);
			setPreferredSize(new Dimension(30, 30));
			setFont(getFont().deriveFont(getFont().getSize2D()*2f));
			//setBorder(BorderFactory.createLineBorder(Color.black, 1));
		}

		public void update(Cell cell) {
			setText(cell.toString());
			if (isEnabled()) {
				setEnabled(cell.toString().isBlank());
			}
		}

		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(Color.green);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(Color.black);
			g.drawRect(0, 0, getWidth(), getHeight());
			if (!getText().isBlank()) {
				if (getText().trim().equalsIgnoreCase("X")) {
					g.setColor(Color.black);
				} else if (getText().trim().equalsIgnoreCase("O")) {
					g.setColor(Color.white);
				} else {
					g.setColor(Color.red);
				}
				g.fillOval(2, 2, getWidth()-4, getHeight()-4);
			}
		}
	}

	public class HumanSelection {
		private int x = 0;
		private int y = 0;

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		private synchronized void setSelection(int x, int y) {
			this.x = x;
			this.y = y;
			notifyAll();
		}
	}
}
