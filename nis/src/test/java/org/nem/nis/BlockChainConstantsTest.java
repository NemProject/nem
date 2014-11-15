package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;

public class BlockChainConstantsTest {

	@Test
	public void maxAllowedTransactionsPerBlockIsCalculatedCorrectly() {
		// Assert:
		assertMaxAllowedTransactions(1, Integer.MAX_VALUE);
		assertMaxAllowedTransactions(BlockMarkerConstants.BETA_HARD_FORK - 1, Integer.MAX_VALUE);
		assertMaxAllowedTransactions(BlockMarkerConstants.BETA_HARD_FORK, Integer.MAX_VALUE);
		assertMaxAllowedTransactions(BlockMarkerConstants.BETA_HARD_FORK + 1, 60);
		assertMaxAllowedTransactions(BlockMarkerConstants.BETA_TX_COUNT_FORK - 1, 60);
		assertMaxAllowedTransactions(BlockMarkerConstants.BETA_TX_COUNT_FORK, 60);
		assertMaxAllowedTransactions(BlockMarkerConstants.BETA_TX_COUNT_FORK + 1, 120);
		assertMaxAllowedTransactions(1000000, 120);
	}

	private static void assertMaxAllowedTransactions(final long height, final int expected) {
		// Assert:
		Assert.assertThat(
				BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK(new BlockHeight(height)),
				IsEqual.equalTo(expected));
	}
}