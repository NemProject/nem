package org.nem.nis.poi.graph;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.test.NisUtils;

import java.util.*;

public class ClusteringResultTest {

	//region constructor

	@Test
	public void resultExposesAllConstructorParameters() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(new Cluster(new ClusterId(7)), new Cluster(new ClusterId(8)));
		final Collection<Cluster> hubs = Arrays.asList(new Cluster(new ClusterId(11)));
		final Collection<Cluster> outliers = Arrays.asList(new Cluster(new ClusterId(10)));

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		Assert.assertThat(result.getClusters(), IsSame.sameInstance(clusters));
		Assert.assertThat(result.getHubs(), IsSame.sameInstance(hubs));
		Assert.assertThat(result.getOutliers(), IsSame.sameInstance(outliers));
	}

	//endregion

	//region numClusters / numNodes

	@Test
	public void numClustersReturnsTotalNumberOfClustersAcrossAllClusterTypes() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(new Cluster(new ClusterId(7)), new Cluster(new ClusterId(8)));
		final Collection<Cluster> hubs = Arrays.asList(new Cluster(new ClusterId(11)));
		final Collection<Cluster> outliers = Arrays.asList(
				new Cluster(new ClusterId(10)),
				new Cluster(new ClusterId(12)),
				new Cluster(new ClusterId(15)));

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert
		Assert.assertThat(result.numClusters(), IsEqual.equalTo(6));
	}

	@Test
	public void numNodesReturnsTotalNumberOfNodesInAllClusters() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4)),
				new Cluster(new ClusterId(5), NisUtils.toNodeIdList(5, 6, 7, 8, 9)));
		final Collection<Cluster> hubs = Arrays.asList(new Cluster(new NodeId(11)));
		final Collection<Cluster> outliers = Arrays.asList(new Cluster(new NodeId(10)));

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		Assert.assertThat(result.numNodes(), IsEqual.equalTo(12));
	}

	//endregion

	// region isRegularCluster/isHub/isOutlier

	@Test
	public void isRegularClusterReturnsTrueWhenClusterIdIsARegularCluster() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(new Cluster(new ClusterId(7)), new Cluster(new ClusterId(8)));
		final Collection<Cluster> hubs = Arrays.asList();
		final Collection<Cluster> outliers = Arrays.asList();

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		Assert.assertThat(result.isRegularCluster(new ClusterId(7)), IsEqual.equalTo(true));
		Assert.assertThat(result.isRegularCluster(new ClusterId(8)), IsEqual.equalTo(true));
	}

	@Test
	public void isRegularClusterReturnsFalseWhenClusterIdIsNotARegularCluster() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(new Cluster(new ClusterId(7)), new Cluster(new ClusterId(8)));
		final Collection<Cluster> hubs = Arrays.asList();
		final Collection<Cluster> outliers = Arrays.asList();

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		Assert.assertThat(result.isRegularCluster(new ClusterId(5)), IsEqual.equalTo(false));
		Assert.assertThat(result.isRegularCluster(new ClusterId(3)), IsEqual.equalTo(false));
	}

	//endregion

	@Test
	public void isHubReturnsTrueWhenClusterIdIsAHub() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList();
		final Collection<Cluster> hubs = Arrays.asList(new Cluster(new ClusterId(7)), new Cluster(new ClusterId(8)));
		final Collection<Cluster> outliers = Arrays.asList();

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		Assert.assertThat(result.isHub(new ClusterId(7)), IsEqual.equalTo(true));
		Assert.assertThat(result.isHub(new ClusterId(8)), IsEqual.equalTo(true));
	}

	@Test
	public void isHubReturnsTrueWhenClusterIdIsNotAHub() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList();
		final Collection<Cluster> hubs = Arrays.asList(new Cluster(new ClusterId(7)), new Cluster(new ClusterId(8)));
		final Collection<Cluster> outliers = Arrays.asList();

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		Assert.assertThat(result.isHub(new ClusterId(5)), IsEqual.equalTo(false));
		Assert.assertThat(result.isHub(new ClusterId(3)), IsEqual.equalTo(false));
	}

	@Test
	public void isOutlierReturnsTrueWhenClusterIdIsAnOutlier() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList();
		final Collection<Cluster> hubs = Arrays.asList();
		final Collection<Cluster> outliers = Arrays.asList(new Cluster(new ClusterId(7)), new Cluster(new ClusterId(8)));

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		Assert.assertThat(result.isOutlier(new ClusterId(7)), IsEqual.equalTo(true));
		Assert.assertThat(result.isOutlier(new ClusterId(8)), IsEqual.equalTo(true));
	}

	@Test
	public void isOutlierReturnsTrueWhenClusterIdIsNotAnOutlier() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList();
		final Collection<Cluster> hubs = Arrays.asList();
		final Collection<Cluster> outliers = Arrays.asList(new Cluster(new ClusterId(7)), new Cluster(new ClusterId(8)));

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		Assert.assertThat(result.isOutlier(new ClusterId(5)), IsEqual.equalTo(false));
		Assert.assertThat(result.isOutlier(new ClusterId(3)), IsEqual.equalTo(false));
	}

	//endregion

	//region getIdForNode

	@Test
	public void getIdForNodeReturnsClusterInformationForAllClusterMembers() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4)),
				new Cluster(new ClusterId(5), NisUtils.toNodeIdList(5, 6, 7, 8, 9)));
		final Collection<Cluster> hubs = Arrays.asList(new Cluster(new NodeId(11)));
		final Collection<Cluster> outliers = Arrays.asList(new Cluster(new NodeId(10)));

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		Assert.assertThat(result.getIdForNode(new NodeId(0)), IsEqual.equalTo(new ClusterId(0)));
		Assert.assertThat(result.getIdForNode(new NodeId(1)), IsEqual.equalTo(new ClusterId(0)));
		Assert.assertThat(result.getIdForNode(new NodeId(2)), IsEqual.equalTo(new ClusterId(0)));
		Assert.assertThat(result.getIdForNode(new NodeId(3)), IsEqual.equalTo(new ClusterId(0)));
		Assert.assertThat(result.getIdForNode(new NodeId(4)), IsEqual.equalTo(new ClusterId(0)));

		Assert.assertThat(result.getIdForNode(new NodeId(5)), IsEqual.equalTo(new ClusterId(5)));
		Assert.assertThat(result.getIdForNode(new NodeId(6)), IsEqual.equalTo(new ClusterId(5)));
		Assert.assertThat(result.getIdForNode(new NodeId(7)), IsEqual.equalTo(new ClusterId(5)));
		Assert.assertThat(result.getIdForNode(new NodeId(8)), IsEqual.equalTo(new ClusterId(5)));
		Assert.assertThat(result.getIdForNode(new NodeId(9)), IsEqual.equalTo(new ClusterId(5)));

		Assert.assertThat(result.getIdForNode(new NodeId(10)), IsEqual.equalTo(new ClusterId(10)));
		Assert.assertThat(result.getIdForNode(new NodeId(11)), IsEqual.equalTo(new ClusterId(11)));
	}

	@Test
	public void getIdForNodeDoesNotReturnClusterInformationForClusterIds() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(1)));
		final Collection<Cluster> hubs = Arrays.asList();
		final Collection<Cluster> outliers = Arrays.asList();

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		Assert.assertThat(result.getIdForNode(new NodeId(0)), IsNull.nullValue());
	}

	@Test
	public void getIdForNodeReturnsNullWhenClusterInformationIsUnknown() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(1)));
		final Collection<Cluster> hubs = Arrays.asList();
		final Collection<Cluster> outliers = Arrays.asList();

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		Assert.assertThat(result.getIdForNode(new NodeId(17)), IsNull.nullValue());
	}

	//endregion
}