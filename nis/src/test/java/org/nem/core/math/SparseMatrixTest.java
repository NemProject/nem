package org.nem.core.math;

import java.security.SecureRandom;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class SparseMatrixTest {

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
	public void sparseMatrixnormalizeColumnsIsFastEnough() {
		// Arrange:
		int size = 1000000;
		int numEntries = 2000000;
		System.out.print("Setting up normalizeColumns test: " + size + " x " + size + " matrix with " + numEntries + " entries...");
		final SparseMatrix sparseMatrix = new SparseMatrix(size, size, 2*size);
		SecureRandom sr = new SecureRandom();
		byte[] rows = new byte[2*numEntries];
		byte[] cols = new byte[2*numEntries];
		sr.nextBytes(rows);
		sr.nextBytes(cols);
		for (int i=0; i<numEntries; i++) {
			long row = Math.abs((rows[2*i] << 8 + rows[2*i+1]) % size);
			long col = Math.abs((cols[2*i] << 8 + cols[2*i+1]) % size);
			sparseMatrix.setAt(row, col, 3.0);
		}
		System.out.println("done.");

		// Act:
		long start = System.currentTimeMillis();
		sparseMatrix.normalizeColumns();
		long stop = System.currentTimeMillis();
		
		// Assert:
		System.out.println("normalizeColumns needed " + (stop - start) + "ms.");
		Assert.assertTrue((stop - start) < 1000);
	}

	@Test
	public void sparseMatrixMultiplyIsFastEnough() {
		// Arrange:
		int size = 1000000;
		int numEntries = 2000000;
		System.out.print("Setting up multiplication test: " + size + " x " + size + " matrix with " + numEntries + " entries...");
		final SparseMatrix sparseMatrix = new SparseMatrix(size, size, 2*size);
		ColumnVector vector = new ColumnVector(size);
		SecureRandom sr = new SecureRandom();
		byte[] rows = new byte[2*numEntries];
		byte[] cols = new byte[2*numEntries];
		sr.nextBytes(rows);
		sr.nextBytes(cols);
		for (int i=0; i<numEntries; i++) {
			vector.setAt(i % size, cols[i]);
			long row = Math.abs((rows[2*i] << 8 + rows[2*i+1]) % size);
			long col = Math.abs((cols[2*i] << 8 + cols[2*i+1]) % size);
			sparseMatrix.setAt(row, col, 3.0);
		}
		System.out.println("done.");

		// Act:
		long start = System.currentTimeMillis();
		ColumnVector result = sparseMatrix.multiply(vector);
		long stop = System.currentTimeMillis();
		
		// Assert:
		System.out.println("Multiply needed " + (stop - start) + "ms.");
		Assert.assertTrue((stop - start) < 1000);
	}

	//endregion

	private static SparseMatrix createThreeByTwoSparseMatrix(final double[] values) {
		if (6 != values.length)
			throw new IllegalArgumentException("values must have 6 elements");

		// Arrange:
		final SparseMatrix sparseMatrix = new SparseMatrix(3, 2, 100);
		sparseMatrix.setAt(0, 0, values[0]);
		sparseMatrix.setAt(1, 0, values[1]);
		sparseMatrix.setAt(2, 0, values[2]);
		sparseMatrix.setAt(0, 1, values[3]);
		sparseMatrix.setAt(1, 1, values[4]);
		sparseMatrix.setAt(2, 1, values[5]);
		return sparseMatrix;
	}
}
