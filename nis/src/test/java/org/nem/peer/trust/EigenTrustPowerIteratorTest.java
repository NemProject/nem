package org.nem.peer.trust;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class EigenTrustPowerIteratorTest {

	@Test
	public void iteratorInitiallyHasNoResult() {
		// Act:
		final EigenTrustPowerIterator iterator = createTestIterator(10, 0, 0.1);
		final Vector result = iterator.getResult();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(null));
		Assert.assertThat(iterator.hasConverged(), IsEqual.equalTo(false));
	}

	@Test
	public void iteratorStopsAfterMaxIterations() {
		// Act:
		final EigenTrustPowerIterator iterator = createTestIterator(2, 0, 0.0001);

		// Act:
		iterator.run();
		final Vector result = iterator.getResult();

		// Assert:
		Assert.assertThat(iterator.hasConverged(), IsEqual.equalTo(false));
		Assert.assertThat(result.getSize(), IsEqual.equalTo(2));
		Assert.assertEquals(1.00, result.absSum(), 0.1);
		Assert.assertEquals(3.00, result.getAt(0) / result.getAt(1), 0.1);
	}

	@Test
	public void iteratorStopsAfterIterationChangeLessThanEpsilon() {
		// Arrange:
		final EigenTrustPowerIterator iterator = createTestIterator(1000, 0, 0.0001);

		// Act:
		iterator.run();
		final Vector result = iterator.getResult();

		// Assert:
		Assert.assertThat(iterator.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(result.getSize(), IsEqual.equalTo(2));
		Assert.assertEquals(1.00, result.absSum(), 0.001);
		Assert.assertEquals(3.00, result.getAt(0) / result.getAt(1), 0.001);
	}

	@Test
	public void alphaValueOfOneResultsInPreTrustVector() {
		// Arrange:
		final EigenTrustPowerIterator iterator = createTestIterator(1000, 1, 0.0001);

		// Act:
		iterator.run();
		final Vector result = iterator.getResult();

		// Assert:
		Assert.assertThat(iterator.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(result.getSize(), IsEqual.equalTo(2));
		Assert.assertEquals(1.00, result.absSum(), 0.001);
		Assert.assertEquals(0.50, result.getAt(0) / result.getAt(1), 0.001);
	}

	@Test
	public void alphaValueBetweenZeroAndOneInfluencesResult() {
		// Arrange:
		final EigenTrustPowerIterator iterator = createTestIterator(1000, 0.01, 0.00001);

		// Act:
		iterator.run();
		final Vector result = iterator.getResult();

		// Assert:
		Assert.assertThat(iterator.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(result.getSize(), IsEqual.equalTo(2));
		Assert.assertEquals(1.00, result.absSum(), 0.001);
		Assert.assertEquals(3.0764, result.getAt(0) / result.getAt(1), 0.001);
	}

	private static EigenTrustPowerIterator createTestIterator(int maxIterations, double alpha, double epsilon) {
		// Arrange: (EigenVector for test matrix is [3, 1])
		final Vector vector = new Vector(2);
		vector.setAt(0, 1.0 / 3);
		vector.setAt(1, 2.0 / 3);

		final Matrix matrix = new Matrix(2, 2);
		matrix.setAt(0, 0, 2);
		matrix.setAt(0, 1, -12);
		matrix.setAt(1, 0, 1);
		matrix.setAt(1, 1, -5);

		return new EigenTrustPowerIterator(vector, matrix, maxIterations, alpha, epsilon);
	}
}
