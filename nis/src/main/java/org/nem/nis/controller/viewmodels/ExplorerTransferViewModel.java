package org.nem.nis.controller.viewmodels;

import org.nem.core.crypto.*;
import org.nem.core.model.Transaction;
import org.nem.core.serialization.*;

/**
 * A transfer view model that is used by NIS services like the block explorer.
 * <br>
 * This currently only supports transfer transactions.
 */
public class ExplorerTransferViewModel implements SerializableEntity {
	private final Transaction transaction;
	private final Hash hash;

	/**
	 * Creates a new explorer transfer view model.
	 *
	 * @param transaction The transaction.
	 * @param hash The hash.
	 */
	public ExplorerTransferViewModel(final Transaction transaction, final Hash hash) {
		this.transaction = transaction;
		this.hash = hash;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("tx", this.transaction);
		serializer.writeBytes("hash", this.hash.getRaw());
	}
}
