package org.nem.core.model.ncc;

import org.nem.core.model.primitive.Supply;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

/**
 * Class holding additional information about mosaic.
 */
public class MosaicMetaData extends DefaultMetaData implements SerializableEntity {
	private final Supply supply;

	/**
	 * Creates a new mosaic meta data.
	 *
	 * @param id The entity id.
	 * @param supply Actual mosaic supply.
	 */
	public MosaicMetaData(final Long id, final Supply supply) {
		super(id);
		this.supply = supply;
	}

	/**
	 * Deserializes a mosaic metadata.
	 *
	 * @param deserializer The deserializer.
	 */
	public MosaicMetaData(final Deserializer deserializer) {
		super(deserializer);
		this.supply = Supply.readFrom(deserializer, "supply");
	}

	/**
	 * Gets the mosaic supply.
	 *
	 * @return The supply.
	 */
	public Supply getSupply() {
		return this.supply;
	}

	@Override
	public void serialize(final Serializer serializer) {
		super.serialize(serializer);
		Supply.writeTo(serializer, "supply", this.supply);
	}
}
