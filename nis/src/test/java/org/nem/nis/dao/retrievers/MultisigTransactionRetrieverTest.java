package org.nem.nis.dao.retrievers;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.DbModelUtils;

import java.util.*;

public class MultisigTransactionRetrieverTest extends TransactionRetrieverTest {

	@Override
	protected TransactionRetriever getTransactionRetriever() {
		return new MultisigTransactionRetriever();
	}

	@Override
	protected List<Integer> getExpectedComparablePairsForIncomingTransactions(final BlockHeight height, final int accountIndex) {
		// returned list must be sorted in descending order of ids!
		final int baseId = (int)((height.getRaw() / 2 - 1) * TRANSACTIONS_PER_BLOCK);
		switch (accountIndex) {
			case 0:
			case 1:
				return Collections.emptyList();
			case 2:
				return Arrays.asList(baseId + 20, baseId + 17, baseId + 14, baseId + 10);
			case 3:
			case 4:
				return Collections.emptyList();
			default:
				throw new RuntimeException("unknown account id.");
		}
	}

	@Override
	protected List<Integer> getExpectedComparablePairsForOutgoingTransactions(final BlockHeight height, final int accountIndex) {
		// returned list must be sorted in descending order of ids!
		final int baseId = (int)((height.getRaw() / 2 - 1) * TRANSACTIONS_PER_BLOCK);
		final List<Integer> expectedIds = Arrays.asList(baseId + 27, baseId + 24, baseId + 20, baseId + 17, baseId + 14, baseId + 10);
		switch (accountIndex) {
			case 0:
			case 1:
			case 2:
				// account 2 is an "inactive" cosignatory and still has to see the outgoing transactions
			case 3:
			case 4:
				return expectedIds;
			default:
				throw new RuntimeException("unknown account id.");
		}
	}

	// region signature check

	@Test
	public void incomingInnerTransactionsHaveNullSignatures() {
		// Assert:
		this.assertInnerTransactionsHaveNullSignatures(ReadOnlyTransferDao.TransferType.INCOMING);
	}

	@Test
	public void outgoingInnerTransactionsHaveNullSignatures() {
		// Assert:
		this.assertInnerTransactionsHaveNullSignatures(ReadOnlyTransferDao.TransferType.OUTGOING);
	}

	private void assertInnerTransactionsHaveNullSignatures(final ReadOnlyTransferDao.TransferType transferType) {
		// Arrange:
		final TransactionRetriever retriever = this.getTransactionRetriever();
		for (final Account ACCOUNT : ACCOUNTS) {
			// Act:
			final Collection<TransferBlockPair> pairs = retriever.getTransfersForAccount(
					this.session,
					this.getAccountId(ACCOUNT),
					Long.MAX_VALUE,
					100,
					transferType);

			// Assert:
			pairs.stream().forEach(p -> Assert.assertThat(
					DbModelUtils.getInnerTransaction((DbMultisigTransaction)p.getTransfer()).getSenderProof(),
					IsNull.nullValue()));
		}
	}

	// endregion
}
