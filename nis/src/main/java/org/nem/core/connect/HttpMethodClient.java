package org.nem.core.connect;

import net.minidev.json.*;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.*;
import org.eclipse.jetty.client.util.*;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.nem.core.serialization.*;
import org.nem.core.utils.ExceptionUtils;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Helper class that wraps an HttpClient.
 *
 * @param <T> The type of responses.
 */
public class HttpMethodClient<T> {

	private final int timeout;
	private final HttpClient httpClient;

	/**
	 * Creates a new HTTP method client.
	 *
	 * @param timeout The timeout (in seconds) that should be used.
	 */
	public HttpMethodClient(final int timeout) {
		this.timeout = timeout;

		try {
			this.httpClient = new HttpClient();
			this.httpClient.setFollowRedirects(false);
			this.httpClient.start();
		} catch (Exception ex) {
			throw new FatalPeerException("HTTP client could not be started", ex);
		}
	}

	/**
	 * Issues a HTTP GET response.
	 *
	 * @param url The url.
	 * @param responseStrategy The response strategy.
	 * @return The response from the server.
	 */
	public T get(final URL url, final HttpResponseStrategy<T> responseStrategy) {
		return sendRequest(url, new RequestFactory() {
			@Override
			public Request createRequest(final HttpClient httpClient, final URI uri) {
				return httpClient.newRequest(uri);
			}
		}, responseStrategy);
	}

	/**
	 * Issues a HTTP POST response.
	 *
	 * @param url    The url.
	 * @param entity The request data.
	 * @param responseStrategy The response strategy.
	 * @return The response from the server.
	 */
	public T post(final URL url, final SerializableEntity entity, final HttpResponseStrategy<T> responseStrategy) {
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
	public T post(final URL url, final JSONObject requestData, final HttpResponseStrategy<T> responseStrategy) {
		return sendRequest(url, new RequestFactory() {
			@Override
			public Request createRequest(final HttpClient httpClient, final URI uri) {
				Request req = httpClient.newRequest(uri);
				req.method(HttpMethod.POST);
				req.content(
						new BytesContentProvider(requestData.toString().getBytes()),
						MimeTypes.Type.APPLICATION_JSON.asString());
				return req;
			}
		}, responseStrategy);
	}

	/**
	 * Sends an HTTP request.
	 *
	 * @param requestFactory The factory that creates the specified request.
	 * @param responseStrategy The response strategy.
	 * @return The response from the server.
	 */
	private T sendRequest(final URL url, final RequestFactory requestFactory, final HttpResponseStrategy<T> responseStrategy) {
		try {
			final URI uri = url.toURI();
			final InputStreamResponseListener listener = new InputStreamResponseListener();

			final Request req = requestFactory.createRequest(this.httpClient, uri);
			req.send(listener);

			final Response res = listener.get(this.timeout, TimeUnit.SECONDS);
			return responseStrategy.coerce(req, res);
		} catch (TimeoutException e) {
			throw new InactivePeerException(e);
		} catch (URISyntaxException | ExecutionException | IOException e) {
			throw new FatalPeerException(e);
		} catch (InterruptedException e) {
			throw ExceptionUtils.toUnchecked(e);
		}
	}

	private static interface RequestFactory {

		public Request createRequest(final HttpClient httpClient, final URI uri);
	}
}