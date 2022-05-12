package org.nem.peer.requests;

import org.nem.core.model.*;
import org.nem.core.model.primitive.HashShortId;
import org.nem.core.serialization.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Request that specifies partial hashes of unconfirmed transactions which should NOT be included.
 */
public class UnconfirmedTransactionsRequest implements SerializableEntity {
	private final List<HashShortId> hashShortIds;

	/**
	 * Creates an unconfirmed transactions request.
	 */
	public UnconfirmedTransactionsRequest() {
		this.hashShortIds = new ArrayList<>();
	}

	/**
	 * Creates an unconfirmed transactions request.
	 *
	 * @param transactions The known transactions.
	 */
	public UnconfirmedTransactionsRequest(final Collection<Transaction> transactions) {
		this.hashShortIds = transactions.stream().map(t -> new HashShortId(HashUtils.calculateHash(t).getShortId()))
				.collect(Collectors.toList());
		// only add the hashes for child signatures because the inner transactions will get rejected (because they have null signatures)
		this.hashShortIds.addAll(transactions.stream().flatMap(TransactionExtensions::getChildSignatures)
				.map(t -> new HashShortId(HashUtils.calculateHash(t).getShortId())).collect(Collectors.toList()));
	}

	/**
	 * Creates an unconfirmed transactions request.
	 *
	 * @param deserializer The deserializer to use.
	 */
	public UnconfirmedTransactionsRequest(final Deserializer deserializer) {
		this.hashShortIds = deserializer.readObjectArray("hashShortIds", HashShortId::new);
	}

	/**
	 * Gets the collection of the hash short ids.
	 *
	 * @return The collection of short ids.
	 */
	public Collection<HashShortId> getHashShortIds() {
		return this.hashShortIds;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObjectArray("hashShortIds", this.hashShortIds);
	}
}
