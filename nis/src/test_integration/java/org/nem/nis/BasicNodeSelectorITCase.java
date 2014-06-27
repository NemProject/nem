package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.KeyPair;
import org.nem.core.math.ColumnVector;
import org.nem.peer.node.*;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;

public class BasicNodeSelectorITCase {
	private static final Logger LOGGER = Logger.getLogger(BasicNodeSelectorITCase.class.getName());

	@Test
	public void selectNodesDoesNotFavorAnyNodeIfAllNodesHaveEqualTrust() {
		// Arrange:
		final int numNodes = 101;
		final int numNodesSelected = 10;

		LOGGER.info(String.format("Generating test context with %d nodes...", numNodes));
		final ColumnVector trustVector = new ColumnVector(numNodes);
		trustVector.setAll(1.0 / (double)numNodes);
		final TestContext context = new TestContext(trustVector);

		// Act:
		int numTries = numNodes * 1000;

		LOGGER.info(String.format("Selecting nodes (%d) iterations...", numTries));
		final long[] observed = new long[numNodes];
		for (int i = 0; i < numTries; ++i) {
			final List<NodeExperiencePair> nodePairs = context.selector.selectNodes(numNodesSelected);
			nodePairs.stream().forEach(nodePair -> observed[context.findIndex(nodePair.getNode())]++);
		}

		// Assuming a discrete uniform distribution
		LOGGER.info("Calculating chiSquare...");
		final double expectedValue = (double)(numTries * numNodesSelected)/(double)numNodes;
		double chiSquare = 0.0;
		for (int i = 0; i < numNodes; ++i) {
			final double difference = observed[i] - expectedValue;
			chiSquare += (difference * difference) / expectedValue;
		}

		LOGGER.info(String.format("chiSquare=%f", chiSquare));
		double probability = 0;
		final double[] chiSquareTable = { 61.98, 67.33, 70.06, 77.93, 82.36, 90.13, 99.33, 109.14, 118.50, 124.34, 129.56, 135.81, 140.17, 149.45 };
		final double[] oneMinusAlpha = { 0.001, 0.005, 0.01, 0.05, 0.100, 0.250, 0.500, 0.750, 0.900, 0.950, 0.975, 0.990, 0.995, 0.999 };
		for (int i = chiSquareTable.length - 1; i >= 0; i--) {
			if (chiSquare > chiSquareTable[i]) {
				probability = oneMinusAlpha[i] * 100;
				LOGGER.info("Hypothesis of randomness of node selection can be rejected with at least " + probability + "% certainty.");
				break;
			}
		}

		if (0 == probability)
			LOGGER.info("Hypothesis of randomness of node selection can be rejected with less than " + (oneMinusAlpha[0]*100) + "% certainty.");

		Assert.assertThat(probability <= 75.0, IsEqual.equalTo(true));
	}

	private static class TestContext {
		private final TrustContext context = Mockito.mock(TrustContext.class);
		private final TrustProvider trustProvider = Mockito.mock(TrustProvider.class);
		private final Node localNode = Mockito.mock(Node.class);
		private final Node[] nodes;
		private final NodeExperiences nodeExperiences;
		private final NodeSelector selector;
		private final Map<Node, Integer> nodeIdMap = new HashMap<>();

		public TestContext(final ColumnVector trustValues) {
			this(trustValues, new SecureRandom());
		}

		public TestContext(final ColumnVector trustValues, final Random random) {
			Mockito.when(context.getLocalNode()).thenReturn(this.localNode);

			this.nodes = new Node[trustValues.size()];
			for (int i = 0; i < this.nodes.length; ++i) {
				this.nodes[i] = new Node(new NodeIdentity(new KeyPair()), NodeEndpoint.fromHost("127.0.0.1"));
				this.nodeIdMap.put(this.nodes[i], i);
			}

			Mockito.when(this.context.getNodes()).thenReturn(this.nodes);

			this.nodeExperiences = new NodeExperiences();
			Mockito.when(this.context.getNodeExperiences()).thenReturn(this.nodeExperiences);

			Mockito.when(this.trustProvider.computeTrust(this.context)).thenReturn(trustValues);
			this.selector = new BasicNodeSelector(this.trustProvider, this.context, random);
		}

		public int findIndex(final Node node) {
			return this.nodeIdMap.get(node);
		}
	}
}
