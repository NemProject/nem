package org.nem.core.connect.client;

import org.nem.core.connect.*;
import org.nem.core.node.NodeEndpoint;
import org.nem.core.serialization.*;
import org.nem.core.utils.ExceptionUtils;

import java.net.*;
import java.util.concurrent.CompletableFuture;

/**
 * A default AsyncNemConnector implementation
 *
 * @param <TApiId> The api id type.
 */
public class DefaultAsyncNemConnector<TApiId> implements AsyncNemConnector<TApiId> {
	private final HttpMethodClient<ErrorResponseDeserializerUnion> httpClient;
	private final ErrorResponseStrategy errorResponseStrategy;
	private HttpErrorResponseDeserializerUnionStrategy httpDeserializerResponseStrategy;

	/**
	 * Creates a new default NIS connector.
	 *
	 * @param httpClient The HTTP client.
	 * @param errorResponseStrategy The error response strategy.
	 */
	public DefaultAsyncNemConnector(
			final HttpMethodClient<ErrorResponseDeserializerUnion> httpClient,
			final ErrorResponseStrategy errorResponseStrategy) {
		this.httpClient = httpClient;
		this.errorResponseStrategy = errorResponseStrategy;
	}

	/**
	 * Sets the account lookup associated with this connector.
	 * This allows address to automatically be deserialized into accounts.
	 *
	 * @param accountLookup The account lookup to use.
	 */
	public void setAccountLookup(final SimpleAccountLookup accountLookup) {
		this.httpDeserializerResponseStrategy = new HttpErrorResponseDeserializerUnionStrategy(
				new DeserializationContext(accountLookup));
	}

	/**
	 * Gets the response strategy in use.
	 *
	 * @return The response strategy.
	 */
	public HttpErrorResponseDeserializerUnionStrategy getResponseStrategy() {
		return this.httpDeserializerResponseStrategy;
	}

	@Override
	public CompletableFuture<Deserializer> getAsync(final NodeEndpoint endpoint, final TApiId apiId, final String query) {
		final String path = apiId + (null == query ? "" : "?" + query);
		return ExceptionUtils.propagate(() ->
				this.httpClient.get(
						this.createNisUrl(endpoint, path),
						this.httpDeserializerResponseStrategy)
						.getFuture()
						.thenApply(response -> {
							if (response.hasError()) {
								throw this.errorResponseStrategy.mapToException(response.getError());
							}

							return response.getDeserializer();
						}));
	}

	@Override
	public CompletableFuture<Deserializer> postAsync(final NodeEndpoint endpoint, final TApiId apiId, final HttpPostRequest postRequest) {
		return this.postAsyncImpl(endpoint, apiId, postRequest).thenApply(ErrorResponseDeserializerUnion::getDeserializer);
	}

	@Override
	public CompletableFuture<Void> postVoidAsync(final NodeEndpoint endpoint, final TApiId apiId, final HttpPostRequest postRequest) {
		return this.postAsyncImpl(endpoint, apiId, postRequest).thenApply(response -> null);
	}

	private CompletableFuture<ErrorResponseDeserializerUnion> postAsyncImpl(final NodeEndpoint endpoint, final TApiId apiId, final HttpPostRequest postRequest) {
		return ExceptionUtils.propagate(() ->
				this.httpClient.post(
						this.createNisUrl(endpoint, apiId.toString()),
						postRequest,
						this.httpDeserializerResponseStrategy)
						.getFuture()
						.thenApply(response -> {
							if (response.hasError()) {
								throw this.errorResponseStrategy.mapToException(response.getError());
							}

							return response;
						}));
	}

	private URL createNisUrl(final NodeEndpoint endpoint, final String nisPath) throws MalformedURLException {
		return new URL(endpoint.getBaseUrl(), nisPath);
	}
}
