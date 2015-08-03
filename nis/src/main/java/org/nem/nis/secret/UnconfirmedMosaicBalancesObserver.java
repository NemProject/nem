package org.nem.nis.secret;

import org.nem.core.model.Account;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.nis.cache.*;
import org.nem.nis.state.ReadOnlyMosaicEntry;

import java.util.*;

/**
 * An observer that updates unconfirmed mosaic balance information.
 * TODO 20150820 J-J: try to merge with unconfirmed mosaic balances observer
 */
public class UnconfirmedMosaicBalancesObserver implements TransactionObserver {
	private final ReadOnlyNamespaceCache namespaceCache;
	private final Map<Account, Map<MosaicId, Long>> map = new HashMap<>();

	/**
	 * Creates an unconfirmed mosaic balances observer.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public UnconfirmedMosaicBalancesObserver(final ReadOnlyNamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	/**
	 * Gets the (unconfirmed) mosaic balance of the specified account and mosaic id.
	 *
	 * @param account The account.
	 * @param mosaicId The mosaic id.
	 * @return The quantity.
	 */
	public Quantity get(final Account account, final MosaicId mosaicId) {
		final Quantity confirmedQuantity = this.getMosaicBalance(account, mosaicId);
		return Quantity.fromValue(confirmedQuantity.getRaw() + this.getDelta(account, mosaicId));
	}

	@Override
	public void notify(final Notification notification) {
		if (!NotificationType.MosaicTransfer.equals(notification.getType())) {
			return;
		}

		final MosaicTransferNotification n = (MosaicTransferNotification)notification;
		this.addToCache(n.getSender());
		this.addToCache(n.getRecipient());
		this.adjustBalance(n.getSender(), n.getMosaicId(), -n.getQuantity().getRaw());
		this.adjustBalance(n.getRecipient(), n.getMosaicId(), n.getQuantity().getRaw());
	}

	private Quantity adjustBalance(final Account account, final MosaicId mosaicId, final Long delta) {
		final Map<MosaicId, Long> mosaics = this.map.get(account);
		final Long newQuantity = mosaics.getOrDefault(mosaicId, 0L) + delta;
		mosaics.put(mosaicId, newQuantity);

		// if, for some reason, a transaction got validated but meanwhile the confirmed mosaic balance changed,
		// it's probably better to let an exception bubble out here (the following line will throw
		// if the new unconfirmed mosaic balance is negative)
		return this.get(account, mosaicId);
	}

	private void addToCache(final Account account) {
		// it's ok to put reference here, thanks to Account being non-mutable
		this.map.putIfAbsent(account, new HashMap<>());
	}

	/**
	 * Clears the cache.
	 */
	public void clearCache() {
		this.map.clear();
	}

	/**
	 * Gets a value indicating whether or not all unconfirmed mosaic balances are valid (non-negative).
	 *
	 * @return true if all unconfirmed mosaic balances are valid, false otherwise.
	 */
	public boolean unconfirmedMosaicBalancesAreValid() {
		boolean valid = true;
		for (final Account account : this.map.keySet()) {
			final Map<MosaicId, Long> mosaics = this.map.get(account);
			valid &= mosaics.keySet().stream()
					.allMatch(mosaicId -> this.getMosaicBalance(account, mosaicId).getRaw() + mosaics.get(mosaicId) >= 0);
		}

		return valid;
	}

	private Long getDelta(final Account account, final MosaicId mosaicId) {
		final Map<MosaicId, Long> mosaics = this.map.get(account);
		return null == mosaics ? 0L : mosaics.getOrDefault(mosaicId, 0L);
	}

	private Quantity getMosaicBalance(final Account account, final MosaicId mosaicId) {
		final ReadOnlyMosaicEntry mosaicEntry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, mosaicId);
		return null == mosaicEntry ? Quantity.ZERO : mosaicEntry.getBalances().getBalance(account.getAddress());
	}
}
