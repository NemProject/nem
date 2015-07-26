package org.nem.core.model.mosaic;

import org.nem.core.model.primitive.Quantity;
import org.nem.core.serialization.*;
import org.nem.core.utils.MustBe;

/**
 * A pair comprised of a mosaic id and a quantity.
 */
public class MosaicTransferPair implements SerializableEntity {
	private final MosaicId mosaicId;
	private final Quantity quantity;

	/**
	 * Creates a new pair.
	 *
	 * @param mosaicId The mosaic id.
	 * @param quantity The quantity.
	 */
	public MosaicTransferPair(final MosaicId mosaicId, final Quantity quantity) {
		MustBe.notNull(mosaicId, "mosaicId");
		MustBe.notNull(quantity, "quantity");
		this.mosaicId = mosaicId;
		this.quantity = quantity;
	}

	/**
	 * Deserializes a pair.
	 *
	 * @param deserializer The deserializer.
	 */
	public MosaicTransferPair(final Deserializer deserializer) {
		this.mosaicId = deserializer.readObject("mosaicId", MosaicId::new);
		this.quantity = Quantity.readFrom(deserializer, "quantity");
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
	 * Gets the quantity.
	 *
	 * @return The quantity.
	 */
	public Quantity getQuantity() {
		return this.quantity;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("mosaicId", this.mosaicId);
		Quantity.writeTo(serializer, "quantity", this.quantity);
	}

	@Override
	public String toString() {
		return String.format("%s : %d", this.mosaicId, this.quantity.getRaw());
	}

	@Override
	public int hashCode() {
		return this.mosaicId.hashCode() ^ this.quantity.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof MosaicTransferPair)) {
			return false;
		}

		final MosaicTransferPair rhs = (MosaicTransferPair)obj;
		return this.mosaicId.equals(rhs.mosaicId) &&
				this.quantity.equals(rhs.quantity);
	}
}
