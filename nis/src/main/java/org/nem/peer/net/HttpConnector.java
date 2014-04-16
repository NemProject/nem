package org.nem.peer.net;

import net.minidev.json.JSONObject;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.peer.*;

import java.net.*;
import java.util.List;

/**
 * An HTTP-based PeerConnector and SyncConnector implementation.
 */
public class HttpConnector implements PeerConnector, SyncConnector {

	private final HttpMethodClient<Deserializer> httpMethodClient;
	private final HttpResponseStrategy<Deserializer> responseStrategy;

	/**
	 * Creates a new HTTP connector.
	 *
	 * @param httpMethodClient The HTTP client to use.
	 * @param responseStrategy The response strategy to use.
	 */
	public HttpConnector(
			HttpMethodClient<Deserializer> httpMethodClient,
			final HttpResponseStrategy<Deserializer> responseStrategy) {
		this.httpMethodClient = httpMethodClient;
		this.responseStrategy = responseStrategy;
	}

	//region PeerConnector

	@Override
	public Node getInfo(final NodeEndpoint endpoint) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_NODE_INFO);
		return new Node(this.get(url));
	}

	@Override
	public NodeCollection getKnownPeers(final NodeEndpoint endpoint) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_NODE_PEER_LIST);
		return new NodeCollection(this.get(url));
	}

	@Override
	public void announce(final NodeEndpoint endpoint, final NodeApiId announceId, final SerializableEntity entity) {
		final URL url = endpoint.getApiUrl(announceId);
		this.post(url, entity);
	}

	//endregion

    // region SyncConnector

	@Override
	public Block getLastBlock(final NodeEndpoint endpoint) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_LAST_BLOCK);
		return BlockFactory.VERIFIABLE.deserialize(this.get(url));
	}

	@Override
	public Block getBlockAt(final NodeEndpoint endpoint, long height) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_BLOCK_AT);
		final JSONObject obj = getJsonObjectWithHeight(height);
		return BlockFactory.VERIFIABLE.deserialize(this.post(url, obj));
	}

	@Override
	public List<Block> getChainAfter(NodeEndpoint endpoint, long height) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_BLOCKS_AFTER);
		final JSONObject obj = getJsonObjectWithHeight(height);
		final Deserializer deserializer = this.post(url, obj);
		return deserializer.readObjectArray("blocks", BlockFactory.VERIFIABLE);
	}

	@Override
	public HashChain getHashesFrom(NodeEndpoint endpoint, long height) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_HASHES_FROM);
		final JSONObject obj = getJsonObjectWithHeight(height);
		return HashChainFactory.deserializer.deserialize(this.post(url, obj));
	}

	//endregion

	private Deserializer get(final URL url) {
		return this.httpMethodClient.get(url, this.responseStrategy);
	}

	private Deserializer post(final URL url, final JSONObject object) {
		return this.httpMethodClient.post(url, object, this.responseStrategy);
	}

	private Deserializer post(final URL url, final SerializableEntity entity) {
		return this.httpMethodClient.post(url, entity, this.responseStrategy);
	}

	private static JSONObject getJsonObjectWithHeight(long height) {
		final JSONObject obj = new JSONObject();
		obj.put("height", height);
		return obj;
	}
}
