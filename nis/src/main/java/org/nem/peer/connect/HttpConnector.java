package org.nem.peer.connect;

import org.nem.core.connect.*;
import org.nem.core.crypto.HashChain;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.peer.node.*;

import java.net.*;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * An HTTP-based PeerConnector and SyncConnector implementation.
 */
public class HttpConnector implements PeerConnector, SyncConnector {

	private final HttpMethodClient<Deserializer> httpMethodClient;
	private final HttpResponseStrategy<Deserializer> responseStrategy;
	private final HttpResponseStrategy<Deserializer> voidResponseStrategy;
	private final NodeChallengeFactory challengeFactory;

	/**
	 * Creates a new HTTP connector.
	 *
	 * @param httpMethodClient The HTTP client to use.
	 * @param responseStrategy The response strategy to use for functions expected to return data.
	 * @param voidResponseStrategy The response strategy to use for functions expected to not return data.
	 */
	public HttpConnector(
			final HttpMethodClient<Deserializer> httpMethodClient,
			final HttpResponseStrategy<Deserializer> responseStrategy,
			final HttpResponseStrategy<Deserializer> voidResponseStrategy) {
		this.httpMethodClient = httpMethodClient;
		this.responseStrategy = responseStrategy;
		this.voidResponseStrategy = voidResponseStrategy;
		this.challengeFactory = new NodeChallengeFactory();
	}

	//region PeerConnector

	@Override
	public CompletableFuture<Node> getInfo(final Node node) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_NODE_INFO);
		return postAuthenticated(url, node.getIdentity(), Node::new);
	}

	@Override
	public CompletableFuture<SerializableList<Node>> getKnownPeers(final Node node) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_NODE_PEER_LIST_ACTIVE);
		return postAuthenticated(url, node.getIdentity(), d -> new SerializableList<>(d, Node::new));
	}

	@Override
	public CompletableFuture<NodeEndpoint> getLocalNodeInfo(
			final Node node,
			final NodeEndpoint localEndpoint) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_NODE_CAN_YOU_SEE_ME);
		return this.post(url, localEndpoint).getFuture().thenApply(NodeEndpoint::new);
	}

	@Override
	public CompletableFuture announce(
			final Node node,
			final NodeApiId announceId,
			final SerializableEntity entity) {
		final URL url = node.getEndpoint().getApiUrl(announceId);
		return this.postVoidAsync(url, entity).getFuture();
	}

	//endregion

    // region SyncConnector

	@Override
	public Block getLastBlock(final Node node) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_CHAIN_LAST_BLOCK);
		return postAuthenticated(url, node.getIdentity(), BlockFactory.VERIFIABLE::deserialize).join();
	}

	@Override
	public Block getBlockAt(final Node node, final BlockHeight height) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_BLOCK_AT);
		return postAuthenticated(url, node.getIdentity(), BlockFactory.VERIFIABLE::deserialize, height).join();
	}

	@Override
	public Collection<Block> getChainAfter(final Node node, final BlockHeight height) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_CHAIN_BLOCKS_AFTER);
		return postAuthenticated(
				url,
				node.getIdentity(),
				d -> new SerializableList<>(d, BlockFactory.VERIFIABLE),
				height).join().asCollection();
	}

	@Override
	public HashChain getHashesFrom(final Node node, final BlockHeight height) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_CHAIN_HASHES_FROM);
		return postAuthenticated(url, node.getIdentity(), HashChain::new, height).join();
	}

	@Override
	public BlockChainScore getChainScore(final Node node) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_CHAIN_SCORE);
		return postAuthenticated(url, node.getIdentity(), BlockChainScore::new).join();
	}

	//endregion

	private HttpMethodClient.AsyncToken<Deserializer> getAsync(final URL url) {
		return this.httpMethodClient.get(url, this.responseStrategy);
	}

	private HttpMethodClient.AsyncToken<Deserializer> post(final URL url, final SerializableEntity entity) {
		return this.httpMethodClient.post(url, entity, this.responseStrategy);
	}

	private HttpMethodClient.AsyncToken<Deserializer> postVoidAsync(final URL url, final SerializableEntity entity) {
		return this.httpMethodClient.post(url, entity, this.voidResponseStrategy);
	}

	private <T extends SerializableEntity> CompletableFuture<T> postAuthenticated(
			final URL url,
			final NodeIdentity remoteNodeIdentity,
			final ObjectDeserializer<T> entityDeserializer) {
		final NodeChallenge challenge = this.challengeFactory.next();
		return unwrapAuthenticatedResponse(
				this.post(url, challenge).getFuture(), challenge, remoteNodeIdentity, entityDeserializer);
	}

	private <TOut extends SerializableEntity> CompletableFuture<TOut> postAuthenticated(
			final URL url,
			final NodeIdentity remoteNodeIdentity,
			final ObjectDeserializer<TOut> entityDeserializer,
			final BlockHeight entity) {
		final NodeChallenge challenge = this.challengeFactory.next();
		final AuthenticatedRequest<?> request = new AuthenticatedRequest<>(entity, challenge);
		return unwrapAuthenticatedResponse(
				this.post(url, request).getFuture(), challenge, remoteNodeIdentity, entityDeserializer);
	}

	private static <T extends SerializableEntity> CompletableFuture<T> unwrapAuthenticatedResponse(
			final CompletableFuture<Deserializer> future,
			final NodeChallenge challenge,
		   	final NodeIdentity remoteNodeIdentity,
		   	final ObjectDeserializer<T> entityDeserializer) {
		return future
				.thenApply(d -> new AuthenticatedResponse<>(d, entityDeserializer).getEntity(remoteNodeIdentity, challenge));
	}
}
