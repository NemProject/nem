package org.nem.core.math;


import no.uib.cipr.matrix.*;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

/**
 * A Matrix implementation that uses CompRowMatrix from MTJ.
 */
public class TunedMtjSparseMatrix extends Matrix {

	private final CompRowMatrix matrix;

	public TunedMtjSparseMatrix(
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
	protected void forEach(final ElementVisitorFunction func) {
		for (final MatrixEntry entry : this.matrix) {
			func.visit(
					entry.row(),
					entry.column(),
					entry.get(),
					entry::set);
		}
	}

	@Override
	public void normalizeColumns() {
		int[] colIndices = this.matrix.getColumnIndices();
		double[] values = this.matrix.getData();
		double[] colSums = new double[this.matrix.numRows()];
		for (int i = 0; i < colIndices.length; ++i)
			colSums[colIndices[i]] += Math.abs(values[i]);

		for (int i = 0; i < colIndices.length; ++i) {
			double sum = colSums[colIndices[i]];
			if (sum > 0.0)
				values[i] /= sum;
		}
	}

	@Override
	public ColumnVector multiply(final ColumnVector vector) {
		final int numCols = this.getColumnCount();
		if (numCols != vector.size())
			throw new IllegalArgumentException("vector size and matrix column count must be equal");

		final DenseVector multiplier = new DenseVector(vector.getRaw());
		final DenseVector result = new DenseVector(this.matrix.numRows());

		this.matrix.mult(multiplier, result);
		return new ColumnVector(result.getData());
	}
}