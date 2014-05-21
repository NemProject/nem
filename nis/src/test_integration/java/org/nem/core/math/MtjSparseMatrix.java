package org.nem.core.math;


import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

/**
 * A Matrix implementation that uses CompRowMatrix from MTJ.
 */
public class MtjSparseMatrix extends Matrix {

	private final CompRowMatrix matrix;

	public MtjSparseMatrix(
			final int numRows,
			final int numCols,
			final CompRowMatrix matrix) {
		super(numRows, numCols);
		this.matrix = matrix;
	}

	@Override
	protected Matrix create(int numRows, int numCols) {
		throw new UnsupportedOperationException("this operation is not currently supported");
	}

	@Override
	protected double getAtUnchecked(int row, int col) {
		return this.matrix.get(row, col);
	}

	@Override
	protected void setAtUnchecked(int row, int col, double val) {
		this.matrix.set(row, col, val);
	}

	@Override
	protected void forEach(ElementVisitorFunction func) {
		for (final MatrixEntry entry : this.matrix) {
			func.visit(
					entry.row(),
					entry.column(),
					entry.get(),
					entry::set);
		}
	}
}