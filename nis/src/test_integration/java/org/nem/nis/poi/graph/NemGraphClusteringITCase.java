package org.nem.nis.poi.graph;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.nis.state.*;

import java.util.*;
/**
 * Integration test class for analysis the Proof-of-Importance algorithm with the NEM transaction graph.
 */
public class NemGraphClusteringITCase extends GraphClusteringITCase {
	private static final String BLOCKCHAIN_TYPE = "Nem";
	/**
	 * Default contructor - where we set the parameters for
	 * analyzing the NEM blockchain.
	 */
	public NemGraphClusteringITCase() {
		super(1337, BLOCKCHAIN_TYPE, new NemDatabaseRepository()); // TODO: determine a good defaultEndHeight
	}

	protected Map<Address, AccountState> createAccountStatesFromTransactionData(final Collection<GraphClusteringTransaction> transactions) {
		return null; //TODO;
	}
}
