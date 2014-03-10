package org.nem.peer.net;

import org.nem.core.model.Transaction;
import org.nem.core.serialization.*;
import org.nem.peer.*;

import java.net.*;

/**
 * An HTTP-based PeerConnector implementation.
 */
public class HttpPeerConnector implements PeerConnector {

    private static final int DEFAULT_TIMEOUT = 30;

    private final HttpMethodClient httpMethodClient;

    /**
     * Creates a new HTTP peer connector.
     */
    public HttpPeerConnector() {
        this.httpMethodClient = new HttpMethodClient(DEFAULT_TIMEOUT);
    }

    @Override
    public Node getInfo(final NodeEndpoint endpoint) {
        final URL url = endpoint.getApiUrl(NodeApiId.REST_NODE_INFO);
        return new Node(this.httpMethodClient.get(url));
    }

    @Override
    public NodeCollection getKnownPeers(final NodeEndpoint endpoint) {
        final URL url = endpoint.getApiUrl(NodeApiId.REST_NODE_PEER_LIST);
        return new NodeCollection(this.httpMethodClient.get(url));
    }

	@Override
	public void pushTransaction(final NodeEndpoint endpoint, final Transaction transaction) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_PUSH_TRANSACTION);
        this.httpMethodClient.post(url, JsonSerializer.serializeToJson(transaction));
	}
}
