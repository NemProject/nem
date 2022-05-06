package org.nem.nis.dbmodel;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.nis.test.*;

public class DbBlockExtensionsTest {

	@Test
	public void countTransactionsReturnsTotalNumberOfTransactionsInBlock() {
		// Arrange;
		final DbBlock block = NisUtils.createDbBlockWithTimeStamp(0);
		for (int i = 0; i < 5; ++i) {
			block.addTransferTransaction(RandomDbTransactionFactory.createTransfer());
		}

		for (int i = 0; i < 3; ++i) {
			block.addMultisigTransaction(RandomDbTransactionFactory.createMultisigTransfer());
		}

		// Act:
		final int numTransaction = DbBlockExtensions.countTransactions(block);

		// Assert:
		MatcherAssert.assertThat(numTransaction, IsEqual.equalTo(5 + 2 * 3));
	}
}
