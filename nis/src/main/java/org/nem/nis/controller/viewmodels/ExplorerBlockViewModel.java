package org.nem.nis.controller.viewmodels;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Address;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.core.time.UnixTime;

import java.util.*;

/**
 * A block view model that is used by NIS services like the block explorer.
 */
public class ExplorerBlockViewModel implements SerializableEntity {
	private final Block block;
	private final Hash blockHash;
	private final List<ExplorerTransferViewModel> transactions;

	public ExplorerBlockViewModel(
			final Block block,
			final Hash blockHash) {
		this.block = block;
		this.blockHash = blockHash;
		this.transactions = new ArrayList<>();
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("block", this.block);
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
