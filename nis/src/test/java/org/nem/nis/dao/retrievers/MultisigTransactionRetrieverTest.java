package org.nem.nis.dao.retrievers;

import org.nem.core.model.primitive.BlockHeight;

import java.util.*;

public class MultisigTransactionRetrieverTest extends TransactionRetrieverTest {

	@Override
	protected TransactionRetriever getTransactionRetriever() {
		return new MultisigTransactionRetriever();
	}

	@Override
	protected List<ComparablePair> getExpectedComparablePairsForIncomingTransactions(final BlockHeight height, final int accountIndex) {
		// you have to supply the pairs in descending order of timestamps!!!
		final ArrayList<ComparablePair> pairs = new ArrayList<>();
		switch (accountIndex) {
			case 0:
				return pairs;
			case 1:
				return pairs;
			case 2:
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 9));
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 8));
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 7));
				return pairs;
			case 3:
				return pairs;
			default:
				throw new RuntimeException("unknown account id.");
		}
	}

	@Override
	protected List<ComparablePair> getExpectedComparablePairsForOutgoingTransactions(final BlockHeight height, final int accountIndex) {
		// you have to supply the pairs in descending order of timestamps!!!
		final ArrayList<ComparablePair> pairs = new ArrayList<>();
		switch (accountIndex) {
			case 0:
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 9));
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 8));
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 7));
				return pairs;
			case 1:
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 9));
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 8));
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 7));
				return pairs;
			case 2:
				return pairs;
			case 3:
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 9));
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 8));
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 7));
				return pairs;
			default:
				throw new RuntimeException("unknown account id.");
		}
	}
}
