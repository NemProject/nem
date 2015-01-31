package org.nem.nis.controller.viewmodels;

import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.UnixTime;

/**
 * A transfer view model that is used by NIS services like the block explorer.
 * <br/>
 * This currently only supports transfer transactions.
 */
public class ExplorerTransferViewModel implements SerializableEntity {
	private final int type;
	private final Amount fee;
	private final long deadline;
	private final Address signerAddress;
	private final String signature;
	private final Hash hash;

	private final Address recipient;
	private final Amount amount;
	private final int msgType;
	private final byte[] message;

	/**
	 * Creates a new explorer transfer view model.
	 *
	 * @param type The type.
	 * @param fee The fee.
	 * @param deadline The deadline.
	 * @param signer The signer.
	 * @param signature The signature.
	 * @param hash The hash.
	 * @param recipient The recipient.
	 * @param amount The amount.
	 * @param msgType The message type.
	 * @param encodedPayload The encoded payload.
	 */
	public ExplorerTransferViewModel(
			final int type,
			final Amount fee,
			final UnixTime deadline,
			final Address signer,
			final Signature signature,
			final Hash hash,
			final Address recipient,
			final Amount amount,
			final int msgType,
			final byte[] encodedPayload) {
		this.type = type;
		this.fee = fee;
		this.deadline = deadline.getMillis();
		this.signerAddress = signer;
		this.signature = signature.toString();
		this.hash = hash;

		this.recipient = recipient;
		this.amount = amount;
		this.msgType = msgType;
		this.message = encodedPayload;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("type", this.type);
		Amount.writeTo(serializer, "fee", this.fee);
		serializer.writeLong("timeStamp", this.deadline);
		Address.writeTo(serializer, "sender", this.signerAddress);
		Address.writeTo(serializer, "senderPk", this.signerAddress, AddressEncoding.PUBLIC_KEY);
		serializer.writeString("signature", this.signature);
		serializer.writeBytes("hash", this.hash.getRaw());

		Address.writeTo(serializer, "recipient", this.recipient);
		Amount.writeTo(serializer, "amount", this.amount);
		serializer.writeInt("msgType", this.msgType);
		serializer.writeBytes("message", this.message);
	}
}
