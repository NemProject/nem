package org.nem.nis.poi.graph;

public class ScanTest extends ScanGraphClusteringTest {

	@Override
	protected GraphClusteringStrategy createClusteringStrategy() {
		return new Scan();
	}
}
