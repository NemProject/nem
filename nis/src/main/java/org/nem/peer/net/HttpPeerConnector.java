package org.nem.peer.net;

import org.nem.core.model.Block;
import org.nem.core.model.BlockFactory;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.core.serialization.SerializableEntity;
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
	public Block getLastBlock(final NodeEndpoint endpoint) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_LAST_BLOCK);
		JsonDeserializer jsonDeserializer = this.httpMethodClient.get(url);
		if (jsonDeserializer == null) {
			return null;
		}
		return BlockFactory.VERIFIABLE.deserialize(jsonDeserializer);
	}

    @Override
    public void announce(final NodeEndpoint endpoint, final NodeApiId announceId, final SerializableEntity entity) {
        final URL url = endpoint.getApiUrl(announceId);
        this.httpMethodClient.post(url, entity);
    }
}
