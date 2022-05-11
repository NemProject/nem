package org.nem.deploy;

import org.nem.core.serialization.SerializableEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.converter.*;

import java.io.IOException;

/**
 * An HttpMessageConverter that maps SerializableEntity responses to application/json.
 */
public class SerializableEntityHttpMessageConverter extends AbstractHttpMessageConverter<SerializableEntity> {
	private final SerializationPolicy policy;

	/**
	 * Creates a new http message converter.
	 *
	 * @param policy The serialization policy.
	 */
	@Autowired(required = true)
	public SerializableEntityHttpMessageConverter(final SerializationPolicy policy) {
		super(policy.getMediaType());
		this.policy = policy;
	}

	@Override
	protected boolean supports(final Class<?> aClass) {
		return SerializableEntity.class.isAssignableFrom(aClass);
	}

	@Override
	public boolean canRead(final Class<?> clazz, final MediaType type) {
		return false;
	}

	@Override
	protected SerializableEntity readInternal(final Class<? extends SerializableEntity> aClass, final HttpInputMessage httpInputMessage)
			throws IOException, HttpMessageNotReadableException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void writeInternal(final SerializableEntity serializableEntity, final HttpOutputMessage httpOutputMessage)
			throws IOException, HttpMessageNotWritableException {

		httpOutputMessage.getBody().write(this.policy.toBytes(serializableEntity));
	}
}
