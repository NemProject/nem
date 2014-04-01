package org.nem.core.model;

import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.ObjectDeserializer;

public class ByteArrayFactory {
	public static final ObjectDeserializer<ByteArray> deserializer = new ObjectDeserializer<ByteArray>() {
		@Override
		public ByteArray deserialize(Deserializer deserializer) {
			return new ByteArray(deserializer);
		}
	};
}
