package org.nem.nis.dbmodel;

import org.nem.core.model.primitive.*;

/**
 * Tuple containing a (DB) mosaic definition along with its corresponding mosaic supply and creation height.
 */
public class DbMosaicDefinitionSupplyTuple {
	private DbMosaicDefinition mosaicDefinition;
	private Supply supply;
	private BlockHeight expirationHeight;

	/**
	 * Creates a new (DB) mosaic definition supply tuple.
	 *
	 * @param mosaicDefinition Mosaic definition.
	 * @param supply Mosaic supply.
	 * @param expirationHeight Creation height of owning namespace.
	 */
	public DbMosaicDefinitionSupplyTuple(final DbMosaicDefinition mosaicDefinition, final Supply supply, final BlockHeight expirationHeight) {
		this.mosaicDefinition = mosaicDefinition;
		this.supply = supply;
		this.expirationHeight = expirationHeight;
	}

	/**
	 * Gets mosaic definition.
	 *
	 * @return Mosaic definition.
	 */
	public DbMosaicDefinition getMosaicDefinition() {
		return this.mosaicDefinition;
	}

	/**
	 * Gets mosaic supply.
	 *
	 * @return Mosaic supply.
	 */
	public Supply getSupply() {
		return this.supply;
	}

	/**
	 * Gets expiration height of owning namespace.
	 *
	 * @return Expiration height of owning namespace.
	 */
	public BlockHeight getExpirationHeight() {
		return this.expirationHeight;
	}
};
