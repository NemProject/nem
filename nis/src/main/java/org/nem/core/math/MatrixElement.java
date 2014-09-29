package org.nem.core.math;

/**
 * Represents a matrix element consisting of row, a column and a value.
 */
public class MatrixElement {

	private final Integer row;
	private final Integer col;
	private final Double value;

	/**
	 * Creates a matrix entry
	 */
	public MatrixElement(final Integer row, final Integer col, final double value) {
		this.row = row;
		this.col = col;
		this.value = value;
	}

	/**
	 * Gets the row the matrix element.
	 *
	 * @return The row.
	 */
	public Integer getRow() {
		return this.row;
	}

	/**
	 * Gets the column the matrix element.
	 *
	 * @return The column.
	 */
	public Integer getColumn() {
		return this.col;
	}

	/**
	 * Gets the value of the matrix element.
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
