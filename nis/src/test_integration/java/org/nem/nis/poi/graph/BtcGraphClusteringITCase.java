package org.nem.nis.poi.graph;

import org.nem.nis.poi.graph.repository.BtcDatabaseRepository;
import org.nem.nis.poi.graph.utils.BtcBlockChainAdapter;

public class BtcGraphClusteringITCase extends GraphClusteringITCase {

	public BtcGraphClusteringITCase() {
		super(new BtcDatabaseRepository(), new BtcBlockChainAdapter());
	}
}
