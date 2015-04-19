package org.nem.core.connect;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.client.*;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.nem.core.async.SleepFuture;
import org.nem.core.utils.ExceptionUtils;

import java.net.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Helper class that wraps an HttpClient.
 *
 * @param <T> The type of responses.
 */
public class HttpMethodClient<T> {
	private static final Logger LOGGER = Logger.getLogger(HttpMethodClient.class.getName());

	private static final int MAX_CONNECTIONS = 100;
	private static final int MAX_CONNECTIONS_PER_ROUTE = 20;

	private final CloseableHttpAsyncClient httpClient;
	private final int requestTimeout;

	/**
	 * Creates a new HTTP method client with default timeouts.
	 */
	public HttpMethodClient() {
		this(5000, 5000, 3 * 60000);
	}

	/**
	 * Creates a new HTTP method client.
	 *
	 * @param connectionTimeout The connection timeout (in milliseconds) that should be used.
	 * @param socketTimeout The socket timeout (in milliseconds) that should be used.
	 * @param requestTimeout The request timeout (in milliseconds) that should be used.
	 */
	public HttpMethodClient(final int connectionTimeout, final int socketTimeout, final int requestTimeout) {
		final RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(connectionTimeout)
				.setConnectionRequestTimeout(connectionTimeout)
				.setSocketTimeout(socketTimeout)
				.setRedirectsEnabled(false)
				.build();

		this.httpClient = HttpAsyncClients.custom()
				.setDefaultRequestConfig(config)
				.setMaxConnPerRoute(MAX_CONNECTIONS_PER_ROUTE)
				.setMaxConnTotal(MAX_CONNECTIONS)
				.build();
		this.httpClient.start();

		this.requestTimeout = requestTimeout;
	}

	/**
	 * Issues a HTTP GET request.
	 *
	 * @param url The url.
	 * @param responseStrategy The response strategy.
	 * @return The response from the server.
	 */
	public AsyncToken<T> get(final URL url, final HttpResponseStrategy<T> responseStrategy) {
		return this.sendRequest(url, HttpGet::new, responseStrategy);
	}

	/**
	 * Issues a HTTP POST request.
	 *
	 * @param url The url.
	 * @param request The request.
	 * @param responseStrategy The response strategy.
	 * @return The response from the server.
	 */
	public AsyncToken<T> post(
			final URL url,
			final HttpPostRequest request,
			final HttpResponseStrategy<T> responseStrategy) {
		return this.sendRequest(url, uri -> createPostRequest(uri, request), responseStrategy);
	}

	private static HttpPost createPostRequest(final URI uri, final HttpPostRequest request) {
		final HttpPost post = new HttpPost(uri);
		final ByteArrayEntity entity = new ByteArrayEntity(request.getPayload());
		entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, request.getContentType()));
		post.setEntity(entity);
		return post;
	}

	/**
	 * Sends an HTTP request.
	 *
	 * @param requestFactory The factory that creates the specified request.
	 * @param responseStrategy The response strategy.
	 * @return The response from the server.
	 */
	private AsyncToken<T> sendRequest(
			final URL url,
			final Function<URI, HttpRequestBase> requestFactory,
			final HttpResponseStrategy<T> responseStrategy) {
		try {
			final URI uri = url.toURI();

			final HttpMethodClientFutureCallback callback = new HttpMethodClientFutureCallback();
			final HttpRequestBase request = requestFactory.apply(uri);
			request.setHeader("Accept", responseStrategy.getSupportedContentType());

			this.httpClient.execute(request, callback);

			final CompletableFuture<T> responseFuture = callback.getFuture()
					.thenApply(response -> responseStrategy.coerce(request, response));

			SleepFuture.create(this.requestTimeout).thenAccept(v -> {
				if (responseFuture.isDone()) {
					return;
				}

				LOGGER.warning(String.format("forcibly aborting request to %s", url));
				request.abort();
			});

			return new AsyncToken<>(request, responseFuture);
		} catch (final URISyntaxException e) {
			throw new FatalPeerException(e);
		}
	}

	public void close() {
		ExceptionUtils.propagateVoid(this.httpClient::close);
	}

	/**
	 * The result of an HttpMethodClient operation. This type exposes a future
	 * for chaining async operations as well as a method for aborting the operation.
	 *
	 * @param <T> The type of result.
	 */
	public static class AsyncToken<T> {

		private final HttpRequestBase request;
		private final CompletableFuture<T> future;

		private AsyncToken(final HttpRequestBase request, final CompletableFuture<T> future) {
			this.request = request;
			this.future = future;
		}

		/**
		 * Gets the future for the HTTP operation.
		 *
		 * @return The future.
		 */
		public CompletableFuture<T> getFuture() {
			return this.future;
		}

		/**
		 * Waits if necessary for the underlying future to complete, and then
		 * returns its result.
		 *
		 * @return The result value.
		 */
		public T get() {
			return ExceptionUtils.propagate(() -> this.getFuture().get());
		}

		/**
		 * Aborts the HTTP operation.
		 */
		public void abort() {
			this.request.abort();
		}
	}

	private static class HttpMethodClientFutureCallback implements FutureCallback<HttpResponse> {

		private final CompletableFuture<HttpResponse> future = new CompletableFuture<>();

		public CompletableFuture<HttpResponse> getFuture() {
			return this.future;
		}

		@Override
		public void completed(final HttpResponse httpResponse) {
			this.future.complete(httpResponse);
		}

		@Override
		public void failed(final Exception e) {
			this.future.completeExceptionally(wrapException(e));
		}

		private static Exception wrapException(final Exception e) {
			if (SocketTimeoutException.class == e.getClass()) {
				return new BusyPeerException(e);
			} else if (ConnectException.class == e.getClass()) {
				return new InactivePeerException(e);
			} else {
				return new FatalPeerException(e);
			}
		}

		@Override
		public void cancelled() {
			this.future.cancel(true);
		}
	}
}