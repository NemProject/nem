package org.nem.nis.pox.poi.graph;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.test.NisUtils;

import java.util.*;

public class ClusteringResultTest {

	// region constructor

	@Test
	public void resultExposesAllConstructorParameters() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(new Cluster(new ClusterId(7)), new Cluster(new ClusterId(8)));
		final Collection<Cluster> hubs = Collections.singletonList(new Cluster(new ClusterId(11)));
		final Collection<Cluster> outliers = Collections.singletonList(new Cluster(new ClusterId(10)));

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		MatcherAssert.assertThat(result.getClusters(), IsSame.sameInstance(clusters));
		MatcherAssert.assertThat(result.getHubs(), IsSame.sameInstance(hubs));
		MatcherAssert.assertThat(result.getOutliers(), IsSame.sameInstance(outliers));
	}

	// endregion

	// region numClusters / numNodes

	@Test
	public void numClustersReturnsTotalNumberOfClustersAcrossAllClusterTypes() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(new Cluster(new ClusterId(7)), new Cluster(new ClusterId(8)));
		final Collection<Cluster> hubs = Collections.singletonList(new Cluster(new ClusterId(11)));
		final Collection<Cluster> outliers = Arrays.asList(new Cluster(new ClusterId(10)), new Cluster(new ClusterId(12)),
				new Cluster(new ClusterId(15)));

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert
		MatcherAssert.assertThat(result.numClusters(), IsEqual.equalTo(6));
	}

	@Test
	public void numNodesReturnsTotalNumberOfNodesInAllClusters() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4)),
				new Cluster(new ClusterId(5), NisUtils.toNodeIdList(5, 6, 7, 8, 9)));
		final Collection<Cluster> hubs = Collections.singletonList(new Cluster(new NodeId(11)));
		final Collection<Cluster> outliers = Collections.singletonList(new Cluster(new NodeId(10)));

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		MatcherAssert.assertThat(result.numNodes(), IsEqual.equalTo(12));
	}

	// endregion

	// region isRegularCluster/isHub/isOutlier

	@Test
	public void isRegularClusterIsOnlyPredicateThatReturnsTrueForRegularClusterId() {
		// Act:
		final ClusteringResult result = createClusteringResultForPredicateTests();

		// Assert:
		for (final int i : Arrays.asList(1, 7)) {
			final ClusterId id = new ClusterId(i);
			MatcherAssert.assertThat(result.isRegularCluster(id), IsEqual.equalTo(true));
			MatcherAssert.assertThat(result.isHub(id), IsEqual.equalTo(false));
			MatcherAssert.assertThat(result.isOutlier(id), IsEqual.equalTo(false));
		}
	}

	@Test
	public void isHubIsOnlyPredicateThatReturnsTrueForHubClusterId() {
		// Act:
		final ClusteringResult result = createClusteringResultForPredicateTests();

		// Assert:
		for (final int i : Arrays.asList(3, 4, 6)) {
			final ClusterId id = new ClusterId(i);
			MatcherAssert.assertThat(result.isRegularCluster(id), IsEqual.equalTo(false));
			MatcherAssert.assertThat(result.isHub(id), IsEqual.equalTo(true));
			MatcherAssert.assertThat(result.isOutlier(id), IsEqual.equalTo(false));
		}
	}

	@Test
	public void isOutlierIsOnlyPredicateThatReturnsTrueForOutlierClusterId() {
		// Act:
		final ClusteringResult result = createClusteringResultForPredicateTests();

		// Assert:
		for (final int i : Arrays.asList(2, 5)) {
			final ClusterId id = new ClusterId(i);
			MatcherAssert.assertThat(result.isRegularCluster(id), IsEqual.equalTo(false));
			MatcherAssert.assertThat(result.isHub(id), IsEqual.equalTo(false));
			MatcherAssert.assertThat(result.isOutlier(id), IsEqual.equalTo(true));
		}
	}

	@Test
	public void allPredicatesReturnFalseForUnknownClusterId() {
		// Act:
		final ClusteringResult result = createClusteringResultForPredicateTests();

		// Assert:
		for (final int i : Arrays.asList(0, 8, 11)) {
			final ClusterId id = new ClusterId(i);
			MatcherAssert.assertThat(result.isRegularCluster(id), IsEqual.equalTo(false));
			MatcherAssert.assertThat(result.isHub(id), IsEqual.equalTo(false));
			MatcherAssert.assertThat(result.isOutlier(id), IsEqual.equalTo(false));
		}
	}

	private static ClusteringResult createClusteringResultForPredicateTests() {
		// Arrange:
		final Collection<Cluster> clusters = mapToClusters(1, 7);
		final Collection<Cluster> hubs = mapToClusters(3, 4, 6);
		final Collection<Cluster> outliers = mapToClusters(2, 5);
		return new ClusteringResult(clusters, hubs, outliers);
	}

	private static Collection<Cluster> mapToClusters(final int... ids) {
		final Collection<Cluster> clusters = new ArrayList<>();
		for (final int id : ids) {
			clusters.add(new Cluster(new ClusterId(id)));
		}

		return clusters;
	}

	// endregion

	// region getAverageClusterSize

	@Test
	public void getAverageClusterSizeReturnsAverageClusterSizeWhenThereAreClusters() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4)),
				new Cluster(new ClusterId(17), NisUtils.toNodeIdList(17, 18)),
				new Cluster(new ClusterId(20), NisUtils.toNodeIdList(20, 21)),
				new Cluster(new ClusterId(5), NisUtils.toNodeIdList(5, 6, 7, 8, 9)));
		final Collection<Cluster> hubs = Collections.singletonList(new Cluster(new NodeId(11)));
		final Collection<Cluster> outliers = Collections.singletonList(new Cluster(new NodeId(10)));

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert: 14 / 4
		MatcherAssert.assertThat(result.getAverageClusterSize(), IsEqual.equalTo(3.5));
	}

	@Test
	public void getAverageClusterSizeReturnsZeroWhenThereAreNoClusters() {
		// Arrange:
		final Collection<Cluster> clusters = new ArrayList<>();
		final Collection<Cluster> hubs = Collections.singletonList(new Cluster(new NodeId(11)));
		final Collection<Cluster> outliers = Collections.singletonList(new Cluster(new NodeId(10)));

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		MatcherAssert.assertThat(result.getAverageClusterSize(), IsEqual.equalTo(0.0));
	}

	// endregion

	// region getIdForNode

	@Test
	public void getIdForNodeReturnsClusterInformationForAllClusterMembers() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4)),
				new Cluster(new ClusterId(5), NisUtils.toNodeIdList(5, 6, 7, 8, 9)));
		final Collection<Cluster> hubs = Collections.singletonList(new Cluster(new NodeId(11)));
		final Collection<Cluster> outliers = Collections.singletonList(new Cluster(new NodeId(10)));

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		MatcherAssert.assertThat(result.getIdForNode(new NodeId(0)), IsEqual.equalTo(new ClusterId(0)));
		MatcherAssert.assertThat(result.getIdForNode(new NodeId(1)), IsEqual.equalTo(new ClusterId(0)));
		MatcherAssert.assertThat(result.getIdForNode(new NodeId(2)), IsEqual.equalTo(new ClusterId(0)));
		MatcherAssert.assertThat(result.getIdForNode(new NodeId(3)), IsEqual.equalTo(new ClusterId(0)));
		MatcherAssert.assertThat(result.getIdForNode(new NodeId(4)), IsEqual.equalTo(new ClusterId(0)));

		MatcherAssert.assertThat(result.getIdForNode(new NodeId(5)), IsEqual.equalTo(new ClusterId(5)));
		MatcherAssert.assertThat(result.getIdForNode(new NodeId(6)), IsEqual.equalTo(new ClusterId(5)));
		MatcherAssert.assertThat(result.getIdForNode(new NodeId(7)), IsEqual.equalTo(new ClusterId(5)));
		MatcherAssert.assertThat(result.getIdForNode(new NodeId(8)), IsEqual.equalTo(new ClusterId(5)));
		MatcherAssert.assertThat(result.getIdForNode(new NodeId(9)), IsEqual.equalTo(new ClusterId(5)));

		MatcherAssert.assertThat(result.getIdForNode(new NodeId(10)), IsEqual.equalTo(new ClusterId(10)));
		MatcherAssert.assertThat(result.getIdForNode(new NodeId(11)), IsEqual.equalTo(new ClusterId(11)));
	}

	@Test
	public void getIdForNodeDoesNotReturnClusterInformationForClusterIds() {
		// Arrange:
		final Collection<Cluster> clusters = Collections.singletonList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(1)));
		final Collection<Cluster> hubs = Collections.emptyList();
		final Collection<Cluster> outliers = Collections.emptyList();

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		MatcherAssert.assertThat(result.getIdForNode(new NodeId(0)), IsNull.nullValue());
	}

	@Test
	public void getIdForNodeReturnsNullWhenClusterInformationIsUnknown() {
		// Arrange:
		final Collection<Cluster> clusters = Collections.singletonList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(1)));
		final Collection<Cluster> hubs = Collections.emptyList();
		final Collection<Cluster> outliers = Collections.emptyList();

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		MatcherAssert.assertThat(result.getIdForNode(new NodeId(17)), IsNull.nullValue());
	}

	// endregion
}
