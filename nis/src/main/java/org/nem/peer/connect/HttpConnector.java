package org.nem.peer.connect;

import org.nem.core.connect.*;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.peer.node.*;

import java.net.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
	public CompletableFuture<NisNodeInfo> getInfo(final NodeEndpoint endpoint) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_NODE_INFO);
		return this.getAsync(url).getFuture().thenApply(NisNodeInfo::new);
	}

	@Override
	public CompletableFuture<NodeCollection> getKnownPeers(final NodeEndpoint endpoint) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_NODE_PEER_LIST);
		return this.getAsync(url).getFuture().thenApply(NodeCollection::new);
	}

	@Override
	public CompletableFuture<NodeEndpoint> getLocalNodeInfo(final NodeEndpoint endpoint) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CAN_YOU_SEE_ME);
		return this.getAsync(url).getFuture().thenApply(NodeEndpoint::new);
	}

	@Override
	public CompletableFuture announce(final NodeEndpoint endpoint, final NodeApiId announceId, final SerializableEntity entity) {
		final URL url = endpoint.getApiUrl(announceId);
		return this.postVoidAsync(url, entity).getFuture();
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
		return deserializer.readObjectArray("data", BlockFactory.VERIFIABLE);
	}

	@Override
	public HashChain getHashesFrom(final NodeEndpoint endpoint, final BlockHeight height) {
		final URL url = endpoint.getApiUrl(NodeApiId.REST_CHAIN_HASHES_FROM);
		return new HashChain(this.post(url, height));
	}

	//endregion

	private Deserializer get(final URL url) {
		return this.getAsync(url).get();
	}

	private HttpMethodClient.AsyncToken<Deserializer> getAsync(final URL url) {
		return this.httpMethodClient.get(url, this.responseStrategy);
	}

	private Deserializer post(final URL url, final SerializableEntity entity) {
		return this.httpMethodClient.post(url, entity, this.responseStrategy).get();
	}

	private HttpMethodClient.AsyncToken<Deserializer> postVoidAsync(final URL url, final SerializableEntity entity) {
		return this.httpMethodClient.post(url, entity, this.voidResponseStrategy);
	}
}
