package org.nem.nis.poi.graph;

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

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
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

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
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

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithLineStructure() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphType.GRAPH_LINE_STRUCTURE);

		// Assert:
		final SparseMatrix a = new SparseMatrix(5, 1, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 0, 1.0);
		a.setAt(4, 0, 1.0);

		final SparseMatrix r = new SparseMatrix(1, 5, 6);
		r.setAt(0, 0, 1.0 / 5.0);
		r.setAt(0, 1, 1.0 / 5.0);
		r.setAt(0, 2, 1.0 / 5.0);
		r.setAt(0, 3, 1.0 / 5.0);
		r.setAt(0, 4, 1.0 / 5.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithRingStructure() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphType.GRAPH_RING_STRUCTURE);

		// Assert:
		final SparseMatrix a = new SparseMatrix(5, 1, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 0, 1.0);
		a.setAt(4, 0, 1.0);

		final SparseMatrix r = new SparseMatrix(1, 5, 6);
		r.setAt(0, 0, 1.0 / 5.0);
		r.setAt(0, 1, 1.0 / 5.0);
		r.setAt(0, 2, 1.0 / 5.0);
		r.setAt(0, 3, 1.0 / 5.0);
		r.setAt(0, 4, 1.0 / 5.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithOneClusterAndNoHubAndNoOutlier() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphType.GRAPH_ONE_CLUSTERS_NO_HUB_NO_OUTLIER);

		// Assert:
		final SparseMatrix a = new SparseMatrix(4, 1, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 0, 1.0);

		final SparseMatrix r = new SparseMatrix(1, 4, 4);
		r.setAt(0, 0, 1.0 / 4.0);
		r.setAt(0, 1, 1.0 / 4.0);
		r.setAt(0, 2, 1.0 / 4.0);
		r.setAt(0, 3, 1.0 / 4.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithOneClusterAndNoHubAndOneOutlier() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphType.GRAPH_ONE_CLUSTERS_NO_HUB_ONE_OUTLIER);

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

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoClustersAndNoHubAndNoOutlier() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphType.GRAPH_TWO_CLUSTERS_NO_HUB_NO_OUTLIER);

		// Assert:
		final SparseMatrix a = new SparseMatrix(6, 2, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 1, 1.0);
		a.setAt(4, 1, 1.0);
		a.setAt(5, 1, 1.0);

		final SparseMatrix r = new SparseMatrix(2, 6, 4);
		r.setAt(0, 0, 1.0 / 3.0);
		r.setAt(0, 1, 1.0 / 3.0);
		r.setAt(0, 2, 1.0 / 6.0);
		r.setAt(1, 2, 1.0 / 6.0);
		r.setAt(1, 3, 1.0 / 3.0);
		r.setAt(1, 4, 1.0 / 3.0);
		r.setAt(1, 5, 1.0 / 3.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoClustersAndNoHubAndOneOutlier() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphType.GRAPH_TWO_CLUSTERS_NO_HUB_ONE_OUTLIER);

		// Assert:
		final SparseMatrix a = new SparseMatrix(7, 3, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 1, 1.0);
		a.setAt(4, 1, 1.0);
		a.setAt(5, 1, 1.0);
		a.setAt(6, 2, 1.0);

		final SparseMatrix r = new SparseMatrix(3, 7, 8);
		r.setAt(0, 0, 1.0 / 3.0);
		r.setAt(0, 1, 1.0 / 3.0);
		r.setAt(0, 2, 1.0 / 9.0);
		r.setAt(1, 2, 1.0 / 9.0);
		r.setAt(1, 3, 1.0 / 3.0);
		r.setAt(1, 4, 1.0 / 3.0);
		r.setAt(1, 5, 1.0 / 3.0);
		r.setAt(2, 2, 1.0 / 3.0);
		r.setAt(2, 6, 1.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoClustersAndOneHubAndNoOutlier() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphType.GRAPH_TWO_CLUSTERS_ONE_HUB_NO_OUTLIER);

		// Assert:
		final SparseMatrix a = new SparseMatrix(7, 3, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 2, 1.0);
		a.setAt(4, 1, 1.0);
		a.setAt(5, 1, 1.0);
		a.setAt(6, 1, 1.0);

		final SparseMatrix r = new SparseMatrix(3, 7, 4);
		r.setAt(0, 0, 1.0 / 3.0);
		r.setAt(0, 1, 1.0 / 3.0);
		r.setAt(0, 2, 1.0 / 6.0);
		r.setAt(1, 3, 1.0 / 6.0);
		r.setAt(1, 4, 1.0 / 3.0);
		r.setAt(1, 5, 1.0 / 3.0);
		r.setAt(1, 6, 1.0 / 3.0);
		r.setAt(2, 2, 1.0 / 2.0);
		r.setAt(2, 3, 1.0 / 2.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoClustersAndTwoHubsAndTwoOutliers() {
		// Act:
		final InterLevelProximityMatrix interLevel = createInterLevelMatrix(GraphType.GRAPH_TWO_CLUSTERS_TWO_HUBS_TWO_OUTLIERS);

		// Assert:
		final SparseMatrix a = new SparseMatrix(11, 6, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 2, 1.0);
		a.setAt(4, 1, 1.0);
		a.setAt(5, 1, 1.0);
		a.setAt(6, 1, 1.0);
		a.setAt(7, 3, 1.0);
		a.setAt(8, 4, 1.0);
		a.setAt(9, 5, 1.0);
		a.setAt(10, 0, 1.0);

		final SparseMatrix r = new SparseMatrix(6, 11, 4);
		r.setAt(0, 0, 1.0 / 8.0);  //  N(0): 2; |A(0)|: 4
		r.setAt(0, 1, 1.0 / 4.0);  //  N(1): 1; |A(0)|: 4
		r.setAt(0, 2, 1.0 / 8.0);  //  N(2): 2; |A(0)|: 4
		r.setAt(0, 10, 1.0 / 4.0); // N(10): 1; |A(0)|: 4
		r.setAt(1, 3, 1.0 / 9.0);  //  N(3): 3; |A(1)|: 3
		r.setAt(1, 4, 1.0 / 3.0);  //  N(4): 1; |A(1)|: 3
		r.setAt(1, 5, 1.0 / 6.0);  //  N(5): 2; |A(1)|: 3
		r.setAt(1, 6, 1.0 / 3.0);  //  N(6): 1; |A(1)|: 3
		r.setAt(1, 7, 1.0 / 6.0);  //  N(7): 2; |A(1)|: 3
		r.setAt(2, 2, 1.0 / 2.0);  //  N(2): 2; |A(2)|: 1
		r.setAt(2, 3, 1.0 / 3.0);  //  N(3): 3; |A(2)|: 1
		r.setAt(3, 0, 1.0 / 2.0);  //  N(0): 2; |A(3)|: 1
		r.setAt(3, 7, 1.0 / 2.0);  //  N(7): 2; |A(3)|: 1
		r.setAt(4, 5, 1.0 / 2.0);  //  N(5): 2; |A(4)|: 1
		r.setAt(4, 8, 1.0);        //  N(8): 1; |A(4)|: 1
		r.setAt(5, 3, 1.0 / 3.0);  //  N(3): 3; |A(5)|: 1
		r.setAt(5, 9, 1.0);        //  N(9): 1; |A(5)|: 1

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
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
		Matrix outlinkMatrix = new DenseMatrix(4, 4);
		outlinkMatrix.setAt(1, 0, 1);
		outlinkMatrix.setAt(0, 2, 1);
		outlinkMatrix.setAt(3, 0, 1);

		// pretend each node is an outlier in its own cluster
		// the important part of this test is that the cluster ids are in NON-ASCENDING order
		final ClusteringResult clusteringResult = new ClusteringResult(
				new ArrayList<>(),
				new ArrayList<>(),
				Arrays.asList(
						new Cluster(new ClusterId(3), NisUtils.toNodeIdList(0)),
						new Cluster(new ClusterId(2), NisUtils.toNodeIdList(1)),
						new Cluster(new ClusterId(1), NisUtils.toNodeIdList(2)),
						new Cluster(new ClusterId(0), NisUtils.toNodeIdList(3))));

		final NodeNeighborMap nodeNeighborMap = new NodeNeighborMap(outlinkMatrix);
		final Neighborhood neighborhood = new Neighborhood(nodeNeighborMap, new DefaultSimilarityStrategy(nodeNeighborMap));
		final InterLevelProximityMatrix interLevel = new InterLevelProximityMatrix(
				clusteringResult,
				neighborhood,
				outlinkMatrix);

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

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	//region test infrastructure

	private static InterLevelProximityMatrix createInterLevelMatrix(final GraphType graphType) {
		final Matrix outlinkMatrix = OutlinkMatrixFactory.create(graphType);
		final ClusteringResult clusteringResult = IdealizedClusterFactory.create(graphType);
		final NodeNeighborMap nodeNeighborMap = new NodeNeighborMap(outlinkMatrix);
		final Neighborhood neighborhood = new Neighborhood(nodeNeighborMap, new DefaultSimilarityStrategy(nodeNeighborMap));
		return new InterLevelProximityMatrix(clusteringResult, neighborhood, outlinkMatrix);
	}

	//endregion
}
