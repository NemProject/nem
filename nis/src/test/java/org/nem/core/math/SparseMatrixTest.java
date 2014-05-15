package org.nem.core.math;

import java.security.SecureRandom;
import java.util.logging.Logger;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class SparseMatrixTest {
	private static final Logger LOGGER = Logger.getLogger(SparseMatrixTest.class.getName());

	//region constructor / getAt / setAt / incrementAt

	@Test
	public void uninitializedSparseMatrixReturnsZeros() {
		// Arrange:
		final SparseMatrix sparseMatrix = new SparseMatrix(2, 3, 100);

		// Assert:
		Assert.assertThat(sparseMatrix.getRowCount(), IsEqual.equalTo(2));
		Assert.assertThat(sparseMatrix.getColumnCount(), IsEqual.equalTo(3));
		Assert.assertThat(sparseMatrix.getAt(0, 0), IsEqual.equalTo(0.0));
		Assert.assertThat(sparseMatrix.getAt(0, 1), IsEqual.equalTo(0.0));
		Assert.assertThat(sparseMatrix.getAt(0, 2), IsEqual.equalTo(0.0));
		Assert.assertThat(sparseMatrix.getAt(1, 0), IsEqual.equalTo(0.0));
		Assert.assertThat(sparseMatrix.getAt(1, 1), IsEqual.equalTo(0.0));
		Assert.assertThat(sparseMatrix.getAt(1, 2), IsEqual.equalTo(0.0));
	}

	@Test
	public void sparseMatrixValuesCanBeSet() {
		// Arrange:
		final SparseMatrix sparseMatrix = new SparseMatrix(3, 2, 100);

		// Act:
		sparseMatrix.setAt(0, 0, 7);
		sparseMatrix.setAt(0, 1, 3);
		sparseMatrix.setAt(1, 0, 5);
		sparseMatrix.setAt(1, 1, 11);
		sparseMatrix.setAt(2, 0, 1);
		sparseMatrix.setAt(2, 1, 9);

		// Assert:
		Assert.assertThat(sparseMatrix.getAt(0, 0), IsEqual.equalTo(7.0));
		Assert.assertThat(sparseMatrix.getAt(0, 1), IsEqual.equalTo(3.0));
		Assert.assertThat(sparseMatrix.getAt(1, 0), IsEqual.equalTo(5.0));
		Assert.assertThat(sparseMatrix.getAt(1, 1), IsEqual.equalTo(11.0));
		Assert.assertThat(sparseMatrix.getAt(2, 0), IsEqual.equalTo(1.0));
		Assert.assertThat(sparseMatrix.getAt(2, 1), IsEqual.equalTo(9.0));
	}

	@Test
	public void sparseMatrixValuesCanBeIncremented() {
		// Arrange:
		final SparseMatrix sparseMatrix = new SparseMatrix(3, 2, 100);

		// Act:
		// Increment values
		sparseMatrix.setAt(0, 0, 7);
		sparseMatrix.setAt(0, 1, 3);
		sparseMatrix.setAt(1, 0, 5);
		sparseMatrix.setAt(1, 1, 12);
		sparseMatrix.setAt(2, 0, 1);
		sparseMatrix.setAt(2, 1, 9);
		
		// Double increment
		sparseMatrix.incrementAt(0, 0, 10);
		sparseMatrix.incrementAt(0, 1, 20);
		sparseMatrix.incrementAt(1, 0, 30);
		sparseMatrix.incrementAt(1, 1, 40);
		sparseMatrix.incrementAt(2, 0, 50);
		sparseMatrix.incrementAt(2, 1, 60);

		// Assert:
		Assert.assertThat(sparseMatrix.getAt(0, 0), IsEqual.equalTo(17.0));
		Assert.assertThat(sparseMatrix.getAt(0, 1), IsEqual.equalTo(23.0));
		Assert.assertThat(sparseMatrix.getAt(1, 0), IsEqual.equalTo(35.0));
		Assert.assertThat(sparseMatrix.getAt(1, 1), IsEqual.equalTo(52.0));
		Assert.assertThat(sparseMatrix.getAt(2, 0), IsEqual.equalTo(51.0));
		Assert.assertThat(sparseMatrix.getAt(2, 1), IsEqual.equalTo(69.0));
	}

	@Test
	public void sparseMatrixGetCannotBeIndexedOutOfBounds() {
		// Assert:
		assertGetOutOfBounds(2, 3, -1, 0);
		assertGetOutOfBounds(2, 3, 0, -1);
		assertGetOutOfBounds(2, 3, 2, 0);
		assertGetOutOfBounds(2, 3, 0, 3);
	}

	@Test
	public void sparseMatrixSetCannotBeIndexedOutOfBounds() {
		// Assert:
		assertSetOutOfBounds(2, 3, -1, 0);
		assertSetOutOfBounds(2, 3, 0, -1);
		assertSetOutOfBounds(2, 3, 2, 0);
		assertSetOutOfBounds(2, 3, 0, 3);
	}

	private static void assertGetOutOfBounds(final int numRows, int numCols, final int row, final int col) {
		try {
			// Arrange:
			final SparseMatrix sparseMatrix = new SparseMatrix(numRows, numCols, 100);

			// Act:
			sparseMatrix.getAt(row, col);

			// Assert:
			Assert.fail("expected exception was not thrown");
		} catch (IllegalArgumentException ex) {
		}
	}

	private static void assertSetOutOfBounds(final int numRows, int numCols, final int row, final int col) {
		try {
			// Arrange:
			final SparseMatrix sparseMatrix = new SparseMatrix(numRows, numCols, 100);

			// Act:
			sparseMatrix.setAt(row, col, 0);

			// Assert:
			Assert.fail("expected exception was not thrown");
		} catch (IllegalArgumentException ex) {
		}
	}
	//endregion
	
	//region removal / reallocation

	@Test
	public void entryCanBeRemoved() {
		// Arrange:
		final SparseMatrix sparseMatrix = createThreeByTwoSparseMatrix(new double[] {
				2, 3, 5, 11, 1, 8
		});

		// Assert:
		Assert.assertThat(sparseMatrix.getNonZeroColumnCount(0), IsEqual.equalTo(2));

		// Act:
		sparseMatrix.setAt(0, 1, 0.0);

		// Assert:
		Assert.assertThat(sparseMatrix.getNonZeroColumnCount(0), IsEqual.equalTo(1));
	}

	@Test
	public void rowCanBeReallocated() {
		// Arrange:
		final SparseMatrix sparseMatrix = new SparseMatrix(3, 2, 1);
		sparseMatrix.setAt(0, 0, 5.0);

		// Assert:
		Assert.assertThat(sparseMatrix.getRowCapacity(0), IsEqual.equalTo(1));

		// Act:
		sparseMatrix.setAt(0, 1, 3.0);

		// Assert:
		Assert.assertThat(sparseMatrix.getRowCapacity(0), IsEqual.equalTo(2));
		Assert.assertThat(sparseMatrix.getAt(0, 0), IsEqual.equalTo(5.0));
		Assert.assertThat(sparseMatrix.getAt(0, 1), IsEqual.equalTo(3.0));
	}

	//endregion
	
	//region normalizeColumns
	@Test
	public void allSparseMatrixColumnsCanBeNormalized() {
		// Arrange:
		final SparseMatrix sparseMatrix = createThreeByTwoSparseMatrix(new double[] {
				2, 3, 5, 11, 1, 8
		});

		// Act:
		sparseMatrix.normalizeColumns();

		// Assert:
		Assert.assertThat(sparseMatrix.getAt(0, 0), IsEqual.equalTo(0.2));
		Assert.assertThat(sparseMatrix.getAt(1, 0), IsEqual.equalTo(0.3));
		Assert.assertThat(sparseMatrix.getAt(2, 0), IsEqual.equalTo(0.5));
		Assert.assertThat(sparseMatrix.getAt(0, 1), IsEqual.equalTo(0.55));
		Assert.assertThat(sparseMatrix.getAt(1, 1), IsEqual.equalTo(0.05));
		Assert.assertThat(sparseMatrix.getAt(2, 1), IsEqual.equalTo(0.4));
	}

	//endregion

	//region getRowSumVector

	@Test
	public void rowSumVectorCanBeCreated() {
		// Arrange:
		final SparseMatrix sparseMatrix = createThreeByTwoSparseMatrix(new double[] {
				2, -3, -5, 11, -1, 8
		});

		// Act:
		ColumnVector rowVector = sparseMatrix.getRowSumVector();
		
		// Assert:
		Assert.assertThat(rowVector.getAt(0), IsEqual.equalTo(13.0));
		Assert.assertThat(rowVector.getAt(1), IsEqual.equalTo(-4.0));
		Assert.assertThat(rowVector.getAt(2), IsEqual.equalTo(3.0));
	}

	//endregion

	//region multiply

	@Test(expected = IllegalArgumentException.class)
	public void sparseMatrixCannotBeMultipliesWithVectorOfDifferentSize() {
		// Arrange:
		final SparseMatrix sparseMatrix = createThreeByTwoSparseMatrix(new double[] {
				2, -3, -5, 11, -1, 8
		});
		ColumnVector vector = new ColumnVector(4);

		// Act:
		sparseMatrix.multiply(vector);
	}

	@Test
	public void sparseMatrixCanBeMultipliesWithVectorOfSameSize() {
		// Arrange:
		final SparseMatrix sparseMatrix = createThreeByTwoSparseMatrix(new double[] {
				2, -3, -5, 11, -1, 8
		});
		ColumnVector vector = new ColumnVector(2);
		vector.setAt(0,2);
		vector.setAt(1,3);

		// Act:
		ColumnVector result = sparseMatrix.multiply(vector);
		
		// Assert:
		Assert.assertThat(result.getAt(0), IsEqual.equalTo(37.0));
		Assert.assertThat(result.getAt(1), IsEqual.equalTo(-9.0));
		Assert.assertThat(result.getAt(2), IsEqual.equalTo(14.0));
	}

	//endregion

	@Test
	public void SparseMatrixVsCompRowMatrixNormalizeColumnsTest() {
		LOGGER.info("SparseMatrixVsCompRowMatrixNormalizeColumnsTest");
		int numRows=1000000;
		int numTries = 5;
		for (int numEntriesPerRow = 1; numEntriesPerRow < 5; numEntriesPerRow++) {
			SecureRandom sr = new SecureRandom();
			byte[] cols = new byte[3*numEntriesPerRow*numRows];
			sr.nextBytes(cols);
			
			// Sparse matrix
			SparseMatrix sparseMatrix = setupSparseMatrix(numRows, numEntriesPerRow, cols);
			long start = System.currentTimeMillis();
			for (int i=0; i<numTries; i++) {
				sparseMatrix.normalizeColumns();
			}
			long stop = System.currentTimeMillis();
			System.out.println("SparseMatrix with " + (numRows*numEntriesPerRow) + " entries, normalizeColumns needed " + (stop - start)/numTries + "ms.");
			
			// Comp row matrix
			CompRowMatrix A = setupCompRowMatrix(numRows, numEntriesPerRow, cols);
			start = System.currentTimeMillis();
			for (int i=0; i<10; i++) {
				normalizeColumns(A);
			}
			stop = System.currentTimeMillis();
			System.out.println("CompRowMatrix with " + (numRows*numEntriesPerRow) + " entries, normalizeColumns needed " + (stop - start)/10 + "ms.");
			System.out.println("");
			
			// Assert
		}
	}

	@Test
	public void SparseMatrixVsCompRowMatrixMultiplicationTest() {
		LOGGER.info("SparseMatrixVsCompRowMatrixMultiplicationTest");
		int numRows=1000000;
		int numTries = 5;
		for (int numEntriesPerRow = 1; numEntriesPerRow < 10; numEntriesPerRow++) {
			SecureRandom sr = new SecureRandom();
			byte[] cols = new byte[3*numEntriesPerRow*numRows];
			sr.nextBytes(cols);
			
			// Sparse matrix
			SparseMatrix sparseMatrix = setupSparseMatrix(numRows, numEntriesPerRow, cols);
			ColumnVector vector = setupColumnVector(numRows, numEntriesPerRow, cols);
			long start = System.currentTimeMillis();
			ColumnVector result=null;
			for (int i=0; i<numTries; i++) {
				result = sparseMatrix.multiply(vector);
			}
			long stop = System.currentTimeMillis();
			System.out.println("SparseMatrix with " + (numRows*numEntriesPerRow) + " entries, multiply needed " + (stop - start)/numTries + "ms.");
			
			// Comp row matrix
			CompRowMatrix A = setupCompRowMatrix(numRows, numEntriesPerRow, cols);
			DenseVector x = setupDenseVector(numRows, numEntriesPerRow, cols);
			DenseVector y = new DenseVector(numRows);
			start = System.currentTimeMillis();
			for (int i=0; i<10; i++) {
				A.mult(x, y);
			}
			stop = System.currentTimeMillis();
			System.out.println("CompRowMatrix with " + (numRows*numEntriesPerRow) + " entries, multiply needed " + (stop - start)/10 + "ms.");
			System.out.println("");
			
			// Assert
			for (int i=0; i<numRows; i++) {
				Assert.assertTrue(result.getAt(i) == y.get(i));
			}
		}
	}
	
	private void normalizeColumns(CompRowMatrix A) {
		int[] colIndices = A.getColumnIndices();
		double[] values = A.getData();
		double[] colSums = new double[A.numRows()];
		for (int i=0; i<colIndices.length; i++) {
			colSums[colIndices[i]] += Math.abs(values[i]);
		}
		for (int i=0; i<colIndices.length; i++) {
			double sum = colSums[colIndices[i]];
			if (sum > 0.0) {
				values[i] /= sum;
			}
		}
	}
	
	private ColumnVector setupColumnVector(int numRows, int numEntriesPerRow, byte[] bytes) {
		ColumnVector x = new ColumnVector(numRows);
		for (int i=0; i<numRows; i++) {
			x.setAt(i, bytes[i]);
		}
		
		return x;
	}
	
	private SparseMatrix setupSparseMatrix(int numRows, int numEntriesPerRow, byte[] cols) {
		final SparseMatrix sparseMatrix = new SparseMatrix(numRows, numRows, numEntriesPerRow);
		for (int i=0; i<numRows; i++) {
			for (int j=0; j<numEntriesPerRow; j++) {
				int col = Math.abs(((cols[3*(i*numEntriesPerRow+j)] << 16) + (cols[3*(i*numEntriesPerRow+j)+1] << 8) + cols[3*(i*numEntriesPerRow+j)+2]) % numRows);
				sparseMatrix.setAt(i, col, 3.0);
			}
		}
		return sparseMatrix;
	}
	
	private DenseVector setupDenseVector(int numRows, int numEntriesPerRow, byte[] bytes) {
		DenseVector x = new DenseVector(numRows);
		for (int i=0; i<numRows; i++) {
			x.set(i, bytes[i]);
		}
		
		return x;
	}
	
	private CompRowMatrix setupCompRowMatrix(int numRows, int numEntriesPerRow, byte[] bytes) {
		int[][] rows = new int[numRows][];
		for (int i=0; i<numRows; i++) {
			rows[i] = new int[numEntriesPerRow];
			for (int j=0; j<numEntriesPerRow; j++) {
				int col = Math.abs(((bytes[3*(i*numEntriesPerRow+j)] << 16) + (bytes[3*(i*numEntriesPerRow+j)+1] << 8) + bytes[3*(i*numEntriesPerRow+j)+2]) % numRows);
				rows[i][j] = col;
			}
		}
		
		// initialize values of the matrix
		CompRowMatrix A = new CompRowMatrix(numRows, numRows, rows);

		for (int i=0; i<numRows; i++) {
			for (int j=0; j<numEntriesPerRow; j++) {
				A.set(i, rows[i][j], 3.0);
			}
		}
		return A;
	}
	
	//endregion

	private static SparseMatrix createThreeByTwoSparseMatrix(final double[] values) {
		if (6 != values.length)
			throw new IllegalArgumentException("values must have 6 elements");

		// Arrange:
		final SparseMatrix sparseMatrix = new SparseMatrix(3, 2, 2);
		sparseMatrix.setAt(0, 0, values[0]);
		sparseMatrix.setAt(1, 0, values[1]);
		sparseMatrix.setAt(2, 0, values[2]);
		sparseMatrix.setAt(0, 1, values[3]);
		sparseMatrix.setAt(1, 1, values[4]);
		sparseMatrix.setAt(2, 1, values[5]);
		return sparseMatrix;
	}
}
