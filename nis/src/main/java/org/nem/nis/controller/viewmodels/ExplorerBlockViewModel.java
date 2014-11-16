package org.nem.nis.controller.viewmodels;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.core.time.UnixTime;

import java.util.*;

/**
 * A block view model that is used by NIS services like the block explorer.
 */
public class ExplorerBlockViewModel implements SerializableEntity {
	private final BlockHeight height;
	private final Address harvesterAddress;
	private final long timeStamp;
	private final Hash blockHash;
	private final List<ExplorerTransferViewModel> transactions;

	/**
	 * Creates a new explorer block view model.
	 *
	 * @param height The block height.
	 * @param harvesterAddress The address of the block harvester.
	 * @param timeStamp The block timestamp.
	 * @param blockHash The block hash.
	 */
	public ExplorerBlockViewModel(
			final BlockHeight height,
			final Address harvesterAddress,
			final UnixTime timeStamp,
			final Hash blockHash) {
		this.height = height;
		this.harvesterAddress = harvesterAddress;
		this.timeStamp = timeStamp.getMillis();
		this.blockHash = blockHash;
		this.transactions = new ArrayList<>();
	}

	@Override
	public void serialize(final Serializer serializer) {
		BlockHeight.writeTo(serializer, "height", this.height);
		Address.writeTo(serializer, "harvester", this.harvesterAddress);
		Address.writeTo(serializer, "harvesterPk", this.harvesterAddress, AddressEncoding.PUBLIC_KEY);
		serializer.writeLong("timeStamp", this.timeStamp);
		serializer.writeBytes("hash", this.blockHash.getRaw());
		serializer.writeObjectArray("txes", this.transactions);
	}

	/**
	 * Adds a transfer view model to this object.
	 *
	 * @param transferViewModel The transfer view model.
	 */
	public void addTransaction(final ExplorerTransferViewModel transferViewModel) {
		this.transactions.add(transferViewModel);
	}
}
