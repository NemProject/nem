package org.nem.core.connect;

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
	private final HttpResponseStrategy<Deserializer> voidResponseStrategy;

	/**
	 * Creates a new HTTP connector.
	 *
	 * @param httpMethodClient The HTTP client to use.
	 * @param responseStrategy The response strategy to use for functions expected to return data.
	 * @param voidResponseStrategy The response strategy to use for functions expected to not return data.
	 */
	public HttpConnector(
			HttpMethodClient<Deserializer> httpMethodClient,
			final HttpResponseStrategy<Deserializer> responseStrategy,
			final HttpResponseStrategy<Deserializer> voidResponseStrategy) {
		this.httpMethodClient = httpMethodClient;
		this.responseStrategy = responseStrategy;
		this.voidResponseStrategy = voidResponseStrategy;
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
		this.postVoid(url, entity);
	}

	//endregion

    // region SyncConnector

	@Override
	public Block getLastBlock(final NodeEndpoint endpoint) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_LAST_BLOCK);
		return BlockFactory.VERIFIABLE.deserialize(this.get(url));
	}

	@Override
	public Block getBlockAt(final NodeEndpoint endpoint, final BlockHeight height) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_BLOCK_AT);
		return BlockFactory.VERIFIABLE.deserialize(this.post(url, height));
	}

	@Override
	public List<Block> getChainAfter(final NodeEndpoint endpoint, final BlockHeight height) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_BLOCKS_AFTER);
		final Deserializer deserializer = this.post(url, height);
		return deserializer.readObjectArray("blocks", BlockFactory.VERIFIABLE);
	}

	@Override
	public HashChain getHashesFrom(final NodeEndpoint endpoint, final BlockHeight height) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_HASHES_FROM);
		return new HashChain(this.post(url, height));
	}

	//endregion

	private Deserializer get(final URL url) {
		return this.httpMethodClient.get(url, this.responseStrategy);
	}

	private Deserializer post(final URL url, final SerializableEntity entity) {
		return this.httpMethodClient.post(url, entity, this.responseStrategy);
	}

	private Deserializer postVoid(final URL url, final SerializableEntity entity) {
		return this.httpMethodClient.post(url, entity, this.voidResponseStrategy);
	}
}
