package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.peer.*;
import org.nem.peer.test.NodeCollectionAssert;

import java.util.HashSet;

public class TrustContextTest {

    //region construction

    @Test
    public void trustContextExposesNodeExperiences() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();
        final Node localNode = Utils.createNodeWithPort(80);
        final NodeExperiences nodeExperiences = new NodeExperiences();
        final PreTrustedNodes preTrustedNodes = new PreTrustedNodes(new HashSet<Node>());

        // Act:
        final TrustContext context = new TrustContext(nodes, localNode, nodeExperiences, preTrustedNodes, new UniformTrustProvider());

        // Assert:
        Assert.assertThat(context.getNodeExperiences(), IsSame.sameInstance(nodeExperiences));
    }

    @Test
    public void trustContextExposesPreTrustedNodes() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();
        final Node localNode = Utils.createNodeWithPort(80);
        final NodeExperiences nodeExperiences = new NodeExperiences();
        final PreTrustedNodes preTrustedNodes = new PreTrustedNodes(new HashSet<Node>());

        // Act:
        final TrustContext context = new TrustContext(nodes, localNode, nodeExperiences, preTrustedNodes, new UniformTrustProvider());

        // Assert:
        Assert.assertThat(context.getPreTrustedNodes(), IsSame.sameInstance(preTrustedNodes));
    }

    @Test
    public void trustContextExposesAllNodes() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();
        nodes.update(Utils.createNodeWithPort(87), NodeStatus.ACTIVE);
        nodes.update(Utils.createNodeWithPort(82), NodeStatus.ACTIVE);
        nodes.update(Utils.createNodeWithPort(86), NodeStatus.INACTIVE);
        nodes.update(Utils.createNodeWithPort(84), NodeStatus.INACTIVE);
        nodes.update(Utils.createNodeWithPort(81), NodeStatus.ACTIVE);

        final Node localNode = Utils.createNodeWithPort(90);

        final NodeExperiences nodeExperiences = new NodeExperiences();
        final PreTrustedNodes preTrustedNodes = new PreTrustedNodes(new HashSet<Node>());

        // Act:
        final TrustContext context = new TrustContext(nodes, localNode, nodeExperiences, preTrustedNodes, new UniformTrustProvider());

        // Assert:
        NodeCollectionAssert.arePortsEquivalent(context.getNodes(), new Integer[]{ 81, 82, 84, 86, 87, 90 });
    }

    //endregion

    //region preTrustVector



    //endregion

    /*
    @Test
    public void transposedLocalTrustMatrixCanBeReturned() {
        // Arrange:
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
        final Node node3 = Utils.createNodeWithPort(83);
        final NodeExperiences experiences = new NodeExperiences();

        experiences.getNodeExperience(node1, node2).setLocalTrust(7);
        experiences.getNodeExperience(node1, node3).setLocalTrust(2);
        experiences.getNodeExperience(node2, node1).setLocalTrust(5);
        experiences.getNodeExperience(node2, node3).setLocalTrust(4);
        experiences.getNodeExperience(node3, node1).setLocalTrust(11);
        experiences.getNodeExperience(node3, node2).setLocalTrust(6);

        // Act:
        final Matrix matrix = experiences.getTransposedLocalTrustMatrix(new Node[] { node1, node2, node3 });

        // Assert:
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(5.0));
        Assert.assertThat(matrix.getAt(0, 2), IsEqual.equalTo(11.0));
        Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(7.0));
        Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(1, 2), IsEqual.equalTo(6.0));
        Assert.assertThat(matrix.getAt(2, 0), IsEqual.equalTo(2.0));
        Assert.assertThat(matrix.getAt(2, 1), IsEqual.equalTo(4.0));
        Assert.assertThat(matrix.getAt(2, 2), IsEqual.equalTo(0.0));
    }
    */

/*
    //region getPreTrustVector

    @Test
    public void preTrustVectorCorrectWhenThereAreNoPreTrustedNodes() {
        // Arrange:
        final PreTrustedNodes preTrustedNodes = new PreTrustedNodes(new HashSet<Node>());
        final Node[] nodes = new Node[] {
                Utils.createNodeWithPort(80),
                Utils.createNodeWithPort(83),
                Utils.createNodeWithPort(84),
                Utils.createNodeWithPort(85)
        };

        // Act:
        final Vector preTrustVector = preTrustedNodes.getPreTrustVector(nodes);

        // Assert:
        Assert.assertThat(preTrustVector.getSize(), IsEqual.equalTo(4));
        Assert.assertThat(preTrustVector.getAt(0), IsEqual.equalTo(0.25));
        Assert.assertThat(preTrustVector.getAt(1), IsEqual.equalTo(0.25));
        Assert.assertThat(preTrustVector.getAt(2), IsEqual.equalTo(0.25));
        Assert.assertThat(preTrustVector.getAt(3), IsEqual.equalTo(0.25));
    }

    @Test
    public void preTrustVectorIsCorrectWhenThereArePreTrustedNodes() {
        // Arrange:
        final PreTrustedNodes preTrustedNodes = createTestPreTrustedNodes();
        final Node[] nodes = new Node[] {
                Utils.createNodeWithPort(80),
                Utils.createNodeWithPort(83),
                Utils.createNodeWithPort(84),
                Utils.createNodeWithPort(85)
        };

        // Act:
        final Vector preTrustVector = preTrustedNodes.getPreTrustVector(nodes);

        // Assert:
        Assert.assertThat(preTrustVector.getSize(), IsEqual.equalTo(4));
        Assert.assertThat(preTrustVector.getAt(0), IsEqual.equalTo(0.00));
        Assert.assertThat(preTrustVector.getAt(1), IsEqual.equalTo(1.0 / 3.0));
        Assert.assertThat(preTrustVector.getAt(2), IsEqual.equalTo(1.0 / 3.0));
        Assert.assertThat(preTrustVector.getAt(3), IsEqual.equalTo(0.00));
    }*/


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
        NodeCollectionAssert.arePortsEquivalent(nodeArray, new Integer[]{ 81, 82, 84, 86, 87, 90 });
    }
}
