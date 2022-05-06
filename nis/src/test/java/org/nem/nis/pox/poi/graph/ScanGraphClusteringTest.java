package org.nem.nis.pox.poi.graph;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.math.Matrix;
import org.nem.core.model.primitive.*;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.pox.poi.*;
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

	// region unit tests with node similarity explicitly specified

	/**
	 * <pre>
	 * 0 - 1
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
		final List<Cluster> expectedOutliers = Arrays.asList(new Cluster(new NodeId(0)), new Cluster(new NodeId(1)));

		MatcherAssert.assertThat(result.getClusters().isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result.getOutliers(), IsEquivalent.equivalentTo(expectedOutliers));
	}

	/**
	 * <pre>
	 * 0 - 1
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
		final List<Cluster> expectedOutliers = Arrays.asList(new Cluster(new NodeId(0)), new Cluster(new NodeId(1)));

		MatcherAssert.assertThat(result.getClusters().isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result.getOutliers(), IsEquivalent.equivalentTo(expectedOutliers));
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
		final List<Cluster> expectedClusters = Collections
				.singletonList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4, 5)));

		MatcherAssert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		MatcherAssert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void networkWithTwoClustersAndDissimilarHubCanBeClustered() {
		// Arrange:
		final TestContext context = this.createContextForTwoClustersAndHub(0.0, 0.0);

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 2, 4, 6)),
				new Cluster(new ClusterId(1), NisUtils.toNodeIdList(1, 3, 5, 7)));
		final List<Cluster> expectedHubs = Collections.singletonList(new Cluster(new NodeId(8)));

		MatcherAssert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		MatcherAssert.assertThat(result.getHubs(), IsEqual.equalTo(expectedHubs));
		MatcherAssert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void networkWithTwoClustersAndPartiallyDissimilarHubIsActuallyTwoClusters() {
		// Assert:
		final TestContext context = this.createContextForTwoClustersAndHub(0.0, 1.0);

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 2, 4, 6, 8)),
				new Cluster(new ClusterId(1), NisUtils.toNodeIdList(1, 3, 5, 7)));

		MatcherAssert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		MatcherAssert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void networkWithTwoClustersAndSimilarHubIsActuallyOneCluster() {
		// Arrange:
		final TestContext context = this.createContextForTwoClustersAndHub(1.0, 1.0);

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Collections
				.singletonList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4, 5, 6, 7, 8)));

		MatcherAssert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		MatcherAssert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));
	}

	/**
	 * <pre>
	 * 0 - - 2       3 - - 5
	 * | \  /|       | \  /|
	 * |  \/ |       |  \/ |
	 * | / \ |       | / \ |
	 * 6 - - 4 - 8 - 1 - - 7
	 * </pre>
	 */
	private TestContext createContextForTwoClustersAndHub(final double similarity1, final double similarity2) {
		// Arrange: { 0, 2, 4, 6 } { 1, 3, 5, 7 } form clusters; 8 is the hub
		final TestContext context = new TestContext(this.createClusteringStrategy(), 9);
		context.setNeighborIds(0, Arrays.asList(0, 2, 4, 6));
		context.setNeighborIds(2, Arrays.asList(0, 2, 4, 6));
		context.setNeighborIds(4, Arrays.asList(0, 2, 4, 6, 8));
		context.setNeighborIds(6, Arrays.asList(0, 2, 4, 6));
		context.setNeighborIds(1, Arrays.asList(1, 3, 5, 7, 8));
		context.setNeighborIds(3, Arrays.asList(1, 3, 5, 7));
		context.setNeighborIds(5, Arrays.asList(1, 3, 5, 7));
		context.setNeighborIds(7, Arrays.asList(1, 3, 5, 7));
		context.setNeighborIds(8, Arrays.asList(1, 4, 8));
		context.makeAllSimilar();
		context.setSimilarity(8, 1, similarity1);
		context.setSimilarity(8, 4, similarity2);
		return context;
	}

	/**
	 * <pre>
	 * 0 - - 2       3 - - 5
	 * | \  /|       | \  /|
	 * |  \/ |       |  \/ |
	 * | / \ |       | / \ |
	 * 6 - - 4 - 8   1 - - 7
	 * </pre>
	 */
	@Test
	public void networkWithTwoClustersAndOutlierCanBeClustered() {
		// Arrange: { 0, 2, 4, 6 } { 1, 3, 5, 7 } form clusters; 8 is an outlier
		final TestContext context = new TestContext(this.createClusteringStrategy(), 9);
		context.setNeighborIds(0, Arrays.asList(0, 2, 4, 6));
		context.setNeighborIds(2, Arrays.asList(0, 2, 4, 6));
		context.setNeighborIds(4, Arrays.asList(0, 2, 4, 6, 8));
		context.setNeighborIds(6, Arrays.asList(0, 2, 4, 6));
		context.setNeighborIds(1, Arrays.asList(1, 3, 5, 7));
		context.setNeighborIds(3, Arrays.asList(1, 3, 5, 7));
		context.setNeighborIds(5, Arrays.asList(1, 3, 5, 7));
		context.setNeighborIds(7, Arrays.asList(1, 3, 5, 7));
		context.setNeighborIds(8, Arrays.asList(4, 8));
		context.makeAllSimilar();
		context.setSimilarity(8, 4, 0.0);

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 2, 4, 6)),
				new Cluster(new ClusterId(1), NisUtils.toNodeIdList(1, 3, 5, 7)));
		final List<Cluster> expectedOutliers = Collections.singletonList(new Cluster(new NodeId(8)));

		MatcherAssert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		MatcherAssert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
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
		context.setNeighborIds(4, Collections.singletonList(4));
		context.makeAllSimilar();

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Collections.singletonList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));
		final List<Cluster> expectedOutliers = Collections.singletonList(new Cluster(new NodeId(4)));

		MatcherAssert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		MatcherAssert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
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
		final List<Cluster> expectedClusters = Collections.singletonList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));
		final List<Cluster> expectedOutliers = Collections.singletonList(new Cluster(new NodeId(4)));

		MatcherAssert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		MatcherAssert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
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
		final List<Cluster> expectedClusters = Collections.singletonList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));
		final List<Cluster> expectedOutliers = Arrays.asList(new Cluster(new NodeId(4)), new Cluster(new NodeId(5)));

		MatcherAssert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		MatcherAssert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	/**
	 * This test is designed to require a cluster merge when clustering with FastScanClusteringStrategy.
	 *
	 * <pre>
	 *    1 -- 0 -- 8 -- 4 -- 5
	 *    | \/ |     <\  | \/ |
	 *    | /\ |       \ | /\ |
	 *    2 -- 3         7 -- 6
	 * </pre>
	 */
	@Test
	public void complexGraphIsMergedAsExpected() {
		// Arrange: { 0, 1, 2, 3, 4, 5, 6, 7, 8 } form a single cluster (this requires a merge of two clusters in the
		// FastScanClusteringStrategy)
		// Cluster pivoted at [0] and cluster pivoted at [4] are merged
		final TestContext context = new TestContext(this.createClusteringStrategy(), 9);

		context.setNeighborIds(0, Arrays.asList(0, 1, 2, 3, 8));
		context.setNeighborIds(1, Arrays.asList(0, 1, 2, 3));
		context.setNeighborIds(2, Arrays.asList(0, 1, 2, 3));
		context.setNeighborIds(3, Arrays.asList(0, 1, 2, 3));
		context.setNeighborIds(4, Arrays.asList(4, 5, 6, 7, 8));
		context.setNeighborIds(5, Arrays.asList(4, 5, 6, 7));
		context.setNeighborIds(6, Arrays.asList(4, 5, 6, 7));
		context.setNeighborIds(7, Arrays.asList(4, 5, 6, 7, 8));
		context.setNeighborIds(8, Arrays.asList(0, 4, 8)); // 7 is not here on purpose, but this merges even with 7 here

		context.makeAllSimilar();

		context.setSimilarity(8, 4, 0); // Force creation of a new cluster around 4

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Collections
				.singletonList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4, 5, 6, 7, 8)));

		MatcherAssert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		MatcherAssert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));
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
		final List<Cluster> expectedClusters = Collections
				.singletonList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4)));
		final List<Cluster> expectedOutliers = Collections.singletonList(new Cluster(new NodeId(5)));

		MatcherAssert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		MatcherAssert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	// endregion

	// region real similarity - epsilon any

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
		// Assert (lines are never clusters):
		this.assertGraphIsClusteredCorrectly(GraphType.GRAPH_LINE_STRUCTURE);
	}

	@Test
	public void graphLine6StructureIsClusteredAsExpected() {
		// Assert (lines are never clusters):
		this.assertGraphIsClusteredCorrectly(GraphType.GRAPH_LINE6_STRUCTURE);
	}

	@Test
	public void graphRingStructureIsClusteredAsExpected() {
		// Assert (rings without additional connections are never clusters):
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
	public void graphDisconnectedBoxWithDiagonalsAndCrossIsClusteredAsExpected() {
		// Assert:
		this.assertGraphIsClusteredCorrectly(GraphType.GRAPH_DISCONNECTED_BOX_WITH_DIAGONAL_AND_CROSS);
	}

	// endregion

	// region real similarity - epsilon default (0.40)

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

	// endregion

	// region real similarity - epsilon custom (0.65)

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

	// endregion

	private void assertGraphIsClusteredCorrectly(final GraphType graphType) {
		// Assert:
		this.assertGraphIsClusteredCorrectly(OutlinkMatrixFactory.create(graphType), IdealizedClusterFactory.create(graphType),
				DEFAULT_OPTIONS.getEpsilonClusteringValue());
	}

	private void assertGraphIsClusteredCorrectly(final GraphTypeEpsilon040 graphType) {
		// Assert:
		this.assertGraphIsClusteredCorrectly(OutlinkMatrixFactory.create(graphType), IdealizedClusterFactory.create(graphType), 0.4);
	}

	private void assertGraphIsClusteredCorrectly(final GraphTypeEpsilon065 graphType) {
		// Assert:
		this.assertGraphIsClusteredCorrectly(OutlinkMatrixFactory.create(graphType), IdealizedClusterFactory.create(graphType), 0.65);
	}

	private void assertGraphIsClusteredCorrectly(final Matrix outlinkMatrix, final ClusteringResult expectedResult, final double epsilon) {
		// Act:
		final ClusteringResult result = this.calculateClusteringResult(this.createClusteringStrategy(), outlinkMatrix, epsilon);
		this.logClusteringResult(result);

		// Assert:
		MatcherAssert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedResult.getClusters()));
		MatcherAssert.assertThat(result.getHubs(), IsEquivalent.equivalentTo(expectedResult.getHubs()));
		MatcherAssert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedResult.getOutliers()));
	}

	private ClusteringResult calculateClusteringResult(final GraphClusteringStrategy graphClusteringStrategy, final Matrix outlinkMatrix,
			final double epsilon) {
		final NodeNeighborMap nodeNeighborMap = new NodeNeighborMap(outlinkMatrix);
		final SimilarityStrategy strategy = new StructuralSimilarityStrategy(nodeNeighborMap);
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
