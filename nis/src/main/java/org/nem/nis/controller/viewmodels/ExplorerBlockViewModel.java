package org.nem.nis.controller.viewmodels;

import java.util.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;

/**
 * A block view model that is used by NIS services like the block explorer.
 */
public class ExplorerBlockViewModel implements SerializableEntity {
	private final Block block;
	private final Hash blockHash;
	private final List<ExplorerTransferViewModel> transactions;

	/**
	 * Creates a new explorer block view model.
	 *
	 * @param block The block.
	 * @param blockHash The block hash.
	 */
	public ExplorerBlockViewModel(final Block block, final Hash blockHash) {
		this.block = block;
		this.blockHash = blockHash;
		this.transactions = new ArrayList<>();
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("block", this.block);
		serializer.writeBytes("hash", this.blockHash.getRaw());
		BlockDifficulty.writeTo(serializer, "difficulty", this.block.getDifficulty());
		serializer.writeObjectArray("txes", this.transactions);
		Account.writeTo(serializer, "beneficiary", null != this.block.getLessor() ? this.block.getLessor() : this.block.getSigner());
		Amount.writeTo(serializer, "totalFee", this.block.getTotalFee());
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
