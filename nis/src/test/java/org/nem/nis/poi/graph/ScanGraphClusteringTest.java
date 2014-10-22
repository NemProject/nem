package org.nem.nis.poi.graph;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.math.Matrix;
import org.nem.core.model.primitive.*;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.poi.*;
import org.nem.nis.test.*;

import java.util.*;
import java.util.logging.Logger;

public abstract class ScanGraphClusteringTest {
	private static final Logger LOGGER = Logger.getLogger(ScanGraphClusteringTest.class.getName());
	private static final PoiOptions DEFAULT_OPTIONS = new PoiOptionsBuilder().create();

	/**
	 * Creates the GraphClusteringStrategy being tested.
	 *
	 * @return The GraphClusteringStrategy being tested
	 */
	protected abstract GraphClusteringStrategy createClusteringStrategy();

	//region unit tests with node similarity explicitly specified

	/**
	 * <pre>
	 *     0 - 1
	 * </pre>
	 */
	@Test
	public void networkWithTwoSimilarConnectedNodesCanBeClustered() {
		// Arrange:
		final TestContext context = new TestContext(this.createClusteringStrategy(), 2);
		context.setNeighborIds(0, Arrays.asList(0, 1));
		context.setNeighborIds(1, Arrays.asList(0, 1));
		context.makeAllSimilar();

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedOutliers = Arrays.asList(
				new Cluster(new NodeId(0)),
				new Cluster(new NodeId(1)));

		Assert.assertThat(result.getClusters().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEquivalent.equivalentTo(expectedOutliers));
	}

	/**
	 * <pre>
	 *     0 - 1
	 * </pre>
	 */
	@Test
	public void networkWithTwoDissimilarConnectedNodesCanBeClustered() {
		// Arrange:
		final TestContext context = new TestContext(this.createClusteringStrategy(), 2);
		context.setNeighborIds(0, Arrays.asList(0, 1));
		context.setNeighborIds(1, Arrays.asList(0, 1));

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedOutliers = Arrays.asList(
				new Cluster(new NodeId(0)),
				new Cluster(new NodeId(1)));

		Assert.assertThat(result.getClusters().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEquivalent.equivalentTo(expectedOutliers));
	}

	/**
	 * <pre>
	 *      0 - 1
	 *     / \ / \
	 *    2 - \ - 5
	 *     \ / \ /
	 *      3 - 4
	 * </pre>
	 */
	@Test
	public void fullyConnectedNetworkResultsInSingleCluster() {
		// Arrange:
		final TestContext context = new TestContext(this.createClusteringStrategy(), 6);
		context.setNeighborIds(0, Arrays.asList(0, 1, 2, 4));
		context.setNeighborIds(1, Arrays.asList(0, 1, 3, 5));
		context.setNeighborIds(2, Arrays.asList(0, 2, 3, 5));
		context.setNeighborIds(3, Arrays.asList(1, 2, 3, 4));
		context.setNeighborIds(4, Arrays.asList(0, 3, 4, 5));
		context.setNeighborIds(5, Arrays.asList(1, 2, 4, 5));
		context.makeAllSimilar();

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4, 5)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void networkWithTwoClustersAndDissimilarHubCanBeClustered() {
		// Arrange:
		final TestContext context = this.createContextForTwoClustersAndHub(0.0, 0.0);

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 2, 4)),
				new Cluster(new ClusterId(1), NisUtils.toNodeIdList(1, 3, 5)));
		final List<Cluster> expectedHubs = Arrays.asList(new Cluster(new NodeId(6)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs(), IsEqual.equalTo(expectedHubs));
		Assert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void networkWithTwoClustersAndPartiallyDissimilarHubIsActuallyTwoClusters() {
		// Assert:
		final TestContext context = this.createContextForTwoClustersAndHub(0.0, 1.0);

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 2, 4, 6)),
				new Cluster(new ClusterId(1), NisUtils.toNodeIdList(1, 3, 5)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void networkWithTwoClustersAndSimilarHubIsActuallyOneCluster() {
		// Arrange:
		final TestContext context = this.createContextForTwoClustersAndHub(1.0, 1.0);

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4, 5, 6)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));
	}

	/**
	 * <pre>
	 *     0             3
	 *    / \           / \
	 *   4 - 2  - 6 -  1 - 5
	 * </pre>
	 */
	private TestContext createContextForTwoClustersAndHub(final double similarity1, final double similarity2) {
		// Arrange: { 0, 2, 4 } { 1, 3, 5 } form clusters; 6 is the hub
		final TestContext context = new TestContext(this.createClusteringStrategy(), 7);
		context.setNeighborIds(0, Arrays.asList(0, 2, 4));
		context.setNeighborIds(2, Arrays.asList(0, 2, 4, 6));
		context.setNeighborIds(4, Arrays.asList(0, 2, 4));
		context.setNeighborIds(1, Arrays.asList(1, 3, 5, 6));
		context.setNeighborIds(3, Arrays.asList(1, 3, 5));
		context.setNeighborIds(5, Arrays.asList(1, 3, 5));
		context.setNeighborIds(6, Arrays.asList(1, 2, 6));
		context.makeAllSimilar();
		context.setSimilarity(6, 1, similarity1);
		context.setSimilarity(6, 2, similarity2);
		return context;
	}

	/**
	 * <pre>
	 *     0            3
	 *    / \          / \
	 *   4 - 2 - 6    1 - 5
	 * </pre>
	 */
	@Test
	public void networkWithTwoClustersAndOutlierCanBeClustered() {
		// Arrange: { 0, 2, 4 } { 1, 3, 5 } form clusters; 6 is an outlier
		final TestContext context = new TestContext(this.createClusteringStrategy(), 7);
		context.setNeighborIds(0, Arrays.asList(0, 2, 4));
		context.setNeighborIds(2, Arrays.asList(0, 2, 4, 6));
		context.setNeighborIds(4, Arrays.asList(0, 2, 4));
		context.setNeighborIds(1, Arrays.asList(1, 3, 5));
		context.setNeighborIds(3, Arrays.asList(1, 3, 5));
		context.setNeighborIds(5, Arrays.asList(1, 3, 5));
		context.setNeighborIds(6, Arrays.asList(2, 6));
		context.makeAllSimilar();
		context.setSimilarity(6, 2, 0.0);

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 2, 4)),
				new Cluster(new ClusterId(1), NisUtils.toNodeIdList(1, 3, 5)));
		final List<Cluster> expectedOutliers = Arrays.asList(new Cluster(new NodeId(6)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	/**
	 * <pre>
	 *    0  - 1
	 *    | \/ |
	 *    | /\ |
	 *    2 -  3   4
	 * </pre>
	 */
	@Test
	public void isolatedNodeIsDetectedAsAnOutlier() {
		// Arrange: { 0, 1, 2, 3 } form clusters; 4 is an isolated outlier
		final TestContext context = new TestContext(this.createClusteringStrategy(), 5);
		context.setNeighborIds(0, Arrays.asList(0, 1, 2, 3));
		context.setNeighborIds(1, Arrays.asList(0, 1, 2, 3));
		context.setNeighborIds(2, Arrays.asList(0, 1, 2, 3));
		context.setNeighborIds(3, Arrays.asList(0, 1, 2, 3));
		context.setNeighborIds(4, Arrays.asList(4));
		context.makeAllSimilar();

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));
		final List<Cluster> expectedOutliers = Arrays.asList(new Cluster(new NodeId(4)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	/**
	 * <pre>
	 *    0  - 1 - 4
	 *    | \/ |
	 *    | /\ |
	 *    2 -  3
	 * </pre>
	 */
	@Test
	public void dissimilarNodeConnectedToSingleClusterIsDetectedAsAnOutlier() {
		// Arrange: { 0, 1, 2, 3 } form clusters; 4 is an outlier connected to the cluster
		final TestContext context = new TestContext(this.createClusteringStrategy(), 5);
		context.setNeighborIds(0, Arrays.asList(0, 1, 2, 3));
		context.setNeighborIds(1, Arrays.asList(0, 1, 2, 3, 4));
		context.setNeighborIds(2, Arrays.asList(0, 1, 2, 3));
		context.setNeighborIds(3, Arrays.asList(0, 1, 2, 3));
		context.setNeighborIds(4, Arrays.asList(1, 4));
		context.makeAllSimilar();
		context.setSimilarity(4, 1, 0);

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));
		final List<Cluster> expectedOutliers = Arrays.asList(new Cluster(new NodeId(4)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	/**
	 * <pre>
	 *  4 - 0  - 2
	 *  |   | \/ |
	 *  |   | /\ |
	 *  |   1 -  3
	 *  |    \  /
	 *  \---- 5
	 * </pre>
	 */
	@Test
	public void nodeConnectedToOutlierAndClusterIsDetectedAsAnOutlier() {
		// Arrange: { 0, 1, 2, 3 } form clusters;
		// 4 is an outlier connected to the cluster
		// 5 is an outlier connected to (4) and the cluster (twice)
		final TestContext context = new TestContext(this.createClusteringStrategy(), 6);
		context.setNeighborIds(0, Arrays.asList(0, 1, 2, 3, 4));
		context.setNeighborIds(1, Arrays.asList(0, 1, 2, 3, 5));
		context.setNeighborIds(2, Arrays.asList(0, 1, 2, 3));
		context.setNeighborIds(3, Arrays.asList(0, 1, 2, 3, 5));
		context.setNeighborIds(4, Arrays.asList(5, 0, 4));
		context.setNeighborIds(5, Arrays.asList(4, 1, 3, 5));
		context.makeAllSimilar();
		context.setSimilarity(4, 0, 0);
		context.setSimilarity(4, 5, 1);
		context.setSimilarity(5, 1, 0);
		context.setSimilarity(5, 3, 0);

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));
		final List<Cluster> expectedOutliers = Arrays.asList(
				new Cluster(new NodeId(4)),
				new Cluster(new NodeId(5)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	/**
	 * <pre>
	 *    0  - 1
	 *    | \/ |
	 *    | /\ |
	 *    2 -  3 - 4 - 5
	 * </pre>
	 */
	@Test
	public void clusterCommunityDoesNotMergeWithNonCoreCommunities() {
		// Arrange:
		// { 0, 1, 2, 3, 4 } form a cluster
		// { 5 } is an outlier connected to 4 but not merged in because 4 is not core
		final TestContext context = new TestContext(this.createClusteringStrategy(), 6);
		context.setNeighborIds(0, Arrays.asList(0, 1, 2, 3));
		context.setNeighborIds(1, Arrays.asList(0, 1, 2, 3));
		context.setNeighborIds(2, Arrays.asList(0, 1, 2, 3));
		context.setNeighborIds(3, Arrays.asList(0, 1, 2, 3, 4));
		context.setNeighborIds(4, Arrays.asList(4, 5));
		context.setNeighborIds(5, Arrays.asList(4, 5));
		context.makeAllSimilar();

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4)));
		final List<Cluster> expectedOutliers = Arrays.asList(new Cluster(new NodeId(5)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	//endregion

	//region tests with real similarity calculations

	//region epsilon any

	@Test
	public void graphSingleNodeIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphType.GRAPH_SINGLE_NODE);
	}

	@Test
	public void graphTwoUnconnectedNodesIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphType.GRAPH_TWO_UNCONNECTED_NODES);
	}

	@Test
	public void graphTwoConnectedNodesIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphType.GRAPH_TWO_CONNECTED_NODES);
	}

	@Test
	public void graphLineStructureIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphType.GRAPH_LINE_STRUCTURE);
	}

	@Test
	public void graphLine6StructureIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphType.GRAPH_LINE6_STRUCTURE);
	}

	@Test
	public void graphRingStructureIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphType.GRAPH_RING_STRUCTURE);
	}

	@Test
	public void graphBoxTwoDiagonalsIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphType.GRAPH_BOX_TWO_DIAGONALS);
	}

	@Test
	public void graphBoxMajorDiagonalsIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphType.GRAPH_BOX_MAJOR_DIAGONAL);
	}

	@Test
	public void graphBoxMinorDiagonalsIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphType.GRAPH_BOX_MINOR_DIAGONAL);
	}

	@Test
	public void graphTreeStructureIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphType.GRAPH_TREE_STRUCTURE);
	}

	@Test
	public void graphDisconnectedBoxAndLIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphType.GRAPH_DISCONNECTED_BOX_AND_L);
	}

	//endregion

	//region epsilon default (0.40)

	@Test
	public void graphTwoClustersNoHubsNoOutliersIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphTypeEpsilon040.GRAPH_TWO_CLUSTERS_NO_HUBS_NO_OUTLIERS);
	}

	@Test
	public void graphTwoClustersOneHubThreeOutliersIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphTypeEpsilon040.GRAPH_TWO_CLUSTERS_ONE_HUB_THREE_OUTLIERS);
	}

	@Test
	public void graphThreeClustersTwoHubsThreeOutliersIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphTypeEpsilon040.GRAPH_THREE_CLUSTERS_TWO_HUBS_THREE_OUTLIERS);
	}

	//endregion

	//region epsilon custom (0.65)

	@Test
	public void graphCustomEpsilonOneClusterNoHubOneOutlierIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphTypeEpsilon065.GRAPH_ONE_CLUSTER_NO_HUB_ONE_OUTLIER);
	}

	@Test
	public void graphCustomEpsilonTwoClustersNoHubNoOutlierIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphTypeEpsilon065.GRAPH_TWO_CLUSTERS_NO_HUB_NO_OUTLIER);
	}

	@Test
	public void graphCustomEpsilonTwoClustersNoHubOneOutlierIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphTypeEpsilon065.GRAPH_TWO_CLUSTERS_NO_HUB_ONE_OUTLIER);
	}

	@Test
	public void graphCustomEpsilonTwoClustersOneHubNoOutlierIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphTypeEpsilon065.GRAPH_TWO_CLUSTERS_ONE_HUB_NO_OUTLIER);
	}

	@Test
	public void graphCustomEpsilonTwoClustersTwoHubsTwoOutliersIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphTypeEpsilon065.GRAPH_TWO_CLUSTERS_TWO_HUBS_TWO_OUTLIERS);
	}

	//endregion

	//endregion

	private void assertGraphIsClusteredCorrectly(final GraphType graphType) {
		// Assert:
		this.assertGraphIsClusteredCorrectly(
				OutlinkMatrixFactory.create(graphType),
				IdealizedClusterFactory.create(graphType),
				DEFAULT_OPTIONS.getEpsilonClusteringValue());
	}

	private void assertGraphIsClusteredCorrectly(final GraphTypeEpsilon040 graphType) {
		// Assert:
		this.assertGraphIsClusteredCorrectly(
				OutlinkMatrixFactory.create(graphType),
				IdealizedClusterFactory.create(graphType),
				DEFAULT_OPTIONS.getEpsilonClusteringValue());
	}

	private void assertGraphIsClusteredCorrectly(final GraphTypeEpsilon065 graphType) {
		// Assert:
		this.assertGraphIsClusteredCorrectly(
				OutlinkMatrixFactory.create(graphType),
				IdealizedClusterFactory.create(graphType),
				0.65);
	}

	private void assertGraphIsClusteredCorrectly(
			final Matrix outlinkMatrix,
			final ClusteringResult expectedResult,
			final double epsilon) {
		// Act:
		final ClusteringResult result = this.calculateClusteringResult(this.createClusteringStrategy(), outlinkMatrix, epsilon);
		this.logClusteringResult(result);

		// Assert:
		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedResult.getClusters()));
		Assert.assertThat(result.getHubs(), IsEquivalent.equivalentTo(expectedResult.getHubs()));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedResult.getOutliers()));
	}

	private ClusteringResult calculateClusteringResult(
			final GraphClusteringStrategy graphClusteringStrategy,
			final Matrix outlinkMatrix,
			final double epsilon) {
		final NodeNeighborMap nodeNeighborMap = new NodeNeighborMap(outlinkMatrix);
		final SimilarityStrategy strategy = new DefaultSimilarityStrategy(nodeNeighborMap);
		final Neighborhood neighborhood = new Neighborhood(nodeNeighborMap, strategy, DEFAULT_OPTIONS.getMuClusteringValue(), epsilon);
		return graphClusteringStrategy.cluster(neighborhood);
	}

	/**
	 * Log result of clustering.
	 *
	 * @param result The clustering result.
	 */
	private void logClusteringResult(final ClusteringResult result) {
		LOGGER.info("Clusters:");
		result.getClusters().stream().forEach(c -> LOGGER.info(c.toString()));
		LOGGER.info("Hubs:");
		result.getHubs().stream().forEach(c -> LOGGER.info(c.toString()));
		LOGGER.info("Outliers:");
		result.getOutliers().stream().forEach(c -> LOGGER.info(c.toString()));
	}

	private static class TestContext {
		private final NeighborhoodRepository repository = Mockito.mock(NodeNeighborMap.class);
		private final SimilarityStrategy similarityStrategy = Mockito.mock(SimilarityStrategy.class);
		private final Neighborhood neighborhood = NisUtils.createNeighborhood(this.repository, this.similarityStrategy);
		private final GraphClusteringStrategy clusteringStrategy;

		public TestContext(final GraphClusteringStrategy clusteringStrategy, final int neighborhoodSize) {
			this.clusteringStrategy = clusteringStrategy;
			Mockito.when(this.repository.getLogicalSize()).thenReturn(neighborhoodSize);
		}

		public void setNeighborIds(final int id, final List<Integer> neighborIds) {
			Mockito.when(this.repository.getNeighbors(new NodeId(id)))
					.thenReturn(new NodeNeighbors(neighborIds.stream().map(NodeId::new).sorted().toArray(NodeId[]::new)));
		}

		public void setSimilarity(final int id1, final int id2, final double similarity) {
			Mockito.when(this.similarityStrategy.calculateSimilarity(new NodeId(id1), new NodeId(id2))).thenReturn(similarity);
			Mockito.when(this.similarityStrategy.calculateSimilarity(new NodeId(id2), new NodeId(id1))).thenReturn(similarity);
		}

		public void makeAllSimilar() {
			Mockito.when(this.similarityStrategy.calculateSimilarity(Mockito.any(), Mockito.any())).thenReturn(1.0);
		}
	}
}
