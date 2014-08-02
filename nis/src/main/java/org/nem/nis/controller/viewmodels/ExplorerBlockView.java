package org.nem.nis.controller.viewmodels;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.utils.HexEncoder;

import java.util.*;

public class ExplorerBlockView implements SerializableEntity {
	private final Long height;
	private final Address foragerAddress;
	private final long timestamp;
	private final Hash blockHash;
	private final List<ExplorerTransferView> transactions;

	public ExplorerBlockView(final Long height, final Address foragerAddress, final long timestamp, final Hash blockHash, int txCount) {
		this.height = height;
		this.foragerAddress = foragerAddress;
		this.timestamp = timestamp;
		this.blockHash = blockHash;
		this.transactions = new ArrayList<>(txCount);
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeLong("height", this.height);
		Address.writeTo(serializer, "harvester", this.foragerAddress);
		serializer.writeString("harvesterPk", HexEncoder.getString(this.foragerAddress.getPublicKey().getRaw()));
		serializer.writeLong("timestamp", this.timestamp);
		serializer.writeString("hash", HexEncoder.getString(this.blockHash.getRaw()));
		serializer.writeObjectArray("txes", this.transactions);
	}

	public void addTransaction(final ExplorerTransferView explorerTransferView) {
		this.transactions.add(explorerTransferView);
	}
}
