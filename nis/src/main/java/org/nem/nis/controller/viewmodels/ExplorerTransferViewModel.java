package org.nem.nis.controller.viewmodels;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.model.Transaction;
import org.nem.core.model.TransactionTypes;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.UnixTime;

import java.math.BigInteger;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * A transfer view model that is used by NIS services like the block explorer.
 * <br>
 * This currently only supports transfer transactions.
 */
public class ExplorerTransferViewModel implements SerializableEntity {
	private final Transaction transaction;
	private final Hash hash;

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
