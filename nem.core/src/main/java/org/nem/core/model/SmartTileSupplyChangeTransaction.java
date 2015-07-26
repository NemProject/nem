package org.nem.core.model;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Supply;
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
	private final Supply delta;

	/**
	 * Creates a new smart tile supply change transaction.
	 *
	 * @param timeStamp The timestamp.
	 * @param sender The sender.
	 * @param mosaicId The mosaic id.
	 * @param supplyType The supply type.
	 * @param delta The delta.
	 */
	public SmartTileSupplyChangeTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final MosaicId mosaicId,
			final SmartTileSupplyType supplyType,
			final Supply delta) {
		super(TransactionTypes.SMART_TILE_SUPPLY_CHANGE, 1, timeStamp, sender);
		this.mosaicId = mosaicId;
		this.supplyType = supplyType;
		this.delta = delta;
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
		this.delta = Supply.readFrom(deserializer, "delta");
		this.validate();
	}

	private void validate() {
		MustBe.notNull(this.mosaicId, "mosaic id");
		MustBe.notNull(this.delta, "delta");
		MustBe.inRange(this.delta.getRaw(), "delta", 1L, MosaicConstants.MAX_QUANTITY);
		MustBe.notNull(this.supplyType, "supply type");
		MustBe.trueValue(this.supplyType.isValid(), "supply type validity");
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
	 * Gets the delta.
	 *
	 * @return The delta.
	 */
	public Supply getDelta() {
		return this.delta;
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
		Supply.writeTo(serializer, "delta", this.delta);
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		observer.notify(new SmartTileSupplyChangeNotification(this.getSigner(), this.mosaicId, this.delta, this.supplyType));
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.getDebtor(), this.getFee()));
	}
}
