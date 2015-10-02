package org.nem.nis.pox.poi.graph;

import org.nem.nis.pox.poi.graph.repository.BtcDatabaseRepository;
import org.nem.nis.pox.poi.graph.utils.BtcBlockChainAdapter;

public class BtcGraphClusteringITCase extends GraphClusteringITCase {

	public BtcGraphClusteringITCase() {
		super(new BtcDatabaseRepository(), new BtcBlockChainAdapter());
	}
}
