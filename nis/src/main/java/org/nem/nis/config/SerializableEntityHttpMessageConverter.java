package org.nem.nis.config;

import org.nem.core.serialization.*;
import org.springframework.http.*;
import org.springframework.http.converter.*;

import java.io.IOException;

/**
 * An HttpMessageConverter that maps SerializableEntity responses to application/json.
 */
public class SerializableEntityHttpMessageConverter extends AbstractHttpMessageConverter<SerializableEntity> {

	/**
	 * Creates a new http message converter.
	 */
	public SerializableEntityHttpMessageConverter() {
		super(new MediaType("application", "json"));
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
	protected SerializableEntity readInternal(
			final Class<? extends SerializableEntity> aClass,
			final HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {

		throw new UnsupportedOperationException();
	}

	@Override
	protected void writeInternal(
			final SerializableEntity serializableEntity,
			final HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {

		final JsonSerializer serializer = new JsonSerializer();
		serializableEntity.serialize(serializer);

		final String rawJson = serializer.getObject().toJSONString() + "\r\n";
		httpOutputMessage.getBody().write(rawJson.getBytes());
	}
}
