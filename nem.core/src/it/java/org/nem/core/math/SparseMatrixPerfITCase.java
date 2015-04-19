package org.nem.core.math;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class SparseMatrixPerfITCase {
	private static final Logger LOGGER = Logger.getLogger(SparseMatrixPerfITCase.class.getName());

	private static final double NONZERO_ELEMENT_VALUE = 3.0;

	//region timeNormalizeColumns

	@Test
	public void timeNormalizeColumnsFewRepetitions() {
		LOGGER.info("timeNormalizeColumnsFewRepetitions");
		timeNormalizeColumns(1000000, 10, 4);
	}

	private static void timeNormalizeColumns(final int numRows, final int numTries, final int maxEntriesPerRow) {
		timeAction("normalizeColumns", numRows, numTries, maxEntriesPerRow, MatrixTestAdapter::normalizeColumns);
	}

	//endregion

	//region timeMatrixVectorMultiply

	@Test
	public void timeMatrixVectorMultiplyFewRepetitions() {
		LOGGER.info("timeMatrixVectorMultiplyFewRepetitions");
		timeMatrixVectorMultiply(1000000, 10, 4);
	}

	public static void timeMatrixVectorMultiply(final int numRows, final int numTries, final int maxEntriesPerRow) {
		timeAction("multiply", numRows, numTries, maxEntriesPerRow, MatrixTestAdapter::multiply);
	}

	//endregion

	public static void timeAction(
			final String actionName,
			final int numRows,
			final int numTries,
			final int maxEntriesPerRow,
			final Consumer<MatrixTestAdapter> action) {
		final MatrixTestAdapterFactory[] factories = createTestAdapterFactories();
		for (int numEntriesPerRow = 1; numEntriesPerRow <= maxEntriesPerRow; numEntriesPerRow++) {
			final SecureRandom sr = new SecureRandom();
			final byte[] cols = new byte[3 * numEntriesPerRow * numRows];
			sr.nextBytes(cols);

			for (final MatrixTestAdapterFactory factory : factories) {
				final MatrixTestAdapter testAdapter = factory.create(numRows, numEntriesPerRow, cols);

				final long start = System.currentTimeMillis();
				for (int i = 0; i < numTries; ++i) {
					action.accept(testAdapter);
				}

				final long stop = System.currentTimeMillis();

				final String message = String.format(
						"%s with (%d) entries, %s needed %d ms",
						testAdapter.getName(),
						numRows * numEntriesPerRow,
						actionName,
						(stop - start) / numTries);
				LOGGER.info(message);
			}

			LOGGER.info("");
		}
	}

	@FunctionalInterface
	private interface MatrixTestAdapterFactory {
		MatrixTestAdapter create(int numRows, int numEntriesPerRow, byte[] bytes);
	}

	//region MatrixTestAdapter

	private static abstract class MatrixTestAdapter {

		private final String name;
		private final ColumnVector columnVector;
		private final DenseVector denseVector;

		public MatrixTestAdapter(final String name, final int numRows, final byte[] bytes) {
			this.name = name;

			this.columnVector = new ColumnVector(numRows);
			this.denseVector = new DenseVector(numRows);
			for (int i = 0; i < numRows; ++i) {
				this.columnVector.setAt(i, bytes[i]);
				this.denseVector.set(i, bytes[i]);
			}
		}

		public String getName() {
			return this.name;
		}

		public abstract void normalizeColumns();

		public abstract void multiply();

		protected ColumnVector getColumnVector() {
			return this.columnVector;
		}

		protected DenseVector getDenseVector() {
			return this.denseVector;
		}
	}

	//endregion

	//region NemMatrixTestAdapter

	public static class NemMatrixTestAdapter extends MatrixTestAdapter {

		private final Matrix matrix;

		public NemMatrixTestAdapter(final Matrix matrix, final int numEntriesPerRow, final byte[] bytes, final String name) {
			super(name, matrix.getRowCount(), bytes);
			this.matrix = matrix;

			final int numRows = matrix.getRowCount();
			for (int i = 0; i < numRows; ++i) {
				for (int j = 0; j < numEntriesPerRow; ++j) {
					final int col = getCol(i, j, numRows, numEntriesPerRow, bytes);
					matrix.setAt(i, col, NONZERO_ELEMENT_VALUE);
				}
			}
		}

		@Override
		public void normalizeColumns() {
			this.matrix.normalizeColumns();
		}

		@Override
		public void multiply() {
			this.matrix.multiply(this.getColumnVector());
		}
	}

	//endregion

	//region MtjMatrixTestAdapter

	public static class MtjMatrixTestAdapter extends MatrixTestAdapter {

		private final CompRowMatrix matrix;

		public MtjMatrixTestAdapter(final int numRows, final int numEntriesPerRow, final byte[] bytes, final String name) {
			super(name, numRows, bytes);

			this.matrix = createMtjMatrix(numRows, numEntriesPerRow, bytes);
		}

		@Override
		public void normalizeColumns() {
			final int[] colIndices = this.matrix.getColumnIndices();
			final double[] values = this.matrix.getData();
			final double[] colSums = new double[this.matrix.numRows()];
			for (int i = 0; i < colIndices.length; ++i) {
				colSums[colIndices[i]] += Math.abs(values[i]);
			}

			for (int i = 0; i < colIndices.length; ++i) {
				final double sum = colSums[colIndices[i]];
				if (sum > 0.0) {
					values[i] /= sum;
				}
			}
		}

		@Override
		public void multiply() {
			final DenseVector result = new DenseVector(this.matrix.numRows());
			this.matrix.mult(this.getDenseVector(), result);
		}
	}

	//endregion

	private static MatrixTestAdapterFactory[] createTestAdapterFactories() {
		return new MatrixTestAdapterFactory[] {
				(rows, entriesPerRow, bytes) -> {
					final Matrix matrix = new TunedSparseMatrix(rows, rows, entriesPerRow);
					return new NemMatrixTestAdapter(matrix, entriesPerRow, bytes, "TunedSparseMatrix");
				},
				(rows, entriesPerRow, bytes) -> {
					final Matrix matrix = new SparseMatrix(rows, rows, entriesPerRow);
					return new NemMatrixTestAdapter(matrix, entriesPerRow, bytes, "SparseMatrix");
				},
				//				(rows, entriesPerRow, bytes) -> {
				//					final CompRowMatrix mtjMatrix = createMtjMatrix(rows, entriesPerRow, bytes);
				//					final Matrix matrix = new MtjSparseMatrix(rows, rows, mtjMatrix);
				//					return new NemMatrixTestAdapter(matrix, entriesPerRow, bytes, "MtjSparseMatrix");
				//				},
				(rows, entriesPerRow, bytes) -> {
					final CompRowMatrix mtjMatrix = createMtjMatrix(rows, entriesPerRow, bytes);
					final Matrix matrix = new TunedMtjSparseMatrix(rows, rows, mtjMatrix);
					return new NemMatrixTestAdapter(matrix, entriesPerRow, bytes, "TunedMtjSparseMatrix");
				},
				(rows, entriesPerRow, bytes) -> new MtjMatrixTestAdapter(rows, entriesPerRow, bytes, "DirectMtjMatrix")
		};
	}

	private static int getCol(final int i, final int j, final int numRows, final int numEntriesPerRow, final byte[] bytes) {
		final int index = 3 * (i * numEntriesPerRow + j);
		final int sum = (bytes[index] << 16) + (bytes[index + 1] << 8) + bytes[index + 2];
		return Math.abs(sum % numRows);
	}

	private static int[][] createMtjMatrixRows(final int numRows, final int numEntriesPerRow, final byte[] bytes) {
		final int[][] rows = new int[numRows][];
		for (int i = 0; i < numRows; ++i) {
			rows[i] = new int[numEntriesPerRow];
			for (int j = 0; j < numEntriesPerRow; ++j) {
				final int col = getCol(i, j, numRows, numEntriesPerRow, bytes);
				rows[i][j] = col;
			}
		}

		return rows;
	}

	private static CompRowMatrix createMtjMatrix(final int numRows, final int numEntriesPerRow, final byte[] bytes) {
		final int[][] rows = createMtjMatrixRows(numRows, numEntriesPerRow, bytes);
		final CompRowMatrix matrix = new CompRowMatrix(numRows, numRows, rows);

		for (int i = 0; i < numRows; ++i) {
			for (int j = 0; j < numEntriesPerRow; ++j) {
				matrix.set(i, rows[i][j], NONZERO_ELEMENT_VALUE);
			}
		}

		return matrix;
	}
}
