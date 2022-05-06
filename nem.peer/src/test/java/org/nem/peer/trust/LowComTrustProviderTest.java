package org.nem.peer.trust;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
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
		MatcherAssert.assertThat(vector, IsEqual.equalTo(new ColumnVector(0.2, 0.2, 0.2, 0.2, 0.2)));
	}

	@Test
	public void nonZeroWeightBiasesInFavorOfLowComNodes() {
		// Act:
		final ColumnVector vector = getAdjustedTrustVector(0, 5, 40);

		// Assert: { 1, 3 } should be boosted by 0.4/2
		final ColumnVector expectedVector = new ColumnVector(0.2 / 1.4, 0.4 / 1.4, 0.2 / 1.4, 0.4 / 1.4, 0.2 / 1.4);
		MatcherAssert.assertThat(vector.roundTo(10), IsEqual.equalTo(expectedVector.roundTo(10)));
	}

	@Test
	public void nodeWithLessThanMinComIsLowComNode() {
		// Act:
		final ColumnVector vector = getAdjustedTrustVector(9, 100, 10);

		// Assert: { 1 } should be boosted by 1/10
		final ColumnVector expectedVector = new ColumnVector(0.2 / 1.1, 0.3 / 1.1, 0.2 / 1.1, 0.2 / 1.1, 0.2 / 1.1);
		MatcherAssert.assertThat(vector.roundTo(10), IsEqual.equalTo(expectedVector.roundTo(10)));
	}

	@Test
	public void nodeWithMinComIsNotLowComNode() {
		// Act:
		final ColumnVector vector = getAdjustedTrustVector(10, 100, 10);

		// Assert:
		MatcherAssert.assertThat(vector, IsEqual.equalTo(new ColumnVector(0.2, 0.2, 0.2, 0.2, 0.2)));
	}

	@Test
	public void nodeWithGreaterThanMinComIsNotLowComNode() {
		// Act:
		final ColumnVector vector = getAdjustedTrustVector(11, 100, 10);

		// Assert:
		MatcherAssert.assertThat(vector, IsEqual.equalTo(new ColumnVector(0.2, 0.2, 0.2, 0.2, 0.2)));
	}

	@Test
	public void innerProviderContextIsUsedAndPropagated() {
		// Arrange:
		final TrustContext context = createTestTrustContext(9, 100);
		final TrustContext innerContext = createTestTrustContext(11, 100);

		final ColumnVector vector = new ColumnVector(context.getNodes().length);
		vector.setAll(1);

		final MockTrustProvider innerProvider = new MockTrustProvider(innerContext, vector);
		final TrustProvider provider = new LowComTrustProvider(innerProvider, 10);

		// Act:
		final TrustResult result = provider.computeTrust(context);

		// Assert:
		MatcherAssert.assertThat(vector, IsEqual.equalTo(new ColumnVector(0.2, 0.2, 0.2, 0.2, 0.2)));
		MatcherAssert.assertThat(result.getTrustContext(), IsSame.sameInstance(innerContext));
	}

	private static ColumnVector getAdjustedTrustVector(final int nodeOneCalls, final int nodeThreeCalls, final int weight) {
		// Arrange:
		final TrustContext context = createTestTrustContext(nodeOneCalls, nodeThreeCalls);
		final ColumnVector vector = new ColumnVector(context.getNodes().length);
		vector.setAll(1);
		final TrustProvider provider = new LowComTrustProvider(new MockTrustProvider(context, vector), weight);

		// Act: as a decorator, this provider should not used the passed in context for anything so set it to null
		return provider.computeTrust(null).getTrustValues();
	}

	private static TrustContext createTestTrustContext(final int nodeOneCalls, final int nodeThreeCalls) {
		final TestTrustContext testContext = new TestTrustContext();
		final TrustContext context = testContext.getContext();

		final Node localNode = context.getLocalNode();
		final Node[] nodes = context.getNodes();
		for (final Node node : nodes) {
			context.getNodeExperiences().getNodeExperience(localNode, node).successfulCalls().set(100);
		}

		context.getNodeExperiences().getNodeExperience(localNode, nodes[1]).successfulCalls().set(nodeOneCalls);
		context.getNodeExperiences().getNodeExperience(localNode, nodes[3]).successfulCalls().set(nodeThreeCalls);
		return context;
	}
}
