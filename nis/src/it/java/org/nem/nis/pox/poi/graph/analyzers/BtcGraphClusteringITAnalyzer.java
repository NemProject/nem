package org.nem.nis.pox.poi.graph.analyzers;

import org.junit.Ignore;
import org.nem.nis.pox.poi.graph.repository.BtcDatabaseRepository;
import org.nem.nis.pox.poi.graph.utils.BtcBlockChainAdapter;

/**
 * Fake test class for analyzing the Proof-of-Importance algorithm with the Bitcoin transaction graph.
 */
@Ignore
public class BtcGraphClusteringITAnalyzer extends GraphClusteringITAnalyzer {

	public BtcGraphClusteringITAnalyzer() {
		super(new BtcDatabaseRepository(), new BtcBlockChainAdapter());
	}
}
