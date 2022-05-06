package org.nem.core.connect;

import net.minidev.json.JSONObject;
import org.nem.core.serialization.*;

import java.nio.charset.Charset;

/**
 * Creates a new JSON HTTP POST request.
 */
public class HttpJsonPostRequest implements HttpPostRequest {
	private static final Charset ENCODING_CHARSET = Charset.forName("UTF-8");

	private final JSONObject jsonEntity;

	/**
	 * Creates a new request.
	 *
	 * @param entity The entity.
	 */
	public HttpJsonPostRequest(final SerializableEntity entity) {
		this(JsonSerializer.serializeToJson(entity));
	}

	/**
	 * Creates a new request.
	 *
	 * @param jsonEntity The JSON entity.
	 */
	public HttpJsonPostRequest(final JSONObject jsonEntity) {
		this.jsonEntity = jsonEntity;
	}

	@Override
	public byte[] getPayload() {
		return this.jsonEntity.toString().getBytes(ENCODING_CHARSET);
	}

	@Override
	public String getContentType() {
		return ContentType.JSON;
	}
}
