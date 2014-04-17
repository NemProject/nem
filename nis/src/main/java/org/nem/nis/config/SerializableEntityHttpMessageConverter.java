package org.nem.nis.config;

import org.nem.core.serialization.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.converter.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * An HttpMessageConverter that maps SerializableEntity responses to application/json.
 */
public class SerializableEntityHttpMessageConverter extends AbstractHttpMessageConverter<SerializableEntity> {

	private final DeserializerHttpMessageConverter deserializerMessageConverter;

	/**
	 * Creates a new http message converter.
	 */
	@Autowired(required = true)
	public SerializableEntityHttpMessageConverter(final DeserializerHttpMessageConverter deserializerMessageConverter) {
		super(new MediaType("application", "json"));
		this.deserializerMessageConverter = deserializerMessageConverter;
	}

	@Override
	protected boolean supports(final Class<?> aClass) {
		return SerializableEntity.class.isAssignableFrom(aClass);
	}

	@Override
	public boolean canRead(final Class<?> clazz, final MediaType type) {
		return null != this.getConstructor(clazz);
	}

	@Override
	protected SerializableEntity readInternal(
			final Class<? extends SerializableEntity> aClass,
			final HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {

		final Deserializer deserializer = this.deserializerMessageConverter.readInternal(
				Deserializer.class,
				httpInputMessage);

		return this.createInstance(aClass, deserializer);
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

	private <T> Constructor<T> getConstructor(final Class<T> aClass) {
		try {
			return aClass.getConstructor(Deserializer.class);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	private SerializableEntity createInstance(
			final Class<? extends SerializableEntity> aClass,
			final Deserializer deserializer) {
		try {
			final Constructor<? extends SerializableEntity> constructor = this.getConstructor(aClass);
			if (null == constructor)
				throw new UnsupportedOperationException("could not find compatible constructor");

			return constructor.newInstance(deserializer);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException)
				throw (RuntimeException)e.getCause();

			throw new UnsupportedOperationException("could not instantiate object");
		}
	}
}
