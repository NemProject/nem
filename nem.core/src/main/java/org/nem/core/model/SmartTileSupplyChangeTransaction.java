package org.nem.core.model;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.observers.TransactionObserver;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.MustBe;

import java.util.*;

/**
 * A transaction that creates or deletes smart tiles.
 */
public class SmartTileSupplyChangeTransaction extends Transaction {
	private final MosaicId mosaicId;
	private final SmartTileSupplyType supplyType;
	private final Quantity quantity;

	/**
	 * Creates a new smart tile supply change transaction.
	 *
	 * @param timeStamp The timestamp.
	 * @param sender The sender.
	 * @param mosaicId The mosaic id.
	 * @param supplyType The supply type.
	 * @param quantity The quantity.
	 */
	public SmartTileSupplyChangeTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final MosaicId mosaicId,
			final SmartTileSupplyType supplyType,
			final Quantity quantity) {
		super(TransactionTypes.SMART_TILE_SUPPLY_CHANGE, 1, timeStamp, sender);
		this.mosaicId = mosaicId;
		this.supplyType = supplyType;
		this.quantity = quantity;
		this.validate();
	}

	/**
	 * Deserializes a provision namespace transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public SmartTileSupplyChangeTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.SMART_TILE_SUPPLY_CHANGE, options, deserializer);
		this.mosaicId = deserializer.readObject("mosaicId", MosaicId::new);
		this.supplyType = SmartTileSupplyType.fromValueOrDefault(deserializer.readInt("supplyType"));
		this.quantity = Quantity.readFrom(deserializer, "quantity");
		this.validate();
	}

	private void validate() {
		MustBe.notNull(this.mosaicId, "mosaic id");
		MustBe.notNull(this.quantity, "quantity");
		MustBe.inRange(this.quantity.getRaw(), "quantity", 1L, MosaicProperties.MAX_QUANTITY);
		MustBe.notNull(this.supplyType, "supply type");
		// TODO 20150709 J-J: consider adding a MustBe.true / MustBe.false
		if (!this.supplyType.isValid()) {
			throw new IllegalArgumentException("invalid supply type");
		}
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
	 * Gets the supply type.
	 *
	 * @return The supply type.
	 */
	public SmartTileSupplyType getSupplyType() {
		return this.supplyType;
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
	protected Collection<Account> getOtherAccounts() {
		return Collections.emptyList();
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeObject("mosaicId", this.mosaicId);
		serializer.writeInt("supplyType", this.supplyType.value());
		Quantity.writeTo(serializer, "quantity", this.quantity);
	}

	@Override
	protected void transfer(final TransactionObserver observer) {

	}
}
