package org.nem.core.math;

/**
 * Represents a matrix element consisting of row, a column, and a value.
 */
public class MatrixElement {
	private final int row;
	private final int col;
	private final double value;

	/**
	 * Creates a matrix entry
	 *
	 * @param row The row index.
	 * @param col The column index.
	 * @param value The value.
	 */
	public MatrixElement(final int row, final int col, final double value) {
		this.row = row;
		this.col = col;
		this.value = value;
	}

	/**
	 * Gets the row index.
	 *
	 * @return The row index.
	 */
	public Integer getRow() {
		return this.row;
	}

	/**
	 * Gets the column index.
	 *
	 * @return The column index.
	 */
	public Integer getColumn() {
		return this.col;
	}

	/**
	 * Gets the value.
	 *
	 * @return The value.
	 */
	public Double getValue() {
		return this.value;
	}

	@Override
	public int hashCode() {
		return this.getRow() ^ this.getColumn();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof MatrixElement)) {
			return false;
		}

		final MatrixElement rhs = (MatrixElement)obj;
		return this.getRow().equals(rhs.getRow()) &&
				this.getColumn().equals(rhs.getColumn()) &&
				this.getValue().equals(rhs.getValue());
	}
}
