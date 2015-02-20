package org.nem.nis.dao;

import org.nem.core.model.primitive.BlockHeight;

import java.util.*;
public class TransferRetrieverTest extends TransactionRetrieverTest {

	@Override
	protected TransactionRetriever getTransactionRetriever() {
		return new TransferRetriever();
	}

	@Override
	protected List<ComparablePair> getExpectedComparablePairsForIncomingTransactions(final BlockHeight height, final int accountIndex) {
		final ArrayList<ComparablePair> pairs = new ArrayList<>();
		switch (accountIndex) {
			case 0:
				return pairs;
			case 1:
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw()));
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 2));
				return pairs;
			case 2:
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 1));
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 7));
				return pairs;
			case 3:
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 3));
				return pairs;
			default:
				throw new RuntimeException("unknown account id.");
		}
	}

	@Override
	protected List<ComparablePair> getExpectedComparablePairsForOutgoingTransactions(final BlockHeight height, final int accountIndex) {
		final ArrayList<ComparablePair> pairs = new ArrayList<>();
		switch (accountIndex) {
			case 0:
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw()));
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 1));
				return pairs;
			case 1:
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 7));
				return pairs;
			case 2:
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 2));
				return pairs;
			case 3:
				pairs.add(new ComparablePair(height.getRaw(), 100 * (int)height.getRaw() + 3));
				return pairs;
			default:
				throw new RuntimeException("unknown account id.");
		}
	}

	// TODO: add more tests
}