package org.nem.nis.pox.poi;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.*;

public class PowerIteratorTest {

	@Test
	public void iteratorInitiallyHasNoResult() {
		// Act:
		final PowerIterator iterator = createTestIterator(10, 0.1);
		final ColumnVector result = iterator.getResult();

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(null));
		MatcherAssert.assertThat(iterator.hasConverged(), IsEqual.equalTo(false));
	}

	@Test
	public void iteratorStopsAfterMaxIterations() {
		// Act:
		final PowerIterator iterator = createTestIterator(3, 0.0001);

		// Act:
		iterator.run();
		final ColumnVector result = iterator.getResult();

		// Assert:
		MatcherAssert.assertThat(iterator.hasConverged(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(result.size(), IsEqual.equalTo(2));
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
		MatcherAssert.assertThat(iterator.hasConverged(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result.size(), IsEqual.equalTo(2));
		Assert.assertEquals(1.00, result.absSum(), 0.001);
		Assert.assertEquals(3.00, result.getAt(0) / result.getAt(1), 0.001);
	}

	private static PowerIterator createTestIterator(final int maxIterations, final double epsilon) {
		// Arrange: (EigenVector for test matrix is [3, 1])
		final ColumnVector vector = new ColumnVector(2);
		vector.setAt(0, 1.0);
		vector.setAt(1, 1.0);

		final Matrix matrix = new DenseMatrix(2, 2);
		matrix.setAt(0, 0, 5);
		matrix.setAt(0, 1, 3);
		matrix.setAt(1, 0, 1);
		matrix.setAt(1, 1, 3);

		return new SimplePowerIterator(vector, matrix, maxIterations, epsilon);
	}

	private static class SimplePowerIterator extends PowerIterator {

		private final Matrix matrix;

		/**
		 * Creates a new poi power iterator.
		 *
		 * @param startVector The start vector.
		 * @param matrix The matrix. This matrix must be a multiple of a left stochastic matrix. (i.e. all the matrix columns must sum to
		 *            the same value).
		 * @param maxIterations The maximum number of iterations.
		 * @param epsilon The convergence epsilon value.
		 */
		public SimplePowerIterator(final ColumnVector startVector, final Matrix matrix, final int maxIterations, final double epsilon) {
			super(startVector, maxIterations, epsilon);
			this.matrix = matrix;
		}

		@Override
		protected ColumnVector stepImpl(final ColumnVector vector) {
			return this.matrix.multiply(vector);
		}
	}
}
