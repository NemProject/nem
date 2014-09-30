package org.nem.nis.poi.graph;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.test.NisUtils;

import java.util.*;

public class ClusteringResultTest {

	@Test
	public void resultExposesAllConstructorParameters() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(
				new Cluster(new ClusterId(7), NisUtils.toNodeIdList(7)),
				new Cluster(new ClusterId(8), NisUtils.toNodeIdList(8)));
		final Collection<Cluster> hubs = Arrays.asList(new Cluster(new ClusterId(11), NisUtils.toNodeIdList(11)));
		final Collection<Cluster> outliers = Arrays.asList(new Cluster(new ClusterId(10), NisUtils.toNodeIdList(10)));

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		Assert.assertThat(result.getClusters(), IsSame.sameInstance(clusters));
		Assert.assertThat(result.getHubs(), IsSame.sameInstance(hubs));
		Assert.assertThat(result.getOutliers(), IsSame.sameInstance(outliers));
	}

	@Test
	public void numClustersCountedCorrectly() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(
				new Cluster(new ClusterId(7), NisUtils.toNodeIdList(7)),
				new Cluster(new ClusterId(8), NisUtils.toNodeIdList(8)));
		final Collection<Cluster> hubs = Arrays.asList(new Cluster(new ClusterId(11), NisUtils.toNodeIdList(11)));
		final Collection<Cluster> outliers = Arrays.asList(new Cluster(new ClusterId(10), NisUtils.toNodeIdList(10)));

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert
		Assert.assertThat(result.numClusters(), IsEqual.equalTo(4));
	}

	@Test
	public void idMapIsCorrect() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4)),
				new Cluster(new ClusterId(5), NisUtils.toNodeIdList(5, 6, 7, 8, 9)));
		final Collection<Cluster> hubs = Arrays.asList(new Cluster(new ClusterId(11), NisUtils.toNodeIdList(11)));
		final Collection<Cluster> outliers = Arrays.asList(new Cluster(new ClusterId(10), NisUtils.toNodeIdList(10)));

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
	public void numNodesReturnsCorrectNumberOfNodes() {
		// Arrange:
		final Collection<Cluster> clusters = Arrays.asList(
				new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4)),
				new Cluster(new ClusterId(5), NisUtils.toNodeIdList(5, 6, 7, 8, 9)));
		final Collection<Cluster> hubs = Arrays.asList(new Cluster(new ClusterId(11), NisUtils.toNodeIdList(11)));
		final Collection<Cluster> outliers = Arrays.asList(new Cluster(new ClusterId(10), NisUtils.toNodeIdList(10)));

		// Act:
		final ClusteringResult result = new ClusteringResult(clusters, hubs, outliers);

		// Assert:
		Assert.assertThat(result.getNumNodes(), IsEqual.equalTo(12));
	}
}