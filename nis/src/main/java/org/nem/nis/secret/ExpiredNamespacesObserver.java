package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.observers.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * Observer that observes expired namespaces and updates the account's owned mosaic ids.
 */
public class ExpiredNamespacesObserver implements BlockTransactionObserver {
	private final NamespaceCache namespaceCache;
	private final AccountStateCache accountStateCache;
	private final ExpiredMosaicCache expiredMosaicCache;
	private final int estimatedBlocksPerYear;
	private final boolean shouldTrackExpiredMosaics;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache The namespace cache.
	 * @param accountStateCache The account state cache.
	 * @param expiredMosaicCache The expired mosaic cache.
	 * @param estimatedBlocksPerYear The estimated number of blocks per year.
	 * @param shouldTrackExpiredMosaics \c true to track expired mosaics.
	 */
	public ExpiredNamespacesObserver(final NamespaceCache namespaceCache, final AccountStateCache accountStateCache,
			final ExpiredMosaicCache expiredMosaicCache, final int estimatedBlocksPerYear, final boolean shouldTrackExpiredMosaics) {
		this.namespaceCache = namespaceCache;
		this.accountStateCache = accountStateCache;
		this.expiredMosaicCache = expiredMosaicCache;
		this.estimatedBlocksPerYear = estimatedBlocksPerYear;
		this.shouldTrackExpiredMosaics = shouldTrackExpiredMosaics;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (NotificationType.BlockHarvest != notification.getType()) {
			return;
		}

		final long blocksPerYear = NemGlobals.getBlockChainConfiguration().getEstimatedBlocksPerYear();
		final Collection<NamespaceId> expiredNamespaces = new ArrayList<>();
		final Collection<NamespaceId> rootIds = this.namespaceCache.getRootNamespaceIds();
		rootIds.stream().map(this.namespaceCache::get)
				.filter(ns -> ns.getNamespace().getHeight().getRaw() + this.estimatedBlocksPerYear == context.getHeight().getRaw())
				.forEach(ns -> {
					final NamespaceId rootId = ns.getNamespace().getId();
					expiredNamespaces.add(rootId);
					expiredNamespaces.addAll(this.namespaceCache.getSubNamespaceIds(rootId));
				});

		expiredNamespaces.stream().flatMap(this::getMosaicEntries).forEach(mosaicEntry -> {
			final MosaicId mosaicId = mosaicEntry.getMosaicDefinition().getId();

			if (this.shouldTrackExpiredMosaics) {
				if (NotificationTrigger.Execute == context.getTrigger()) {
					this.expiredMosaicCache.addExpiration(context.getHeight(), mosaicId, mosaicEntry.getBalances(),
							ExpiredMosaicType.Expired);
				} else {
					this.expiredMosaicCache.removeExpiration(context.getHeight(), mosaicId);
				}
			}

			mosaicEntry.getBalances().getOwners().forEach(address -> {
				final AccountInfo info = this.accountStateCache.findStateByAddress(address).getAccountInfo();
				if (NotificationTrigger.Execute == context.getTrigger()) {
					info.removeMosaicId(mosaicId);
				} else {
					info.addMosaicId(mosaicId);
				}
			});
		});
	}

	private Stream<ReadOnlyMosaicEntry> getMosaicEntries(final NamespaceId namespaceId) {
		final ReadOnlyMosaics mosaics = this.namespaceCache.get(namespaceId).getMosaics();
		return mosaics.getMosaicIds().stream().map(mosaics::get);
	}
}
