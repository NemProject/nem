package org.nem.peer.net;

import net.minidev.json.*;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.*;
import org.eclipse.jetty.client.util.*;
import org.eclipse.jetty.http.HttpMethod;
import org.nem.core.dbmodel.Account;
import org.nem.core.serialization.*;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.AccountAnalyzer;
import org.nem.peer.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Helper class that wraps an HttpClient.
 */
public class HttpMethodClient {

    private static final int HTTP_STATUS_OK = 200;

    private final int timeout;
    private final HttpClient httpClient;

	private AccountAnalyzer accountAnalyzer;

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
        }
        catch (Exception ex) {
            throw new FatalPeerException("HTTP client could not be started", ex);
        }
    }

	public void setAccountAnalyzer(AccountAnalyzer accountAnalyzer) {
		this.accountAnalyzer = accountAnalyzer;
	}

    /**
     * Issues a HTTP GET response.
     *
     * @param url The url.
     * @return The response from the server.
     */
    public JsonDeserializer get(final URL url) {
        return sendRequest(url, new RequestFactory() {
            @Override
            public Request createRequest(final HttpClient httpClient, final URI uri) {
                return httpClient.newRequest(uri);
            }
        });
    }

    /**
     * Issues a HTTP POST response.
     *
     * @param url The url.
     * @param entity The request data.
     * @return The response from the server.
     */
    public JsonDeserializer post(final URL url, final SerializableEntity entity) {
        return this.post(url, JsonSerializer.serializeToJson(entity));
    }

    /**
     * Issues a HTTP POST response.
     *
     * @param url The url.
     * @param requestData The request data.
     * @return The response from the server.
     */
    public JsonDeserializer post(final URL url, final JSONObject requestData) {
        return sendRequest(url, new RequestFactory() {
            @Override
            public Request createRequest(final HttpClient httpClient, final URI uri) {
                Request req = httpClient.newRequest(uri);
                req.method(HttpMethod.POST);
                req.content(new BytesContentProvider(requestData.toString().getBytes()), "text/plain");
                return req;
            }
        });
    }

    /**
     * Sends an HTTP request.
     *
     * @param requestFactory The factory that creates the specified request.
     * @return The response from the server.
     */
    private JsonDeserializer sendRequest(final URL url, final RequestFactory requestFactory) {
        try {
            final URI uri = url.toURI();
            final InputStreamResponseListener listener = new InputStreamResponseListener();

            final Request req = requestFactory.createRequest(this.httpClient, uri);
            req.send(listener);

            Response res = listener.get(this.timeout, TimeUnit.SECONDS);
            if (res.getStatus() != HTTP_STATUS_OK)
                return null;

            try (InputStream responseStream = listener.getInputStream()) {
                return new JsonDeserializer(
                    (JSONObject)JSONValue.parse(responseStream),
                    new DeserializationContext(accountAnalyzer));
            }
        }
        catch (TimeoutException e) {
            throw new InactivePeerException(e);
        }
        catch (URISyntaxException|ExecutionException|IOException e) {
            throw new FatalPeerException(e);
        }
        catch (InterruptedException e) {
            throw ExceptionUtils.toUnchecked(e);
        }
    }

    private static interface RequestFactory {

        public Request createRequest(final HttpClient httpClient, final URI uri);
    }
}