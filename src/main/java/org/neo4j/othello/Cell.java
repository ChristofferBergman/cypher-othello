package org.neo4j.othello;

import java.util.Map;

import org.neo4j.driver.Value;

public class Cell implements Comparable<Cell> {
	private static final String KEY_ROW = "row";
	private static final String KEY_COLUMN = "column";
	private static final String KEY_STATE = "state";

	private final int row;
	private final int column;
	private String state; // "X", "O" or null

	public Cell(Value cell) {
		this.row = cell.get(KEY_ROW).asInt();
		this.column = cell.get(KEY_COLUMN).asInt();
		this.state = cell.get(KEY_STATE).isNull() ? null : cell.get(KEY_STATE).asString();
	}

	public Cell(int row, int column, String state) {
		this.row = row;
		this.column = column;
		this.state = state;
	}

	public int getRow() {
		return row;
	}

	public int getColumn() {
		return column;
	}

	public int getY() {
		return getRow();
	}

	public int getX() {
		return getColumn();
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Override
	public String toString() {
		return state == null ? "" : state;
	}

	public Map<String, Object> asMap() {
		return Map.of(KEY_ROW, getRow(), KEY_COLUMN, getColumn(), KEY_STATE, getState());
	}

	@Override
	public int compareTo(Cell o) {
		if (o.getRow() == getRow()) {
			return getColumn() - o.getColumn();
		}
		return getRow() - o.getRow();
	}
}
