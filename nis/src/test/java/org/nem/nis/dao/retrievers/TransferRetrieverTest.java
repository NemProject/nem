package org.nem.nis.dao.retrievers;

import org.nem.core.model.primitive.BlockHeight;

import java.util.*;

public class TransferRetrieverTest extends TransactionRetrieverTest {

	@Override
	protected TransactionRetriever getTransactionRetriever() {
		return new TransferRetriever();
	}

	@Override
	protected List<Integer> getExpectedComparablePairsForIncomingTransactions(final BlockHeight height, final int accountIndex) {
		// returned list must be sorted in descending order of ids!
		final int baseId = (int)((height.getRaw() / 2 - 1) * TRANSACTIONS_PER_BLOCK);
		switch (accountIndex) {
			case 0:
				return new ArrayList<>();
			case 1:
				return Arrays.asList(baseId + 27, baseId + 25);
			case 2:
				return Collections.singletonList(baseId + 26);
			case 3:
				return Collections.singletonList(baseId + 28);
			default:
				throw new RuntimeException("unknown account id.");
		}
	}

	@Override
	protected List<Integer> getExpectedComparablePairsForOutgoingTransactions(final BlockHeight height, final int accountIndex) {
		// returned list must be sorted in descending order of ids!
		final int baseId = (int)((height.getRaw() / 2 - 1) * TRANSACTIONS_PER_BLOCK);
		switch (accountIndex) {
			case 0:
				return Arrays.asList(baseId + 26, baseId + 25);
			case 1:
				return new ArrayList<>();
			case 2:
				return Collections.singletonList(baseId + 27);
			case 3:
				return Collections.singletonList(baseId + 28);
			default:
				throw new RuntimeException("unknown account id.");
		}
	}
}