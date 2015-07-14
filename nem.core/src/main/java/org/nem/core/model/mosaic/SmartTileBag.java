package org.nem.core.model.mosaic;

import java.util.*;
import java.util.stream.Collectors;

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
		this.validate();
	}

	private void validate() {
		final Set<MosaicId> set = this.smartTiles.stream().map(SmartTile::getMosaicId).collect(Collectors.toSet());
		if (set.size() != this.smartTiles.size()) {
			throw new IllegalArgumentException("duplicate mosaic id in bag detected");
		}
	}

	/**
	 * Gets the smart tile collections.
	 *
	 * @return The collection of smart tiles.
	 */
	public Collection<SmartTile> getSmartTiles() {
		return Collections.unmodifiableCollection(this.smartTiles);
	}

	/**
	 * Gets a values indicating whether or not the bag is empty.
	 *
	 * @return true if the bag is empty, false otherwise.
	 */
	public boolean isEmpty() {
		return this.smartTiles.isEmpty();
	}
}
