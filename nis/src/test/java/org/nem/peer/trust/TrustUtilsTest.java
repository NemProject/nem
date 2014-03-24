package org.nem.peer.trust;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.peer.*;
import org.nem.peer.test.NodeCollectionAssert;

import java.util.HashSet;
import java.util.Set;

public class TrustUtilsTest {

    @Test
    public void allNodesCanBeFlattenedIntoSingleArray() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();
        nodes.update(Utils.createNodeWithPort(87), NodeStatus.ACTIVE);
        nodes.update(Utils.createNodeWithPort(82), NodeStatus.ACTIVE);
        nodes.update(Utils.createNodeWithPort(86), NodeStatus.INACTIVE);
        nodes.update(Utils.createNodeWithPort(84), NodeStatus.INACTIVE);
        nodes.update(Utils.createNodeWithPort(81), NodeStatus.ACTIVE);

        final Node localNode = Utils.createNodeWithPort(90);

        // Act:
        final Node[] nodeArray = TrustUtils.toNodeArray(nodes, localNode);

        // Assert:
        NodeCollectionAssert.arePortsEquivalent(nodeArray, new Integer[] { 81, 82, 84, 86, 87, 90 });
    }

    @Test
    public void localTrustIsSetToCorrectDefaultsWhenNoCallsAreMadeBetweenNodes() {
        // Arrange:
        final UpdateLocalTrustTestContext context = new UpdateLocalTrustTestContext();

        // Act:
        double result = context.update();
        final Vector vector = context.getLocalTrustVector();

        // Assert:
        Assert.assertThat(result, IsEqual.equalTo(3.0));
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
        final UpdateLocalTrustTestContext context = new UpdateLocalTrustTestContext();
        context.setCallCounts(0, 1, 2); // 2
        context.setCallCounts(1, 2, 2); // 4
        context.setCallCounts(2, 3, 3); // 9
        context.setCallCounts(3, 4, 4); // 16

        // Act:
        double result = context.update();
        final Vector vector = context.getLocalTrustVector();

        // Assert:
        Assert.assertThat(result, IsEqual.equalTo(32.0));
        Assert.assertThat(vector.getSize(), IsEqual.equalTo(5));
        Assert.assertThat(vector.getAt(0), IsEqual.equalTo(2.0 / 32)); // pre-trusted
        Assert.assertThat(vector.getAt(1), IsEqual.equalTo(4.0 / 32));
        Assert.assertThat(vector.getAt(2), IsEqual.equalTo(9.0 / 32));
        Assert.assertThat(vector.getAt(3), IsEqual.equalTo(16.0 / 32)); // pre-trusted
        Assert.assertThat(vector.getAt(4), IsEqual.equalTo(1.0 / 32)); // self
    }

    private static class UpdateLocalTrustTestContext {

        private final Node localNode;
        private final Node[] nodes;
        private final NodeExperiences nodeExperiences;
        private final PreTrustedNodes preTrustedNodes;

        public UpdateLocalTrustTestContext() {
            this.localNode = Utils.createNodeWithPort(80);
            this.nodes = new Node[] {
                Utils.createNodeWithPort(81),
                Utils.createNodeWithPort(87),
                Utils.createNodeWithPort(86),
                Utils.createNodeWithPort(89),
                localNode
            };

            this.nodeExperiences = new NodeExperiences();
            this.nodeExperiences.getNodeExperience(this.localNode, this.nodes[0]);

            Set<Node> preTrustedNodeSet = new HashSet<>();
            preTrustedNodeSet.add(this.nodes[0]);
            preTrustedNodeSet.add(this.nodes[3]);
            this.preTrustedNodes = new PreTrustedNodes(preTrustedNodeSet);
        }

        public Vector getLocalTrustVector() {
            return this.nodeExperiences.getLocalTrustVector(this.localNode, this.nodes);
        }

        public void setCallCounts(final int nodeIndex, final int numSuccessfulCalls, final int numFailedCalls) {
            final NodeExperience experience = this.nodeExperiences.getNodeExperience(this.localNode, this.nodes[nodeIndex]);
            experience.successfulCalls().set(numSuccessfulCalls);
            experience.failedCalls().set(numFailedCalls);
        }

        public double update() {
            return TrustUtils.updateLocalTrust(
                this.localNode,
                this.nodes,
                this.nodeExperiences,
                this.preTrustedNodes,
                new TrustProvider() {
                    @Override
                    public double calculateScore(long numSuccessfulCalls, long numFailedCalls) {
                        return (numFailedCalls * numSuccessfulCalls) * (numSuccessfulCalls + numFailedCalls);
                    }
                });
        }

    }
}