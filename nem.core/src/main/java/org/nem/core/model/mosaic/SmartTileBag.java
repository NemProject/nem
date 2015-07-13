package org.nem.core.model.mosaic;

import java.util.*;

/**
 * A bag for smart tiles.
 */
public class SmartTileBag {
	private Collection<SmartTile> smartTiles = new ArrayList<>();

	/**
	 * Creates a bag from a collection of smart tiles.
	 *
	 * @param smartTiles The collection of smart tiles.
	 */
	public SmartTileBag(final Collection<SmartTile> smartTiles) {
		this.smartTiles.addAll(smartTiles);
	}

	/**
	 * Gets the smart tile collections.
	 *
	 * @return The collection of smart tiles.
	 */
	public Collection<SmartTile> getSmartTiles() {
		return Collections.unmodifiableCollection(this.smartTiles);
	}
}
