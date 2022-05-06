package org.nem.nis.pox.poi.graph;

public class FastScanClusteringStrategyTest extends ScanGraphClusteringTest {

	@Override
	protected GraphClusteringStrategy createClusteringStrategy() {
		return new FastScanClusteringStrategy();
	}
}
