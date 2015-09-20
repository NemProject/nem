package org.nem.nis.pox.poi.graph;

public class ScanClusteringStrategyTest extends ScanGraphClusteringTest {

	@Override
	protected GraphClusteringStrategy createClusteringStrategy() {
		return new ScanClusteringStrategy();
	}
}
