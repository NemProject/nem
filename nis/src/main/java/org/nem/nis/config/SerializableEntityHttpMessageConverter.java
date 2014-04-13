package org.nem.nis.config;

import org.nem.core.model.Block;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.serialization.SerializableEntity;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.nio.charset.Charset;

public class SerializableEntityHttpMessageConverter extends AbstractHttpMessageConverter<Block> {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	public SerializableEntityHttpMessageConverter() {
		super(new MediaType("application", "json", DEFAULT_CHARSET));
	}

	@Override
	protected boolean supports(final Class<?> aClass) {
		final Class[] interfaces = aClass.getInterfaces();
		for (final Class i : interfaces) {
			if (i == SerializableEntity.class) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected Block readInternal(
			final Class<? extends Block> aClass,
			final HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {

		throw new UnsupportedOperationException();
	}

	@Override
	protected void writeInternal(
			final Block serializableEntity,
			final HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {

		final JsonSerializer serializer = new JsonSerializer();
		serializableEntity.serialize(serializer);
	}
}
