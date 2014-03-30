package org.nem.peer.trust;

import org.hamcrest.core.IsSame;
import org.junit.Assert;
import org.junit.Test;
import org.nem.peer.Node;
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

        for (int i = 0; i < context.getNodes().length; ++i)
                testContext.setCallCounts(i, 100, 0);

        final Vector vector = new Vector(context.getNodes().length);
        vector.setAt(2, value);

        final Node localNode = context.getLocalNode();
        final Node otherNode = context.getNodes()[2];
        final NodeExperience experience = context.getNodeExperiences().getNodeExperience(localNode, otherNode);
        final NodeSelector selector = new BasicNodeSelector(new MockTrustProvider(vector));

        // Act:
        final NodeInfo info = selector.selectNode(context);

        // Assert:
        Assert.assertThat(info.getNode(), IsSame.sameInstance(otherNode));
        Assert.assertThat(info.getExperience(), IsSame.sameInstance(experience));
    }


    private static class MockTrustProvider implements TrustProvider {

        private final Vector trustVector;

        public MockTrustProvider(final Vector trustVector) {
            this.trustVector = trustVector;
        }

        @Override
        public Vector computeTrust(TrustContext context) {
            return this.trustVector;
        }
    }
}
