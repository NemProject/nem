package org.nem.nis.config;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.h2.util.IOUtils;
import org.nem.core.serialization.*;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.Foraging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.converter.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;

public class DeserializerHttpMessageConverter extends AbstractHttpMessageConverter<JsonDeserializer> {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private final AccountLookup accountLookup;

	@Autowired(required = true)
	DeserializerHttpMessageConverter(final AccountLookup accountLookup) {
		super(new MediaType("application", "json", DEFAULT_CHARSET));
		this.accountLookup = accountLookup;
	}

	@Override
	protected boolean supports(final Class<?> aClass) {
		return aClass.isAssignableFrom(JsonDeserializer.class);
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
