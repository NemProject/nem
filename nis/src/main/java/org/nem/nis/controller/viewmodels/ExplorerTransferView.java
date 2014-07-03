package org.nem.nis.controller.viewmodels;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.SystemTimeProvider;
import org.nem.core.utils.HexEncoder;

public class ExplorerTransferView implements SerializableEntity {
	private int type;
	private Amount fee;
	private long deadline;
	private Address signerAddress;
	private String signature;
	private Hash hash;

	// TODO: split it into two

	private Address recipient;
	private Amount amount;
	private int msgType;
	private byte[] message;

	public ExplorerTransferView(int type, final Amount fee, final long deadline, final Address signer, final byte[] signature, final Hash transactionHash, final Address recipient1, final Amount amount, final int msgType, final byte[] encodedPayload) {
		this.type = type;
		this.fee = fee;
		this.deadline = SystemTimeProvider.getEpochTimeMillis() + deadline*1000;
		this.signerAddress = signer;
		this.signature = HexEncoder.getString(signature);
		this.hash = transactionHash;

		this.recipient = recipient1;
		this.amount = amount;
		this.msgType = msgType;
		this.message = encodedPayload;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("type", this.type);
		Amount.writeTo(serializer, "fee", this.fee);
		serializer.writeLong("timestamp", this.deadline);
		Address.writeTo(serializer, "sender", this.signerAddress);
		serializer.writeString("signature", this.signature);
		serializer.writeObject("hash", this.hash);

		Address.writeTo(serializer, "recipient", this.recipient);
		Amount.writeTo(serializer, "amount", this.amount);
		serializer.writeInt("msgType", this.msgType);
		serializer.writeBytes("message", this.message);
	}
}
