package org.nem.nis.dao.retrievers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNull;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.cache.DefaultAccountCache;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.util.*;
import java.util.stream.Collectors;

public class MultisigTransactionRetrieverTest extends TransactionRetrieverTest {

	@Override
	protected TransactionRetriever getTransactionRetriever() {
		return new MultisigTransactionRetriever();
	}

	@Override
	protected List<Integer> getExpectedComparablePairsForIncomingTransactions(final BlockHeight height, final int accountIndex) {
		// returned list must be sorted in descending order of ids!
		final int baseId = (int) ((height.getRaw() / 2 - 1) * TRANSACTIONS_PER_BLOCK);
		switch (accountIndex) {
			case 0:
			case 1:
				return Collections.emptyList();
			case 2:
				return Arrays.asList(baseId + 20, baseId + 17, baseId + 14, baseId + 10);
			case 3:
			case 4:
				return Collections.emptyList();
			default :
				throw new RuntimeException("unknown account id.");
		}
	}

	@Override
	protected List<Integer> getExpectedComparablePairsForOutgoingTransactions(final BlockHeight height, final int accountIndex) {
		// returned list must be sorted in descending order of ids!
		final int baseId = (int) ((height.getRaw() / 2 - 1) * TRANSACTIONS_PER_BLOCK);
		final List<Integer> expectedIds = Arrays.asList(baseId + 27, baseId + 24, baseId + 20, baseId + 17, baseId + 14, baseId + 10);
		switch (accountIndex) {
			case 0:
			case 1:
			case 2:
				// account 2 is an "inactive" cosignatory and still has to see the outgoing transactions
			case 3:
			case 4:
				return expectedIds;
			default :
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
			final Collection<TransferBlockPair> pairs = retriever.getTransfersForAccount(this.session, this.getAccountId(ACCOUNT),
					Long.MAX_VALUE, 100, transferType);

			// Assert:
			pairs.stream()
					.forEach(p -> MatcherAssert.assertThat(
							DbModelUtils.getInnerTransaction((DbMultisigTransaction) p.getTransfer()).getSenderProof(),
							IsNull.nullValue()));
		}
	}

	// endregion

	// transfer transaction attachment check

	@Test
	public void attachmentsHaveExpectedQuantity() {
		// Arrange:
		final TransactionRetriever retriever = this.getTransactionRetriever();
		final DefaultMapperFactory factory = new DefaultMapperFactory(this.mosaicIdCache);
		final MappingRepository repository = factory.createDbModelToModelMapper(new DefaultAccountCache().copy());

		// Act:
		final Collection<TransferBlockPair> pairs = retriever.getTransfersForAccount(this.session, this.getAccountId(ACCOUNTS[0]),
				Long.MAX_VALUE, 100, ReadOnlyTransferDao.TransferType.OUTGOING);
		final Collection<Long> quantities = pairs.stream()
				.map(p -> DbModelUtils.getInnerTransaction((DbMultisigTransaction) p.getTransfer()))
				.map(t -> repository.map(t, Transaction.class)).filter(t -> TransactionTypes.TRANSFER == t.getType())
				.map(t -> (TransferTransaction) t).map(t -> t.getAttachment().getMosaics()).findFirst().get().stream()
				.map(m -> m.getQuantity().getRaw()).collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(quantities, IsEquivalent.equivalentTo(Arrays.asList(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L)));
	}

	// endregion
}
