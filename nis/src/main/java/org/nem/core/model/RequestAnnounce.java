package org.nem.core.model;

import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

public class RequestAnnounce implements SerializableEntity {
	private byte[] data;
	private byte[] signature;

	public RequestAnnounce(final byte[] data, final byte[] signature) {
		this.data = data;
		this.signature = signature;
	}

	public RequestAnnounce(Deserializer deserializer) {
		this.data = deserializer.readBytes("data");
		this.signature = deserializer.readBytes("signature");
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public byte[] getSignature() {
		return signature;
	}

	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeBytes("data", this.getData());
		serializer.writeBytes("signature", this.getSignature());
	}
}
