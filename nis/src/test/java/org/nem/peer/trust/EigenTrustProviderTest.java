package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.peer.Node;
import org.nem.peer.trust.score.*;

import java.util.*;

public class EigenTrustProviderTest {

    //region updateLocalTrust

    @Test
    public void localTrustIsSetToCorrectDefaultsWhenNoCallsAreMadeBetweenNodes() {
        // Arrange:
        final TestTrustContext testContext = new TestTrustContext();
        final TrustContext context = testContext.getContext();
        final EigenTrustProvider provider = new EigenTrustProvider();
        final TrustScores trustScores = provider.getTrustScores();
        final Node localNode = context.getLocalNode();

        // Act:
        provider.updateLocalTrust(localNode, context);
        final Vector vector = trustScores.getScoreVector(localNode, context.getNodes());
        final RealDouble sum = trustScores.getScoreWeight(localNode);

        // Assert:
        Assert.assertThat(sum.get(), IsEqual.equalTo(3.0));
        Assert.assertThat(vector.getSize(), IsEqual.equalTo(5));
        Assert.assertThat(vector.getAt(0), IsEqual.equalTo(1.0 / 3)); // pre-trusted
        Assert.assertThat(vector.getAt(1), IsEqual.equalTo(0.0));
        Assert.assertThat(vector.getAt(2), IsEqual.equalTo(0.0));
        Assert.assertThat(vector.getAt(3), IsEqual.equalTo(1.0 / 3)); // pre-trusted
        Assert.assertThat(vector.getAt(4), IsEqual.equalTo(1.0 / 3)); // self
    }

    @Test
    public void localTrustIsSetToCorrectValuesWhenCallsAreMadeBetweenNodes() {
        // Arrange:
        final TestTrustContext testContext = new TestTrustContext();
        final TrustContext context = testContext.getContext();
        final EigenTrustProvider provider = new MockEigenTrustProvider();
        final TrustScores trustScores = provider.getTrustScores();
        final Node localNode = context.getLocalNode();

        testContext.setCallCounts(0, 1, 2); // 2
        testContext.setCallCounts(1, 2, 2); // 4
        testContext.setCallCounts(2, 3, 3); // 9
        testContext.setCallCounts(3, 4, 4); // 16

        // Act:
        provider.updateLocalTrust(localNode, context);
        final Vector vector = trustScores.getScoreVector(localNode, context.getNodes());
        final RealDouble sum = trustScores.getScoreWeight(localNode);

        // Assert:
        Assert.assertThat(sum.get(), IsEqual.equalTo(32.0));
        Assert.assertThat(vector.getSize(), IsEqual.equalTo(5));
        Assert.assertThat(vector.getAt(0), IsEqual.equalTo(2.0 / 32)); // pre-trusted
        Assert.assertThat(vector.getAt(1), IsEqual.equalTo(4.0 / 32));
        Assert.assertThat(vector.getAt(2), IsEqual.equalTo(9.0 / 32));
        Assert.assertThat(vector.getAt(3), IsEqual.equalTo(16.0 / 32)); // pre-trusted
        Assert.assertThat(vector.getAt(4), IsEqual.equalTo(1.0 / 32)); // self
    }

    //endregion

    //region getTrustMatrix

    @Test
    public void localTrustMatrixIsSetToCorrectValues() {
        // Arrange:
        final TestTrustContext testContext = new TestTrustContext();
        final TrustContext context = testContext.getContext();
        final EigenTrustProvider provider = new MockEigenTrustProvider();
        final Node localNode = context.getLocalNode();

        testContext.setCallCounts(0, 1, 2); // 2
        testContext.setCallCounts(1, 2, 2); // 4
        testContext.setCallCounts(2, 3, 3); // 9
        testContext.setCallCounts(3, 4, 4); // 16

        // Act:
        provider.updateLocalTrust(localNode, context);
        final Matrix matrix = provider.getTrustMatrix(context.getNodes());

        // Assert:
        Assert.assertThat(matrix.sum(), IsEqual.equalTo(1.0));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(5));
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(5));
        Assert.assertThat(matrix.getAt(0, 4), IsEqual.equalTo(2.0 / 32)); // pre-trusted
        Assert.assertThat(matrix.getAt(1, 4), IsEqual.equalTo(4.0 / 32));
        Assert.assertThat(matrix.getAt(2, 4), IsEqual.equalTo(9.0 / 32));
        Assert.assertThat(matrix.getAt(3, 4), IsEqual.equalTo(16.0 / 32)); // pre-trusted
        Assert.assertThat(matrix.getAt(4, 4), IsEqual.equalTo(1.0 / 32)); // self
    }

    //endregion

    private static class TestTrustContext {
        private final Node localNode;
        private final Node[] nodes;
        private final NodeExperiences nodeExperiences;
        private final PreTrustedNodes preTrustedNodes;

        public TestTrustContext() {
            this.localNode = Utils.createNodeWithPort(80);
            this.nodes = new Node[] {
                Utils.createNodeWithPort(81),
                Utils.createNodeWithPort(87),
                Utils.createNodeWithPort(86),
                Utils.createNodeWithPort(89),
                localNode
            };

            this.nodeExperiences = new NodeExperiences();

            Set<Node> preTrustedNodeSet = new HashSet<>();
            preTrustedNodeSet.add(this.nodes[0]);
            preTrustedNodeSet.add(this.nodes[3]);
            this.preTrustedNodes = new PreTrustedNodes(preTrustedNodeSet);
        }

        public TrustContext getContext() {
            return new TrustContext(
                this.nodes,
                this.localNode,
                this.nodeExperiences,
                this.preTrustedNodes);
        }

        public void setCallCounts(final int nodeIndex, final int numSuccessfulCalls, final int numFailedCalls) {
            final NodeExperience experience = this.nodeExperiences.getNodeExperience(this.localNode, this.nodes[nodeIndex]);
            experience.successfulCalls().set(numSuccessfulCalls);
            experience.failedCalls().set(numFailedCalls);
        }
    }

    private static class MockEigenTrustProvider extends EigenTrustProvider {

        @Override
        public double calculateTrustScore(final NodeExperience experience) {
            long numSuccessfulCalls = experience.successfulCalls().get();
            long numFailedCalls = experience.failedCalls().get();
            return (numFailedCalls * numSuccessfulCalls) * (numSuccessfulCalls + numFailedCalls);
        }
    }
}
