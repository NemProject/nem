package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.*;

public class EigenTrustConvergencePolicyTest {

	@Test
	public void policyInitiallyHasNoResult() {
		// Act:
		final EigenTrustConvergencePolicy policy = createTestPolicy(10, 0.1, 0.05);
		final ColumnVector result = policy.getResult();

		// Assert:
		Assert.assertThat(result, IsNull.nullValue());
		Assert.assertThat(policy.hasConverged(), IsEqual.equalTo(false));
	}

	@Test
	public void convergeStopsAfterMaxIterations() {
		// Act:
		final EigenTrustConvergencePolicy policy = createTestPolicy(1, 0.0001, 0.05);

		// Act:
		policy.converge();
		final ColumnVector result = policy.getResult();

		// Assert:
		Assert.assertThat(policy.hasConverged(), IsEqual.equalTo(false));
		Assert.assertThat(result.size(), IsEqual.equalTo(2));
		Assert.assertEquals(1.00, result.absSum(), 0.1);
		Assert.assertEquals(1.20, result.getAt(0) / result.getAt(1), 0.1);
	}

	@Test
	public void convergeStopsAfterIterationChangeIsLessThanEpsilon() {
		// Arrange:
		final EigenTrustConvergencePolicy policy = createTestPolicy(1000, 0.0001, 0.05);

		// Act:
		policy.converge();
		final ColumnVector result = policy.getResult();

		// Assert:
		Assert.assertThat(policy.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(result.size(), IsEqual.equalTo(2));
		Assert.assertEquals(1.00, result.absSum(), 0.001);
		Assert.assertEquals(1.369, result.getAt(0) / result.getAt(1), 0.001);
	}

	@Test
	public void convergeDoesNotFavorHigherPreTrustValues() {
		// Arrange:
		final EigenTrustConvergencePolicy policy = createTestPolicyWithOnesMatrix(1000, 0.0001, 0.05);

		// Act:
		policy.converge();
		final ColumnVector result = policy.getResult();

		// Assert:
		Assert.assertThat(policy.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(result.size(), IsEqual.equalTo(2));
		Assert.assertEquals(1.00, result.absSum(), 0.001);
		Assert.assertEquals(0.967, result.getAt(0) / result.getAt(1), 0.001);
	}

	private static EigenTrustConvergencePolicy createTestPolicy(final int maxIterations, final double epsilon, final double alpha) {
		// Arrange: (EigenVector for test matrix is [3, 2])
		final ColumnVector vector = new ColumnVector(2);
		vector.setAt(0, 1.0 / 3);
		vector.setAt(1, 2.0 / 3);

		final Matrix matrix = new DenseMatrix(2, 2);
		matrix.setAt(0, 0, 2.0/3.0);
		matrix.setAt(0, 1, 1.0/2.0);
		matrix.setAt(1, 0, 1.0/3.0);
		matrix.setAt(1, 1, 1.0/2.0);

		return new EigenTrustConvergencePolicy(vector, matrix, maxIterations, epsilon, alpha);
	}

	private static EigenTrustConvergencePolicy createTestPolicyWithOnesMatrix(final int maxIterations, final double epsilon, final double alpha) {
		// Arrange: (EigenVector for test matrix is [1,1])
		final ColumnVector vector = new ColumnVector(2);
		vector.setAt(0, 1.0 / 3);
		vector.setAt(1, 2.0 / 3);

		final Matrix matrix = new DenseMatrix(2, 2);
		matrix.setAt(0, 0, 1.0/2);
		matrix.setAt(0, 1, 1.0/2);
		matrix.setAt(1, 0, 1.0/2);
		matrix.setAt(1, 1, 1.0/2);

		return new EigenTrustConvergencePolicy(vector, matrix, maxIterations, epsilon, alpha);
	}
}
