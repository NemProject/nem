package org.nem.nis.poi.graph;

import org.nem.nis.poi.graph.repository.NxtDatabaseRepository;
import org.nem.nis.poi.graph.utils.NxtBlockChainAdapter;

public class NxtGraphClusteringITCase extends GraphClusteringITCase {

	public NxtGraphClusteringITCase() {
		super(new NxtDatabaseRepository(), new NxtBlockChainAdapter());
	}
}
