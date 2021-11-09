package org.nem.nis.connect;

import org.nem.core.crypto.HashChain;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.*;
import org.nem.core.serialization.*;
import org.nem.core.time.synchronization.CommunicationTimeStamps;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.time.synchronization.TimeSynchronizationConnector;
import org.nem.peer.connect.*;
import org.nem.peer.node.*;
import org.nem.peer.requests.*;
import org.nem.peer.trust.score.NodeExperiencesPair;

import java.net.URL;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * An HTTP-based PeerConnector and SyncConnector implementation.
 */
public class HttpConnector implements PeerConnector, SyncConnector, TimeSynchronizationConnector {

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

	// region PeerConnector

	@Override
	public CompletableFuture<Node> getInfo(final Node node) {
		final URL url = getUrl(node, NisPeerId.REST_NODE_INFO);
		return this.postAuthenticated(url, node.getIdentity(), Node::new);
	}

	@Override
	public CompletableFuture<SerializableList<Node>> getKnownPeers(final Node node) {
		final URL url = getUrl(node, NisPeerId.REST_NODE_PEER_LIST_ACTIVE);
		return this.postAuthenticated(url, node.getIdentity(), d -> new SerializableList<>(d, Node::new));
	}

	@Override
	public CompletableFuture<NodeEndpoint> getLocalNodeInfo(final Node node, final NodeEndpoint localEndpoint) {
		final URL url = getUrl(node, NisPeerId.REST_NODE_CAN_YOU_SEE_ME);
		return this.post(url, localEndpoint).thenApply(NodeEndpoint::new);
	}

	@Override
	public CompletableFuture<NodeExperiencesPair> getNodeExperiences(Node node) {
		final URL url = getUrl(node, NisPeerId.REST_NODE_EXPERIENCES);
		return this.postAuthenticated(url, node.getIdentity(), NodeExperiencesPair::new);
	}

	@Override
	public CompletableFuture<?> announce(final Node node, final NisPeerId announceId, final SerializableEntity entity) {
		final URL url = getUrl(node, announceId);
		return this.postVoidAsync(url, entity);
	}

	// endregion

	// region SyncConnector

	@Override
	public Block getLastBlock(final Node node) {
		final URL url = getUrl(node, NisPeerId.REST_CHAIN_LAST_BLOCK);
		return this.postAuthenticated(url, node.getIdentity(), BlockFactory.VERIFIABLE::deserialize).join();
	}

	@Override
	public Block getBlockAt(final Node node, final BlockHeight height) {
		final URL url = getUrl(node, NisPeerId.REST_BLOCK_AT);
		return this.postAuthenticated(url, node.getIdentity(), BlockFactory.VERIFIABLE::deserialize, height).join();
	}

	@Override
	public Collection<Block> getChainAfter(final Node node, final ChainRequest chainRequest) {
		final URL url = getUrl(node, NisPeerId.REST_CHAIN_BLOCKS_AFTER);
		return this.postAuthenticated(url, node.getIdentity(), d -> new SerializableList<>(d, BlockFactory.VERIFIABLE), chainRequest).join()
				.asCollection();
	}

	@Override
	public HashChain getHashesFrom(final Node node, final BlockHeight height) {
		final URL url = getUrl(node, NisPeerId.REST_CHAIN_HASHES_FROM);
		return this.postAuthenticated(url, node.getIdentity(), HashChain::new, height).join();
	}

	@Override
	public BlockChainScore getChainScore(final Node node) {
		final URL url = getUrl(node, NisPeerId.REST_CHAIN_SCORE);
		return this.postAuthenticated(url, node.getIdentity(), BlockChainScore::new).join();
	}

	@Override
	public Collection<Transaction> getUnconfirmedTransactions(final Node node,
			final UnconfirmedTransactionsRequest unconfirmedTransactionsRequest) {
		final URL url = getUrl(node, NisPeerId.REST_TRANSACTIONS_UNCONFIRMED);
		return this.postAuthenticated(url, node.getIdentity(), d -> new SerializableList<>(d, TransactionFactory.VERIFIABLE),
				unconfirmedTransactionsRequest).join().asCollection();
	}

	@Override
	public CompletableFuture<BlockHeight> getChainHeightAsync(final Node node) {
		final URL url = getUrl(node, NisPeerId.REST_CHAIN_HEIGHT);
		return this.postAuthenticated(url, node.getIdentity(), BlockHeight::new);
	}

	// endregion

	// region TimeSynchronizationConnector

	public CompletableFuture<CommunicationTimeStamps> getCommunicationTimeStamps(final Node node) {
		final URL url = getUrl(node, NisPeerId.REST_TIME_SYNC_NETWORK_TIME);
		return this.postAuthenticated(url, node.getIdentity(), CommunicationTimeStamps::new);
	}

	// endregion

	private static URL getUrl(final Node node, final NisPeerId id) {
		return ExceptionUtils.propagate(() -> new URL(node.getEndpoint().getBaseUrl(), id.toString()));
	}

	private CompletableFuture<Deserializer> post(final URL url, final SerializableEntity entity) {
		return this.communicator.post(url, entity);
	}

	private CompletableFuture<Deserializer> postVoidAsync(final URL url, final SerializableEntity entity) {
		return this.communicator.post(url, entity);
	}

	private <T extends SerializableEntity> CompletableFuture<T> postAuthenticated(final URL url, final NodeIdentity remoteNodeIdentity,
			final ObjectDeserializer<T> entityDeserializer) {
		final NodeChallenge challenge = this.challengeFactory.next();
		return unwrapAuthenticatedResponse(this.post(url, challenge), challenge, remoteNodeIdentity, entityDeserializer);
	}

	private <TOut extends SerializableEntity, TEntity extends SerializableEntity> CompletableFuture<TOut> postAuthenticated(final URL url,
			final NodeIdentity remoteNodeIdentity, final ObjectDeserializer<TOut> entityDeserializer, final TEntity entity) {
		final NodeChallenge challenge = this.challengeFactory.next();
		final AuthenticatedRequest<?> request = new AuthenticatedRequest<>(entity, challenge);
		return unwrapAuthenticatedResponse(this.post(url, request), challenge, remoteNodeIdentity, entityDeserializer);
	}

	private static <T extends SerializableEntity> CompletableFuture<T> unwrapAuthenticatedResponse(
			final CompletableFuture<Deserializer> future, final NodeChallenge challenge, final NodeIdentity remoteNodeIdentity,
			final ObjectDeserializer<T> entityDeserializer) {
		return future.thenApply(d -> new AuthenticatedResponse<>(d, entityDeserializer).getEntity(remoteNodeIdentity, challenge));
	}
}
