package org.nem.peer.connect;

import org.nem.core.connect.*;
import org.nem.core.serialization.*;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP-base communicator implementation.
 */
public class HttpCommunicator implements Communicator {
	private final HttpMethodClient<Deserializer> httpMethodClient;
	private final HttpResponseStrategy<Deserializer> responseStrategy;
	private final HttpResponseStrategy<Deserializer> voidResponseStrategy;

	/**
	 * Creates a new HTTP communicator.
	 *
	 * @param httpMethodClient The HTTP client to use.
	 * @param responseStrategy The response strategy to use for functions expected to return data.
	 * @param voidResponseStrategy The response strategy to use for functions expected to not return data.
	 */
	public HttpCommunicator(
			final HttpMethodClient<Deserializer> httpMethodClient,
			final HttpResponseStrategy<Deserializer> responseStrategy,
			final HttpResponseStrategy<Deserializer> voidResponseStrategy) {
		this.httpMethodClient = httpMethodClient;
		this.responseStrategy = responseStrategy;
		this.voidResponseStrategy = voidResponseStrategy;
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
