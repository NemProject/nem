package org.nem.core.model.ncc;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Supply;
import org.nem.core.serialization.*;
import org.nem.core.utils.MustBe;

/**
 * Pair containing a mosaic definition and the corresponding mosaic supply.
 */
public class MosaicDefinitionSupplyPair implements SerializableEntity {
	private final MosaicDefinition mosaicDefinition;
	private final Supply supply;

	/**
	 * Creates a new mosaic definition supply pair.
	 *
	 * @param mosaicDefinition The mosaic definition.
	 * @param supply The supply.
	 */
	public MosaicDefinitionSupplyPair(final MosaicDefinition mosaicDefinition, final Supply supply) {
		this.mosaicDefinition = mosaicDefinition;
		this.supply = supply;
		this.validate();
	}

	/**
	 * Deserializes a mosaic definition supply pair.
	 *
	 * @param deserializer The deserializer.
	 */
	public MosaicDefinitionSupplyPair(final Deserializer deserializer) {
		this.mosaicDefinition = deserializer.readObject("mosaicDefinition", MosaicDefinition::new);
		this.supply = Supply.readFrom(deserializer, "supply");
	}

	private void validate() {
		MustBe.notNull(this.mosaicDefinition, "mosaic definition");
		MustBe.notNull(this.supply, "supply");
	}

	/**
	 * Gets the mosaic definition.
	 *
	 * @return The mosaic definition.
	 */
	public MosaicDefinition getMosaicDefinition() {
		return this.mosaicDefinition;
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
		serializer.writeObject("mosaicDefinition", this.mosaicDefinition);
		Supply.writeTo(serializer, "supply", this.supply);
	}

	@Override
	public int hashCode() {
		return this.mosaicDefinition.hashCode() ^ this.supply.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof MosaicDefinitionSupplyPair)) {
			return false;
		}

		final MosaicDefinitionSupplyPair rhs = (MosaicDefinitionSupplyPair)obj;
		return this.mosaicDefinition.equals(rhs.mosaicDefinition) &&
				this.supply.equals(rhs.supply);
	}
}
