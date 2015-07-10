package org.nem.nis.state;

import org.nem.core.model.mosaic.*;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Container for smart tile information for an account.
 */
public class SmartTileMap implements ReadOnlySmartTileMap {
	private final ConcurrentHashMap<MosaicId, SmartTile> map = new ConcurrentHashMap<>();

	// region ReadOnlySmartTileMap

	@Override
	public int size() {
		return this.map.size();
	}

	@Override
	public SmartTile get(final MosaicId mosaicId) {
		return this.map.get(mosaicId);
	}

	@Override
	public boolean contains(final MosaicId mosaicId) {
		return this.map.containsKey(mosaicId);
	}

	// endregion

	/**
	 * Add a smart tile to the map. If there already is an entry with the same mosaic id,
	 * the map is updated with a smart tile which has the sum of the quantities.
	 *
	 * @param smartTile The smart tile to add.
	 * @return The result smart tile.
	 */
	public SmartTile add(final SmartTile smartTile) {
		final SmartTile existingSmartTile = this.map.putIfAbsent(smartTile.getMosaicId(), smartTile);
		if (null != existingSmartTile) {
			final SmartTile newSmartTile = existingSmartTile.add(smartTile);
			this.map.put(newSmartTile.getMosaicId(), newSmartTile);
			return newSmartTile;
		}

		return smartTile;
	}

	/**
	 * Subtracts a smart tile. If there is no entry with the same mosaic id or if the resulting quantity is negative,
	 * an exception is thrown. Else the map is updated with a smart tile which has the difference of the quantities.
	 *
	 * @param smartTile The smart tile to add.
	 * @return The result smart tile.
	 */
	public SmartTile subtract(final SmartTile smartTile) {
		final SmartTile existingSmartTile = this.map.get(smartTile.getMosaicId());
		if (null == existingSmartTile) {
			throw new IllegalArgumentException(String.format("smart tile with id '%s' does not exist.", smartTile.getMosaicId()));
		}

		// note: don't remove entry even if it has zero quantity
		// (needed to check if account ever owned a smart tile with that mosaic id)
		final SmartTile newSmartTile = existingSmartTile.subtract(smartTile);
		this.map.put(newSmartTile.getMosaicId(), newSmartTile);
		return newSmartTile;
	}

	/**
	 * Creates a copy of this SmartTileMap.
	 *
	 * @return The copy.
	 */
	public SmartTileMap copy() {
		// mosaic ids and smart tiles are immutable
		final SmartTileMap copy = new SmartTileMap();
		copy.map.putAll(this.map);
		return copy;
	}
}
