package org.nem.core.math;

import java.security.SecureRandom;
import java.util.logging.Logger;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class SparseMatrixV2Test {
	private static final Logger LOGGER = Logger.getLogger(SparseMatrixV2Test.class.getName());

	//region constructor / getAt / setAt / incrementAt

	@Test
	public void uninitializedSparseMatrixV2ReturnsZeros() {
		// Arrange:
		final SparseMatrixV2 sparseMatrix = new SparseMatrixV2(2, 3, 100);

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
		final SparseMatrixV2 sparseMatrix = new SparseMatrixV2(3, 2, 100);

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
		final SparseMatrixV2 sparseMatrix = new SparseMatrixV2(3, 2, 100);

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
			final SparseMatrixV2 sparseMatrix = new SparseMatrixV2(numRows, numCols, 100);

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
			final SparseMatrixV2 sparseMatrix = new SparseMatrixV2(numRows, numCols, 100);

			// Act:
			sparseMatrix.setAt(row, col, 0);

			// Assert:
			Assert.fail("expected exception was not thrown");
		} catch (IllegalArgumentException ex) {
		}
	}
	//endregion
	
	//region normalizeColumns

	@Test
	public void allSparseMatrixV2ColumnsCanBeNormalized() {
		// Arrange:
		final SparseMatrixV2 sparseMatrix = createThreeByTwoSparseMatrixV2(new double[] {
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
		final SparseMatrixV2 sparseMatrix = createThreeByTwoSparseMatrixV2(new double[] {
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
		final SparseMatrixV2 sparseMatrix = createThreeByTwoSparseMatrixV2(new double[] {
				2, -3, -5, 11, -1, 8
		});
		ColumnVector vector = new ColumnVector(4);

		// Act:
		sparseMatrix.multiply(vector);
	}

	@Test
	public void sparseMatrixCanBeMultipliesWithVectorOfSameSize() {
		// Arrange:
		final SparseMatrixV2 sparseMatrix = createThreeByTwoSparseMatrixV2(new double[] {
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
	public void sparseMatrixNormalizeColumnsIsFastEnough() {
		LOGGER.info("sparseMatrixnormalizeColumnsIsFastEnough");

		// Arrange:
		int numRows = 1000000;
		int numEntriesPerRow = 5;
		System.out.print("Setting up normalizeColumns test: " + numRows + " x " + numRows + " matrix with " + (numRows*numEntriesPerRow) + " entries...");
		SecureRandom sr = new SecureRandom();
		byte[] cols = new byte[3*numEntriesPerRow*numRows];
		sr.nextBytes(cols);
		long start = System.currentTimeMillis();
		final SparseMatrixV2 sparseMatrix = new SparseMatrixV2(numRows, numRows, numEntriesPerRow);
		for (int i=0; i<numRows; i++) {
			for (int j=0; j<numEntriesPerRow; j++) {
				int col = Math.abs(((cols[3*(i*numEntriesPerRow+j)] << 16) + (cols[3*(i*numEntriesPerRow+j)+1] << 8) + cols[3*(i*numEntriesPerRow+j)+2]) % numRows);
				sparseMatrix.setAt(i, col, 3.0);
			}
		}
		long stop = System.currentTimeMillis();
		System.out.println("done.");
		System.out.println("Setup needed " + (stop - start) + "ms.");

		// Act:
		start = System.currentTimeMillis();
		for (int i=0; i<10; i++) {
			sparseMatrix.normalizeColumns();
		}
		stop = System.currentTimeMillis();
		
		// Assert:
		System.out.println("normalizeColumns needed " + (stop - start)/10 + "ms.");
		Assert.assertTrue((stop - start)/10 < 1000);
		System.out.println("");
	}


	@Test
	public void sparseMatrixV2VsCompRowMatrixMultiplicationTest() {
		LOGGER.info("sparseMatrixV2VsCompRowMatrixMultiplicationTest");
		int numRows=1000000;
		for (int numEntriesPerRow = 1; numEntriesPerRow < 20; numEntriesPerRow++) {
			SecureRandom sr = new SecureRandom();
			byte[] cols = new byte[3*numEntriesPerRow*numRows];
			sr.nextBytes(cols);
			
			// Sparse matrix
			SparseMatrixV2 sparseMatrix = setupSparseMatrixV2(numRows, numEntriesPerRow, cols);
			ColumnVector vector = setupColumnVector(numRows, numEntriesPerRow, cols);
			long start = System.currentTimeMillis();
			ColumnVector result=null;
			for (int i=0; i<10; i++) {
				result = sparseMatrix.multiply(vector);
			}
			long stop = System.currentTimeMillis();
			System.out.println("SparseMatrixV2 with " + (numRows*numEntriesPerRow) + " entries, multiply needed " + (stop - start)/10 + "ms.");
			
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
	
	private ColumnVector setupColumnVector(int numRows, int numEntriesPerRow, byte[] bytes) {
		ColumnVector x = new ColumnVector(numRows);
		for (int i=0; i<numRows; i++) {
			x.setAt(i, bytes[i]);
		}
		
		return x;
	}
	
	private SparseMatrixV2 setupSparseMatrixV2(int numRows, int numEntriesPerRow, byte[] cols) {
		final SparseMatrixV2 sparseMatrix = new SparseMatrixV2(numRows, numRows, numEntriesPerRow);
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

	private static SparseMatrixV2 createThreeByTwoSparseMatrixV2(final double[] values) {
		if (6 != values.length)
			throw new IllegalArgumentException("values must have 6 elements");

		// Arrange:
		final SparseMatrixV2 sparseMatrix = new SparseMatrixV2(3, 2, 100);
		sparseMatrix.setAt(0, 0, values[0]);
		sparseMatrix.setAt(1, 0, values[1]);
		sparseMatrix.setAt(2, 0, values[2]);
		sparseMatrix.setAt(0, 1, values[3]);
		sparseMatrix.setAt(1, 1, values[4]);
		sparseMatrix.setAt(2, 1, values[5]);
		return sparseMatrix;
	}
}
