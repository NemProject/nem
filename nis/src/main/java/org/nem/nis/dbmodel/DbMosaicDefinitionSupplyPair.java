package org.nem.nis.dbmodel;

import org.nem.core.model.primitive.Supply;

/**
 * Pair of DbMosaicDefinition and Supply.
 */
public class DbMosaicDefinitionSupplyPair {
	private DbMosaicDefinition mosaicDefinition;
	private Supply supply;

	/**
	 * Creates a DbMosaicDefinition and Supply pair.
	 *
	 * @param mosaicDefinition Mosaic definition.
	 * @param supply Mosaic supply.
	 */
	public DbMosaicDefinitionSupplyPair(DbMosaicDefinition mosaicDefinition, Supply supply) {
		this.mosaicDefinition = mosaicDefinition;
		this.supply = supply;
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
};
