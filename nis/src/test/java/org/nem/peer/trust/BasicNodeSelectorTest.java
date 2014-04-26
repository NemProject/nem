package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.peer.node.Node;
import org.nem.peer.test.MockTrustProvider;
import org.nem.peer.test.TestTrustContext;
import org.nem.peer.trust.score.*;

public class BasicNodeSelectorTest {

	@Test
	public void selectorSelectsNodesWithNonZeroTrustValuesWhenValuesSumToOne() {
		// Assert:
		assertSingleNodeWithNonZeroTrustValuesIsSelected(1.0);
	}

	@Test
	public void selectorSelectsNodesWithNonZeroTrustValuesWhenValuesDoNotSumToOne() {
		// Assert:
		assertSingleNodeWithNonZeroTrustValuesIsSelected(0.0001);
	}

	@Test
	public void selectorSelectsNullWhenAllNodesHaveZeroTrustValues() {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();
		final ColumnVector vector = new ColumnVector(context.getNodes().length);
		final NodeSelector selector = new BasicNodeSelector(new MockTrustProvider(vector), context);

		// Act:
		final NodeExperiencePair pair = selector.selectNode();

		// Assert:
		Assert.assertThat(pair, IsNull.nullValue());
	}

	@Test
	public void constructorRecalculatesTrustValues() {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();
		final ColumnVector vector = new ColumnVector(context.getNodes().length);
		final MockTrustProvider trustProvider = new MockTrustProvider(vector);

		// Act:
		new BasicNodeSelector(trustProvider, context);

		// Assert:
		Assert.assertThat(trustProvider.getNumTrustComputations(), IsEqual.equalTo(1));
	}

	@Test
	public void selectNodeDoesNotRecalculateTrustValues() {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();
		final ColumnVector vector = new ColumnVector(context.getNodes().length);
		final MockTrustProvider trustProvider = new MockTrustProvider(vector);
		final NodeSelector selector = new BasicNodeSelector(trustProvider, context);

		// Act:
		for (int i = 0; i < 10; ++i)
			selector.selectNode();

		// Assert:
		Assert.assertThat(trustProvider.getNumTrustComputations(), IsEqual.equalTo(1));
	}

	private static void assertSingleNodeWithNonZeroTrustValuesIsSelected(double value) {
		// Arrange:
		final TestTrustContext testContext = new TestTrustContext();
		final TrustContext context = testContext.getContext();

		final ColumnVector vector = new ColumnVector(context.getNodes().length);
		vector.setAt(2, value);

		final Node localNode = context.getLocalNode();
		final Node otherNode = context.getNodes()[2];
		final NodeExperience experience = context.getNodeExperiences().getNodeExperience(localNode, otherNode);
		final NodeSelector selector = new BasicNodeSelector(new MockTrustProvider(vector), context);

		// Act:
		final NodeExperiencePair pair = selector.selectNode();

		// Assert:
		Assert.assertThat(pair.getNode(), IsSame.sameInstance(otherNode));
		Assert.assertThat(pair.getExperience(), IsSame.sameInstance(experience));
	}
}
