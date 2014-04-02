package org.nem.peer.net;

import net.minidev.json.JSONObject;
import org.nem.core.model.Block;
import org.nem.core.model.BlockFactory;
import org.nem.core.model.ByteArray;
import org.nem.core.model.ByteArrayFactory;
import org.nem.core.serialization.*;
import org.nem.peer.*;

import java.net.*;
import java.util.List;

/**
 * An HTTP-based PeerConnector implementation.
 */
public class HttpPeerConnector implements PeerConnector {

	private static final int DEFAULT_TIMEOUT = 30;

	private final HttpMethodClient httpMethodClient;

	/**
	 * Creates a new HTTP peer connector.
	 *
	 * @param context The deserialization context to use when deserializing responses.
	 */
	public HttpPeerConnector(final DeserializationContext context) {
		this.httpMethodClient = new HttpMethodClient(context, DEFAULT_TIMEOUT);
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
	public Block getBlockAt(final NodeEndpoint endpoint, long height) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_BLOCK_AT);
		JSONObject obj = new JSONObject();
		obj.put("height", height);
		JsonDeserializer jsonDeserializer = this.httpMethodClient.post(url, obj);
		if (jsonDeserializer == null) {
			return null;
		}
		return BlockFactory.VERIFIABLE.deserialize(jsonDeserializer);
	}

	@Override
	public List<Block> getChainAfter(NodeEndpoint endpoint, long height) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_BLOCKS_AFTER);
		JSONObject obj = new JSONObject();
		obj.put("height", height);
		JsonDeserializer jsonDeserializer = this.httpMethodClient.post(url, obj);
		if (jsonDeserializer == null) {
			return null;
		}
		return jsonDeserializer.readObjectArray("blocks", BlockFactory.VERIFIABLE);
	}

	@Override
	public List<ByteArray> getHashesFrom(NodeEndpoint endpoint, long height) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_HASHES_FROM);
		JSONObject obj = new JSONObject();
		obj.put("height", height);
		JsonDeserializer jsonDeserializer = this.httpMethodClient.post(url, obj);
		if (jsonDeserializer == null) {
			return null;
		}
		return jsonDeserializer.readObjectArray("hashes", ByteArrayFactory.deserializer);
	}

	@Override
	public void announce(final NodeEndpoint endpoint, final NodeApiId announceId, final SerializableEntity entity) {
		final URL url = endpoint.getApiUrl(announceId);
		this.httpMethodClient.post(url, entity);
	}
}
