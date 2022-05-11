package org.nem.peer.connect;

import org.nem.core.connect.*;
import org.nem.core.serialization.*;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP JSON-base communicator implementation.
 */
public class HttpCommunicator implements Communicator {
	private final HttpMethodClient<Deserializer> httpMethodClient;
	private final boolean isBinaryMode;
	private final HttpResponseStrategy<Deserializer> responseStrategy;
	private final HttpResponseStrategy<Deserializer> voidResponseStrategy;

	/**
	 * Creates a new HTTP communicator.
	 *
	 * @param httpMethodClient The HTTP client to use.
	 * @param communicationMode The communication mode.
	 * @param context The deserialization context.
	 */
	public HttpCommunicator(final HttpMethodClient<Deserializer> httpMethodClient, final CommunicationMode communicationMode,
			final DeserializationContext context) {
		this.httpMethodClient = httpMethodClient;
		this.isBinaryMode = CommunicationMode.BINARY == communicationMode;
		this.responseStrategy = this.createResponseStrategy(context);
		this.voidResponseStrategy = new HttpVoidResponseStrategy();
	}

	@Override
	public CompletableFuture<Deserializer> post(final URL url, final SerializableEntity entity) {
		return this.httpMethodClient.post(url, this.createPostRequest(entity), this.responseStrategy).getFuture();
	}

	@Override
	public CompletableFuture<Deserializer> postVoid(final URL url, final SerializableEntity entity) {
		return this.httpMethodClient.post(url, this.createPostRequest(entity), this.voidResponseStrategy).getFuture();
	}

	private HttpResponseStrategy<Deserializer> createResponseStrategy(final DeserializationContext context) {
		return this.isBinaryMode ? new HttpBinaryResponseStrategy(context) : new HttpJsonResponseStrategy(context);
	}

	private HttpPostRequest createPostRequest(final SerializableEntity entity) {
		return this.isBinaryMode ? new HttpBinaryPostRequest(entity) : new HttpJsonPostRequest(entity);
	}
}
