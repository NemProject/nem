package org.nem.core.model;

import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.ObjectDeserializer;

/**
 * similar to transaction factory, but for the blocks
 * <p/>
 * I doubt there is need for this now or in future, but let's have similar API
 * for both transactions and blocks
 */
public class BlockFactory {

	/**
	 * An object deserializer that wraps this factory.
	 */
	public static final ObjectDeserializer<Block> VERIFIABLE =
			deserializer -> deserialize(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);

	public static final ObjectDeserializer<Block> NON_VERIFIABLE =
			deserializer -> deserialize(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);

	/**
	 * Deserializes a block.
	 *
	 * @param options      The deserialization options.
	 * @param deserializer The deserializer.
	 *
	 * @return The deserialized block.
	 */
	private static Block deserialize(final VerifiableEntity.DeserializationOptions options, final Deserializer deserializer) {
		int type = deserializer.readInt("type");

		switch (type) {
			case 1:
				return new Block(1, options, deserializer);
		}

		throw new IllegalArgumentException("Unknown block type: " + type);
	}
}
