package org.nem.peer.trust;

import static org.junit.Assert.*;

import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.KeyPair;
import org.nem.core.math.ColumnVector;
import org.nem.nis.BlockChain;
import org.nem.peer.node.*;
import org.nem.peer.trust.score.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BasicNodeSelectorTest {
	private static final Logger LOGGER = Logger.getLogger(BasicNodeSelectorTest.class.getName());

	//region recalculations

	@Test
	public void constructorRecalculatesTrustValues() {
		// Act:
		final TestContext context = new TestContext(new ColumnVector(1, 1, 1, 1, 1));

		// Assert:
		Mockito.verify(context.trustProvider, Mockito.times(1)).computeTrust(context.context);
	}

	@Test
	public void selectNodeDoesNotRecalculateTrustValues() {
		// Arrange:
		final TestContext context = new TestContext(new ColumnVector(1, 1, 1, 1, 1));

		// Act:
		for (int i = 0; i < 10; ++i)
			context.selector.selectNode();

		// Assert:
		Mockito.verify(context.trustProvider, Mockito.times(1)).computeTrust(context.context);
	}

	//endregion

	//region selectNode

	@Test
	public void selectNodeReturnsNullWhenAllNodesHaveZeroTrustValues() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(new ColumnVector(0, 0, 0), random);

		// Act:
		final NodeExperiencePair nodePair = context.selector.selectNode();

		// Assert:
		Assert.assertThat(nodePair, IsNull.nullValue());
	}

	@Test
	public void selectNodeReturnsNonNullNodeWhenAtLeastOneNodeHasNonZeroTrustValue() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(new ColumnVector(0, 1, 0), random);

		// Act:
		final NodeExperiencePair nodePair = context.selector.selectNode();

		// Assert:
		Assert.assertThat(nodePair.getNode(), IsEqual.equalTo(context.nodes[1]));
	}

	//endregion

	//region selectNodes

	@Test
	public void selectNodesReturnsEmptyListWhenAllNodesHaveZeroTrustValues() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(new ColumnVector(0, 0, 0, 0), random);

		// Act:
		final List<NodeExperiencePair> nodePairs = context.selector.selectNodes(10);

		// Assert:
		Assert.assertThat(nodePairs.size(), IsEqual.equalTo(0));
	}

	@Test
	public void selectNodesSelectsNodesCorrectlyWhenTrustValuesSumToOne() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40);
		final TestContext context = new TestContext(new ColumnVector(0.1, 0.2, 0.3, 0.4), random);

		// Act:
		final List<NodeExperiencePair> nodePairs = context.selector.selectNodes(2);

		// Assert:
		Assert.assertThat(
				nodePairs.stream().map(NodeExperiencePair::getNode).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesSelectsNodesCorrectlyWhenTrustValuesDoNotSumToOne() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40);
		final TestContext context = new TestContext(new ColumnVector(20, 40, 60, 80), random);

		// Act:
		final List<NodeExperiencePair> nodePairs = context.selector.selectNodes(2);

		// Assert:
		Assert.assertThat(
				nodePairs.stream().map(NodeExperiencePair::getNode).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesOnlyReturnsUniqueNodesWithNonZeroTrust() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(new ColumnVector(1, 0, 1, 0), random);

		// Act:
		final List<NodeExperiencePair> nodePairs = context.selector.selectNodes(10);

		// Assert:
		Assert.assertThat(
				nodePairs.stream().map(NodeExperiencePair::getNode).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesDoesNotReturnMoreThanMaxNodes() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40);
		final TestContext context = new TestContext(new ColumnVector(10, 20, 30, 40), random);

		// Act:
		final List<NodeExperiencePair> nodePairs = context.selector.selectNodes(2);

		// Assert:
		Assert.assertThat(
				nodePairs.stream().map(NodeExperiencePair::getNode).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesReturnsAllNodesIfMaxNodesIsGreaterThanAvailableNodes() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40, 0.10, 0.40);
		final TestContext context = new TestContext(new ColumnVector(10, 20, 30, 40), random);

		// Act:
		final List<NodeExperiencePair> nodePairs = context.selector.selectNodes(10);

		// Assert:
		Assert.assertThat(
				nodePairs.stream().map(NodeExperiencePair::getNode).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2], context.nodes[1], context.nodes[3])));
	}

	@Test
	public void selectNodesDoesNotFavorAnyNodeIfAllNodesHaveEqualTrust() {
		// I admit, it really belongs to integration tests.
		// Arrange:
		int numNodes = 101;
		int numNodesSelected = 10;
		final SecureRandom random = new SecureRandom(); 
		final ColumnVector trustVector = new ColumnVector(numNodes);
		trustVector.setAll(1.0/(double)numNodes);
		final TestContext context = new TestContext(trustVector, random);

		// Act:
		int numTries = numNodes*1000;
		
		// Assuming a discrete uniform distribution
		double expectedValue = (double)(numTries*numNodesSelected)/(double)numNodes;
		double[] expected = new double[numNodes];
		Arrays.fill(expected, expectedValue);
		
		long[] observed = new long[numNodes];
		int i=0;
		for (i=0; i<numTries; i++) {
			final List<NodeExperiencePair> nodePairs = context.selector.selectNodes(numNodesSelected);
			nodePairs.stream()
					 .forEach(nodePair -> observed[context.findIndex(nodePair.getNode())]++);
		}
		double chiSquare = 0.0;
		for (i=0; i<numNodes; i++) {
			chiSquare += (observed[i]-expectedValue)*(observed[i]-expectedValue)/expectedValue;
		}
		LOGGER.info("chiSquare=" + chiSquare);
		double[] chiSquareTable = {61.98, 67.33, 70.06, 77.93, 82.36, 90.13, 99.33, 109.14, 118.50, 124.34, 129.56, 135.81, 140.17, 149.45 };
		double[] oneMinusAlpha = { 0.001, 0.005, 0.01, 0.05, 0.100, 0.250, 0.500, 0.750, 0.900, 0.950, 0.975, 0.990, 0.995, 0.999 };
		for (i=chiSquareTable.length-1; i>=0; i--) {
			if (chiSquare > chiSquareTable[i]) {
				LOGGER.info("Hypothesis of randomness of node selection can be rejected with at least " + oneMinusAlpha[i]*100 + "% certainty.");
				break;
			}
		}
		if (i == -1) {
			LOGGER.info("Hypothesis of randomness of node selection can be rejected with less than " + (oneMinusAlpha[0]*100) + "% certainty.");
		}
	}

	private static class TestContext {
		private final TrustContext context = Mockito.mock(TrustContext.class);
		private final TrustProvider trustProvider = Mockito.mock(TrustProvider.class);
		private final Node localNode = Mockito.mock(Node.class);
		private final Node[] nodes;
		private final NodeExperiences nodeExperiences;
		private final NodeSelector selector;

		public TestContext(final ColumnVector trustValues) {
			this(trustValues, new SecureRandom());
		}

		public TestContext(final ColumnVector trustValues, final Random random) {
			Mockito.when(context.getLocalNode()).thenReturn(this.localNode);

			this.nodes = new Node[trustValues.size()];
			for (int i = 0; i < this.nodes.length; ++i)
				this.nodes[i] = new Node(new NodeIdentity(new KeyPair()), NodeEndpoint.fromHost("127.0.0.1"));

			Mockito.when(this.context.getNodes()).thenReturn(this.nodes);

			this.nodeExperiences = new NodeExperiences();
			Mockito.when(this.context.getNodeExperiences()).thenReturn(this.nodeExperiences);

			Mockito.when(this.trustProvider.computeTrust(this.context)).thenReturn(trustValues);
			this.selector = new BasicNodeSelector(this.trustProvider, this.context, random);
		}
		
		public int findIndex(Node node) {
			for (int i=0; i<nodes.length; i++) {
				if (nodes[i] == node) {
					return i;
				}
			}
			return -1;
		}
	}
}
