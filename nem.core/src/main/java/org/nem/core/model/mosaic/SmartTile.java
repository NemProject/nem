package org.nem.core.model.mosaic;

import org.nem.core.model.primitive.Quantity;
import org.nem.core.serialization.*;
import org.nem.core.utils.MustBe;

/**
 * An instance of a mosaic.
 * TODO 20150715 J-B: i'm not sure if i see a good reason for this class
 * > i think in most cases it can be replaced with just a mosaic id
 * > the supply change notification is the only place i see it being used where mosaic + quantity are both needed (without an external quantity too)
 * TODO 20150716 BR -> J: see trello mosaic card.
 */
public class SmartTile implements SerializableEntity {
	private final MosaicId mosaicId;
	private final Quantity quantity;

	/**
	 * Creates a new smart tile.
	 *
	 * @param mosaicId The mosaic id.
	 * @param quantity The quantity.
	 */
	public SmartTile(final MosaicId mosaicId, final Quantity quantity) {
		MustBe.notNull(mosaicId, "mosaicId");
		MustBe.notNull(quantity, "quantity");
		MustBe.inRange(quantity.getRaw(), "quantity", 0L, MosaicConstants.MAX_QUANTITY);

		this.mosaicId = mosaicId;
		this.quantity = quantity;
	}

	/**
	 * Deserializes a smart tile.
	 *
	 * @param deserializer The deserializer.
	 */
	public SmartTile(final Deserializer deserializer) {
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

	/**
	 * Adds a smart tile to this smart tile.
	 *
	 * @param smartTile The smart tile to add.
	 * @return The resulting smart tile.
	 */
	public SmartTile add(final SmartTile smartTile) {
		if (!this.mosaicId.equals(smartTile.mosaicId)) {
			throw new IllegalArgumentException("cannot add smart tiles with different mosaic id");
		}

		final Quantity newQuantity = this.quantity.add(smartTile.quantity);
		// TODO 20150710 J-B: why not validate this in the quantity constructor?
		// TODO 20150711 BR -> J: that would make the Quantity class mosaic specific. Is that wanted?
		MustBe.inRange(newQuantity.getRaw(), "new quantity", 0L, MosaicConstants.MAX_QUANTITY);
		return new SmartTile(this.mosaicId, newQuantity);
	}

	/**
	 * Subtracts a smart tile from this smart tile.
	 *
	 * @param smartTile The smart tile to subtract.
	 * @return The resulting smart tile.
	 */
	public SmartTile subtract(final SmartTile smartTile) {
		if (!this.mosaicId.equals(smartTile.mosaicId)) {
			throw new IllegalArgumentException("cannot subtract smart tiles with different mosaic id");
		}

		// note: Quantity class checks for negative quantity
		final Quantity newQuantity = this.quantity.subtract(smartTile.quantity);
		return new SmartTile(this.mosaicId, newQuantity);
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
		if (!(obj instanceof SmartTile)) {
			return false;
		}

		final SmartTile rhs = (SmartTile)obj;

		return this.mosaicId.equals(rhs.mosaicId) &&
				this.quantity.equals(rhs.quantity);
	}
}
