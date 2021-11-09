package org.nem.nis.pox.poi.graph;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.ClusterId;
import org.nem.nis.test.NisUtils;

import java.util.List;
import java.util.stream.Collectors;

public class SingleClusterScanTest {

	@Test
	public void clusterReturnsNoClustersWhenNeighborhoodIsEmpty() {
		// Arrange:
		final Neighborhood neighborhood = Mockito.mock(Neighborhood.class);
		Mockito.when(neighborhood.size()).thenReturn(0);
		final GraphClusteringStrategy strategy = new SingleClusterScan();

		// Act:
		final ClusteringResult result = strategy.cluster(neighborhood);

		// Assert:
		MatcherAssert.assertThat(result.numClusters(), IsEqual.equalTo(0));
	}

	@Test
	public void clusterReturnsOneClusterZeroHubsAndZeroOutliersWhenNeighborhoodIsNotEmpty() {
		// Arrange:
		final Neighborhood neighborhood = Mockito.mock(Neighborhood.class);
		Mockito.when(neighborhood.size()).thenReturn(5);
		final GraphClusteringStrategy strategy = new SingleClusterScan();

		// Act:
		final ClusteringResult result = strategy.cluster(neighborhood);

		// Assert:
		MatcherAssert.assertThat(result.numClusters(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(result.getClusters().size(), IsEqual.equalTo(1));

		final Cluster expectedCluster = new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4));
		final List<Cluster> clusters = result.getClusters().stream().collect(Collectors.toList());
		MatcherAssert.assertThat(clusters.get(0), IsEqual.equalTo(expectedCluster));
	}
}
