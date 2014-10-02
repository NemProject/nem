package org.nem.nis.poi.graph;

public class FastScanTest extends ScanGraphClusteringTest {

	@Override
	protected GraphClusteringStrategy createClusteringStrategy() {
		return new FastScan();
	}
}
