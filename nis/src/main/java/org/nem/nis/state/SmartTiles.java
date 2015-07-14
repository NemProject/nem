package org.nem.nis.state;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Smart tile information.
 */
public class SmartTiles implements ReadOnlySmartTiles {
	private final ConcurrentHashMap<MosaicId, Quantity> map = new ConcurrentHashMap<>();

	// region ReadOnlySmartTiles

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

	/**
	 * Increases the supply for a given smart tile.
	 *
	 * @param smartTile The smart tile
	 * @return The new supply quantity.
	 */
	public Quantity increaseSupply(final SmartTile smartTile) {
		final Quantity newQuantity = this.getCurrentSupply(smartTile.getMosaicId()).add(smartTile.getQuantity());
		this.map.put(smartTile.getMosaicId(), newQuantity);
		return newQuantity;
	}

	/**
	 * Decreases the supply for a given smart tile.
	 *
	 * @param smartTile The smart tile
	 * @return The new supply quantity.
	 */
	public Quantity decreaseSupply(final SmartTile smartTile) {
		final Quantity newQuantity = this.getCurrentSupply(smartTile.getMosaicId()).subtract(smartTile.getQuantity());
		this.map.put(smartTile.getMosaicId(), newQuantity);
		return newQuantity;
	}

	/**
	 * Creates a copy of this container.
	 *
	 * @return A copy of this container.
	 */
	public SmartTiles copy() {
		// note that mosaic ids and quantities are immutable
		final SmartTiles copy = new SmartTiles();
		copy.map.putAll(this.map);
		return copy;
	}
}
