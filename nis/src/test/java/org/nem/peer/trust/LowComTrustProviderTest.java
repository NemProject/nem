package org.nem.peer.trust;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.node.Node;
import org.nem.peer.test.*;

public class LowComTrustProviderTest {

	@Test
	public void zeroWeightDoesNotBiasLowComNodes() {
		// Act:
		final ColumnVector vector = getAdjustedTrustVector(0, 0, 0);

		// Assert:
		Assert.assertThat(vector, IsEqual.equalTo(new ColumnVector(0.2, 0.2, 0.2, 0.2, 0.2)));
	}

	@Test
	public void nonZeroWeightBiasesInFavorOfLowComNodes() {
		// Act:
		final ColumnVector vector = getAdjustedTrustVector(0, 5, 40);

		// Assert:
		final ColumnVector expectedVector = new ColumnVector(
				0.2 / 1.4,
				0.4 / 1.4, // this node should be boosted by 0.4/2
				0.2 / 1.4,
				0.4 / 1.4, // this node should be boosted by 0.4/2
				0.2 / 1.4);
		Assert.assertThat(vector.roundTo(10), IsEqual.equalTo(expectedVector.roundTo(10)));
	}

	@Test
	public void nodeWithLessThanMinComIsLowComNode() {
		// Act:
		final ColumnVector vector = getAdjustedTrustVector(9, 100, 10);

		// Assert:
		final ColumnVector expectedVector = new ColumnVector(
				0.2 / 1.1,
				0.3 / 1.1, // this node should be boosted by 1/10
				0.2 / 1.1,
				0.2 / 1.1,
				0.2 / 1.1);
		Assert.assertThat(vector.roundTo(10), IsEqual.equalTo(expectedVector.roundTo(10)));
	}

	@Test
	public void nodeWithMinComIsNotLowComNode() {
		// Act:
		final ColumnVector vector = getAdjustedTrustVector(10, 100, 10);

		// Assert:
		Assert.assertThat(vector, IsEqual.equalTo(new ColumnVector(0.2, 0.2, 0.2, 0.2, 0.2)));
	}

	@Test
	public void nodeWithGreaterThanMinComIsNotLowComNode() {
		// Act:
		final ColumnVector vector = getAdjustedTrustVector(11, 100, 10);

		// Assert:
		Assert.assertThat(vector, IsEqual.equalTo(new ColumnVector(0.2, 0.2, 0.2, 0.2, 0.2)));
	}

	private static ColumnVector getAdjustedTrustVector(final int nodeOneCalls, final int nodeThreeCalls, final int weight) {
		// Arrange:
		final TestTrustContext testContext = new TestTrustContext();
		final TrustContext context = testContext.getContext();
		final ColumnVector vector = new ColumnVector(context.getNodes().length);
		vector.setAll(1);

		final Node localNode = context.getLocalNode();
		final Node[] nodes = context.getNodes();
		for (final Node node : nodes) {
			context.getNodeExperiences().getNodeExperience(localNode, node).successfulCalls().set(100);
		}

		context.getNodeExperiences().getNodeExperience(localNode, nodes[1]).successfulCalls().set(nodeOneCalls);
		context.getNodeExperiences().getNodeExperience(localNode, nodes[3]).successfulCalls().set(nodeThreeCalls);

		final TrustProvider provider = new LowComTrustProvider(new MockTrustProvider(vector), weight);

		// Act:
		return provider.computeTrust(context);
	}
}
