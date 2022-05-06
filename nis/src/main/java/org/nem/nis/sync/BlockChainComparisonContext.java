package org.nem.nis.sync;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockChainScore;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.service.BlockChainLastBlockLayer;

/**
 * Context that is used during block chain comparison.
 */
public class BlockChainComparisonContext {
	private final AccountCache accountCache;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final BlockDao blockDao;
	private final BlockChainServices services;
	private final BlockChainScore ourScore;

	public BlockChainComparisonContext(final ReadOnlyAccountCache accountCache, final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final BlockDao blockDao, final BlockChainServices services, final BlockChainScore ourScore) {
		this.accountCache = this.createAccountCacheCopy(accountCache);
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.blockDao = blockDao;
		this.services = services;
		this.ourScore = ourScore;
	}

	/**
	 * Gets the working copy of the account cache.
	 *
	 * @return The account cache.
	 */
	public AccountCache accountCache() {
		return this.accountCache;
	}

	/**
	 * Creates a local block lookup adapter.
	 *
	 * @return The local block lookup adapter.
	 */
	public BlockLookup createLocalBlockLookup() {
		return new LocalBlockLookupAdapter(this.blockDao, this.services.createMapper(this.accountCache),
				this.blockChainLastBlockLayer.getLastDbBlock(), this.ourScore,
				NemGlobals.getBlockChainConfiguration().getMaxBlocksPerSyncAttempt());
	}

	private AccountCache createAccountCacheCopy(final ReadOnlyAccountCache cache) {
		final AccountCache accountCache = new DefaultAccountCache();
		cache.contents().stream().forEach(a -> accountCache.addAccountToCache(a.getAddress()));
		return accountCache;
	}
}
