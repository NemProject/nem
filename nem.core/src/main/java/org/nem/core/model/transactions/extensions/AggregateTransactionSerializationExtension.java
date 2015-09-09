package org.nem.core.model.transactions.extensions;

import org.nem.core.serialization.*;

import java.util.Collection;

/**
 * An aggregate transaction serialization extension.
 *
 * @param <TData> The extended serialization data.
 */
public class AggregateTransactionSerializationExtension<TData> {
	private final Collection<TransactionSerializationExtension<TData>> extensions;

	/**
	 * Creates a new aggregate extension.
	 *
	 * @param extensions The child extensions.
	 */
	public AggregateTransactionSerializationExtension(final Collection<TransactionSerializationExtension<TData>> extensions) {
		this.extensions = extensions;
	}

	/**
	 * Serializes the extended data.
	 *
	 * @param serializer The serializer.
	 * @param version The transaction entity version.
	 * @param data The extended data.
	 */
	public void serialize(final Serializer serializer, final int version, final TData data) {
		this.extensions.stream()
				.filter(e -> e.isApplicable(version))
				.forEach(e -> e.serialize(serializer, data));
	}

	/**
	 * Deserializes the extended data.
	 *
	 * @param deserializer The deserializer.
	 * @param version The transaction entity version.
	 * @param data The extended data.
	 */
	public void deserialize(final Deserializer deserializer, final int version, final TData data) {
		this.extensions.stream()
				.filter(e -> e.isApplicable(version))
				.forEach(e -> e.deserialize(deserializer, data));
	}
}