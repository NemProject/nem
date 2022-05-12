package org.nem.peer.trust;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.node.Node;
import org.nem.core.test.*;

import java.util.*;

public class PreTrustedNodesTest {

	// region basic operations

	@Test
	public void numberOfPreTrustedNodesCanBeReturned() {
		// Arrange:
		final PreTrustedNodes preTrustedNodes = createTestPreTrustedNodes();

		// Assert:
		MatcherAssert.assertThat(preTrustedNodes.getSize(), IsEqual.equalTo(3));
	}

	@Test
	public void allPreTrustedNodesCanBeReturned() {
		// Arrange:
		final PreTrustedNodes preTrustedNodes = createTestPreTrustedNodes();

		// Assert:
		final Node[] expectedPreTrustedNodes = new Node[]{
				NodeUtils.createNodeWithName("n"), NodeUtils.createNodeWithName("e"), NodeUtils.createNodeWithName("m")
		};
		MatcherAssert.assertThat(preTrustedNodes.getNodes(), IsEquivalent.equivalentTo(expectedPreTrustedNodes));
	}

	@Test
	public void preTrustedNodesAreIdentifiedCorrectly() {
		// Arrange:
		final PreTrustedNodes preTrustedNodes = createTestPreTrustedNodes();

		// Assert:
		MatcherAssert.assertThat(preTrustedNodes.isPreTrusted(NodeUtils.createNodeWithName("n")), IsEqual.equalTo(true));
		MatcherAssert.assertThat(preTrustedNodes.isPreTrusted(NodeUtils.createNodeWithName("e")), IsEqual.equalTo(true));
		MatcherAssert.assertThat(preTrustedNodes.isPreTrusted(NodeUtils.createNodeWithName("m")), IsEqual.equalTo(true));
	}

	@Test
	public void nonPreTrustedNodesAreIdentifiedCorrectly() {
		// Arrange:
		final PreTrustedNodes preTrustedNodes = createTestPreTrustedNodes();

		// Assert:
		MatcherAssert.assertThat(preTrustedNodes.isPreTrusted(NodeUtils.createNodeWithName("a")), IsEqual.equalTo(false));
		MatcherAssert.assertThat(preTrustedNodes.isPreTrusted(NodeUtils.createNodeWithName("p")), IsEqual.equalTo(false));
		MatcherAssert.assertThat(preTrustedNodes.isPreTrusted(NodeUtils.createNodeWithName("z")), IsEqual.equalTo(false));
	}

	// endregion

	// region getPreTrustVector

	@Test
	public void preTrustVectorCorrectWhenThereAreNoPreTrustedNodes() {
		// Arrange:
		final PreTrustedNodes preTrustedNodes = new PreTrustedNodes(new HashSet<>());
		final Node[] nodes = new Node[]{
				NodeUtils.createNodeWithName("a"), NodeUtils.createNodeWithName("e"), NodeUtils.createNodeWithName("m"),
				NodeUtils.createNodeWithName("z")
		};

		// Act:
		final ColumnVector preTrustVector = preTrustedNodes.getPreTrustVector(nodes);

		// Assert:
		MatcherAssert.assertThat(preTrustVector.size(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(preTrustVector.getAt(0), IsEqual.equalTo(0.25));
		MatcherAssert.assertThat(preTrustVector.getAt(1), IsEqual.equalTo(0.25));
		MatcherAssert.assertThat(preTrustVector.getAt(2), IsEqual.equalTo(0.25));
		MatcherAssert.assertThat(preTrustVector.getAt(3), IsEqual.equalTo(0.25));
	}

	@Test
	public void preTrustVectorIsCorrectWhenThereArePreTrustedNodes() {
		// Arrange:
		final PreTrustedNodes preTrustedNodes = createTestPreTrustedNodes();
		final Node[] nodes = new Node[]{
				NodeUtils.createNodeWithName("a"), NodeUtils.createNodeWithName("e"), NodeUtils.createNodeWithName("m"),
				NodeUtils.createNodeWithName("z")
		};

		// Act:
		final ColumnVector preTrustVector = preTrustedNodes.getPreTrustVector(nodes);

		// Assert: (the vector should be normalized)
		MatcherAssert.assertThat(preTrustVector.size(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(preTrustVector.getAt(0), IsEqual.equalTo(0.00));
		MatcherAssert.assertThat(preTrustVector.getAt(1), IsEqual.equalTo(0.50));
		MatcherAssert.assertThat(preTrustVector.getAt(2), IsEqual.equalTo(0.50));
		MatcherAssert.assertThat(preTrustVector.getAt(3), IsEqual.equalTo(0.00));
	}

	// endregion

	private static PreTrustedNodes createTestPreTrustedNodes() {
		// Arrange:
		final Set<Node> nodeSet = new HashSet<>();
		nodeSet.add(NodeUtils.createNodeWithName("n"));
		nodeSet.add(NodeUtils.createNodeWithName("e"));
		nodeSet.add(NodeUtils.createNodeWithName("m"));
		return new PreTrustedNodes(nodeSet);
	}
}
