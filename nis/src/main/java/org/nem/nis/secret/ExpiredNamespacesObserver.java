package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.observers.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;

import java.util.*;

/**
 * Observer that observes expired namespaces and updates the account's owned mosaic ids.
 */
public class ExpiredNamespacesObserver implements BlockTransactionObserver {
	private final NamespaceCache namespaceCache;
	private final AccountStateCache accountStateCache;
	private final int estimatedBlocksPerYear;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache The namespace cache.
	 * @param accountStateCache The account state cache.
	 * @param estimatedBlocksPerYear The estimated number of blocks per year.
	 */
	public ExpiredNamespacesObserver(final NamespaceCache namespaceCache, final AccountStateCache accountStateCache,
			final int estimatedBlocksPerYear) {
		this.namespaceCache = namespaceCache;
		this.accountStateCache = accountStateCache;
		this.estimatedBlocksPerYear = estimatedBlocksPerYear;
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
		expiredNamespaces.stream().map(this::createMosaicIdToAddressMap).forEach(map -> {
			map.forEach((key, value) -> value.forEach(address -> {
				final AccountInfo info = this.accountStateCache.findStateByAddress(address).getAccountInfo();
				if (NotificationTrigger.Execute == context.getTrigger()) {
					info.removeMosaicId(key);
				} else {
					info.addMosaicId(key);
				}
			}));
		});
	}

	private Map<MosaicId, Collection<Address>> createMosaicIdToAddressMap(final NamespaceId namespaceId) {
		final Map<MosaicId, Collection<Address>> map = new HashMap<>();
		final ReadOnlyMosaics mosaics = this.namespaceCache.get(namespaceId).getMosaics();
		final Collection<MosaicId> mosaicIds = mosaics.getMosaicIds();
		mosaicIds.forEach(mosaicId -> map.put(mosaicId, mosaics.get(mosaicId).getBalances().getOwners()));
		return map;
	}
}
