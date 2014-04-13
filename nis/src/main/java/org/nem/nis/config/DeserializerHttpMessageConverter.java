package org.nem.nis.config;

import net.minidev.json.*;
import org.nem.core.serialization.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.converter.*;

import java.io.IOException;
import java.security.InvalidParameterException;

/**
 * An HttpMessageConverter that maps application/json requests to Deserializer objects.
 */
public class DeserializerHttpMessageConverter extends AbstractHttpMessageConverter<JsonDeserializer> {

	private final AccountLookup accountLookup;

	/**
	 * Creates a new http message converter.
	 *
	 * @param accountLookup The account lookup to use.
	 */
	@Autowired(required = true)
	DeserializerHttpMessageConverter(final AccountLookup accountLookup) {
		super(new MediaType("application", "json"));
		this.accountLookup = accountLookup;
	}

	@Override
	protected boolean supports(final Class<?> aClass) {
		return aClass.isAssignableFrom(JsonDeserializer.class);
	}

	@Override
	public boolean canWrite(final Class<?> clazz, final MediaType type) {
		return false;
	}

	@Override
	protected JsonDeserializer readInternal(
			final Class<? extends JsonDeserializer> aClass,
			final HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {

		final Object result = JSONValue.parse(httpInputMessage.getBody());
		if (result instanceof JSONObject)
			return new JsonDeserializer((JSONObject)result, new DeserializationContext(this.accountLookup));

		throw new InvalidParameterException("JSON Object was expected");
	}

	@Override
	protected void writeInternal(
			final JsonDeserializer serializableEntity,
			final HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {

		throw new UnsupportedOperationException();
	}
}
