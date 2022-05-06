package org.nem.nis;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.KeyPair;
import org.nem.core.math.ColumnVector;
import org.nem.core.node.*;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.NodeExperiences;

import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;

public class BasicNodeSelectorITCase {
	private static final Logger LOGGER = Logger.getLogger(BasicNodeSelectorITCase.class.getName());

	private static final int NUM_NODES = 101;
	private static final int NUM_NODES_SELECTED = 10;
	private static final int NUM_TRIES = NUM_NODES * 1000;

	@Test
	public void selectNodesDoesNotFavorAnyNodeIfAllNodesHaveEqualTrust() {
		// Arrange:
		LOGGER.info(String.format("Generating test context with %d nodes...", NUM_NODES));
		final ColumnVector trustVector = new ColumnVector(NUM_NODES);
		trustVector.setAll(1.0 / (double)NUM_NODES);
		final TestContext context = new TestContext(trustVector);

		// Act:
		LOGGER.info(String.format("Selecting nodes (%d iterations)...", NUM_TRIES));
		final long[] observed = new long[NUM_NODES];
		for (int i = 0; i < NUM_TRIES; ++i) {
			final List<Node> nodes = context.selector.selectNodes();
			nodes.stream().forEach(node -> ++observed[context.findIndex(node)]);
		}

		// Assuming a discrete uniform distribution
		LOGGER.info("Calculating chiSquare...");
		final double expectedValue = (double)(NUM_TRIES * NUM_NODES_SELECTED) / (double)NUM_NODES;
		final double chiSquare = calculateChiSquare(observed, expectedValue);

		// Assert:
		assertRandomness(chiSquare);
	}

	@Test
	public void secureRandomIsRandom() {
		// Arrange:
		final SecureRandom random = new SecureRandom();

		// Act:
		LOGGER.info(String.format("Selecting (%d) random values ...", NUM_TRIES));
		final long[] observed = new long[NUM_NODES];
		for (int i = 0; i < NUM_TRIES; ++i) {
			final int value = (int)(random.nextDouble() * NUM_NODES);
			++observed[value];
		}

		// Assuming a discrete uniform distribution
		LOGGER.info("Calculating chiSquare...");
		final double expectedValue = (double)NUM_TRIES / (double)NUM_NODES;
		final double chiSquare = calculateChiSquare(observed, expectedValue);

		// Assert:
		assertRandomness(chiSquare);
	}

	private static double calculateChiSquare(final long[] observedValues, final double expectedValue) {
		LOGGER.info("Calculating chiSquare...");
		double chiSquare = 0.0;
		double min = expectedValue;
		double max = expectedValue;
		for (final long observedValue : observedValues) {
			min = observedValue < min ? observedValue : min;
			max = observedValue > max ? observedValue : max;

			final double difference = observedValue - expectedValue;
			chiSquare += (difference * difference) / expectedValue;
		}

		LOGGER.info(String.format("chiSquare=%f, min=%f, max=%f", chiSquare, min, max));
		return chiSquare;
	}

	private static void assertRandomness(final double chiSquare) {
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

		if (0 == probability) {
			LOGGER.info("Hypothesis of randomness of node selection can be rejected with less than " + (oneMinusAlpha[0] * 100) + "% certainty.");
		}

		MatcherAssert.assertThat(probability <= 75.0, IsEqual.equalTo(true));
	}

	private static class TestContext {
		private final TrustContext context = Mockito.mock(TrustContext.class);
		private final Node localNode = Mockito.mock(Node.class);
		private final Node[] nodes;
		private final NodeExperiences nodeExperiences;
		private final NodeSelector selector;
		private final Map<Node, Integer> nodeIdMap = new HashMap<>();

		public TestContext(final ColumnVector trustValues) {
			this(trustValues, new SecureRandom());
		}

		public TestContext(final ColumnVector trustValues, final Random random) {
			Mockito.when(this.context.getLocalNode()).thenReturn(this.localNode);

			this.nodes = new Node[trustValues.size()];
			for (int i = 0; i < this.nodes.length; ++i) {
				this.nodes[i] = new Node(new NodeIdentity(new KeyPair()), NodeEndpoint.fromHost("127.0.0.1"));
				this.nodeIdMap.put(this.nodes[i], i);
			}

			Mockito.when(this.context.getNodes()).thenReturn(this.nodes);

			this.nodeExperiences = new NodeExperiences();
			Mockito.when(this.context.getNodeExperiences()).thenReturn(this.nodeExperiences);

			this.selector = new BasicNodeSelector(NUM_NODES_SELECTED, trustValues, this.context.getNodes(), random);
		}

		public int findIndex(final Node node) {
			return this.nodeIdMap.get(node);
		}
	}
}
