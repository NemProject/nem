package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.test.IsEquivalent;
import org.nem.peer.test.PeerUtils;
import org.nem.peer.node.Node;

import java.util.*;

public class PreTrustedNodesTest {

	//region basic operations

	@Test
	public void numberOfPreTrustedNodesCanBeReturned() {
		// Arrange:
		final PreTrustedNodes preTrustedNodes = createTestPreTrustedNodes();

		// Assert:
		Assert.assertThat(preTrustedNodes.getSize(), IsEqual.equalTo(3));
	}

	@Test
	public void allPreTrustedNodesCanBeReturned() {
		// Arrange:
		final PreTrustedNodes preTrustedNodes = createTestPreTrustedNodes();

		// Assert:
		final Node[] expectedPreTrustedNodes = new Node[] {
				PeerUtils.createNodeWithName("n"),
				PeerUtils.createNodeWithName("e"),
				PeerUtils.createNodeWithName("m")
		};
		Assert.assertThat(preTrustedNodes.getNodes(), IsEquivalent.equivalentTo(expectedPreTrustedNodes));
	}

	@Test
	public void preTrustedNodesAreIdentifiedCorrectly() {
		// Arrange:
		final PreTrustedNodes preTrustedNodes = createTestPreTrustedNodes();

		// Assert:
		Assert.assertThat(preTrustedNodes.isPreTrusted(PeerUtils.createNodeWithName("n")), IsEqual.equalTo(true));
		Assert.assertThat(preTrustedNodes.isPreTrusted(PeerUtils.createNodeWithName("e")), IsEqual.equalTo(true));
		Assert.assertThat(preTrustedNodes.isPreTrusted(PeerUtils.createNodeWithName("m")), IsEqual.equalTo(true));
	}

	@Test
	public void nonPreTrustedNodesAreIdentifiedCorrectly() {
		// Arrange:
		final PreTrustedNodes preTrustedNodes = createTestPreTrustedNodes();

		// Assert:
		Assert.assertThat(preTrustedNodes.isPreTrusted(PeerUtils.createNodeWithName("a")), IsEqual.equalTo(false));
		Assert.assertThat(preTrustedNodes.isPreTrusted(PeerUtils.createNodeWithName("p")), IsEqual.equalTo(false));
		Assert.assertThat(preTrustedNodes.isPreTrusted(PeerUtils.createNodeWithName("z")), IsEqual.equalTo(false));
	}

	//endregion

	//region getPreTrustVector

	@Test
	public void preTrustVectorCorrectWhenThereAreNoPreTrustedNodes() {
		// Arrange:
		final PreTrustedNodes preTrustedNodes = new PreTrustedNodes(new HashSet<>());
		final Node[] nodes = new Node[] {
				PeerUtils.createNodeWithName("a"),
				PeerUtils.createNodeWithName("e"),
				PeerUtils.createNodeWithName("m"),
				PeerUtils.createNodeWithName("z")
		};

		// Act:
		final ColumnVector preTrustVector = preTrustedNodes.getPreTrustVector(nodes);

		// Assert:
		Assert.assertThat(preTrustVector.size(), IsEqual.equalTo(4));
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
				PeerUtils.createNodeWithName("a"),
				PeerUtils.createNodeWithName("e"),
				PeerUtils.createNodeWithName("m"),
				PeerUtils.createNodeWithName("z")
		};

		// Act:
		final ColumnVector preTrustVector = preTrustedNodes.getPreTrustVector(nodes);

		// Assert:
		Assert.assertThat(preTrustVector.size(), IsEqual.equalTo(4));
		Assert.assertThat(preTrustVector.getAt(0), IsEqual.equalTo(0.00));
		Assert.assertThat(preTrustVector.getAt(1), IsEqual.equalTo(1.0 / 3.0));
		Assert.assertThat(preTrustVector.getAt(2), IsEqual.equalTo(1.0 / 3.0));
		Assert.assertThat(preTrustVector.getAt(3), IsEqual.equalTo(0.00));
	}

	//endregion

	public static PreTrustedNodes createTestPreTrustedNodes() {
		// Arrange:
		Set<Node> nodeSet = new HashSet<>();
		nodeSet.add(PeerUtils.createNodeWithName("n"));
		nodeSet.add(PeerUtils.createNodeWithName("e"));
		nodeSet.add(PeerUtils.createNodeWithName("m"));
		return new PreTrustedNodes(nodeSet);
	}
}
