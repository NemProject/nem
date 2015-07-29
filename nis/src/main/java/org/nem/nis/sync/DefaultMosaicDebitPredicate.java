package org.nem.nis.sync;

import org.nem.core.model.Account;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.nis.cache.*;
import org.nem.nis.state.ReadOnlyMosaicEntry;
import org.nem.nis.validators.DebitPredicate;

/**
 * A default mosaic debit predicate implementation.
 */
public class DefaultMosaicDebitPredicate implements DebitPredicate<Mosaic> {
	private final ReadOnlyNamespaceCache namespaceCache;

	/**
	 * Creates a default mosaic debit predicate.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public DefaultMosaicDebitPredicate(final ReadOnlyNamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public boolean canDebit(final Account account, final Mosaic mosaic) {
		final ReadOnlyMosaicEntry mosaicEntry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, mosaic.getMosaicId());
		return null != mosaicEntry &&  mosaicEntry.getBalances().getBalance(account.getAddress()).compareTo(mosaic.getQuantity()) >= 0;
	}
}
