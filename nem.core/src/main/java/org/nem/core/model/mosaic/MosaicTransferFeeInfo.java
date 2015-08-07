package org.nem.core.model.mosaic;

import org.nem.core.model.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.serialization.*;

/**
 * A class that contains information about the fee when transferring mosaics.
 */
public class MosaicTransferFeeInfo implements SerializableEntity {
	private final MosaicTransferFeeType type;
	private final Account recipient;
	private final Quantity fee;

	/**
	 * Creates a new mosaic transfer fee info.
	 *
	 * @param type The fee type.
	 * @param recipient The recipient.
	 * @param fee The fee.
	 */
	public MosaicTransferFeeInfo(
			final MosaicTransferFeeType type,
			final Account recipient,
			final Quantity fee) {
		this.type = type;
		this.recipient = recipient;
		this.fee = fee;
		this.validate();
	}

	/**
	 * Deserializes a mosaic transfer fee info.
	 *
	 * @param deserializer The deserializer.
	 */
	public MosaicTransferFeeInfo(final Deserializer deserializer) {
		this.type = MosaicTransferFeeType.fromValue(deserializer.readInt("type"));
		this.recipient = Account.readFrom(deserializer, "recipient");
		this.fee = Quantity.readFrom(deserializer, "fee");
		this.validate();
	}

	private void validate() {
		if (!this.recipient.getAddress().isValid()) {
			throw new IllegalArgumentException("recipient is not a valid address");
		}
	}

	/**
	 * Gets the fee type.
	 *
	 * @return The fee type.
	 */
	public MosaicTransferFeeType getType() {
		return this.type;
	}

	/**
	 * Gets the recipient.
	 *
	 * @return The recipient.
	 */
	public Account getRecipient() {
		return this.recipient;
	}

	/**
	 * Gets the fee.
	 *
	 * @return The fee.
	 */
	public Quantity getFee() {
		return this.fee;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("type", this.type.value());
		Account.writeTo(serializer, "recipient", this.recipient);
		Quantity.writeTo(serializer, "fee", this.fee);
	}

	@Override
	public int hashCode() {
		return this.type.hashCode() ^
				this.recipient.hashCode() ^
				this.fee.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof MosaicTransferFeeInfo)) {
			return false;
		}

		final MosaicTransferFeeInfo rhs = (MosaicTransferFeeInfo)obj;
		return this.type.equals(rhs.type) &&
				this.recipient.equals(rhs.recipient) &&
				this.fee.equals(rhs.fee);
	}
}
