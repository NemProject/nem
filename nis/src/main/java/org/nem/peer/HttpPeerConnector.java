package org.nem.peer;

import net.minidev.json.*;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.*;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;
import org.nem.core.model.RequestPrepare;
import org.nem.core.model.Transaction;
import org.nem.core.serialization.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * An HTTP-based PeerConnector implementation.
 */
public class HttpPeerConnector implements PeerConnector {

    private static final int HTTP_STATUS_OK = 200;
    private static final int DEFAULT_TIMEOUT = 30;

    private final HttpClient httpClient;

    /**
     * Creates a new HTTP peer connector.
     */
    public HttpPeerConnector() {
        try {
            this.httpClient = new HttpClient();
            this.httpClient.setFollowRedirects(false);
            this.httpClient.start();
        }
        catch (Exception ex) {
            throw new FatalPeerException("HTTP client could not be started", ex);
        }
    }

    @Override
    public Node getInfo(final NodeEndpoint endpoint) {
        final URL url = endpoint.getApiUrl(NodeApiId.REST_NODE_INFO);
        return new Node(this.getResponse(url));
    }

    @Override
    public NodeCollection getKnownPeers(final NodeEndpoint endpoint) {
        final URL url = endpoint.getApiUrl(NodeApiId.REST_NODE_PEER_LIST);
        return new NodeCollection(this.getResponse(url));
    }

	@Override
	public void pushTransaction(final NodeEndpoint endpoint, final Transaction transaction) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_PUSH_TRANSACTION);
		this.postResponse(url, JsonSerializer.serializeToJson(transaction));
	}

    private JsonDeserializer getResponse(final URL url) {
        try {
            final URI uri = url.toURI();
            final InputStreamResponseListener listener = new InputStreamResponseListener();

            final Request req = this.httpClient.newRequest(uri);
            req.send(listener);

            Response res = listener.get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
            if (res.getStatus() != HTTP_STATUS_OK)
                return null;

            try (InputStream responseStream = listener.getInputStream()) {
                return new JsonDeserializer(
                    (JSONObject)JSONValue.parse(responseStream),
                    new DeserializationContext(null));
            }
        }
        catch (TimeoutException e) {
            throw new InactivePeerException(e);
        }
        catch (URISyntaxException|InterruptedException|ExecutionException|IOException e) {
            throw new FatalPeerException(e);
        }
    }

	protected JsonDeserializer postResponse(URL url, JSONObject request) {
		JSONObject retObj = null;
		try {
			InputStreamResponseListener listener = new InputStreamResponseListener();

			URI uri = url.toURI();
			Request req = httpClient.newRequest(uri);

			req.method(HttpMethod.POST);
			req.content(new BytesContentProvider(request.toString().getBytes()), "text/plain");
			req.send(listener);

			Response res = listener.get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
			if (res.getStatus() != HTTP_STATUS_OK) {
				return null;
			}

			try (InputStream responseStream = listener.getInputStream()) {
				return new JsonDeserializer(
						(JSONObject)JSONValue.parse(responseStream),
						new DeserializationContext(null));
			}

		}
		catch (TimeoutException e) {
			throw new InactivePeerException(e);
		}
		catch (URISyntaxException|InterruptedException|ExecutionException|IOException e) {
			throw new FatalPeerException(e);
		}
	}
}
