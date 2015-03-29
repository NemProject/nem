package org.nem.core.math;

/**
 * Interface for iterating through the nonzero elements of a matrix row.
 */
public interface MatrixNonZeroElementRowIterator {

	/**
	 * Gets a value indicating whether or not the matrix row has more non-zero elements.
	 *
	 * @return true if the matrix row has more non-zero elements, false otherwise.
	 */
	boolean hasNext();

	/**
	 * Gets the next non-zero matrix row element.
	 *
	 * @return The next non-zero matrix element of the row.
	 */
	MatrixElement next();
}
