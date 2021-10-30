package org.nem.deploy;

import org.nem.core.serialization.Deserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.converter.*;

import java.io.IOException;
import java.lang.reflect.*;

public class DeserializableEntityMessageConverter extends AbstractHttpMessageConverter<Object> {
	private final DeserializerHttpMessageConverter deserializerMessageConverter;

	/**
	 * Creates a new http message converter.
	 *
	 * @param policy The serialization policy.
	 */
	@Autowired(required = true)
	public DeserializableEntityMessageConverter(final SerializationPolicy policy) {
		super(policy.getMediaType());
		this.deserializerMessageConverter = new DeserializerHttpMessageConverter(policy);
	}

	@Override
	protected boolean supports(final Class<?> clazz) {
		return null != this.getConstructor(clazz);
	}

	@Override
	public boolean canWrite(final Class<?> clazz, final MediaType type) {
		return false;
	}

	@Override
	protected Object readInternal(final Class<?> aClass, final HttpInputMessage httpInputMessage)
			throws IOException, HttpMessageNotReadableException {
		final Deserializer deserializer = this.deserializerMessageConverter.readInternal(Deserializer.class, httpInputMessage);

		return this.createInstance(aClass, deserializer);
	}

	@Override
	protected void writeInternal(final Object o, final HttpOutputMessage httpOutputMessage)
			throws IOException, HttpMessageNotWritableException {
		throw new UnsupportedOperationException();
	}

	private Constructor<?> getConstructor(final Class<?> aClass) {
		try {
			return aClass.getConstructor(Deserializer.class);
		} catch (final NoSuchMethodException e) {
			return null;
		}
	}

	private Object createInstance(final Class<?> aClass, final Deserializer deserializer) {
		try {
			final Constructor<?> constructor = this.getConstructor(aClass);
			if (null == constructor) {
				throw new UnsupportedOperationException("could not find compatible constructor");
			}

			return constructor.newInstance(deserializer);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			}

			throw new UnsupportedOperationException("could not instantiate object");
		}
	}
}
