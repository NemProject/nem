package org.nem.core.math;

import java.security.SecureRandom;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class SparseMatrixV2Test {

	//region constructor / getAt / setAt / incrementAt

	@Test
	public void uninitializedSparseMatrixV2ReturnsZeros() {
		// Arrange:
		final SparseMatrixV2 SparseMatrixV2 = new SparseMatrixV2(2, 3, 100);

		// Assert:
		Assert.assertThat(SparseMatrixV2.getRowCount(), IsEqual.equalTo(2));
		Assert.assertThat(SparseMatrixV2.getColumnCount(), IsEqual.equalTo(3));
		Assert.assertThat(SparseMatrixV2.getAt(0, 0), IsEqual.equalTo(0.0));
		Assert.assertThat(SparseMatrixV2.getAt(0, 1), IsEqual.equalTo(0.0));
		Assert.assertThat(SparseMatrixV2.getAt(0, 2), IsEqual.equalTo(0.0));
		Assert.assertThat(SparseMatrixV2.getAt(1, 0), IsEqual.equalTo(0.0));
		Assert.assertThat(SparseMatrixV2.getAt(1, 1), IsEqual.equalTo(0.0));
		Assert.assertThat(SparseMatrixV2.getAt(1, 2), IsEqual.equalTo(0.0));
	}

	@Test
	public void SparseMatrixV2ValuesCanBeSet() {
		// Arrange:
		final SparseMatrixV2 SparseMatrixV2 = new SparseMatrixV2(3, 2, 100);

		// Act:
		SparseMatrixV2.setAt(0, 0, 7);
		SparseMatrixV2.setAt(0, 1, 3);
		SparseMatrixV2.setAt(1, 0, 5);
		SparseMatrixV2.setAt(1, 1, 11);
		SparseMatrixV2.setAt(2, 0, 1);
		SparseMatrixV2.setAt(2, 1, 9);

		// Assert:
		Assert.assertThat(SparseMatrixV2.getAt(0, 0), IsEqual.equalTo(7.0));
		Assert.assertThat(SparseMatrixV2.getAt(0, 1), IsEqual.equalTo(3.0));
		Assert.assertThat(SparseMatrixV2.getAt(1, 0), IsEqual.equalTo(5.0));
		Assert.assertThat(SparseMatrixV2.getAt(1, 1), IsEqual.equalTo(11.0));
		Assert.assertThat(SparseMatrixV2.getAt(2, 0), IsEqual.equalTo(1.0));
		Assert.assertThat(SparseMatrixV2.getAt(2, 1), IsEqual.equalTo(9.0));
	}

	@Test
	public void SparseMatrixV2ValuesCanBeIncremented() {
		// Arrange:
		final SparseMatrixV2 SparseMatrixV2 = new SparseMatrixV2(3, 2, 100);

		// Act:
		// Increment values
		SparseMatrixV2.setAt(0, 0, 7);
		SparseMatrixV2.setAt(0, 1, 3);
		SparseMatrixV2.setAt(1, 0, 5);
		SparseMatrixV2.setAt(1, 1, 12);
		SparseMatrixV2.setAt(2, 0, 1);
		SparseMatrixV2.setAt(2, 1, 9);
		
		// Double increment
		SparseMatrixV2.incrementAt(0, 0, 10);
		SparseMatrixV2.incrementAt(0, 1, 20);
		SparseMatrixV2.incrementAt(1, 0, 30);
		SparseMatrixV2.incrementAt(1, 1, 40);
		SparseMatrixV2.incrementAt(2, 0, 50);
		SparseMatrixV2.incrementAt(2, 1, 60);

		// Assert:
		Assert.assertThat(SparseMatrixV2.getAt(0, 0), IsEqual.equalTo(17.0));
		Assert.assertThat(SparseMatrixV2.getAt(0, 1), IsEqual.equalTo(23.0));
		Assert.assertThat(SparseMatrixV2.getAt(1, 0), IsEqual.equalTo(35.0));
		Assert.assertThat(SparseMatrixV2.getAt(1, 1), IsEqual.equalTo(52.0));
		Assert.assertThat(SparseMatrixV2.getAt(2, 0), IsEqual.equalTo(51.0));
		Assert.assertThat(SparseMatrixV2.getAt(2, 1), IsEqual.equalTo(69.0));
	}

	@Test
	public void SparseMatrixV2GetCannotBeIndexedOutOfBounds() {
		// Assert:
		assertGetOutOfBounds(2, 3, -1, 0);
		assertGetOutOfBounds(2, 3, 0, -1);
		assertGetOutOfBounds(2, 3, 2, 0);
		assertGetOutOfBounds(2, 3, 0, 3);
	}

	@Test
	public void SparseMatrixV2SetCannotBeIndexedOutOfBounds() {
		// Assert:
		assertSetOutOfBounds(2, 3, -1, 0);
		assertSetOutOfBounds(2, 3, 0, -1);
		assertSetOutOfBounds(2, 3, 2, 0);
		assertSetOutOfBounds(2, 3, 0, 3);
	}

	private static void assertGetOutOfBounds(final int numRows, int numCols, final int row, final int col) {
		try {
			// Arrange:
			final SparseMatrixV2 SparseMatrixV2 = new SparseMatrixV2(numRows, numCols, 100);

			// Act:
			SparseMatrixV2.getAt(row, col);

			// Assert:
			Assert.fail("expected exception was not thrown");
		} catch (IllegalArgumentException ex) {
		}
	}

	private static void assertSetOutOfBounds(final int numRows, int numCols, final int row, final int col) {
		try {
			// Arrange:
			final SparseMatrixV2 SparseMatrixV2 = new SparseMatrixV2(numRows, numCols, 100);

			// Act:
			SparseMatrixV2.setAt(row, col, 0);

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
		final SparseMatrixV2 SparseMatrixV2 = createThreeByTwoSparseMatrixV2(new double[] {
				2, 3, 5, 11, 1, 8
		});

		// Act:
		SparseMatrixV2.normalizeColumns();

		// Assert:
		Assert.assertThat(SparseMatrixV2.getAt(0, 0), IsEqual.equalTo(0.2));
		Assert.assertThat(SparseMatrixV2.getAt(1, 0), IsEqual.equalTo(0.3));
		Assert.assertThat(SparseMatrixV2.getAt(2, 0), IsEqual.equalTo(0.5));
		Assert.assertThat(SparseMatrixV2.getAt(0, 1), IsEqual.equalTo(0.55));
		Assert.assertThat(SparseMatrixV2.getAt(1, 1), IsEqual.equalTo(0.05));
		Assert.assertThat(SparseMatrixV2.getAt(2, 1), IsEqual.equalTo(0.4));
	}

	//endregion

	//region getRowSumVector

	@Test
	public void rowSumVectorCanBeCreated() {
		// Arrange:
		final SparseMatrixV2 SparseMatrixV2 = createThreeByTwoSparseMatrixV2(new double[] {
				2, -3, -5, 11, -1, 8
		});

		// Act:
		double[] rowVector = SparseMatrixV2.getRowSumVector();
		
		// Assert:
		Assert.assertThat(rowVector[0], IsEqual.equalTo(13.0));
		Assert.assertThat(rowVector[1], IsEqual.equalTo(-4.0));
		Assert.assertThat(rowVector[2], IsEqual.equalTo(3.0));
	}

	//endregion

	//region multiply

	@Test(expected = IllegalArgumentException.class)
	public void SparseMatrixV2CannotBeMultipliesWithVectorOfDifferentSize() {
		// Arrange:
		final SparseMatrixV2 SparseMatrixV2 = createThreeByTwoSparseMatrixV2(new double[] {
				2, -3, -5, 11, -1, 8
		});
		ColumnVector vector = new ColumnVector(4);

		// Act:
		SparseMatrixV2.multiply(vector);
	}

	@Test
	public void SparseMatrixV2CanBeMultipliesWithVectorOfSameSize() {
		// Arrange:
		final SparseMatrixV2 SparseMatrixV2 = createThreeByTwoSparseMatrixV2(new double[] {
				2, -3, -5, 11, -1, 8
		});
		ColumnVector vector = new ColumnVector(2);
		vector.setAt(0,2);
		vector.setAt(1,3);

		// Act:
//		ColumnVector result = SparseMatrixV2.multiply(vector);
//		
//		// Assert:
//		Assert.assertThat(result.getAt(0), IsEqual.equalTo(37.0));
//		Assert.assertThat(result.getAt(1), IsEqual.equalTo(-9.0));
//		Assert.assertThat(result.getAt(2), IsEqual.equalTo(14.0));
	}

	//endregion

	@Test
	public void SparseMatrixV2normalizeColumnsIsFastEnough() {
		// Arrange:
		int size = 1000000;
		int numEntries = 20000;
		System.out.print("Setting up normalizeColumns test: " + size + " x " + size + " matrix with " + numEntries + " entries...");
		final SparseMatrixV2 SparseMatrixV2 = new SparseMatrixV2(size, size, numEntries);
		SecureRandom sr = new SecureRandom();
		byte[] rows = new byte[2*numEntries];
		byte[] cols = new byte[2*numEntries];
		sr.nextBytes(rows);
		sr.nextBytes(cols);
		for (int i=0; i<numEntries; i++) {
			long row = Math.abs((rows[2*i] << 8 + rows[2*i+1]) % size);
			long col = Math.abs((cols[2*i] << 8 + cols[2*i+1]) % size);
			SparseMatrixV2.setAt(row, col, 3.0);
		}
		System.out.println("done.");

		// Act:
		long start = System.currentTimeMillis();
		SparseMatrixV2.normalizeColumns();
		long stop = System.currentTimeMillis();
		
		// Assert:
		System.out.println("normalizeColumns needed " + (stop - start) + "ms.");
		Assert.assertTrue((stop - start) < 1000);
	}

	@Test
	public void SparseMatrixV2MultiplyIsFastEnough() {
		// Arrange:
		int size = 1000000;
		int numEntries = 500000;
		System.out.print("Setting up multiplication test: " + size + " x " + size + " matrix with " + numEntries + " entries...");
		final SparseMatrixV2 SparseMatrixV2 = new SparseMatrixV2(size, size, numEntries);
		ColumnVector vector = new ColumnVector(size);
		SecureRandom sr = new SecureRandom();
		byte[] rows = new byte[3*numEntries];
		byte[] cols = new byte[3*numEntries];
		sr.nextBytes(rows);
		sr.nextBytes(cols);
		for (int i=0; i<numEntries; i++) {
			if (i % 100000 == 0) {
				System.out.println(i);
			}
			vector.setAt(i % size, cols[i]);
			long row = Math.abs(((rows[3*i] << 16) + (rows[3*i+1] << 8) + rows[3*i+2]) % size);
			long col = Math.abs(((cols[3*i] << 16) + (cols[3*i+1] << 8) + cols[3*i+2]) % size);
			SparseMatrixV2.setAt(row, col, 3.0);
		}
		System.out.println("done.");
		System.out.println(SparseMatrixV2.getEntryCount() + " entries.");

		// Act:
		long start = System.currentTimeMillis();
		double[] result1 = SparseMatrixV2.multiply(vector);
		long stop = System.currentTimeMillis();
		
		// Assert:
		System.out.println("Multiply needed " + (stop - start) + "ms.");
		Assert.assertTrue((stop - start) < 2000);

		// Act:
//		start = System.currentTimeMillis();
//		SparseMatrixV2.convert();
//		stop = System.currentTimeMillis();
//		System.out.println("Convert needed " + (stop - start) + "ms.");
//		start = System.currentTimeMillis();
//		double[] result2 = SparseMatrixV2.multiply(vector);
//		stop = System.currentTimeMillis();
//		
//		// Assert:
//		System.out.println("Multiply needed " + (stop - start) + "ms.");
//		Assert.assertTrue((stop - start) < 1000);
	}

	@Test
	public void mtjTest() {
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
		A.mult(x, y);
		long stop = System.currentTimeMillis();
		
		System.out.println("Multiply needed " + (stop - start) + "ms.");
	}
	//endregion

	private static SparseMatrixV2 createThreeByTwoSparseMatrixV2(final double[] values) {
		if (6 != values.length)
			throw new IllegalArgumentException("values must have 6 elements");

		// Arrange:
		final SparseMatrixV2 SparseMatrixV2 = new SparseMatrixV2(3, 2, 100);
		SparseMatrixV2.setAt(0, 0, values[0]);
		SparseMatrixV2.setAt(1, 0, values[1]);
		SparseMatrixV2.setAt(2, 0, values[2]);
		SparseMatrixV2.setAt(0, 1, values[3]);
		SparseMatrixV2.setAt(1, 1, values[4]);
		SparseMatrixV2.setAt(2, 1, values[5]);
		return SparseMatrixV2;
	}

}
