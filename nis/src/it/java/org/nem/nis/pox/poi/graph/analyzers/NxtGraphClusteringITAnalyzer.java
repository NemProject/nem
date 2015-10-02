package org.nem.nis.pox.poi.graph.analyzers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.nis.pox.poi.graph.repository.*;
import org.nem.nis.pox.poi.graph.utils.NxtBlockChainAdapter;

import java.util.Collection;

/**
 * Fake test class for analyzing the Proof-of-Importance algorithm with the NXT transaction graph.
 */
@Ignore
public class NxtGraphClusteringITAnalyzer extends GraphClusteringITAnalyzer {

	public NxtGraphClusteringITAnalyzer() {
		super(new NxtDatabaseRepository(), new NxtBlockChainAdapter());
	}

	@Test
	public void canQueryNxtTransactionTable() {
		// Act:
		final Collection<GraphClusteringTransaction> transactions = this.loadTransactionData(0, 0);

		// Assert:
		Assert.assertThat(transactions.size(), IsEqual.equalTo(73));
	}
}
