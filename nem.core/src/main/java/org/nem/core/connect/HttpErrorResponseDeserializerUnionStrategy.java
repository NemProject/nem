package org.nem.core.connect;

import net.minidev.json.JSONValue;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.utils.ExceptionUtils;

import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Strategy for coercing an HTTP response into an ErrorResponseDeserializerUnion.
 */
public class HttpErrorResponseDeserializerUnionStrategy implements HttpResponseStrategy<ErrorResponseDeserializerUnion> {

	private final DeserializationContext context;

	/**
	 * Creates a new HTTP ErrorResponseDeserializerUnion response strategy.
	 *
	 * @param context The deserialization context to use when deserializing responses.
	 */
	public HttpErrorResponseDeserializerUnionStrategy(final DeserializationContext context) {
		this.context = context;
	}

	@Override
	public ErrorResponseDeserializerUnion coerce(final HttpRequestBase request, final HttpResponse response) {
		return ExceptionUtils.propagate(() ->
						new ErrorResponseDeserializerUnion(
								response.getStatusLine().getStatusCode(),
								JSONValue.parse(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8"))),
								this.context),
				FatalPeerException::new);
	}

	@Override
	public String getSupportedContentType() {
		return "application/json";
	}
}
