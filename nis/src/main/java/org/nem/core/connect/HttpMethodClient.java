package org.nem.core.connect;

import net.minidev.json.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.client.*;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.eclipse.jetty.http.MimeTypes;
import org.nem.core.serialization.*;

import java.net.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Helper class that wraps an HttpClient.
 *
 * @param <T> The type of responses.
 */
public class HttpMethodClient<T> {

	private final CloseableHttpAsyncClient httpClient;

	/**
	 * Creates a new HTTP method client.
	 *
	 * @param timeout The timeout (in seconds) that should be used.
	 */
	public HttpMethodClient(final int timeout) {
		final RequestConfig config = RequestConfig.custom()
				.setSocketTimeout(timeout)
				.setConnectTimeout(timeout)
				.setRedirectsEnabled(false)
				.build();

		this.httpClient = HttpAsyncClients.custom()
				.setDefaultRequestConfig(config)
				.build();
		this.httpClient.start();
	}

	/**
	 * Issues a HTTP GET response.
	 *
	 * @param url The url.
	 * @param responseStrategy The response strategy.
	 * @return The response from the server.
	 */
	public CompletableFuture<T> get(final URL url, final HttpResponseStrategy<T> responseStrategy) {
		return sendRequest(url, HttpGet::new, responseStrategy);
	}

	/**
	 * Issues a HTTP POST response.
	 *
	 * @param url    The url.
	 * @param entity The request data.
	 * @param responseStrategy The response strategy.
	 * @return The response from the server.
	 */
	public CompletableFuture<T> post(
			final URL url,
			final SerializableEntity entity,
			final HttpResponseStrategy<T> responseStrategy) {
		return this.post(url, JsonSerializer.serializeToJson(entity), responseStrategy);
	}

	/**
	 * Issues a HTTP POST response.
	 *
	 * @param url         The url.
	 * @param requestData The request data.
	 * @param responseStrategy The response strategy.
	 * @return The response from the server.
	 */
	public CompletableFuture<T> post(
			final URL url,
			final JSONObject requestData,
			final HttpResponseStrategy<T> responseStrategy) {
		return sendRequest(url, uri -> createPostRequest(uri, requestData), responseStrategy);
	}

	private static HttpPost createPostRequest(final URI uri, final JSONObject requestData) {
		final HttpPost request = new HttpPost(uri);
		final ByteArrayEntity entity = new ByteArrayEntity(requestData.toString().getBytes()); // TODO: use UTF-8 charset
		entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON.asString()));
		request.setEntity(entity);
		return request;
	}

	/**
	 * Sends an HTTP request.
	 *
	 * @param requestFactory The factory that creates the specified request.
	 * @param responseStrategy The response strategy.
	 * @return The response from the server.
	 */
	private CompletableFuture<T> sendRequest(
			final URL url,
			final Function<URI, HttpRequestBase> requestFactory,
			final HttpResponseStrategy<T> responseStrategy) {
		try {
			final URI uri = url.toURI();

			final HttpMethodClientFutureCallback callback = new HttpMethodClientFutureCallback();
			final HttpRequestBase request = requestFactory.apply(uri);
			this.httpClient.execute(request, callback);

			return callback.getFuture().thenApply(response -> responseStrategy.coerce(request, response));

		} catch (URISyntaxException e) {
			throw new FatalPeerException(e);
		}
	}

	// TODO: move out of this file and test
	private class HttpMethodClientFutureCallback implements FutureCallback<HttpResponse> {

		private final CompletableFuture<HttpResponse> future = new CompletableFuture<>();

		public CompletableFuture<HttpResponse> getFuture() { return this.future; }

		@Override
		public void completed(final HttpResponse httpResponse) {
			this.future.complete(httpResponse);
		}

		@Override
		public void failed(final Exception e) {
			this.future.completeExceptionally(e);
		}

		@Override
		public void cancelled() {
			this.future.completeExceptionally(new CancellationException());
		}
	}
}