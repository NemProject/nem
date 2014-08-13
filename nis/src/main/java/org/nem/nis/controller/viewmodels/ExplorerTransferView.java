package org.nem.nis.controller.viewmodels;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.*;
import org.nem.core.utils.HexEncoder;

// TODO-CR: add public documentation
// TODO-CR: add basic tests

public class ExplorerTransferView implements SerializableEntity {
	private final int type;
	private final Amount fee;
	private final long deadline;
	private final Address signerAddress;
	private final String signature;
	private final Hash hash;

	// TODO: split it into two

	private final Address recipient;
	private final Amount amount;
	private final int msgType;
	private final byte[] message;

	public ExplorerTransferView(final int type, final Amount fee, final int deadline, final Address signer, final byte[] signature, final Hash transactionHash, final Address recipient, final Amount amount, final int msgType, final byte[] encodedPayload) {
		this.type = type;
		this.fee = fee;
		this.deadline = UnixTime.fromTimeInstant(new TimeInstant(deadline)).getMillis();
		this.signerAddress = signer;
		this.signature = HexEncoder.getString(signature);
		this.hash = transactionHash;

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
		serializer.writeBytes("senderPk", this.signerAddress.getPublicKey().getRaw());
		serializer.writeString("signature", this.signature);
		serializer.writeBytes("hash", this.hash.getRaw());

		Address.writeTo(serializer, "recipient", this.recipient);
		Amount.writeTo(serializer, "amount", this.amount);
		serializer.writeInt("msgType", this.msgType);
		serializer.writeBytes("message", this.message);
	}
}
