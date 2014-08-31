package org.nem.peer.connect;

import org.nem.core.crypto.HashChain;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.*;
import org.nem.core.serialization.*;
import org.nem.nis.time.synchronization.*;
import org.nem.peer.node.*;

import java.net.URL;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * An HTTP-based PeerConnector and SyncConnector implementation.
 */
public class HttpConnector implements PeerConnector, SyncConnector, TimeSyncConnector {

	private final Communicator communicator;
	private final NodeChallengeFactory challengeFactory;

	/**
	 * Creates a new HTTP connector.
	 *
	 * @param communicator The communicator to use.
	 */
	public HttpConnector(final Communicator communicator) {
		this.communicator = communicator;
		this.challengeFactory = new NodeChallengeFactory();
	}

	//region PeerConnector

	@Override
	public CompletableFuture<Node> getInfo(final Node node) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_NODE_INFO);
		return this.postAuthenticated(url, node.getIdentity(), obj -> new Node(obj));
	}

	@Override
	public CompletableFuture<SerializableList<Node>> getKnownPeers(final Node node) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_NODE_PEER_LIST_ACTIVE);
		return this.postAuthenticated(url, node.getIdentity(), d -> new SerializableList<>(d, obj -> new Node(obj)));
	}

	@Override
	public CompletableFuture<NodeEndpoint> getLocalNodeInfo(
			final Node node,
			final NodeEndpoint localEndpoint) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_NODE_CAN_YOU_SEE_ME);
		return this.post(url, localEndpoint).thenApply(obj -> new NodeEndpoint(obj));
	}

	@Override
	public CompletableFuture announce(
			final Node node,
			final NodeApiId announceId,
			final SerializableEntity entity) {
		final URL url = node.getEndpoint().getApiUrl(announceId);
		return this.postVoidAsync(url, entity);
	}

	//endregion

	// region SyncConnector

	@Override
	public Block getLastBlock(final Node node) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_CHAIN_LAST_BLOCK);
		return this.postAuthenticated(url, node.getIdentity(), obj -> BlockFactory.VERIFIABLE.deserialize(obj)).join();
	}

	@Override
	public Block getBlockAt(final Node node, final BlockHeight height) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_BLOCK_AT);
		return this.postAuthenticated(url, node.getIdentity(), obj -> BlockFactory.VERIFIABLE.deserialize(obj), height).join();
	}

	@Override
	public Collection<Block> getChainAfter(final Node node, final BlockHeight height) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_CHAIN_BLOCKS_AFTER);
		return this.postAuthenticated(
				url,
				node.getIdentity(),
				d -> new SerializableList<>(d, BlockFactory.VERIFIABLE),
				height).join().asCollection();
	}

	@Override
	public HashChain getHashesFrom(final Node node, final BlockHeight height) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_CHAIN_HASHES_FROM);
		return this.postAuthenticated(url, node.getIdentity(), obj -> new HashChain(obj), height).join();
	}

	@Override
	public BlockChainScore getChainScore(final Node node) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_CHAIN_SCORE);
		return this.postAuthenticated(url, node.getIdentity(), obj -> new BlockChainScore(obj)).join();
	}

	@Override
	public Collection<Transaction> getUnconfirmedTransactions(final Node node) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_TRANSACTIONS_UNCONFIRMED);
		return this.postAuthenticated(
				url,
				node.getIdentity(),
				d -> new SerializableList<>(d, TransactionFactory.VERIFIABLE)).join().asCollection();
	}

	//endregion

	// region TimeSyncConnector

	public CompletableFuture<CommunicationTimeStamps> getCommunicationTimeStamps(final Node node) {
		final URL url = node.getEndpoint().getApiUrl(NodeApiId.REST_TIME_SYNC_TIME_STAMPS);
		return this.postAuthenticated(url, node.getIdentity(), obj -> new CommunicationTimeStamps(obj));
	}

	//endregion

	private CompletableFuture<Deserializer> post(final URL url, final SerializableEntity entity) {
		return this.communicator.post(url, entity);
	}

	private CompletableFuture<Deserializer> postVoidAsync(final URL url, final SerializableEntity entity) {
		return this.communicator.post(url, entity);
	}

	private <T extends SerializableEntity> CompletableFuture<T> postAuthenticated(
			final URL url,
			final NodeIdentity remoteNodeIdentity,
			final ObjectDeserializer<T> entityDeserializer) {
		final NodeChallenge challenge = this.challengeFactory.next();
		return unwrapAuthenticatedResponse(this.post(url, challenge), challenge, remoteNodeIdentity, entityDeserializer);
	}

	private <TOut extends SerializableEntity> CompletableFuture<TOut> postAuthenticated(
			final URL url,
			final NodeIdentity remoteNodeIdentity,
			final ObjectDeserializer<TOut> entityDeserializer,
			final BlockHeight entity) {
		final NodeChallenge challenge = this.challengeFactory.next();
		final AuthenticatedRequest<?> request = new AuthenticatedRequest<>(entity, challenge);
		return unwrapAuthenticatedResponse(this.post(url, request), challenge, remoteNodeIdentity, entityDeserializer);
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
