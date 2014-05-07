package org.nem.nis.poi;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.math.Matrix;


public class PowerIteratorTest {

	@Test
	public void iteratorInitiallyHasNoResult() {
		// Act:
		final PowerIterator iterator = createTestIterator(10, 0.1);
		final ColumnVector result = iterator.getResult();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(null));
		Assert.assertThat(iterator.hasConverged(), IsEqual.equalTo(false));
	}

	@Test
	public void iteratorStopsAfterMaxIterations() {
		// Act:
		final PowerIterator iterator = createTestIterator(2, 0.0001);

		// Act:
		iterator.run();
		final ColumnVector result = iterator.getResult();

		// Assert:
		Assert.assertThat(iterator.hasConverged(), IsEqual.equalTo(false));
		Assert.assertThat(result.size(), IsEqual.equalTo(2));
		Assert.assertEquals(1.00, result.absSum(), 0.1);
		Assert.assertEquals(3.00, result.getAt(0) / result.getAt(1), 0.1);
	}

	@Test
	public void iteratorStopsAfterIterationChangeLessThanEpsilon() {
		// Arrange:
		final PowerIterator iterator = createTestIterator(1000, 0.0001);

		// Act:
		iterator.run();
		final ColumnVector result = iterator.getResult();

		// Assert:
		Assert.assertThat(iterator.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(result.size(), IsEqual.equalTo(2));
		Assert.assertEquals(1.00, result.absSum(), 0.001);
		Assert.assertEquals(3.00, result.getAt(0) / result.getAt(1), 0.001);
	}

	private static PowerIterator createTestIterator(int maxIterations, double epsilon) {
		// Arrange: (EigenVector for test matrix is [3, 1])
		final ColumnVector vector = new ColumnVector(2);
		vector.setAt(0, 1.0 / 3);
		vector.setAt(1, 2.0 / 3);

		final Matrix matrix = new Matrix(2, 2);
		matrix.setAt(0, 0, 2);
		matrix.setAt(0, 1, -12);
		matrix.setAt(1, 0, 1);
		matrix.setAt(1, 1, -5);

		return new SimplePowerIterator(vector, matrix, maxIterations, epsilon);
	}

	private static class SimplePowerIterator extends PowerIterator {

		private Matrix matrix;

		/**
		 * Creates a new poi power iterator.
		 *
		 * @param startVector	 The start vector.
		 * @param matrix    	 The matrix.
		 * @param maxIterations  The maximum number of iterations.
		 * @param epsilon        The convergence epsilon value.
		 */
		public SimplePowerIterator(
				final ColumnVector startVector,
				final Matrix matrix,
				int maxIterations,
				double epsilon) {
			super(startVector, maxIterations, epsilon);
			this.matrix = matrix;
		}

		@Override
		protected ColumnVector stepImpl(final ColumnVector vector) {
			final ColumnVector updatedVector = vector.multiply(this.matrix);
			// TODO: this makes sure the vectors are always pointing in the same direction
			// TODO: but i'm not sure why it's needed
			// TODO: if removed, the unit tests fail
			updatedVector.align();
			return updatedVector;
		}
	}
}