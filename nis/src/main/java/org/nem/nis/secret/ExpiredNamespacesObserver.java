package org.nem.nis.secret;

import org.nem.core.model.Address;
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
	private static final int ESTIMATED_BLOCKS_PER_YEAR = 1440 * 365;
	private final NamespaceCache namespaceCache;
	private final AccountStateCache accountStateCache;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache The namespace cache.
	 * @param accountStateCache The account state cache.
	 */
	public ExpiredNamespacesObserver(final NamespaceCache namespaceCache, final AccountStateCache accountStateCache) {
		this.namespaceCache = namespaceCache;
		this.accountStateCache = accountStateCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (NotificationType.BlockHarvest != notification.getType()) {
			return;
		}

		final Collection<NamespaceId> expiredNamespaces = new ArrayList<>();
		final Collection<NamespaceId> rootIds = this.namespaceCache.getRootNamespaceIds();
		rootIds.stream()
				.map(this.namespaceCache::get)
				.filter(ns -> ns.getNamespace().getHeight().getRaw() + ESTIMATED_BLOCKS_PER_YEAR == context.getHeight().getRaw())
				.forEach(ns -> {
					final NamespaceId rootId = ns.getNamespace().getId();
					expiredNamespaces.add(rootId);
					expiredNamespaces.addAll(this.namespaceCache.getSubNamespaceIds(rootId));
				});
		expiredNamespaces.stream()
				.map(this::createMosaicIdToAddressMap)
				.forEach(map -> {
						map.entrySet().stream()
							.forEach(e -> {
									e.getValue().forEach(address -> {
										final AccountInfo info = this.accountStateCache.findStateByAddress(address).getAccountInfo();
										if (NotificationTrigger.Execute == context.getTrigger()) {
											info.removeMosaicId(e.getKey());
										} else {
											info.addMosaicId(e.getKey());
										}
									});
							});
				});
	}

	private Map<MosaicId, Collection<Address>> createMosaicIdToAddressMap(final NamespaceId namespaceId) {
		final Map<MosaicId, Collection<Address>> map = new HashMap<>();
		final ReadOnlyMosaics mosaics = this.namespaceCache.get(namespaceId).getMosaics();
		final Collection<MosaicId> mosaicIds = mosaics.getMosaicIds();
		mosaicIds.stream().forEach(mosaicId -> map.put(mosaicId, mosaics.get(mosaicId).getBalances().getOwners()));
		return map;
	}
}
