package org.nem.nis.dao.retrievers;

import java.util.*;
import org.nem.core.model.primitive.BlockHeight;

public class MosaicSupplyChangeRetrieverTest extends TransactionRetrieverTest {

	@Override
	protected TransactionRetriever getTransactionRetriever() {
		return new MosaicSupplyChangeRetriever();
	}

	@Override
	protected List<Integer> getExpectedComparablePairsForIncomingTransactions(final BlockHeight height, final int accountIndex) {
		// returned list must be sorted in descending order of ids!
		switch (accountIndex) {
			case 0:
			case 1:
			case 2:
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
		switch (accountIndex) {
			case 0:
				return Collections.singletonList(baseId + 6);
			case 1:
			case 2:
			case 3:
			case 4:
				return Collections.emptyList();
			default :
				throw new RuntimeException("unknown account id.");
		}
	}
}
