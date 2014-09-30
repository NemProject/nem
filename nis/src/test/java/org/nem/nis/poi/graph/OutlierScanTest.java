package org.nem.nis.poi.graph;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.ClusterId;
import org.nem.nis.test.NisUtils;

public class OutlierScanTest {

	@Test
	public void clusterReturnsClusteringWithZeroClustersZeroHubsAndNeighborhoodSizeOutliers() {
		// Arrange:
		Neighborhood neighborhood = Mockito.mock(Neighborhood.class);
		OutlierScan scan = new OutlierScan();
		Mockito.when(neighborhood.size()).thenReturn(5);

		// Act:
		ClusteringResult result = scan.cluster(neighborhood);

		// Assert:
		Assert.assertThat(result.getClusters().size(), IsEqual.equalTo(0));
		Assert.assertThat(result.getHubs().size(), IsEqual.equalTo(0));
		Assert.assertThat(result.getOutliers().size(), IsEqual.equalTo(5));
		int i = 0;
		for (Cluster outlier : result.getOutliers()) {
			Assert.assertThat(outlier, IsEqual.equalTo(new Cluster(new ClusterId(i), NisUtils.toNodeIdList(i++))));
		}
	}
}
