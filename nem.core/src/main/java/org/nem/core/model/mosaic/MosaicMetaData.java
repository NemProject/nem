package org.nem.core.model.mosaic;

import org.nem.core.model.primitive.Supply;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

/**
 * Class holding additional information about mosaic.
 */
public class MosaicMetaData implements SerializableEntity {
	private final Supply supply;

	/**
	 * Creates a new mosaic meta data.
	 *
	 * @param supply Actual mosaic supply.
	 */
	public MosaicMetaData(Supply supply) {
		this.supply = supply;
	}

	/**
	 * Deserializes a mosaic definition.
	 * @param deserializer
	 */
	public MosaicMetaData(final Deserializer deserializer) {
		supply = Supply.readFrom(deserializer, "supply");
	}

	/**
	 * Gets the mosaic supply.
	 *
	 * @return The supply.
	 */
	public Supply getSupply() {
		return supply;
	}

	@Override
	public void serialize(final Serializer serializer) {
		Supply.writeTo(serializer, "supply", this.supply);
	}
}
