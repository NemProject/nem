package org.nem.core.model.transactions.extensions;

import org.nem.core.serialization.*;

/**
 * A transaction serialization extension that can be used to serialize different data for different
 * transaction versions.
 *
 * @param <TData> The extended serialization data.
 */
public interface TransactionSerializationExtension<TData> {

	/**
	 * Gets a value indicating whether or not this extension applies to the specified transaction version.
	 *
	 * @param version The transaction entity version.
	 * @return true if this extension should be applied.
	 */
	boolean isApplicable(final int version);

	/**
	 * Serializes the extended data.
	 *
	 * @param serializer The serializer.
	 * @param data The extended data.
	 */
	void serialize(final Serializer serializer, final TData data);

	/**
	 * Deserializes the extended data.
	 *
	 * @param deserializer The deserializer.
	 * @param data The extended data.
	 */
	void deserialize(final Deserializer deserializer, final TData data);
}
