package org.nem.nis.controller.viewmodels;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;

import java.util.*;

// TODO-CR: add public documentation
// TODO-CR: add basic tests

public class ExplorerBlockView implements SerializableEntity {
	private final Long height;
	private final Address foragerAddress;
	private final long timeStamp;
	private final Hash blockHash;
	private final List<ExplorerTransferView> transactions;

	public ExplorerBlockView(final Long height, final Address foragerAddress, final long timeStamp, final Hash blockHash, final int txCount) {
		this.height = height;
		this.foragerAddress = foragerAddress;
		this.timeStamp = timeStamp;
		this.blockHash = blockHash;
		this.transactions = new ArrayList<>(txCount);
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeLong("height", this.height);
		Address.writeTo(serializer, "harvester", this.foragerAddress);
		serializer.writeBytes("harvesterPk", this.foragerAddress.getPublicKey().getRaw());
		serializer.writeLong("timeStamp", this.timeStamp);
		serializer.writeBytes("hash", this.blockHash.getRaw());
		serializer.writeObjectArray("txes", this.transactions);
	}

	public void addTransaction(final ExplorerTransferView explorerTransferView) {
		this.transactions.add(explorerTransferView);
	}
}
