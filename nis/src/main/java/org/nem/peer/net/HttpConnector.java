package org.nem.peer.net;

import net.minidev.json.JSONObject;
import org.nem.core.model.Block;
import org.nem.core.model.BlockFactory;
import org.nem.core.serialization.*;
import org.nem.peer.*;

import java.net.*;
import java.util.List;

/**
 * An HTTP-based PeerConnector and SyncConnector implementation.
 */
public class HttpConnector implements PeerConnector, SyncConnector {

	private static final int DEFAULT_TIMEOUT = 30;

	private final HttpMethodClient httpMethodClient;

	/**
	 * Creates a new HTTP peer connector.
	 *
	 * @param context The deserialization context to use when deserializing responses.
	 */
	public HttpConnector(final DeserializationContext context) {
		this.httpMethodClient = new HttpMethodClient(context, DEFAULT_TIMEOUT);
	}

	//region PeerConnector

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
	public void announce(final NodeEndpoint endpoint, final NodeApiId announceId, final SerializableEntity entity) {
		final URL url = endpoint.getApiUrl(announceId);
		this.httpMethodClient.post(url, entity);
	}

	//endregion

    // region SyncConnector

	@Override
	public Block getLastBlock(final NodeEndpoint endpoint) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_LAST_BLOCK);
		return BlockFactory.VERIFIABLE.deserialize(this.httpMethodClient.get(url));
	}

	@Override
	public Block getBlockAt(final NodeEndpoint endpoint, long height) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_BLOCK_AT);
		final JSONObject obj = getJsonObjectWithHeight(height);
		return BlockFactory.VERIFIABLE.deserialize(this.httpMethodClient.post(url, obj));
	}

	@Override
	public List<Block> getChainAfter(NodeEndpoint endpoint, long height) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_BLOCKS_AFTER);
		final JSONObject obj = getJsonObjectWithHeight(height);
		final JsonDeserializer jsonDeserializer = this.httpMethodClient.post(url, obj);
		return jsonDeserializer.readObjectArray("blocks", BlockFactory.VERIFIABLE);
	}

	//endregion

	private static JSONObject getJsonObjectWithHeight(long height) {
		final JSONObject obj = new JSONObject();
		obj.put("height", height);
		return obj;
	}
}
