package org.nem.nis.pox.poi.graph;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.model.primitive.ClusterId;
import org.nem.nis.test.*;

import java.util.*;

public class InterLevelProximityMatrixTest {

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithSingleNode() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphType.GRAPH_SINGLE_NODE);

		// Assert:
		final SparseMatrix a = new SparseMatrix(1, 1, 1);
		a.setAt(0, 0, 1.0);
		final SparseMatrix r = new SparseMatrix(1, 1, 1);
		r.setAt(0, 0, 1.0);

		MatcherAssert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		MatcherAssert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoUnconnectedNodes() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphType.GRAPH_TWO_UNCONNECTED_NODES);

		// Assert:
		final SparseMatrix a = new SparseMatrix(2, 2, 2);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 1, 1.0);
		final SparseMatrix r = new SparseMatrix(2, 2, 2);
		r.setAt(0, 0, 1.0);
		r.setAt(1, 1, 1.0);

		MatcherAssert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		MatcherAssert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoConnectedNodes() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphType.GRAPH_TWO_CONNECTED_NODES);

		// Assert:
		final SparseMatrix a = new SparseMatrix(2, 2, 2);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 1, 1.0);
		final SparseMatrix r = new SparseMatrix(2, 2, 2);
		r.setAt(0, 0, 1.0 / 2.0);
		r.setAt(1, 0, 1.0 / 2.0);
		r.setAt(1, 1, 1.0);

		MatcherAssert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		MatcherAssert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithLineStructure() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphType.GRAPH_LINE_STRUCTURE);

		// Assert:
		// only outliers, a matrix must be diagonal
		final SparseMatrix a = new SparseMatrix(5, 5, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 1, 1.0);
		a.setAt(2, 2, 1.0);
		a.setAt(3, 3, 1.0);
		a.setAt(4, 4, 1.0);

		// cluster neighborhoods: {0, 1}, {1, 2}, {2, 3}, {3, 4}, {4}
		// cluster 0 is covered by only {0, 1}, cluster 1 is covered by {0, 1} and {1, 2}, ..., cluster 4 is covered by {3, 4} and {4}
		final SparseMatrix r = new SparseMatrix(5, 5, 4);
		r.setAt(0, 0, 1.0 / 2.0);
		r.setAt(1, 0, 1.0 / 2.0);
		r.setAt(1, 1, 1.0 / 2.0);
		r.setAt(2, 1, 1.0 / 2.0);
		r.setAt(2, 2, 1.0 / 2.0);
		r.setAt(3, 2, 1.0 / 2.0);
		r.setAt(3, 3, 1.0 / 2.0);
		r.setAt(4, 3, 1.0 / 2.0);
		r.setAt(4, 4, 1.0 / 1.0);

		MatcherAssert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		MatcherAssert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithRingStructure() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphType.GRAPH_RING_STRUCTURE);

		// Assert:
		// only outliers, a matrix must be diagonal
		final SparseMatrix a = new SparseMatrix(5, 5, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 1, 1.0);
		a.setAt(2, 2, 1.0);
		a.setAt(3, 3, 1.0);
		a.setAt(4, 4, 1.0);

		// cluster neighborhoods: {0, 1}, {1, 2}, {2, 3}, {3, 4}, {0, 4}
		final SparseMatrix r = new SparseMatrix(5, 5, 4);
		r.setAt(0, 0, 1.0 / 2.0);
		r.setAt(0, 4, 1.0 / 2.0);
		r.setAt(1, 0, 1.0 / 2.0);
		r.setAt(1, 1, 1.0 / 2.0);
		r.setAt(2, 1, 1.0 / 2.0);
		r.setAt(2, 2, 1.0 / 2.0);
		r.setAt(3, 2, 1.0 / 2.0);
		r.setAt(3, 3, 1.0 / 2.0);
		r.setAt(4, 3, 1.0 / 2.0);
		r.setAt(4, 4, 1.0 / 2.0);

		MatcherAssert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		MatcherAssert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithOneClusterAndNoHubAndNoOutlier() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphType.GRAPH_BOX_TWO_DIAGONALS);

		// Assert:
		final SparseMatrix a = new SparseMatrix(4, 1, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 0, 1.0);

		// one cluster with neighborhood: {0, 1, 2, 3}
		final SparseMatrix r = new SparseMatrix(1, 4, 4);
		r.setAt(0, 0, 1.0 / 4.0);
		r.setAt(0, 1, 1.0 / 4.0);
		r.setAt(0, 2, 1.0 / 4.0);
		r.setAt(0, 3, 1.0 / 4.0);

		MatcherAssert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		MatcherAssert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithOneClusterAndNoHubAndOneOutlier() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphTypeEpsilon065.GRAPH_ONE_CLUSTER_NO_HUB_ONE_OUTLIER);

		// Assert:
		final SparseMatrix a = new SparseMatrix(5, 2, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 0, 1.0);
		a.setAt(4, 1, 1.0);

		final SparseMatrix r = new SparseMatrix(2, 5, 4);
		r.setAt(0, 0, 1.0 / 4.0);
		r.setAt(0, 1, 1.0 / 8.0);
		r.setAt(0, 2, 1.0 / 4.0);
		r.setAt(0, 3, 1.0 / 4.0);
		r.setAt(1, 1, 1.0 / 2.0);
		r.setAt(1, 4, 1.0);

		MatcherAssert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		MatcherAssert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoClustersAndNoHubAndNoOutlier() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphTypeEpsilon065.GRAPH_TWO_CLUSTERS_NO_HUB_NO_OUTLIER);

		// Assert:
		final SparseMatrix a = new SparseMatrix(8, 2, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 0, 1.0);
		a.setAt(4, 1, 1.0);
		a.setAt(5, 1, 1.0);
		a.setAt(6, 1, 1.0);
		a.setAt(7, 1, 1.0);

		// two clusters: id 0: {0, 1, 2, 3}, id 4: {4, 5, 6, 7}
		// cluster ids for coverage of nodes: {0}, {0}, {0, 4}, {0}, {4}, {4}, {4}, {4}
		final SparseMatrix r = new SparseMatrix(2, 8, 4);
		r.setAt(0, 0, 1.0 / 4.0);
		r.setAt(0, 1, 1.0 / 4.0);
		r.setAt(0, 2, 1.0 / 8.0);
		r.setAt(0, 3, 1.0 / 4.0);
		r.setAt(1, 2, 1.0 / 8.0);
		r.setAt(1, 4, 1.0 / 4.0);
		r.setAt(1, 5, 1.0 / 4.0);
		r.setAt(1, 6, 1.0 / 4.0);
		r.setAt(1, 7, 1.0 / 4.0);

		MatcherAssert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		MatcherAssert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoClustersAndNoHubAndOneOutlier() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphTypeEpsilon065.GRAPH_TWO_CLUSTERS_NO_HUB_ONE_OUTLIER);

		// Assert:
		final SparseMatrix a = new SparseMatrix(9, 3, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 0, 1.0);
		a.setAt(4, 1, 1.0);
		a.setAt(5, 1, 1.0);
		a.setAt(6, 1, 1.0);
		a.setAt(7, 1, 1.0);
		a.setAt(8, 2, 1.0);

		// two clusters: id 0: {0, 1, 2, 3}, id 4: {4, 5, 6, 7}, one outlier: {8}
		// cluster ids for coverage of nodes: {0}, {0}, {0, 4}, {0}, {4}, {4}, {4}, {4}, {0, 8}
		final SparseMatrix r = new SparseMatrix(3, 9, 8);
		r.setAt(0, 0, 1.0 / 4.0);
		r.setAt(0, 1, 1.0 / 4.0);
		r.setAt(0, 2, 1.0 / 8.0);
		r.setAt(0, 3, 1.0 / 4.0);
		r.setAt(0, 8, 1.0 / 8.0);
		r.setAt(1, 2, 1.0 / 8.0);
		r.setAt(1, 4, 1.0 / 4.0);
		r.setAt(1, 5, 1.0 / 4.0);
		r.setAt(1, 6, 1.0 / 4.0);
		r.setAt(1, 7, 1.0 / 4.0);
		r.setAt(2, 8, 0.5);

		MatcherAssert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		MatcherAssert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoClustersAndOneHubAndNoOutlier() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphTypeEpsilon065.GRAPH_TWO_CLUSTERS_ONE_HUB_NO_OUTLIER);

		// Assert:
		final SparseMatrix a = new SparseMatrix(9, 3, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 0, 1.0);
		a.setAt(4, 1, 1.0);
		a.setAt(5, 1, 1.0);
		a.setAt(6, 1, 1.0);
		a.setAt(7, 1, 1.0);
		a.setAt(8, 2, 1.0);

		// two clusters: id 0: {0, 1, 2, 3}, id 4: {4, 5, 6, 7}, one hub: {8}
		// cluster ids for coverage of nodes: {0}, {0}, {0}, {0}, {4, 8}, {4}, {4}, {4}, {0, 8}
		final SparseMatrix r = new SparseMatrix(3, 9, 4);
		r.setAt(0, 0, 1.0 / 4.0);
		r.setAt(0, 1, 1.0 / 4.0);
		r.setAt(0, 2, 1.0 / 4.0);
		r.setAt(0, 3, 1.0 / 4.0);
		r.setAt(0, 8, 1.0 / 8.0);
		r.setAt(1, 4, 1.0 / 8.0);
		r.setAt(1, 5, 1.0 / 4.0);
		r.setAt(1, 6, 1.0 / 4.0);
		r.setAt(1, 7, 1.0 / 4.0);
		r.setAt(2, 4, 0.5);
		r.setAt(2, 8, 0.5);

		MatcherAssert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		MatcherAssert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoClustersAndTwoHubsAndTwoOutliers() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphTypeEpsilon065.GRAPH_TWO_CLUSTERS_TWO_HUBS_TWO_OUTLIERS);

		// Assert:
		final SparseMatrix a = new SparseMatrix(13, 6, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 0, 1.0);
		a.setAt(12, 0, 1.0);
		a.setAt(4, 1, 1.0);
		a.setAt(5, 1, 1.0);
		a.setAt(6, 1, 1.0);
		a.setAt(7, 1, 1.0);
		a.setAt(8, 2, 1.0);
		a.setAt(9, 3, 1.0);
		a.setAt(10, 4, 1.0);
		a.setAt(11, 5, 1.0);

		// two clusters: id 0: {0, 1, 2, 3, 12}, id 4: {4, 5, 6, 7}, two hubs: {8}, {9}, two outliers: {10}, {11}
		// cluster ids for coverage of nodes: {0}, {0}, {0}, {0}, {4, 8}, {4, 9}, {4}, {4}, {0, 8}, {0, 9}, {4, 10}, {8, 11}, {0}
		final SparseMatrix r = new SparseMatrix(6, 13, 4);
		r.setAt(0, 0, 1.0 / 5.0);
		r.setAt(0, 1, 1.0 / 5.0);
		r.setAt(0, 2, 1.0 / 5.0);
		r.setAt(0, 3, 1.0 / 5.0);
		r.setAt(0, 12, 1.0 / 5.0);
		r.setAt(0, 8, 1.0 / 10.0);
		r.setAt(0, 9, 1.0 / 10.0);
		r.setAt(1, 4, 1.0 / 8.0);
		r.setAt(1, 5, 1.0 / 8.0);
		r.setAt(1, 6, 1.0 / 4.0);
		r.setAt(1, 7, 1.0 / 4.0);
		r.setAt(1, 10, 1.0 / 8.0);
		r.setAt(2, 4, 0.5);
		r.setAt(2, 8, 0.5);
		r.setAt(2, 11, 0.5);
		r.setAt(3, 5, 0.5);
		r.setAt(3, 9, 0.5);
		r.setAt(4, 10, 0.5);
		r.setAt(5, 11, 0.5);

		MatcherAssert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		MatcherAssert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithThreeClustersTwoHubsAndThreeOutliers() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(
				GraphTypeEpsilon040.GRAPH_THREE_CLUSTERS_TWO_HUBS_THREE_OUTLIERS);

		// Assert:
		assertInterLevelMatrixForGraphWithThreeClustersTwoHubsAndThreeOutliers(interLevel);
	}

	/**
	 * Asserts that the inter-level matrix is correct for the well known graph GRAPH_THREE_CLUSTERS_TWO_HUBS_THREE_OUTLIERS.
	 *
	 * @param interLevel The inter-level matrix to check.
	 */
	public static void assertInterLevelMatrixForGraphWithThreeClustersTwoHubsAndThreeOutliers(final InterLevelProximityMatrix interLevel) {
		final SparseMatrix a = new SparseMatrix(20, 8, 4);
		final SparseMatrix r = new SparseMatrix(8, 20, 4);

		// note: the entries in the a and r matrices depend on the order in which the clusters are found!
		// for example if cluster 1 and cluster 2 are switched in the list of clusters, then column 0 and 1
		// have to be switched for matrix a (row 0 and 1 for matrix r).
		// cluster 1
		for (final int i : Arrays.asList(0, 1, 4, 10, 14)) {
			a.setAt(i, 1, 1);
			r.setAt(1, i, 1.0 / 5.0); // N(i): 1; |A(0)|: 5
		}

		// cluster 2
		for (final int i : Arrays.asList(2, 3, 7, 9, 15)) {
			a.setAt(i, 0, 1);
			r.setAt(0, i, 1.0 / 5.0); // N(i): 1; |A(1)|: 5
		}

		// cluster 3
		for (final int i : Arrays.asList(5, 6, 8, 11, 12)) {
			a.setAt(i, 2, 1);
			r.setAt(2, i, 1.0 / 5.0); // N(i): 1; |A(2)|: 5
		}

		// hubs
		a.setAt(16, 3, 1);
		a.setAt(18, 4, 1);

		for (final int i : Arrays.asList(0, 1, 2)) {
			r.setAt(i, 16, 1.0 / 25.0); // N(16): 5; |A(i)|: 5
			r.setAt(i, 18, 1.0 / 25.0);
		}

		r.setAt(3, 16, 1.0 / 5.0); // N(16): 5; |A(3)|: 1
		r.setAt(4, 18, 1.0 / 5.0); // N(18): 5; |A(4)|: 1
		r.setAt(6, 16, 1.0 / 5.0); // N(16): 5; |A(6)|: 1
		r.setAt(7, 18, 1.0 / 5.0); // N(18): 5; |A(7)|: 1

		// outliers
		a.setAt(13, 5, 1);
		a.setAt(17, 6, 1);
		a.setAt(19, 7, 1);

		r.setAt(5, 13, 1.0); // N(13): 1; |A(5)|: 1
		r.setAt(6, 17, 1.0); // N(17): 1; |A(6)|: 1
		r.setAt(7, 19, 1.0); // N(19): 1; |A(7)|: 1

		MatcherAssert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		MatcherAssert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	/**
	 * <pre>
	 *      0
	 *    / o \
	 *   o  |  o
	 *   1  2  3
	 * </pre>
	 */
	@Test
	public void matricesAreCalculatedCorrectlyWhenClusterIdsAreNonAscending() {
		// Arrange:
		final Matrix outlinkMatrix = new DenseMatrix(4, 4);
		outlinkMatrix.setAt(1, 0, 1);
		outlinkMatrix.setAt(0, 2, 1);
		outlinkMatrix.setAt(3, 0, 1);

		// pretend each node is an outlier in its own cluster
		// the important part of this test is that the cluster ids are in NON-ASCENDING order
		final ClusteringResult clusteringResult = new ClusteringResult(new ArrayList<>(), new ArrayList<>(),
				Arrays.asList(new Cluster(new ClusterId(3), NisUtils.toNodeIdList(0)),
						new Cluster(new ClusterId(2), NisUtils.toNodeIdList(1)), new Cluster(new ClusterId(1), NisUtils.toNodeIdList(2)),
						new Cluster(new ClusterId(0), NisUtils.toNodeIdList(3))));

		final NodeNeighborMap nodeNeighborMap = new NodeNeighborMap(outlinkMatrix);
		final Neighborhood neighborhood = NisUtils.createNeighborhood(nodeNeighborMap, new StructuralSimilarityStrategy(nodeNeighborMap));
		final InterLevelProximityMatrix interLevel = new InterLevelProximityMatrix(clusteringResult, neighborhood, outlinkMatrix);

		// Assert:
		// note that A is not a map of node-id -> cluster-id;
		// it is a map of node-id to block-number, which is always ascending
		final SparseMatrix a = new SparseMatrix(4, 4, 2);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 1, 1.0);
		a.setAt(2, 2, 1.0);
		a.setAt(3, 3, 1.0);

		final SparseMatrix r = new SparseMatrix(4, 4, 2);
		r.setAt(0, 0, 1.0 / 3.0); // N(0): 3; |A(1..4)|: 1
		r.setAt(0, 2, 1.0 / 2.0); // N(2): 2
		r.setAt(1, 0, 1.0 / 3.0); // N(0): 3
		r.setAt(1, 1, 1.0 / 1.0); // N(1): 1
		r.setAt(2, 2, 1.0 / 2.0); // N(2): 2
		r.setAt(3, 0, 1.0 / 3.0); // N(0): 3
		r.setAt(3, 3, 1.0 / 1.0); // N(3): 1

		MatcherAssert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		MatcherAssert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	// region test infrastructure

	private static InterLevelProximityMatrix createInterLevelMatrix(final GraphType graphType) {
		return createInterLevelMatrix(OutlinkMatrixFactory.create(graphType), IdealizedClusterFactory.create(graphType));
	}

	private static InterLevelProximityMatrix createInterLevelMatrix(final GraphTypeEpsilon040 graphType) {
		return createInterLevelMatrix(OutlinkMatrixFactory.create(graphType), IdealizedClusterFactory.create(graphType));
	}

	private static InterLevelProximityMatrix createInterLevelMatrix(final GraphTypeEpsilon065 graphType) {
		return createInterLevelMatrix(OutlinkMatrixFactory.create(graphType), IdealizedClusterFactory.create(graphType));
	}

	private static InterLevelProximityMatrix createInterLevelMatrix(final Matrix outlinkMatrix, final ClusteringResult clusteringResult) {
		final NodeNeighborMap nodeNeighborMap = new NodeNeighborMap(outlinkMatrix);
		final Neighborhood neighborhood = NisUtils.createNeighborhood(nodeNeighborMap, new StructuralSimilarityStrategy(nodeNeighborMap));
		return new InterLevelProximityMatrix(clusteringResult, neighborhood, outlinkMatrix);
	}

	// endregion
}
