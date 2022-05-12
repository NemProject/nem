package org.nem.deploy;

import org.apache.commons.io.IOUtils;
import org.nem.core.serialization.*;
import org.nem.core.utils.ExceptionUtils;
import org.springframework.http.MediaType;

import java.io.InputStream;

/**
 * A binary serialization policy.
 */
public class BinarySerializationPolicy implements SerializationPolicy {
	private final SimpleAccountLookup accountLookup;

	/**
	 * Creates a new binary serialization policy.
	 *
	 * @param accountLookup The account lookup to use.
	 */
	public BinarySerializationPolicy(final SimpleAccountLookup accountLookup) {
		this.accountLookup = accountLookup;
	}

	@Override
	public MediaType getMediaType() {
		return new MediaType("application", "binary");
	}

	@Override
	public byte[] toBytes(final SerializableEntity entity) {
		return BinarySerializer.serializeToBytes(entity);
	}

	@Override
	public Deserializer fromStream(final InputStream stream) {
		final byte[] bytes = ExceptionUtils.propagate(() -> IOUtils.toByteArray(stream));
		final DeserializationContext context = new DeserializationContext(this.accountLookup);
		return new BinaryDeserializer(bytes, context);
	}
}
