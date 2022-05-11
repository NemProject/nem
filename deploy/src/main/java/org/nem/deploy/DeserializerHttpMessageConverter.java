package org.nem.deploy;

import org.nem.core.serialization.Deserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.converter.*;

import java.io.IOException;

/**
 * An HttpMessageConverter that maps application/json requests to Deserializer objects.
 */
public class DeserializerHttpMessageConverter extends AbstractHttpMessageConverter<Deserializer> {

	private final SerializationPolicy policy;

	/**
	 * Creates a new http message converter.
	 *
	 * @param policy The serialization policy to use.
	 */
	@Autowired(required = true)
	public DeserializerHttpMessageConverter(final SerializationPolicy policy) {
		super(policy.getMediaType());
		this.policy = policy;
	}

	@Override
	protected boolean supports(final Class<?> aClass) {
		return Deserializer.class.isAssignableFrom(aClass);
	}

	@Override
	public boolean canWrite(final Class<?> clazz, final MediaType type) {
		return false;
	}

	@Override
	protected Deserializer readInternal(final Class<? extends Deserializer> aClass, final HttpInputMessage httpInputMessage)
			throws IOException, HttpMessageNotReadableException {

		return this.policy.fromStream(httpInputMessage.getBody());
	}

	@Override
	protected void writeInternal(final Deserializer serializableEntity, final HttpOutputMessage httpOutputMessage)
			throws IOException, HttpMessageNotWritableException {

		throw new UnsupportedOperationException();
	}
}
