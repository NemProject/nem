package org.nem.nis.pox.poi.graph;

import org.nem.nis.pox.poi.graph.repository.NxtDatabaseRepository;
import org.nem.nis.pox.poi.graph.utils.NxtBlockChainAdapter;

public class NxtGraphClusteringITCase extends GraphClusteringITCase {

	public NxtGraphClusteringITCase() {
		super(new NxtDatabaseRepository(), new NxtBlockChainAdapter());
	}
}
