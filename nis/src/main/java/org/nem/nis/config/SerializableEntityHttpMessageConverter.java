package org.nem.nis.config;

import org.nem.core.serialization.*;
import org.springframework.http.*;
import org.springframework.http.converter.*;

import java.io.IOException;
import java.nio.charset.Charset;

public class SerializableEntityHttpMessageConverter extends AbstractHttpMessageConverter<SerializableEntity> {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	public SerializableEntityHttpMessageConverter() {
		super(new MediaType("application", "json", DEFAULT_CHARSET));
	}

	@Override
	protected boolean supports(final Class<?> aClass) {
		return SerializableEntity.class.isAssignableFrom(aClass);
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
