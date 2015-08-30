package org.nem.nis.controller.viewmodels;

import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.Supply;
import org.nem.core.serialization.*;
import org.nem.core.utils.MustBe;

/**
 * Pair containing a mosaic id and the corresponding mosaic supply.
 */
public class MosaicIdSupplyPair implements SerializableEntity {
	final MosaicId mosaicId;
	final Supply supply;

	/**
	 * Creates a new mosaic id supply pair.
	 *
	 * @param mosaicId The mosaic id.
	 * @param supply The supply.
	 */
	public MosaicIdSupplyPair(final MosaicId mosaicId, final Supply supply) {
		this.mosaicId = mosaicId;
		this.supply = supply;
		this.validate();
	}

	/**
	 * Deserializes a mosaic id supply pair.
	 *
	 * @param deserializer The deserializer.
	 */
	public MosaicIdSupplyPair(final Deserializer deserializer) {
		this.mosaicId = deserializer.readObject("mosaicId", MosaicId::new);
		this.supply = Supply.readFrom(deserializer, "supply");
	}

	private void validate() {
		MustBe.notNull(this.mosaicId, "mosaic id");
		MustBe.notNull(this.supply, "supply");
	}

	/**
	 * Gets the mosaic id.
	 *
	 * @return The mosaic id.
	 */
	public MosaicId getMosaicId() {
		return this.mosaicId;
	}

	/**
	 * Gets the supply.
	 *
	 * @return The supply.
	 */
	public Supply getSupply() {
		return this.supply;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("mosaicId", this. mosaicId);
		Supply.writeTo(serializer, "supply", this.supply);
	}
}
