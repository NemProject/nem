package org.nem.nis.controller.viewmodels;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Address;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockDifficulty;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;
import org.nem.core.time.TimeInstant;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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
		serializer.writeLong("timestamp", this.timestamp);
		serializer.writeObject("hash", this.blockHash);
		serializer.writeObjectArray("txes", this.transactions);
	}

	public void addTransaction(final ExplorerTransferView explorerTransferView) {
		this.transactions.add(explorerTransferView);
	}
}
