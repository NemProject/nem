package org.nem.core.math;

import no.uib.cipr.matrix.*;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

import java.util.*;

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
	protected Matrix create(final int numRows, final int numCols) {
		throw new UnsupportedOperationException("this operation is not currently supported");
	}

	@Override
	protected double getAtUnchecked(final int row, final int col) {
		return this.matrix.get(row, col);
	}

	@Override
	protected void setAtUnchecked(final int row, final int col, final double val) {
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
	public Collection<Integer> normalizeColumns() {
		final int[] colIndices = this.matrix.getColumnIndices();
		final List<Integer> zeroColumns = new ArrayList<>();
		final double[] values = this.matrix.getData();
		final double[] colSums = new double[this.matrix.numColumns()];
		for (int i = 0; i < colIndices.length; ++i) {
			colSums[colIndices[i]] += Math.abs(values[i]);
		}
		for (int i = 0; i < this.matrix.numColumns(); i++) {
			if (colSums[i] == 0.0) {
				zeroColumns.add(i);
			}
		}

		for (int i = 0; i < colIndices.length; ++i) {
			final double sum = colSums[colIndices[i]];
			if (sum > 0.0) {
				values[i] /= sum;
			}
		}

		return zeroColumns;
	}

	@Override
	public ColumnVector multiply(final ColumnVector vector) {
		final int numCols = this.getColumnCount();
		if (numCols != vector.size()) {
			throw new IllegalArgumentException("vector size and matrix column count must be equal");
		}

		final DenseVector multiplier = new DenseVector(vector.getRaw());
		final DenseVector result = new DenseVector(this.matrix.numRows());

		this.matrix.mult(multiplier, result);
		return new ColumnVector(result.getData());
	}

	@Override
	public MatrixNonZeroElementRowIterator getNonZeroElementRowIterator(final int row) {
		throw new UnsupportedOperationException("this operation is not currently supported");
	}
}