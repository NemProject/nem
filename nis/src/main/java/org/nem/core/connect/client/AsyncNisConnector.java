package org.nem.core.connect.client;

import org.nem.core.connect.HttpPostRequest;
import org.nem.core.node.NodeEndpoint;
import org.nem.core.serialization.Deserializer;

import java.util.concurrent.CompletableFuture;

/**
 * An asynchronous NIS connector.
 */
public interface AsyncNisConnector {

	/**
	 * Gets a response from the specified NIS relative url path.
	 *
	 * @param endpoint The NIS endpoint.
	 * @param apiId The api to call.
	 * @param query The get query string or null.
	 * @return The result.
	 */
	public CompletableFuture<Deserializer> getAsync(
			final NodeEndpoint endpoint,
			final NisApiId apiId,
			final String query);

	/**
	 * Posts a request to the specified NIS relative url path.
	 *
	 * @param endpoint The NIS endpoint.
	 * @param apiId The api to call.
	 * @param postRequest The request data.
	 * @return The result.
	 */
	public CompletableFuture<Deserializer> postAsync(
			final NodeEndpoint endpoint,
			final NisApiId apiId,
			final HttpPostRequest postRequest);

	/**
	 * Posts a request to the specified NIS relative url path.
	 *
	 * @param endpoint The NIS endpoint.
	 * @param apiId The api to call.
	 * @param postRequest The request data.
	 */
	public CompletableFuture<Void> postVoidAsync(
			final NodeEndpoint endpoint,
			final NisApiId apiId,
			final HttpPostRequest postRequest);
}
