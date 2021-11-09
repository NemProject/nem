package org.nem.nis.dao.retrievers;

import org.hamcrest.MatcherAssert;
import org.junit.*;
import org.nem.core.model.TransferTransaction;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.cache.DefaultAccountCache;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.dbmodel.TransferBlockPair;
import org.nem.nis.mappers.*;

import java.util.*;
import java.util.stream.Collectors;

public class TransferRetrieverTest extends TransactionRetrieverTest {

	@Override
	protected TransactionRetriever getTransactionRetriever() {
		return new TransferRetriever();
	}

	@Override
	protected List<Integer> getExpectedComparablePairsForIncomingTransactions(final BlockHeight height, final int accountIndex) {
		// returned list must be sorted in descending order of ids!
		final int baseId = (int) ((height.getRaw() / 2 - 1) * TRANSACTIONS_PER_BLOCK);
		switch (accountIndex) {
			case 0:
				return new ArrayList<>();
			case 1:
				return Arrays.asList(baseId + 33, baseId + 31);
			case 2:
				return Collections.singletonList(baseId + 32);
			case 3:
				return Collections.singletonList(baseId + 34);
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
		switch (accountIndex) {
			case 0:
				return Arrays.asList(baseId + 32, baseId + 31);
			case 1:
				return new ArrayList<>();
			case 2:
				return Collections.singletonList(baseId + 33);
			case 3:
				return Collections.singletonList(baseId + 34);
			case 4:
				return Collections.emptyList();
			default :
				throw new RuntimeException("unknown account id.");
		}
	}

	// transfer transaction attachment check

	@Test
	public void attachmentsHaveExpectedQuantity() {
		// Arrange:
		final TransactionRetriever retriever = this.getTransactionRetriever();
		final DefaultMapperFactory factory = new DefaultMapperFactory(this.mosaicIdCache);
		final MappingRepository repository = factory.createDbModelToModelMapper(new DefaultAccountCache());

		// Act:
		final Collection<TransferBlockPair> pairs = retriever.getTransfersForAccount(this.session, this.getAccountId(ACCOUNTS[0]),
				Long.MAX_VALUE, 100, ReadOnlyTransferDao.TransferType.OUTGOING);
		final Collection<Long> quantities = pairs.stream().map(p -> repository.map(p.getTransfer(), TransferTransaction.class))
				.map(t -> t.getAttachment().getMosaics()).findFirst().get().stream().map(m -> m.getQuantity().getRaw())
				.collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(quantities, IsEquivalent.equivalentTo(Arrays.asList(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L)));
	}

	// endregion
}
