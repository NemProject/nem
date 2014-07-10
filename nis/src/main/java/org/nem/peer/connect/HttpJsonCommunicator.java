package org.nem.peer.connect;

import org.nem.core.connect.*;
import org.nem.core.serialization.*;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP JSON-base communicator implementation.
 */
public class HttpJsonCommunicator implements Communicator {
	private final HttpMethodClient<Deserializer> httpMethodClient;
	private final HttpResponseStrategy<Deserializer> responseStrategy;
	private final HttpResponseStrategy<Deserializer> voidResponseStrategy;

	/**
	 * Creates a new HTTP communicator.
	 *
	 * @param httpMethodClient The HTTP client to use.
	 * @param context The deserialization context.
	 */
	public HttpJsonCommunicator(
			final HttpMethodClient<Deserializer> httpMethodClient,
			final DeserializationContext context) {
		this.httpMethodClient = httpMethodClient;
		this.responseStrategy = new HttpJsonResponseStrategy(context);
		this.voidResponseStrategy = new HttpVoidResponseStrategy();
	}

	@Override
	public CompletableFuture<Deserializer> post(final URL url, final SerializableEntity entity) {
		return this.httpMethodClient.post(url, new HttpJsonPostRequest(entity), this.responseStrategy).getFuture();
	}

	@Override
	public CompletableFuture<Deserializer> postVoid(final URL url, final SerializableEntity entity) {
		return this.httpMethodClient.post(url, new HttpJsonPostRequest(entity), this.voidResponseStrategy).getFuture();
	}
}
