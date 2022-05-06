package org.nem.deploy;

import net.minidev.json.*;
import org.apache.commons.io.IOUtils;
import org.nem.core.serialization.*;
import org.nem.core.utils.*;
import org.springframework.http.MediaType;

import java.io.InputStream;

/**
 * A json serialization policy.
 */
public class JsonSerializationPolicy implements SerializationPolicy {
	private final SimpleAccountLookup accountLookup;

	/**
	 * Creates a new json serialization policy.
	 *
	 * @param accountLookup The account lookup to use.
	 */
	public JsonSerializationPolicy(final SimpleAccountLookup accountLookup) {
		this.accountLookup = accountLookup;
	}

	@Override
	public MediaType getMediaType() {
		return new MediaType("application", "json");
	}

	@Override
	public byte[] toBytes(final SerializableEntity entity) {
		final JsonSerializer serializer = new JsonSerializer();
		entity.serialize(serializer);

		final String rawJson = serializer.getObject().toJSONString() + "\r\n";
		return StringEncoder.getBytes(rawJson);
	}

	@Override
	public Deserializer fromStream(final InputStream stream) {
		final DeserializationContext context = new DeserializationContext(this.accountLookup);

		final Object result = JSONValue.parse(readAllAsString(stream));
		if (result instanceof JSONObject) {
			return new JsonDeserializer((JSONObject) result, context);
		}

		throw new IllegalArgumentException("JSON Object was expected");
	}

	private static String readAllAsString(final InputStream stream) {
		final byte[] bytes = ExceptionUtils.propagate(() -> IOUtils.toByteArray(stream));
		return StringEncoder.getString(bytes);
	}
}
