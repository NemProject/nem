package org.nem.nis.poi.graph;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.ClusterId;
import org.nem.nis.test.NisUtils;

public class OutlierScanTest {

	@Test
	public void clusterReturnsNoClustersWhenNeighborhoodIsEmpty() {
		// Arrange:
		final Neighborhood neighborhood = Mockito.mock(Neighborhood.class);
		Mockito.when(neighborhood.size()).thenReturn(0);
		final GraphClusteringStrategy strategy = new OutlierScan();

		// Act:
		final ClusteringResult result = strategy.cluster(neighborhood);

		// Assert:
		Assert.assertThat(result.numClusters(), IsEqual.equalTo(0));
	}

	@Test
	public void clusterReturnsZeroClustersZeroHubsAndOneOutlierForEachNodeWhenNeighborhoodIsNotEmpty() {
		// Arrange:
		final Neighborhood neighborhood = Mockito.mock(Neighborhood.class);
		Mockito.when(neighborhood.size()).thenReturn(5);
		final GraphClusteringStrategy strategy = new OutlierScan();

		// Act:
		final ClusteringResult result = strategy.cluster(neighborhood);

		// Assert:
		Assert.assertThat(result.numClusters(), IsEqual.equalTo(5));
		Assert.assertThat(result.getOutliers().size(), IsEqual.equalTo(5));

		int i = 0;
		for (final Cluster outlier : result.getOutliers()) {
			Assert.assertThat(outlier, IsEqual.equalTo(new Cluster(new ClusterId(i), NisUtils.toNodeIdList(i))));
			++i;
		}
	}
}
