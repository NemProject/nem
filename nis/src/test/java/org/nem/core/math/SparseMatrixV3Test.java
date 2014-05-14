package org.nem.core.math;

import java.security.SecureRandom;
import java.util.logging.Logger;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class SparseMatrixV3Test {
	private static final Logger LOGGER = Logger.getLogger(SparseMatrixTest.class.getName());

	//region constructor / getAt / setAt / incrementAt

	@Test
	public void uninitializedsparseMatrixV3ReturnsZeros() {
		// Arrange:
		final SparseMatrixV3 sparseMatrixV3 = new SparseMatrixV3(2, 3, 100);

		// Assert:
		Assert.assertThat(sparseMatrixV3.getRowCount(), IsEqual.equalTo(2));
		Assert.assertThat(sparseMatrixV3.getColumnCount(), IsEqual.equalTo(3));
		Assert.assertThat(sparseMatrixV3.getAt(0, 0), IsEqual.equalTo(0.0));
		Assert.assertThat(sparseMatrixV3.getAt(0, 1), IsEqual.equalTo(0.0));
		Assert.assertThat(sparseMatrixV3.getAt(0, 2), IsEqual.equalTo(0.0));
		Assert.assertThat(sparseMatrixV3.getAt(1, 0), IsEqual.equalTo(0.0));
		Assert.assertThat(sparseMatrixV3.getAt(1, 1), IsEqual.equalTo(0.0));
		Assert.assertThat(sparseMatrixV3.getAt(1, 2), IsEqual.equalTo(0.0));
	}

	@Test
	public void sparseMatrixV3ValuesCanBeSet() {
		// Arrange:
		final SparseMatrixV3 sparseMatrixV3 = new SparseMatrixV3(3, 2, 100);

		// Act:
		sparseMatrixV3.setAt(0, 0, 7);
		sparseMatrixV3.setAt(0, 1, 3);
		sparseMatrixV3.setAt(1, 0, 5);
		sparseMatrixV3.setAt(1, 1, 11);
		sparseMatrixV3.setAt(2, 0, 1);
		sparseMatrixV3.setAt(2, 1, 9);

		// Assert:
		Assert.assertThat(sparseMatrixV3.getAt(0, 0), IsEqual.equalTo(7.0));
		Assert.assertThat(sparseMatrixV3.getAt(0, 1), IsEqual.equalTo(3.0));
		Assert.assertThat(sparseMatrixV3.getAt(1, 0), IsEqual.equalTo(5.0));
		Assert.assertThat(sparseMatrixV3.getAt(1, 1), IsEqual.equalTo(11.0));
		Assert.assertThat(sparseMatrixV3.getAt(2, 0), IsEqual.equalTo(1.0));
		Assert.assertThat(sparseMatrixV3.getAt(2, 1), IsEqual.equalTo(9.0));
	}

	@Test
	public void sparseMatrixV3ValuesCanBeIncremented() {
		// Arrange:
		final SparseMatrixV3 sparseMatrixV3 = new SparseMatrixV3(3, 2, 100);

		// Act:
		// Increment values
		sparseMatrixV3.setAt(0, 0, 7);
		sparseMatrixV3.setAt(0, 1, 3);
		sparseMatrixV3.setAt(1, 0, 5);
		sparseMatrixV3.setAt(1, 1, 12);
		sparseMatrixV3.setAt(2, 0, 1);
		sparseMatrixV3.setAt(2, 1, 9);
		
		// Double increment
		sparseMatrixV3.incrementAt(0, 0, 10);
		sparseMatrixV3.incrementAt(0, 1, 20);
		sparseMatrixV3.incrementAt(1, 0, 30);
		sparseMatrixV3.incrementAt(1, 1, 40);
		sparseMatrixV3.incrementAt(2, 0, 50);
		sparseMatrixV3.incrementAt(2, 1, 60);

		// Assert:
		Assert.assertThat(sparseMatrixV3.getAt(0, 0), IsEqual.equalTo(17.0));
		Assert.assertThat(sparseMatrixV3.getAt(0, 1), IsEqual.equalTo(23.0));
		Assert.assertThat(sparseMatrixV3.getAt(1, 0), IsEqual.equalTo(35.0));
		Assert.assertThat(sparseMatrixV3.getAt(1, 1), IsEqual.equalTo(52.0));
		Assert.assertThat(sparseMatrixV3.getAt(2, 0), IsEqual.equalTo(51.0));
		Assert.assertThat(sparseMatrixV3.getAt(2, 1), IsEqual.equalTo(69.0));
	}

	@Test
	public void sparseMatrixV3GetCannotBeIndexedOutOfBounds() {
		// Assert:
		assertGetOutOfBounds(2, 3, -1, 0);
		assertGetOutOfBounds(2, 3, 0, -1);
		assertGetOutOfBounds(2, 3, 2, 0);
		assertGetOutOfBounds(2, 3, 0, 3);
	}

	@Test
	public void sparseMatrixV3SetCannotBeIndexedOutOfBounds() {
		// Assert:
		assertSetOutOfBounds(2, 3, -1, 0);
		assertSetOutOfBounds(2, 3, 0, -1);
		assertSetOutOfBounds(2, 3, 2, 0);
		assertSetOutOfBounds(2, 3, 0, 3);
	}

	private static void assertGetOutOfBounds(final int numRows, int numCols, final int row, final int col) {
		try {
			// Arrange:
			final SparseMatrixV3 sparseMatrixV3 = new SparseMatrixV3(numRows, numCols, 100);

			// Act:
			sparseMatrixV3.getAt(row, col);

			// Assert:
			Assert.fail("expected exception was not thrown");
		} catch (IllegalArgumentException ex) {
		}
	}

	private static void assertSetOutOfBounds(final int numRows, int numCols, final int row, final int col) {
		try {
			// Arrange:
			final SparseMatrixV3 sparseMatrixV3 = new SparseMatrixV3(numRows, numCols, 100);

			// Act:
			sparseMatrixV3.setAt(row, col, 0);

			// Assert:
			Assert.fail("expected exception was not thrown");
		} catch (IllegalArgumentException ex) {
		}
	}
	//endregion
	
	//region normalizeColumns

	@Test
	public void allsparseMatrixV3ColumnsCanBeNormalized() {
		// Arrange:
		final SparseMatrixV3 sparseMatrixV3 = createThreeByTwoSparseMatrixV3(new double[] {
				2, 3, 5, 11, 1, 8
		});

		// Act:
		sparseMatrixV3.normalizeColumns();

		// Assert:
		Assert.assertThat(sparseMatrixV3.getAt(0, 0), IsEqual.equalTo(0.2));
		Assert.assertThat(sparseMatrixV3.getAt(1, 0), IsEqual.equalTo(0.3));
		Assert.assertThat(sparseMatrixV3.getAt(2, 0), IsEqual.equalTo(0.5));
		Assert.assertThat(sparseMatrixV3.getAt(0, 1), IsEqual.equalTo(0.55));
		Assert.assertThat(sparseMatrixV3.getAt(1, 1), IsEqual.equalTo(0.05));
		Assert.assertThat(sparseMatrixV3.getAt(2, 1), IsEqual.equalTo(0.4));
	}

	//endregion

	//region getRowSumVector

	@Test
	public void rowSumVectorCanBeCreated() {
		// Arrange:
		final SparseMatrixV3 sparseMatrixV3 = createThreeByTwoSparseMatrixV3(new double[] {
				2, -3, -5, 11, -1, 8
		});

		// Act:
		ColumnVector rowVector = sparseMatrixV3.getRowSumVector();
		
		// Assert:
		Assert.assertThat(rowVector.getAt(0), IsEqual.equalTo(13.0));
		Assert.assertThat(rowVector.getAt(1), IsEqual.equalTo(-4.0));
		Assert.assertThat(rowVector.getAt(2), IsEqual.equalTo(3.0));
	}

	//endregion

	//region multiply

	@Test(expected = IllegalArgumentException.class)
	public void sparseMatrixV3CannotBeMultipliesWithVectorOfDifferentSize() {
		// Arrange:
		final SparseMatrixV3 sparseMatrixV3 = createThreeByTwoSparseMatrixV3(new double[] {
				2, -3, -5, 11, -1, 8
		});
		ColumnVector vector = new ColumnVector(4);

		// Act:
		sparseMatrixV3.multiply(vector);
	}

	@Test
	public void sparseMatrixV3CanBeMultipliesWithVectorOfSameSize() {
		// Arrange:
		final SparseMatrixV3 sparseMatrixV3 = createThreeByTwoSparseMatrixV3(new double[] {
				2, -3, -5, 11, -1, 8
		});
		ColumnVector vector = new ColumnVector(2);
		vector.setAt(0,2);
		vector.setAt(1,3);

		// Act:
//		ColumnVector result = sparseMatrixV3.multiply(vector);
//		
//		// Assert:
//		Assert.assertThat(result.getAt(0), IsEqual.equalTo(37.0));
//		Assert.assertThat(result.getAt(1), IsEqual.equalTo(-9.0));
//		Assert.assertThat(result.getAt(2), IsEqual.equalTo(14.0));
	}

	//endregion

	@Test
	public void sparseMatrixV3normalizeColumnsIsFastEnough() {
		LOGGER.info("sparseMatrixV3normalizeColumnsIsFastEnough");

		// Arrange:
		int size = 1000000;
		int numEntries = 5000000;
		System.out.print("Setting up normalizeColumns test: " + size + " x " + size + " matrix with " + numEntries + " entries...");
		long start = System.currentTimeMillis();
		final SparseMatrixV3 sparseMatrixV3 = new SparseMatrixV3(size, size, 2*size);
		SecureRandom sr = new SecureRandom();
		byte[] rows = new byte[2*numEntries];
		byte[] cols = new byte[2*numEntries];
		sr.nextBytes(rows);
		sr.nextBytes(cols);
		for (int i=0; i<numEntries; i++) {
			long row = Math.abs((rows[2*i] << 8 + rows[2*i+1]) % size);
			long col = Math.abs((cols[2*i] << 8 + cols[2*i+1]) % size);
			sparseMatrixV3.setAt(row, col, 3.0);
		}
		long stop = System.currentTimeMillis();
		System.out.println("done.");
		System.out.println("Setup needed " + (stop - start) + "ms.");

		// Act:
		start = System.currentTimeMillis();
		sparseMatrixV3.normalizeColumns();
		stop = System.currentTimeMillis();
		
		// Assert:
		System.out.println("Trove TLongDoubleHashMap: normalizeColumns needed " + (stop - start) + "ms.");
		Assert.assertTrue((stop - start) < 2000);
		for (int i=0; i<numEntries; i++) {
			long row = Math.abs((rows[2*i] << 8 + rows[2*i+1]) % size);
			long col = Math.abs((cols[2*i] << 8 + cols[2*i+1]) % size);
			sparseMatrixV3.setAt(row, col, 3.0);
		}
		start = System.currentTimeMillis();
		sparseMatrixV3.convert();
		stop = System.currentTimeMillis();
		System.out.println("Convert needed " + (stop - start) + "ms.");
		start = System.currentTimeMillis();
		for (int i=0; i<10; i++) {
			sparseMatrixV3.normalizeColumns();
		}
		stop = System.currentTimeMillis();
		
		// Assert:
		System.out.println("Arrays: normalizeColumns needed " + (stop - start)/10 + "ms.");
		Assert.assertTrue((stop - start) < 1000);
		System.out.println("");
	}

	@Test
	public void sparseMatrixV3MultiplyIsFastEnough() {
		LOGGER.info("sparseMatrixV3MultiplyIsFastEnough");

		// Arrange:
		int size = 1000000;
		int numEntries = 5000000;
		System.out.print("Setting up multiplication test: " + size + " x " + size + " matrix with " + numEntries + " entries...");
		long start = System.currentTimeMillis();
		final SparseMatrixV3 sparseMatrixV3 = new SparseMatrixV3(size, size, 2*numEntries);
		ColumnVector vector = new ColumnVector(size);
		SecureRandom sr = new SecureRandom();
		byte[] rows = new byte[3*numEntries];
		byte[] cols = new byte[3*numEntries];
		sr.nextBytes(rows);
		sr.nextBytes(cols);
		for (int i=0; i<numEntries; i++) {
			vector.setAt(i % size, cols[i]);
			long row = Math.abs(((rows[3*i] << 16) + (rows[3*i+1] << 8) + rows[3*i+2]) % size);
			long col = Math.abs(((cols[3*i] << 16) + (cols[3*i+1] << 8) + cols[3*i+2]) % size);
			sparseMatrixV3.setAt(row, col, 3.0);
		}
		long stop = System.currentTimeMillis();
		System.out.println("done.");
		System.out.println(sparseMatrixV3.getEntryCount() + " entries.");
		System.out.println("Setup needed " + (stop - start) + "ms.");

		// Act:
		start = System.currentTimeMillis();
		ColumnVector result1 = sparseMatrixV3.multiply(vector);
		stop = System.currentTimeMillis();
		
		// Assert:
		System.out.println("Trove TLongDoubleHashMap: multiply needed " + (stop - start) + "ms.");
		Assert.assertTrue((stop - start) < 2000);

		// Act:
		start = System.currentTimeMillis();
		sparseMatrixV3.convert();
		stop = System.currentTimeMillis();
		System.out.println("Convert needed " + (stop - start) + "ms.");
		start = System.currentTimeMillis();
		for (int i=0; i<10; i++) {
			ColumnVector result2 = sparseMatrixV3.multiply(vector);
		}
		stop = System.currentTimeMillis();
		
		// Assert:
		System.out.println("Array: multiply needed " + (stop - start)/10 + "ms.");
		Assert.assertTrue((stop - start) < 1000);
		System.out.println("");
	}

	@Test
	public void mtjTest() {
		LOGGER.info("mtjTest");

		// create matrix A
		int numRows=1000000;
		int numEntriesPerRow = 5;

		SecureRandom sr = new SecureRandom();
		byte[] cols = new byte[numEntriesPerRow*numRows];
		sr.nextBytes(cols);

		int[][] rows = new int[numRows][];
		for (int i=0; i<numRows; i++) {
			rows[i] = new int[numEntriesPerRow];
		}
		
		// initialize values of the matrix
		CompRowMatrix A = new CompRowMatrix(numRows, numRows, rows);

		// initialize values of the vector
		DenseVector x = new DenseVector(numRows);
		
		for (int i=0; i<numRows; i++) {
			for (int j=0; j<numEntriesPerRow; j++) {
				A.set(i, rows[i][j], cols[numEntriesPerRow*i+j]);
				x.set(i, cols[i]);
			}
		}

		// create vector y to store result of multiplication
		DenseVector y = new DenseVector(numRows);

		// perform multiplication
		long start = System.currentTimeMillis();
		for (int i=0; i<10; i++) {
			A.mult(x, y);
		}
		long stop = System.currentTimeMillis();
		
		System.out.println("mtj CompRowMatrix with " + (numRows*numEntriesPerRow) + " entries, multiply needed " + (stop - start)/10 + "ms.");
		System.out.println("");
	}
	//endregion

	private static SparseMatrixV3 createThreeByTwoSparseMatrixV3(final double[] values) {
		if (6 != values.length)
			throw new IllegalArgumentException("values must have 6 elements");

		// Arrange:
		final SparseMatrixV3 sparseMatrixV3 = new SparseMatrixV3(3, 2, 100);
		sparseMatrixV3.setAt(0, 0, values[0]);
		sparseMatrixV3.setAt(1, 0, values[1]);
		sparseMatrixV3.setAt(2, 0, values[2]);
		sparseMatrixV3.setAt(0, 1, values[3]);
		sparseMatrixV3.setAt(1, 1, values[4]);
		sparseMatrixV3.setAt(2, 1, values[5]);
		return sparseMatrixV3;
	}
}
