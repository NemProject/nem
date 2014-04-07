package org.nem.peer.trust;

import org.hamcrest.core.IsSame;
import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.peer.Node;
import org.nem.peer.test.MockTrustProvider;
import org.nem.peer.test.TestTrustContext;
import org.nem.peer.trust.score.NodeExperience;

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

	@Test(expected = TrustException.class)
	public void selectorThrowsExceptionWhenAllNodesHaveZeroTrustValues() {
		// Assert:
		assertSingleNodeWithNonZeroTrustValuesIsSelected(0);
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
		final NodeSelector selector = new BasicNodeSelector(new MockTrustProvider(vector));

		// Act:
		final NodeExperiencePair pair = selector.selectNode(context);

		// Assert:
		Assert.assertThat(pair.getNode(), IsSame.sameInstance(otherNode));
		Assert.assertThat(pair.getExperience(), IsSame.sameInstance(experience));
	}
}
