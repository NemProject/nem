package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.IsEquivalent;
import org.nem.peer.test.Utils;
import org.nem.peer.Node;

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
				Utils.createNodeWithPort(81),
				Utils.createNodeWithPort(83),
				Utils.createNodeWithPort(84)
		};
		Assert.assertThat(preTrustedNodes.getNodes(), IsEquivalent.equivalentTo(expectedPreTrustedNodes));
	}

	@Test
	public void preTrustedNodesAreIdentifiedCorrectly() {
		// Arrange:
		final PreTrustedNodes preTrustedNodes = createTestPreTrustedNodes();

		// Assert:
		Assert.assertThat(preTrustedNodes.isPreTrusted(Utils.createNodeWithPort(81)), IsEqual.equalTo(true));
		Assert.assertThat(preTrustedNodes.isPreTrusted(Utils.createNodeWithPort(83)), IsEqual.equalTo(true));
		Assert.assertThat(preTrustedNodes.isPreTrusted(Utils.createNodeWithPort(84)), IsEqual.equalTo(true));
	}

	@Test
	public void nonPreTrustedNodesAreIdentifiedCorrectly() {
		// Arrange:
		final PreTrustedNodes preTrustedNodes = createTestPreTrustedNodes();

		// Assert:
		Assert.assertThat(preTrustedNodes.isPreTrusted(Utils.createNodeWithPort(80)), IsEqual.equalTo(false));
		Assert.assertThat(preTrustedNodes.isPreTrusted(Utils.createNodeWithPort(82)), IsEqual.equalTo(false));
		Assert.assertThat(preTrustedNodes.isPreTrusted(Utils.createNodeWithPort(85)), IsEqual.equalTo(false));
	}

	//endregion

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
	}

	//endregion

	public static PreTrustedNodes createTestPreTrustedNodes() {
		// Arrange:
		Set<Node> nodeSet = new HashSet<>();
		nodeSet.add(Utils.createNodeWithPort(81));
		nodeSet.add(Utils.createNodeWithPort(83));
		nodeSet.add(Utils.createNodeWithPort(84));
		return new PreTrustedNodes(nodeSet);
	}
}
