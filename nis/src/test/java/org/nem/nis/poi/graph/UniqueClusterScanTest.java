package org.nem.nis.poi.graph;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.ClusterId;
import org.nem.nis.test.NisUtils;

public class UniqueClusterScanTest {

	@Test
	public void clusterReturnsClusteringWithOneClusterZeroHubsAndZeroOutliers() {
		// Arrange:
		Neighborhood neighborhood = Mockito.mock(Neighborhood.class);
		UniqueClusterScan scan = new UniqueClusterScan();
		Mockito.when(neighborhood.size()).thenReturn(5);

		// Act:
		ClusteringResult result = scan.cluster(neighborhood);

		// Assert:
		Assert.assertThat(result.getClusters().size(), IsEqual.equalTo(1));
		for (Cluster cluster : result.getClusters()) {
			Assert.assertThat(cluster, IsEqual.equalTo(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4))));
		}
		Assert.assertThat(result.getHubs().size(), IsEqual.equalTo(0));
		Assert.assertThat(result.getOutliers().size(), IsEqual.equalTo(0));
	}
}
