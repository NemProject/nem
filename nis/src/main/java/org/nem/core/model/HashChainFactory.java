package org.nem.core.model;

import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.ObjectDeserializer;

public class HashChainFactory {
	public static final ObjectDeserializer<HashChain> deserializer = new ObjectDeserializer<HashChain>() {

		@Override
		public HashChain deserialize(Deserializer deserializer) {
			return new HashChain(deserializer);
		}
	};
}
