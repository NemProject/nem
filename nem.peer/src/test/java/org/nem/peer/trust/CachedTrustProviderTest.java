package org.nem.peer.trust;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.node.Node;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.peer.trust.score.NodeExperiences;

import java.util.*;
import java.util.function.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CachedTrustProviderTest {
	private static final Logger LOGGER = Logger.getLogger(CachedTrustProviderTest.class.getName());
	private static final int MAX_MATRIX_SIZE = 101;

	// region caching

	@Test
	public void trustValuesAreComputedFirstTime() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setCurrentTime(0);

		// Act:
		final TrustResult result = context.computeTrust();

		// Assert:
		MatcherAssert.assertThat(result.getTrustContext().getNodes(), IsEqual.equalTo(context.trust1Nodes));
		MatcherAssert.assertThat(result.getTrustValues(), IsEqual.equalTo(new ColumnVector(0.5, 0.5)));
		context.assertTrustProviderCalls(1);
	}

	@Test
	public void trustValuesAreNotComputedWithinCacheInterval() {
		// Assert:
		assertSingleTrustComputation(10, 10);
		assertSingleTrustComputation(10, 11);
		assertSingleTrustComputation(10, 75);
		assertSingleTrustComputation(10, 110);
	}

	private static void assertSingleTrustComputation(final int time1, final int time2) {
		// Arrange:
		final TestContext context = new TestContext();
		context.setCurrentTime(time1);
		context.computeTrust();
		context.setCurrentTime(time2);

		// Act:
		final TrustResult result = context.computeTrust();

		// Assert:
		MatcherAssert.assertThat(result.getTrustContext().getNodes(), IsEqual.equalTo(context.trust1Nodes));
		MatcherAssert.assertThat(result.getTrustValues(), IsEqual.equalTo(new ColumnVector(0.5, 0.5)));
		context.assertTrustProviderCalls(1);
	}

	@Test
	public void trustValuesAreComputedOutsideOfCacheInterval() {
		// Assert:
		assertTwoTrustComputations(10, 111);
		assertTwoTrustComputations(10, 181);
		assertTwoTrustComputations(10, 333);
	}

	private static void assertTwoTrustComputations(final int time1, final int time2) {
		// Arrange:
		final TestContext context = new TestContext();
		context.setCurrentTime(time1);
		context.computeTrust();
		context.setCurrentTime(time2);

		// Act:
		final TrustResult result = context.computeTrust();

		// Assert:
		MatcherAssert.assertThat(result.getTrustContext().getNodes(), IsEqual.equalTo(context.trust2Nodes));
		MatcherAssert.assertThat(result.getTrustValues(), IsEqual.equalTo(new ColumnVector(0.25, 0.75)));
		context.assertTrustProviderCalls(2);
	}

	@Test
	public void lastTrustValueComputationIsCached() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setCurrentTime(10);
		context.computeTrust();
		context.setCurrentTime(111);
		context.computeTrust();
		context.setCurrentTime(211);

		// Act:
		final TrustResult result = context.computeTrust();

		// Assert: the value calculated at 111 is cached and returned when querying at 211
		MatcherAssert.assertThat(result.getTrustContext().getNodes(), IsEqual.equalTo(context.trust2Nodes));
		MatcherAssert.assertThat(result.getTrustValues(), IsEqual.equalTo(new ColumnVector(0.25, 0.75)));
		context.assertTrustProviderCalls(2);
	}

	@Test
	public void copyOfTrustValuesIsReturned() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setCurrentTime(0);

		// Act:
		final ColumnVector trustValues1 = context.computeTrust().getTrustValues();
		trustValues1.setAt(0, 0);
		final ColumnVector trustValues2 = context.computeTrust().getTrustValues();

		// Assert:
		MatcherAssert.assertThat(trustValues1, IsEqual.equalTo(new ColumnVector(0.0, 0.5)));
		MatcherAssert.assertThat(trustValues2, IsEqual.equalTo(new ColumnVector(0.5, 0.5)));
		context.assertTrustProviderCalls(1);
	}

	private static class TestContext {
		private final TrustProvider innerTrustProvider = Mockito.mock(TrustProvider.class);
		private final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		private final TrustContext context = Mockito.mock(TrustContext.class);
		private final CachedTrustProvider trustProvider = new CachedTrustProvider(this.innerTrustProvider, 100, this.timeProvider);

		private final Node[] trust1Nodes = new Node[]{
				NodeUtils.createNodeWithName("1a"), NodeUtils.createNodeWithName("1b")
		};
		private final Node[] trust2Nodes = new Node[]{
				NodeUtils.createNodeWithName("2c"), NodeUtils.createNodeWithName("2d")
		};
		private final Node[] trust3Nodes = new Node[]{
				NodeUtils.createNodeWithName("3e"), NodeUtils.createNodeWithName("3f")
		};

		public TestContext() {
			final TrustResult result1 = createTrustResult(this.trust1Nodes, new ColumnVector(1, 1));
			final TrustResult result2 = createTrustResult(this.trust2Nodes, new ColumnVector(1, 3));
			final TrustResult result3 = createTrustResult(this.trust3Nodes, new ColumnVector(1, 7));
			Mockito.when(this.innerTrustProvider.computeTrust(Mockito.any())).thenReturn(result1, result2, result3);

			Mockito.when(this.context.getNodes()).thenReturn(new Node[]{});
		}

		public TrustResult computeTrust() {
			return this.trustProvider.computeTrust(this.context);
		}

		public void setCurrentTime(final int time) {
			Mockito.when(this.timeProvider.getCurrentTime()).thenReturn(new TimeInstant(time));
		}

		public void assertTrustProviderCalls(final int numCalls) {
			Mockito.verify(this.innerTrustProvider, Mockito.times(numCalls)).computeTrust(this.context);
		}
	}

	// endregion

	// region truncated nodes

	@Test
	public void trustCalculationIsDelegatedToInnerProviderWhenTruncationOccurs() {
		// Arrange:
		final TruncationTestContext context = new TruncationTestContext();

		// Act:
		final TrustResult actualResult = context.computeTrust();

		// Assert:
		Mockito.verify(context.innerTrustProvider, Mockito.only()).computeTrust(Mockito.any());
		MatcherAssert.assertThat(actualResult.getTrustContext(), IsEqual.equalTo(context.expectedResult.getTrustContext()));
		MatcherAssert.assertThat(actualResult.getTrustValues(), IsEqual.equalTo(context.expectedResult.getTrustValues()));
	}

	@Test
	public void trustContextPassedToInnerProviderContainsTruncatedNodes() {
		// Arrange:
		final TruncationTestContext context = new TruncationTestContext();

		// Act:
		final TrustContext trustContext = context.computeTrustAndCaptureContext();

		// Assert:
		MatcherAssert.assertThat(trustContext.getLocalNode(), IsEqual.equalTo(context.localNode));
		MatcherAssert.assertThat(trustContext.getNodes().length, IsEqual.equalTo(MAX_MATRIX_SIZE));
		MatcherAssert.assertThat(trustContext.getNodeExperiences(), IsEqual.equalTo(context.trustContext.getNodeExperiences()));
		MatcherAssert.assertThat(trustContext.getPreTrustedNodes(), IsEqual.equalTo(context.trustContext.getPreTrustedNodes()));
		MatcherAssert.assertThat(trustContext.getParams(), IsEqual.equalTo(context.trustContext.getParams()));
	}

	@Test
	public void trustContextPassedToInnerProviderContainsLocalNode() {
		// Arrange:
		final TruncationTestContext context = new TruncationTestContext();

		// Act:
		final TrustContext trustContext = context.computeTrustAndCaptureContext();

		// Assert:
		final Node lastNode = trustContext.getNodes()[trustContext.getNodes().length - 1];
		MatcherAssert.assertThat(lastNode, IsEqual.equalTo(context.localNode));
	}

	@Test
	public void trustContextPassedToInnerProviderDoesNotContainAnyDuplicateNodes() {
		// Arrange:
		final TruncationTestContext context = new TruncationTestContext();

		// Act:
		final TrustContext trustContext = context.computeTrustAndCaptureContext();
		final Set<Node> nodesSet = new HashSet<>(Arrays.asList(trustContext.getNodes()));

		// Assert:
		MatcherAssert.assertThat(nodesSet.size(), IsEqual.equalTo(MAX_MATRIX_SIZE));
	}

	@Test
	public void trustContextPassedToInnerProviderDoesNotSelectHighTrustNodesThatAreNotInCurrentContext() {
		// Assert:
		assertTopTrustedNodeTruncationSelection((nodes, initialNodes) -> nodes,
				names -> MatcherAssert.assertThat(names.isEmpty(), IsEqual.equalTo(true)));
	}

	@Test
	public void trustContextPassedToInnerProviderDoesNotAutomaticallySelectLowTrustNodesWhenHighTrustNodesAreNotInCurrentContext() {
		// Assert:
		assertTopTrustedNodeTruncationSelection(
				(nodes, initialNodes) -> org.apache.commons.lang3.ArrayUtils.addAll(nodes, Arrays.copyOfRange(initialNodes, 5, 15)),
				names -> names.size() < 10, // note that *occasionally* all low trust nodes can be selected due to randomness
				names -> {
					MatcherAssert.assertThat(names.size() < 10, IsEqual.equalTo(true));
					for (final String name : Arrays.asList("i6", "i8", "i10", "i12", "i14")) {
						MatcherAssert.assertThat(names.contains(name), IsEqual.equalTo(true));
					}
				});
	}

	@Test
	public void trustContextPassedToInnerProviderContainsHighTrustNodes() {
		// Assert:
		assertTopTrustedNodeTruncationSelection(org.apache.commons.lang3.ArrayUtils::addAll, names -> {
			for (final String name : Arrays.asList("i0", "i2", "i4", "i6", "i8", "i10", "i12", "i14", "i16", "i18")) {
				MatcherAssert.assertThat(names.contains(name), IsEqual.equalTo(true));
			}
		});
	}

	private static void assertTopTrustedNodeTruncationSelection(final BiFunction<Node[], Node[], Node[]> mergeSecondRoundNodes,
			final Consumer<List<String>> assertInitialNodeNames) {
		assertTopTrustedNodeTruncationSelection(mergeSecondRoundNodes, nodes -> true, assertInitialNodeNames);
	}

	private static void assertTopTrustedNodeTruncationSelection(final BiFunction<Node[], Node[], Node[]> mergeSecondRoundNodes,
			final Predicate<List<String>> useGeneratedNodeNames, final Consumer<List<String>> assertInitialNodeNames) {
		// allow up to k retries
		for (int k = 0; k < 5; ++k) {
			// Arrange:
			// - create 20 nodes such that the even nodes have high trust
			final Node localNode = NodeUtils.createNodeWithName("l");
			final Node[] initialNodes = new Node[20];
			final ColumnVector initialTrustVector = new ColumnVector(initialNodes.length);
			for (int i = 0; i < initialNodes.length; ++i) {
				initialNodes[i] = NodeUtils.createNodeWithName(String.format("i%d", i));
				initialTrustVector.setAt(i, 0 == i % 2 ? 1.0 : 0.1);
			}
			final TrustResult trustResult1 = createTrustResult(initialNodes, initialTrustVector);

			// - create 200 random nodes
			Node[] nodes = new Node[200];
			for (int i = 0; i < nodes.length; ++i) {
				nodes[i] = NodeUtils.createNodeWithName(String.format("p%d", i));
			}
			nodes = mergeSecondRoundNodes.apply(nodes, initialNodes);
			final TrustResult trustResult2 = createTrustResult(nodes, new ColumnVector(nodes.length));

			// - set up the test providers
			final TrustProvider innerTrustProvider = Mockito.mock(TrustProvider.class);
			Mockito.when(innerTrustProvider.computeTrust(Mockito.any())).thenReturn(trustResult1, trustResult2);
			final CachedTrustProvider trustProvider = new CachedTrustProvider(innerTrustProvider, 0, Utils.createMockTimeProvider(1, 2, 3));

			// Act:
			// - trigger the trust calculation on the initial nodes (high, low ...)
			trustProvider.computeTrust(createTrustContext(initialNodes, localNode));
			// - trigger the trust calculation on the next nodes (zero ...)
			// (note that some of the high trust nodes from the previous calculation should be selected, if they are included)
			trustProvider.computeTrust(createTrustContext(nodes, localNode));

			// Assert:
			final ArgumentCaptor<TrustContext> trustContextCaptor = ArgumentCaptor.forClass(TrustContext.class);
			Mockito.verify(innerTrustProvider, Mockito.times(2)).computeTrust(trustContextCaptor.capture());
			final TrustContext trustContext = trustContextCaptor.getValue();

			final Set<Node> nodesSet = new HashSet<>(Arrays.asList(trustContext.getNodes()));
			final List<String> names = nodesSet.stream().map(n -> n.getIdentity().getName()).filter(n -> n.startsWith("i"))
					.collect(Collectors.toList());
			LOGGER.info("names of selected initial nodes: " + StringUtils.join(names, ","));

			MatcherAssert.assertThat(nodesSet.size(), IsEqual.equalTo(MAX_MATRIX_SIZE));
			if (!useGeneratedNodeNames.test(names)) {
				LOGGER.info("regenerating nodes for test ...");
				continue;
			}

			assertInitialNodeNames.accept(names);
			return;
		}

		Assert.fail("test could not generate appropropriate nodes");
	}

	private static class TruncationTestContext {
		private final Node[] nodes;
		private final Node localNode;
		private final TrustContext trustContext;
		private final TrustResult expectedResult = createTrustResult(new Node[]{
				NodeUtils.createNodeWithName("r1")
		}, new ColumnVector(1));
		private final TrustProvider innerTrustProvider = Mockito.mock(TrustProvider.class);
		private final CachedTrustProvider trustProvider = new CachedTrustProvider(this.innerTrustProvider, 0,
				Mockito.mock(TimeProvider.class));

		public TruncationTestContext() {
			this.nodes = new Node[200];
			for (int i = 0; i < this.nodes.length; ++i) {
				this.nodes[i] = NodeUtils.createNodeWithName(String.format("p%d", i));
			}

			this.localNode = NodeUtils.createNodeWithName("l");
			this.trustContext = createTrustContext(this.nodes, this.localNode);

			Mockito.when(this.innerTrustProvider.computeTrust(Mockito.any())).thenReturn(this.expectedResult);
		}

		public TrustResult computeTrust() {
			return this.trustProvider.computeTrust(this.trustContext);
		}

		public TrustContext computeTrustAndCaptureContext() {
			// Act:
			this.computeTrust();

			// Assert:
			final ArgumentCaptor<TrustContext> trustContextCaptor = ArgumentCaptor.forClass(TrustContext.class);
			Mockito.verify(this.innerTrustProvider, Mockito.only()).computeTrust(trustContextCaptor.capture());
			return trustContextCaptor.getValue();
		}
	}

	// endregion

	private static TrustContext createTrustContext(final Node[] nodes, final Node localNode) {
		return new TrustContext(nodes, localNode, Mockito.mock(NodeExperiences.class), Mockito.mock(PreTrustedNodes.class),
				Mockito.mock(TrustParameters.class));
	}

	private static TrustResult createTrustResult(final Node[] nodes, final ColumnVector vector) {
		final TrustContext context = Mockito.mock(TrustContext.class);
		Mockito.when(context.getNodes()).thenReturn(nodes);
		return new TrustResult(context, vector);
	}
}
