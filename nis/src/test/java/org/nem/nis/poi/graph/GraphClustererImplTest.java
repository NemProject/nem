package org.nem.nis.poi.graph;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.math.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.logging.Logger;

public class GraphClustererImplTest {
	private static final Logger LOGGER = Logger.getLogger(GraphClustererImplTest.class.getName());

	@Test
	public void networkWithTwoSimilarConnectedNodesCanBeClusteredWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			networkWithTwoSimilarConnectedNodesCanBeClustered(clusteringStrategy);
		}
	}
	
	public void networkWithTwoSimilarConnectedNodesCanBeClustered(final GraphClusteringStrategy clusteringStrategy) {
		// Arrange:
		final TestContext context = new TestContext(clusteringStrategy, 2);
		context.setNeighborIds(0, Arrays.asList(0,1));
		context.setNeighborIds(1, Arrays.asList(0,1));
		context.makeAllSimilar();

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedOutliers = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0)),
				new Cluster(new ClusterId(1), NisUtils.toNodeIdList(1)));

		Assert.assertThat(result.getClusters().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEquivalent.equivalentTo(expectedOutliers));
	}

	@Test
	public void networkWithTwoDissimilarConnectedNodesCanBeClusteredWithAllClusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			networkWithTwoDissimilarConnectedNodesCanBeClustered(clusteringStrategy);
		}
	}
	
	public void networkWithTwoDissimilarConnectedNodesCanBeClustered(final GraphClusteringStrategy clusteringStrategy) {
		// Arrange:
		final TestContext context = new TestContext(clusteringStrategy, 2);
		context.setNeighborIds(0, Arrays.asList(0,1));
		context.setNeighborIds(1, Arrays.asList(1));

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedOutliers = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0)),
				new Cluster(new ClusterId(1), NisUtils.toNodeIdList(1)));

		Assert.assertThat(result.getClusters().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEquivalent.equivalentTo(expectedOutliers));
	}

	@Test
	public void fullyConnectedNetworkResultsInSingleClusterWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			fullyConnectedNetworkResultsInSingleCluster(clusteringStrategy);
		}
	}
	
	public void fullyConnectedNetworkResultsInSingleCluster(final GraphClusteringStrategy clusteringStrategy) {
		// Arrange:
		final TestContext context = new TestContext(clusteringStrategy, 5);
		context.setNeighborIds(0, Arrays.asList(0, 1, 2, 3, 4));
		context.setNeighborIds(1, Arrays.asList(0, 1, 2, 3, 4));
		context.setNeighborIds(2, Arrays.asList(0, 1, 2, 3, 4));
		context.setNeighborIds(3, Arrays.asList(0, 1, 2, 3, 4));
		context.setNeighborIds(4, Arrays.asList(0, 1, 2, 3, 4));
		context.makeAllSimilar();

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void networkWithTwoClustersAndDissimilarHubCanBeClusteredWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			networkWithTwoClustersAndDissimilarHubCanBeClustered(clusteringStrategy);
		}
	}
	
	public void networkWithTwoClustersAndDissimilarHubCanBeClustered(final GraphClusteringStrategy clusteringStrategy) {
		// Arrange: { 0, 2, 4 } { 1, 3, 5 } form clusters; 6 is the hub
		final TestContext context = new TestContext(clusteringStrategy, 7);
		context.setNeighborIds(0, Arrays.asList(0, 2, 4));
		context.setNeighborIds(2, Arrays.asList(0, 2, 4, 6));
		context.setNeighborIds(4, Arrays.asList(0, 2, 4));
		context.setNeighborIds(1, Arrays.asList(1, 3, 5, 6));
		context.setNeighborIds(3, Arrays.asList(1, 3, 5));
		context.setNeighborIds(5, Arrays.asList(1, 3, 5));
		context.setNeighborIds(6, Arrays.asList(1, 2, 6));
		context.makeAllSimilar();
		context.setSimilarity(6, 1, 0.0);
		context.setSimilarity(6, 2, 0.0);

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 2, 4)),
				new Cluster(new ClusterId(3), NisUtils.toNodeIdList(1, 3, 5)));
		final List<Cluster> expectedHubs = Arrays.asList(
				new Cluster(new ClusterId(6), NisUtils.toNodeIdList(6)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs(), IsEqual.equalTo(expectedHubs));
		Assert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void networkWithTwoClustersAndPartiallyDissimilarHubCanBeClusteredWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			networkWithTwoClustersAndPartiallyDissimilarHubCanBeClustered(clusteringStrategy);
		}
	}
	
	public void networkWithTwoClustersAndPartiallyDissimilarHubCanBeClustered(final GraphClusteringStrategy clusteringStrategy) {
		// TODO: not sure if this test can actually happen (where there is a uni-directional edge)
		// Arrange: { 0, 2, 4 } { 1, 3, 5 } form clusters; 6 is the hub
		final TestContext context = new TestContext(clusteringStrategy, 7);
		context.setNeighborIds(0, Arrays.asList(0, 2, 4));
		context.setNeighborIds(2, Arrays.asList(0, 2, 4));
		context.setNeighborIds(4, Arrays.asList(0, 2, 4));
		context.setNeighborIds(1, Arrays.asList(1, 3, 5, 6));
		context.setNeighborIds(3, Arrays.asList(1, 3, 5));
		context.setNeighborIds(5, Arrays.asList(1, 3, 5));
		context.setNeighborIds(6, Arrays.asList(1, 2, 6));
		context.makeAllSimilar();
		context.setSimilarity(6, 1, 0.0);
		context.setSimilarity(6, 2, 1.0);

		// Act:
		final ClusteringResult result = context.clusteringStrategy.cluster(context.neighborhood);

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 2, 4)),
				new Cluster(new ClusterId(1), NisUtils.toNodeIdList(1, 3, 5)));
		final List<Cluster> expectedHubs = Arrays.asList(
				new Cluster(new ClusterId(6), NisUtils.toNodeIdList(6)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs(), IsEqual.equalTo(expectedHubs));
		Assert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void networkWithTwoClustersAndOutlierCanBeClusteredWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			networkWithTwoClustersAndOutlierCanBeClustered(clusteringStrategy);
		}
	}
	
	public void networkWithTwoClustersAndOutlierCanBeClustered(final GraphClusteringStrategy clusteringStrategy) {
		// Arrange: { 0, 2, 4 } { 1, 3, 5 } form clusters; 6 is an outlier
		final TestContext context = new TestContext(clusteringStrategy, 7);
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
		final List<Cluster> expectedOutliers = Arrays.asList(
				new Cluster(new ClusterId(6), NisUtils.toNodeIdList(6)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	@Test
	public void isolatedNodeIsDetectedAsAnOutlierWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			isolatedNodeIsDetectedAsAnOutlier(clusteringStrategy);
		}
	}
	
	public void isolatedNodeIsDetectedAsAnOutlier(final GraphClusteringStrategy clusterer) {
		// Arrange: { 0, 1, 2, 3 } form clusters; 4 is an isolated outlier
		final TestContext context = new TestContext(clusterer, 5);
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
		final List<Cluster> expectedOutliers = Arrays.asList(
				new Cluster(new ClusterId(4), NisUtils.toNodeIdList(4)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	@Test
	public void dissimilarNodeConnectedToSingleClusterIsDetectedAsAnOutlierWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			dissimilarNodeConnectedToSingleClusterIsDetectedAsAnOutlier(clusteringStrategy);
		}
	}
	
	public void dissimilarNodeConnectedToSingleClusterIsDetectedAsAnOutlier(final GraphClusteringStrategy clusteringStrategy) {
		// Arrange: { 0, 1, 2, 3 } form clusters; 4 is an outlier connected to the cluster
		final TestContext context = new TestContext(clusteringStrategy, 5);
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
		final List<Cluster> expectedOutliers = Arrays.asList(
				new Cluster(new ClusterId(4), NisUtils.toNodeIdList(4)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	@Test
	public void nodeConnectedToOutlierAndClusterIsDetectedAsAnOutlierWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			nodeConnectedToOutlierAndClusterIsDetectedAsAnOutlier(clusteringStrategy);
		}
	}
	
	public void nodeConnectedToOutlierAndClusterIsDetectedAsAnOutlier(final GraphClusteringStrategy clusteringStrategy) {
		// Arrange: { 0, 1, 2, 3 } form clusters;
		// 4 is an outlier connected to the cluster
		// 5 is an outlier connected to (4) and the cluster (twice)
		final TestContext context = new TestContext(clusteringStrategy, 6);
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
				new Cluster(new ClusterId(4), NisUtils.toNodeIdList(4)),
				new Cluster(new ClusterId(5), NisUtils.toNodeIdList(5)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}

	// TODO: Maybe the following tests are better off in the integration tests
	//       since they involve calculations in more than one class.
	/**
	 * First Graph: 0 --- 1
	 *              | \   |
	 *              |   \ |
	 *              3 --- 2
	 *        
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
	 *                 
	 * Communities in form (node id, epsilon neighbors, non-epsilon neighbors):
	 *        com(0) = (0, {1,2,3}, {})
	 *        com(1) = (1, {0,2}, {})
	 *        com(2) = (2, {0,1,3}, {})
	 *        com(3) = (3, {0,2}, {})
	 *        
	 * Expected: cluster {0, 1, 2, 3}, no hubs, no outliers
	 *        
	 * First Graph: 0 --- 1 (essentially the same graph but we start scanning from a different node)
	 * (isomorphic) |   / |
	 *              | /   |
	 *              3 --- 2
	 *        
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
	 *                 
	 * Communities in form (node id, epsilon neighbors, non-epsilon neighbors):
	 *        com(0) = (0, {1,3}, {})
	 *        com(1) = (1, {0,2,3}, {})
	 *        com(2) = (2, {1,3}, {})
	 *        com(3) = (3, {0,1,2}, {})
	 *        
	 * Expected: cluster {0, 1, 2, 3}, no hubs, no outliers
	 */
	@Test
	public void firstGraphIsClusteredAsExpectedWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			firstGraphIsClusteredAsExpected(clusteringStrategy);
		}
	}

	public void firstGraphIsClusteredAsExpected(final GraphClusteringStrategy clusteringStrategy) {
		// Arrange:
		// Using dense matrix for easier debugging
		final DenseMatrix outlinkMatrix = new DenseMatrix(4,4);
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
		ClusteringResult result = calculateClusteringResult(clusteringStrategy, outlinkMatrix);
		logClusteringResult(result);
	
		// Assert:
		List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));
		
		final DenseMatrix outlinkMatrix2 = new DenseMatrix(4,4);
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
		result = calculateClusteringResult(clusteringStrategy, outlinkMatrix);
		logClusteringResult(result);

		// Assert:
		expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers().isEmpty(), IsEqual.equalTo(true));
	}
	
	/**
	 *                   _____                         1-----2--3
	 *                 /      \                         \    | /|\
	 * Second graph:  0----1   5   is equivalent to      \   |/ | \
	 *                |\    \ / \                         \  5  /  \
	 *                | \    2  /                          \ | /   |
	 *                |  \  /  /                            \|/    |
	 *                4----3--/                              0-----4
	 * 
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
	 *        
	 * Communities in form (node id, similar neighbors, dissimilar neighbors):
	 *         com(0) = (0, {3,4}, {1,5})
	 *         com(1) = (1, {}, {0,2})
	 *         com(2) = (2, {5}, {1,3})
	 *         com(3) = (3, {0,4,5}, {2})
	 *         com(4) = (4, {0,3}, {})
	 *         com(5) = (5, {2,3}, {0})
	 *     
	 * Expected: cluster {0,2,3,4,5}, no hubs, one outlier {1}
	 */
	@Test
	public void secondGraphIsClusteredAsExpectedWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			secondGraphIsClusteredAsExpected(clusteringStrategy);
		}
	}

	public void secondGraphIsClusteredAsExpected(final GraphClusteringStrategy clusteringStrategy) {
		// Arrange:
		// Using dense matrix for easier debugging
		final DenseMatrix outlinkMatrix = new DenseMatrix(6,6);
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
		final ClusteringResult result = calculateClusteringResult(clusteringStrategy, outlinkMatrix);
		logClusteringResult(result);
		
		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 2, 3, 4, 5)));
		final List<Cluster> expectedOutliers = Arrays.asList(
				new Cluster(new ClusterId(1), NisUtils.toNodeIdList(1)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}
	
	/**
	 * Graph:         0
	 *               / \
	 *              /   \
	 *             1-----2-----6
	 *                   |
	 *                   |
	 *                   3
	 *                  / \
	 *                 /   \
	 *                4-----5
	 *                
	 * Expected: clusters {0,1,2} and {3,4,5}, no hubs, one outlier {6}
	 */
	@Test
	public void thirdGraphIsClusteredAsExpectedWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			thirdGraphIsClusteredAsExpected(clusteringStrategy);
		}
	}

	public void thirdGraphIsClusteredAsExpected(final GraphClusteringStrategy clusteringStrategy) {
		// Arrange:
		// This is the example used in the paper:
		// NCDawareRank: a novel ranking method that exploits the decomposable structure of the web
		final DenseMatrix outlinkMatrix = new DenseMatrix(7,7);
		outlinkMatrix.setAt(1, 0, 1);
		outlinkMatrix.setAt(2, 0, 1);
		outlinkMatrix.setAt(0, 1, 1);
		outlinkMatrix.setAt(2, 1, 1);
		outlinkMatrix.setAt(0, 2, 1);
		outlinkMatrix.setAt(1, 2, 1);
		outlinkMatrix.setAt(3, 2, 1);
		outlinkMatrix.setAt(6, 2, 1);
		outlinkMatrix.setAt(2, 3, 1);
		outlinkMatrix.setAt(4, 3, 1);
		outlinkMatrix.setAt(5, 3, 1);
		outlinkMatrix.setAt(3, 4, 1);
		outlinkMatrix.setAt(5, 4, 1);
		outlinkMatrix.setAt(3, 5, 1);
		outlinkMatrix.setAt(4, 5, 1);
		outlinkMatrix.setAt(2, 6, 1);

		// Act:
		final ClusteringResult result = calculateClusteringResult(clusteringStrategy, outlinkMatrix);
		logClusteringResult(result);
		
		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2)),
				new Cluster(new ClusterId(3), NisUtils.toNodeIdList(3, 4, 5)));
		final List<Cluster> expectedOutliers = Arrays.asList(
				new Cluster(new ClusterId(6), NisUtils.toNodeIdList(6)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}
	
	/**
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
	 *                
	 * Expected: clusters {0,1,2} and {4,5,6}, one hub {3}, one outlier {7}
	 */
	@Test
	public void forthGraphIsClusteredAsExpectedWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			forthGraphIsClusteredAsExpected(clusteringStrategy);
		}
	}

	public void forthGraphIsClusteredAsExpected(final GraphClusteringStrategy clusteringStrategy) {
		// Arrange:
		final DenseMatrix outlinkMatrix = new DenseMatrix(8,8);
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
		final ClusteringResult result = calculateClusteringResult(clusteringStrategy, outlinkMatrix);
		logClusteringResult(result);
		
		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2)),
				new Cluster(new ClusterId(4), NisUtils.toNodeIdList(4, 5, 6)));
		final List<Cluster> expectedHubs = Arrays.asList(
				new Cluster(new ClusterId(3), NisUtils.toNodeIdList(3)));
		final List<Cluster> expectedOutliers = Arrays.asList(
				new Cluster(new ClusterId(7), NisUtils.toNodeIdList(7)));

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs(), IsEquivalent.equivalentTo(expectedHubs));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}
	
	/**
	 * Graph:      0-----1
	 *             |     |
	 *             |     |
	 *             3-----2

	 *                4
	 *                |
	 *                |
	 *                5-----6
	 *                
	 * Expected: clusters {0,1,2} and {4,5,6}
	 */
	@Test
	public void fifthGraphIsClusteredAsExpectedWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			fifthGraphIsClusteredAsExpected(clusteringStrategy);
		}
	}

	public void fifthGraphIsClusteredAsExpected(final GraphClusteringStrategy clusteringStrategy) {
		// Arrange:
		final DenseMatrix outlinkMatrix = new DenseMatrix(7,7);
		outlinkMatrix.setAt(1, 0, 1);
		outlinkMatrix.setAt(2, 1, 1);
		outlinkMatrix.setAt(3, 2, 1);
		outlinkMatrix.setAt(0, 3, 1);
		
		outlinkMatrix.setAt(5, 4, 1);
		outlinkMatrix.setAt(6, 5, 1);
		
		outlinkMatrix.removeNegatives(); //shouldn't make a difference either way
		
		// Act:
		final ClusteringResult result = calculateClusteringResult(clusteringStrategy, outlinkMatrix);
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
	 * Graph:      0
	 *            /|\
	 *           / | \
	 *          /  |  \
	 *         1   2   3
	 *                
	 * Expected: clusters: {0,1,2,3}
	 *           hubs    : none
	 *           outliers: none
	 */
	@Test
	public void sixthGraphIsClusteredAsExpectedWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			sixthGraphIsClusteredAsExpected(clusteringStrategy);
		}
	}

	public void sixthGraphIsClusteredAsExpected(final GraphClusteringStrategy clusteringStrategy) {
		// Arrange:
		final DenseMatrix outlinkMatrix = new DenseMatrix(4,4);
		outlinkMatrix.setAt(1, 0, 1);
		outlinkMatrix.setAt(2, 0, 1);
		outlinkMatrix.setAt(3, 0, 1);
		
		outlinkMatrix.removeNegatives(); //shouldn't make a difference either way
		
		// Act:
		final ClusteringResult result = calculateClusteringResult(clusteringStrategy, outlinkMatrix);
		logClusteringResult(result);
		
		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0,1,2,3)));
		final List<Cluster> expectedHubs = Arrays.asList();
		final List<Cluster> expectedOutliers = Arrays.asList();

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs(), IsEquivalent.equivalentTo(expectedHubs));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}
	
	/**
	 * Graph:      0---1---2---3---4---5 
	 *                
	 * Expected: clusters: {0,1,2,3,4,5}
	 *           hubs    : none
	 *           outliers: none
	 */
	@Test
	public void seventhGraphIsClusteredAsExpectedWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			seventhGraphIsClusteredAsExpected(clusteringStrategy);
		}
	}

	public void seventhGraphIsClusteredAsExpected(final GraphClusteringStrategy clusteringStrategy) {
		// Arrange:
		final DenseMatrix outlinkMatrix = new DenseMatrix(6,6);
		outlinkMatrix.setAt(1, 0, 1);
		outlinkMatrix.setAt(2, 1, 1);
		outlinkMatrix.setAt(3, 2, 1);
		outlinkMatrix.setAt(4, 3, 1);
		outlinkMatrix.setAt(5, 4, 1);
		
		outlinkMatrix.removeNegatives(); //shouldn't make a difference either way
		
		// Act:
		final ClusteringResult result = calculateClusteringResult(clusteringStrategy, outlinkMatrix);
		logClusteringResult(result);
		
		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0,1,2,3,4,5)));
		final List<Cluster> expectedHubs = Arrays.asList();
		final List<Cluster> expectedOutliers = Arrays.asList();

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs(), IsEquivalent.equivalentTo(expectedHubs));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}
	
	/**
	 * Graph:      0---1---2---3---4
	 *                
	 * Expected: clusters: {0,1,2,3,4}
	 *           hubs    : none
	 *           outliers: none
	 */
	@Test
	public void eighthGraphIsClusteredAsExpectedWithAllclusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
		for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
			eighthGraphIsClusteredAsExpected(clusteringStrategy);
		}
	}

	public void eighthGraphIsClusteredAsExpected(final GraphClusteringStrategy clusteringStrategy) {
		// Arrange:
		final DenseMatrix outlinkMatrix = new DenseMatrix(5,5);
		outlinkMatrix.setAt(1, 0, 1);
		outlinkMatrix.setAt(2, 1, 1);
		outlinkMatrix.setAt(3, 2, 1);
		outlinkMatrix.setAt(4, 3, 1);
		
		outlinkMatrix.removeNegatives(); //shouldn't make a difference either way
		
		// Act:
		final ClusteringResult result = calculateClusteringResult(clusteringStrategy, outlinkMatrix);
		logClusteringResult(result);
		
		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0,1,2,3,4)));
		final List<Cluster> expectedHubs = Arrays.asList();
		final List<Cluster> expectedOutliers = Arrays.asList();

		Assert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(result.getHubs(), IsEquivalent.equivalentTo(expectedHubs));
		Assert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedOutliers));
	}
	
	private ClusteringResult calculateClusteringResult(final GraphClusteringStrategy graphClusteringStrategy, final Matrix outlinkMatrix) {
		final NodeNeighborMap nodeNeighbordMap = new NodeNeighborMap(outlinkMatrix);
		final SimilarityStrategy strategy = new DefaultSimilarityStrategy(nodeNeighbordMap);
		final Neighborhood neighborhood = new Neighborhood(nodeNeighbordMap, strategy);
		final ClusteringResult result = graphClusteringStrategy.cluster(neighborhood);
		return result;
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
	
	/**
	 * Returns a list of all graph clustering strategies to be tested.
	 * 
	 * @return The list of graph clustering strategies..
	 */
	private List<GraphClusteringStrategy> getClusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = new ArrayList<>();
		clusteringStrategies.add(new Scan());
		clusteringStrategies.add(new FastScan());
		
		return clusteringStrategies;
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