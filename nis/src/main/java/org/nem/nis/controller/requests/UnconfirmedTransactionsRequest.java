package org.nem.nis.controller.requests;

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
		this.hashShortIds = transactions.stream()
				.map(t -> new HashShortId(HashUtils.calculateHash(t).getShortId()))
				.collect(Collectors.toList());
		// TODO 20150327 J-B: any reason not to use getChildTransactions?
		// TODO 20150327 BR -> J: if I use getChildTransactions then the inner transaction will be handled as own entity.
		// > While on this side it would not matter cause the remote node knows about the inner transaction if it knows the outer transaction,
		// > using getChildTransactions in the remote node's DefaultUnconfirmedTransactionsFilter would return the inner transaction in the list
		// > and this node would put that transaction in the wrong cache.
		this.hashShortIds.addAll(
				transactions.stream()
						.filter(t -> TransactionTypes.MULTISIG == t.getType())
						.flatMap(t -> ((MultisigTransaction)t).getCosignerSignatures().stream())
						.map(t -> new HashShortId(HashUtils.calculateHash(t).getShortId()))
						.collect(Collectors.toList()));
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
