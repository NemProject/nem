package org.nem.nis.cache;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;

import java.util.concurrent.ConcurrentHashMap;

/**
 * General class for keeping track of smart tile supply.
 */
public class DefaultSmartTileSupplyCache implements SmartTileSupplyCache, CopyableCache<DefaultSmartTileSupplyCache> {
	private final ConcurrentHashMap<MosaicId, Quantity> map = new ConcurrentHashMap<>();

	// region ReadOnlySmartTileSupplyCache

	@Override
	public int size() {
		return this.map.size();
	}

	@Override
	public Quantity getCurrentSupply(final MosaicId mosaicId) {
		final Quantity quantity = this.map.get(mosaicId);
		return null == quantity ? Quantity.ZERO : quantity;
	}

	// endregion

	// region SmartTileSupplyCache

	@Override
	public Quantity increaseSupply(final SmartTile smartTile) {
		final Quantity newQuantity = this.getCurrentSupply(smartTile.getMosaicId()).add(smartTile.getQuantity());
		this.map.put(smartTile.getMosaicId(), newQuantity);
		return newQuantity;
	}

	@Override
	public Quantity decreaseSupply(final SmartTile smartTile) {
		final Quantity newQuantity = this.getCurrentSupply(smartTile.getMosaicId()).subtract(smartTile.getQuantity());
		this.map.put(smartTile.getMosaicId(), newQuantity);
		return newQuantity;
	}

	// endregion

	// region CopyableCache

	@Override
	public void shallowCopyTo(final DefaultSmartTileSupplyCache rhs) {
		rhs.map.clear();
		rhs.map.putAll(this.map);
	}

	@Override
	public DefaultSmartTileSupplyCache copy() {
		// Mosaic id and Quantity objects are immutable
		final DefaultSmartTileSupplyCache copy = new DefaultSmartTileSupplyCache();
		copy.map.putAll(this.map);
		return copy;
	}

	// endregion
}
