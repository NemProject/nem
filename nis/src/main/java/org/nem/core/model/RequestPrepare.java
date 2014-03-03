package org.nem.core.model;

import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

public class RequestPrepare implements SerializableEntity {
	private byte[] data;

	public RequestPrepare(final byte[] data) {
		this.data = data;
	}

	public RequestPrepare(Deserializer deserializer) {
		this.data = deserializer.readBytes("data");
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeBytes("data", this.getData());
	}
}
