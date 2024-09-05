package org.nem.nis.controller.viewmodels;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;

/**
 * Tuple containing a mosaic definition along with its corresponding mosaic supply and creation height.
 */
public class MosaicDefinitionSupplyTuple implements SerializableEntity {
	private final MosaicDefinition mosaicDefinition;
	private final Supply supply;
	private final BlockHeight expirationHeight;

	/**
	 * Creates a new mosaic definition supply tuple.
	 *
	 * @param mosaicDefinition Mosaic definition.
	 * @param supply Mosaic supply.
	 * @param expirationHeight Expiration height of owning namespace.
	 */
	public MosaicDefinitionSupplyTuple(final MosaicDefinition mosaicDefinition, final Supply supply, final BlockHeight expirationHeight){
		this.mosaicDefinition = mosaicDefinition;
		this.supply = supply;
		this.expirationHeight = expirationHeight;
	}

	/**
	 * Gets mosaic definition.
	 *
	 * @return Mosaic definition.
	 */
	public MosaicDefinition getMosaicDefinition() {
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

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("mosaicDefinition", this.mosaicDefinition);
		Supply.writeTo(serializer, "supply", this.supply);
		BlockHeight.writeTo(serializer, "expirationHeight", this.expirationHeight);
	}
}
