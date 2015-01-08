package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;

public class BlockChainConstantsTest {

	@Test
	public void maxAllowedTransactionsPerBlockIsCalculatedCorrectly() {
		// Assert:
		assertMaxAllowedTransactions(1, 120);
		assertMaxAllowedTransactions(1234, 120);
		assertMaxAllowedTransactions(1000000, 120);
	}

	private static void assertMaxAllowedTransactions(final long height, final int expected) {
		// Assert:
		Assert.assertThat(
				BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK(new BlockHeight(height)),
				IsEqual.equalTo(expected));
	}
}