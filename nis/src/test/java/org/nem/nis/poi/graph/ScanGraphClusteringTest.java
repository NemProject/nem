package org.nem.nis.poi.graph;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.math.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.test.*;

import java.util.*;
import java.util.logging.Logger;

public abstract class ScanGraphClusteringTest {
	private static final Logger LOGGER = Logger.getLogger(ScanGraphClusteringTest.class.getName());

	/**
	 * Creates the GraphClusteringStrategy being tested.
	 *
	 * @return The GraphClusteringStrategy being tested
	 */
	protected abstract GraphClusteringStrategy createClusteringStrategy();

	// TODO 20141001 J-M please review the first group of tests for correctness ... i added a few

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

	/**
	 * <pre>
	 * First Graph: 0 --- 1
	 *              | \   |
	 *              |   \ |
	 *              3 --- 2
	 * <br/>
	 * Similarities:
	 *        sim(0,1) = (1+1+1)/sqrt(4*3) = sqrt(3/4)
	 *                 = sim(1,0)
	 *                 = sim(0,3) = sim(3,0)
	 *                 = sim(2,3) = sim(3,2)
	 *                 = sim(1,2) = sim(2,1) > EPSILON
	 *        sim(0,2) = (2+1+1)/sqrt(4*4) = 1
	 *                 = sim(2,0) > EPSILON
	 *        sim(1,3) = (2+0+0)/sqrt(3*3) = 2/3
	 *                 = sim(3,1) < EPSILON
	 * <br/>
	 * Communities in form (node id, epsilon neighbors, non-epsilon neighbors):
	 *        com(0) = (0, {1,2,3}, {})
	 *        com(1) = (1, {0,2}, {})
	 *        com(2) = (2, {0,1,3}, {})
	 *        com(3) = (3, {0,2}, {})
	 * <br/>
	 * Expected: cluster {0, 1, 2, 3}, no hubs, no outliers
	 * <br/>
	 * First Graph: 0 --- 1 (essentially the same graph but we start scanning from a different node)
	 * (isomorphic) |   / |
	 *              | /   |
	 *              3 --- 2
	 * <br/>
	 * Similarities:
	 *        sim(0,1) = (1+1+1)/sqrt(3*4) = sqrt(3/4)
	 *                 = sim(1,0)
	 *                 = sim(0,3) = sim(3,0)
	 *                 = sim(2,3) = sim(3,2)
	 *                 = sim(1,2) = sim(2,1) > EPSILON
	 *        sim(0,2) = (2+0+0)/sqrt(3*3) = 2/3
	 *                 = sim(2,0) < EPSILON
	 *        sim(1,3) = (2+1+1)/sqrt(4*4) = 1
	 *                 = sim(3,1) > EPSILON
	 * <br/>
	 * Communities in form (node id, epsilon neighbors, non-epsilon neighbors):
	 *        com(0) = (0, {1,3}, {})
	 *        com(1) = (1, {0,2,3}, {})
	 *        com(2) = (2, {1,3}, {})
	 *        com(3) = (3, {0,1,2}, {})
	 * <br/>
	 * Expected: cluster {0, 1, 2, 3}, no hubs, no outliers
	 * </pre>
	 */
	@Test
	public void firstGraphIsClusteredAsExpected() {
		// Arrange:
		// Using dense matrix for easier debugging
		final DenseMatrix outlinkMatrix = new DenseMatrix(4, 4);
		outlinkMatrix.setAt(1, 0, 1);
		outlinkMatrix.setAt(2, 0, 1);
		outlinkMatrix.setAt(3, 0, 1);
		outlinkMatrix.setAt(0, 1, 1);
		outlinkMatrix.setAt(2, 1, 1);
		outlinkMatrix.setAt(0, 2, 1);
		outlinkMatrix.setAt(1, 2, 1);
		outlinkMatrix.setAt(3, 2, 1);
		outlinkMatrix.setAt(0, 3, 1);
		outlinkMatrix.setAt(2, 3, 1);

		// Act:
		ClusteringResult result = calculateClusteringResult(this.createClusteringStrategy(), outlinkMatrix);
		logClusteringResult(result);

		// Assert:
		List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));

		final DenseMatrix outlinkMatrix2 = new DenseMatrix(4, 4);
		outlinkMatrix2.setAt(1, 0, 1);
		outlinkMatrix2.setAt(3, 0, 1);
		outlinkMatrix2.setAt(0, 1, 1);
		outlinkMatrix2.setAt(2, 1, 1);
		outlinkMatrix2.setAt(3, 1, 1);
		outlinkMatrix2.setAt(1, 2, 1);
		outlinkMatrix2.setAt(3, 2, 1);
		outlinkMatrix2.setAt(0, 3, 1);
		outlinkMatrix2.setAt(1, 3, 1);
		outlinkMatrix2.setAt(2, 3, 1);

		// Act:
		result = calculateClusteringResult(this.createClusteringStrategy(), outlinkMatrix);
		logClusteringResult(result);

		// Assert:
		expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));
	}

	/**
	 * <pre>
	 *                   _____                         1-----2--3
	 *                 /      \                         \    | /|\
	 * Second graph:  0----1   5   is equivalent to      \   |/ | \
	 *                |\    \ / \                         \  5  /  \
	 *                | \    2  /                          \ | /   |
	 *                |  \  /  /                            \|/    |
	 *                4----3--/                              0-----4
	 * <br/>
	 * Similarities:
	 *        sim(0,1) = (0+1+1)/sqrt(5*3) = 2/sqrt(15)
	 *                 = sim(1,0) < EPSILON
	 *        sim(0,3) = (2+1+1)/sqrt(5*5) = 4/5
	 *                 = sim(3,0) > EPSILON
	 *        sim(0,4) = (1+1+1)/sqrt(5*3) = 3/sqrt(15)
	 *                 = sim(4,0) > EPSILON
	 *        sim(0,5) = (1+1+1)/sqrt(5*4) = 3/sqrt(20)
	 *                 = sim(5,0) < EPSILON
	 *        sim(1,2) = (0+1+1)/sqrt(3*4) = 2/sqrt(12)
	 *                 = sim(2,1) < EPSILON
	 *        sim(2,3) = (1+1+1)/sqrt(4*5) = 3/sqrt(20)
	 *                 = sim(3,2) < EPSILON
	 *        sim(2,5) = (1+1+1)/sqrt(4*4) = 3/4
	 *                 = sim(5,2) > EPSILON
	 *        sim(3,4) = (1+1+1)/sqrt(5*3) = 3/sqrt(15)
	 *                 = sim(4,3) > EPSILON
	 *        sim(3,5) = (2+1+1)/sqrt(5*4) = 4/sqrt(20)
	 *                 = sim(5,3) > EPSILON
	 * <br/>
	 * Communities in form (node id, similar neighbors, dissimilar neighbors):
	 *         com(0) = (0, {3,4}, {1,5})
	 *         com(1) = (1, {}, {0,2})
	 *         com(2) = (2, {5}, {1,3})
	 *         com(3) = (3, {0,4,5}, {2})
	 *         com(4) = (4, {0,3}, {})
	 *         com(5) = (5, {2,3}, {0})
	 * <br/>
	 * Expected: cluster {0,2,3,4,5}, no hubs, one outlier {1}
	 * </pre>
	 */
	@Test
	public void secondGraphIsClusteredAsExpected() {
		// Arrange:
		// Using dense matrix for easier debugging
		final DenseMatrix outlinkMatrix = new DenseMatrix(6, 6);
		outlinkMatrix.setAt(1, 0, 1);
		outlinkMatrix.setAt(3, 0, 1);
		outlinkMatrix.setAt(4, 0, 1);
		outlinkMatrix.setAt(5, 0, 1);
		outlinkMatrix.setAt(0, 1, 1);
		outlinkMatrix.setAt(2, 1, 1);
		outlinkMatrix.setAt(1, 2, 1);
		outlinkMatrix.setAt(3, 2, 1);
		outlinkMatrix.setAt(5, 2, 1);
		outlinkMatrix.setAt(0, 3, 1);
		outlinkMatrix.setAt(2, 3, 1);
		outlinkMatrix.setAt(4, 3, 1);
		outlinkMatrix.setAt(5, 3, 1);
		outlinkMatrix.setAt(0, 4, 1);
		outlinkMatrix.setAt(3, 4, 1);
		outlinkMatrix.setAt(0, 5, 1);
		outlinkMatrix.setAt(2, 5, 1);
		outlinkMatrix.setAt(3, 5, 1);

		// Act:
		final ClusteringResult result = calculateClusteringResult(this.createClusteringStrategy(), outlinkMatrix);
		logClusteringResult(result);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 2, 3, 4, 5)));
		final List<Cluster> expectedOutliers = Arrays.asList(new Cluster(new NodeId(1)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	@Test
	public void thirdGraphIsClusteredAsExpected() {
		// Assert:
		assertGraphIsClusteredCorrectly(GraphType.GRAPH_TWO_CLUSTERS_NO_HUB_ONE_OUTLIER);
	}

	/**
	 * <pre>
	 * Graph:         0
	 *               / \
	 *              /   \
	 *             1-----2-----7
	 *                   |
	 *                   |
	 *                   3
	 *                   |
	 *                   |
	 *                   4
	 *                  / \
	 *                 /   \
	 *                5-----6
	 * <br/>
	 * Expected: clusters {0,1,2} and {4,5,6}, one hub {3}, one outlier {7}
	 * </pre>
	 */
	@Test
	public void forthGraphIsClusteredAsExpected() {
		// Arrange:
		final DenseMatrix outlinkMatrix = new DenseMatrix(8, 8);
		outlinkMatrix.setAt(1, 0, 1);
		outlinkMatrix.setAt(2, 0, 1);
		outlinkMatrix.setAt(0, 1, 1);
		outlinkMatrix.setAt(2, 1, 1);
		outlinkMatrix.setAt(0, 2, 1);
		outlinkMatrix.setAt(1, 2, 1);
		outlinkMatrix.setAt(3, 2, 1);
		outlinkMatrix.setAt(7, 2, 1);
		outlinkMatrix.setAt(2, 3, 1);
		outlinkMatrix.setAt(4, 3, 1);
		outlinkMatrix.setAt(3, 4, 1);
		outlinkMatrix.setAt(5, 4, 1);
		outlinkMatrix.setAt(6, 4, 1);
		outlinkMatrix.setAt(4, 5, 1);
		outlinkMatrix.setAt(6, 6, 1);
		outlinkMatrix.setAt(4, 6, 1);
		outlinkMatrix.setAt(5, 6, 1);
		outlinkMatrix.setAt(2, 7, 1);

		outlinkMatrix.removeNegatives(); //shouldn't make a difference either way

		// Act:
		final ClusteringResult result = calculateClusteringResult(this.createClusteringStrategy(), outlinkMatrix);
		logClusteringResult(result);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2)),
				new Cluster(new ClusterId(4), NisUtils.toNodeIdList(4, 5, 6)));
		final List<Cluster> expectedHubs = Arrays.asList(new Cluster(new NodeId(3)));
		final List<Cluster> expectedOutliers = Arrays.asList(new Cluster(new NodeId(7)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs(), IsEquivalent.equivalentTo(expectedHubs));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	/**
	 * <pre>
	 * Graph:      0-----1
	 *             |     |
	 *             |     |
	 *             3-----2
	 * <br/>
	 *                4
	 *                |
	 *                |
	 *                5-----6
	 * <br/>
	 * Expected: clusters {0,1,2} and {4,5,6}
	 * </pre>
	 */
	@Test
	public void fifthGraphIsClusteredAsExpected() {
		// Arrange:
		final DenseMatrix outlinkMatrix = new DenseMatrix(7, 7);
		outlinkMatrix.setAt(1, 0, 1);
		outlinkMatrix.setAt(2, 1, 1);
		outlinkMatrix.setAt(3, 2, 1);
		outlinkMatrix.setAt(0, 3, 1);

		outlinkMatrix.setAt(5, 4, 1);
		outlinkMatrix.setAt(6, 5, 1);

		outlinkMatrix.removeNegatives(); //shouldn't make a difference either way

		// Act:
		final ClusteringResult result = calculateClusteringResult(this.createClusteringStrategy(), outlinkMatrix);
		logClusteringResult(result);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)),
				new Cluster(new ClusterId(5), NisUtils.toNodeIdList(4, 5, 6)));
		final List<Cluster> expectedHubs = Arrays.asList();
		final List<Cluster> expectedOutliers = Arrays.asList();

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs(), IsEquivalent.equivalentTo(expectedHubs));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	/**
	 * <pre>
	 * Graph:      0
	 *            /|\
	 *           / | \
	 *          /  |  \
	 *         1   2   3
	 * <br/>
	 * Expected: clusters: {0,1,2,3}
	 *           hubs    : none
	 *           outliers: none
	 * </pre>
	 */
	@Test
	public void sixthGraphIsClusteredAsExpected() {
		// Arrange:
		final DenseMatrix outlinkMatrix = new DenseMatrix(4, 4);
		outlinkMatrix.setAt(1, 0, 1);
		outlinkMatrix.setAt(2, 0, 1);
		outlinkMatrix.setAt(3, 0, 1);

		outlinkMatrix.removeNegatives(); //shouldn't make a difference either way

		// Act:
		final ClusteringResult result = calculateClusteringResult(this.createClusteringStrategy(), outlinkMatrix);
		logClusteringResult(result);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));
		final List<Cluster> expectedHubs = Arrays.asList();
		final List<Cluster> expectedOutliers = Arrays.asList();

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs(), IsEquivalent.equivalentTo(expectedHubs));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	/**
	 * <pre>
	 * Graph:      0---1---2---3---4---5
	 * </pre>
	 * Expected: clusters: {0,1,2,3,4,5}
	 * hubs    : none
	 * outliers: none
	 */
	@Test
	public void seventhGraphIsClusteredAsExpected() {
		// Arrange:
		final DenseMatrix outlinkMatrix = new DenseMatrix(6, 6);
		outlinkMatrix.setAt(1, 0, 1);
		outlinkMatrix.setAt(2, 1, 1);
		outlinkMatrix.setAt(3, 2, 1);
		outlinkMatrix.setAt(4, 3, 1);
		outlinkMatrix.setAt(5, 4, 1);

		outlinkMatrix.removeNegatives(); //shouldn't make a difference either way

		// Act:
		final ClusteringResult result = calculateClusteringResult(this.createClusteringStrategy(), outlinkMatrix);
		logClusteringResult(result);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(new Cluster(new ClusterId(1), NisUtils.toNodeIdList(0, 1, 2, 3, 4, 5)));
		final List<Cluster> expectedHubs = Arrays.asList();
		final List<Cluster> expectedOutliers = Arrays.asList();

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs(), IsEquivalent.equivalentTo(expectedHubs));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	@Test
	public void eighthGraphIsClusteredAsExpected() {
		// Assert:
		assertGraphIsClusteredCorrectly(GraphType.GRAPH_LINE_STRUCTURE);
	}

	private void assertGraphIsClusteredCorrectly(final GraphType graphType) {
		// Arrange:
		final Matrix outlinkMatrix = OutlinkMatrixFactory.create(graphType);

		// Act:
		final ClusteringResult result = calculateClusteringResult(this.createClusteringStrategy(), outlinkMatrix);
		logClusteringResult(result);

		// Assert:
		final ClusteringResult expectedResult = IdealizedClusterFactory.create(graphType);
		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedResult.getClusters()));
		Assert.assertThat(result.getHubs(), IsEquivalent.equivalentTo(expectedResult.getHubs()));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedResult.getOutliers()));
	}

	private ClusteringResult calculateClusteringResult(final GraphClusteringStrategy graphClusteringStrategy, final Matrix outlinkMatrix) {
		final NodeNeighborMap nodeNeighborMap = new NodeNeighborMap(outlinkMatrix);
		final SimilarityStrategy strategy = new DefaultSimilarityStrategy(nodeNeighborMap);
		final Neighborhood neighborhood = new Neighborhood(nodeNeighborMap, strategy);
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
		private final Neighborhood neighborhood = new Neighborhood(this.repository, this.similarityStrategy);
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
