package org.nem.nis.service;

import org.nem.core.connect.HttpMethodClient;
import org.nem.core.deploy.CommonStarter;
import org.nem.core.model.primitive.BlockChainScore;
import org.nem.core.node.Node;
import org.nem.core.serialization.DeserializationContext;
import org.nem.nis.BlockChain;
import org.nem.nis.audit.AuditCollection;
import org.nem.peer.connect.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * This class provides higher-level functions around accessing information about the NIS block chain of other nodes.
 *
 * TODO 20140909 J-B: what is the relation between this class and the one in NCC?
 *                    (i do like moving getMaxChainScoreAsync to NIS where i think it is more natural anyway)
 */
public class ChainServices {
	private static final int MAX_AUDIT_HISTORY_SIZE = 10;

	private final BlockChain blockChain;
	private final HttpConnector connector;

	/**
	 * Creates a new chain service instance.
	 *
	 * @param blockChain The block chain.
	 */
	public ChainServices(final BlockChain blockChain) {
		this.blockChain = blockChain;
		// TODO 20140905 BR: What is the correct way to get a HttpConnector object into this class?
		// TODO 20140909 J: You couldn't inject it with spring?
		final Communicator communicator = new HttpCommunicator(new HttpMethodClient<>(), CommunicationMode.JSON, new DeserializationContext(null));
		this.connector = new HttpConnector(new AuditedCommunicator(communicator, new AuditCollection(MAX_AUDIT_HISTORY_SIZE, CommonStarter.TIME_PROVIDER)));
	}

	/**
	 * Creates a new chain service instance.
	 *
	 * @param blockChain The block chain.
	 * @param connector The http connector.
	 */
	public ChainServices(final BlockChain blockChain, final HttpConnector connector) {
		this.blockChain = blockChain;
		this.connector = connector;
	}

	/**
	 * Gets a value indicating whether or not the local chain is synchronized with the network.
	 *
	 * @return true if the local chain is synchronized, false otherwise.
	 */
	public boolean isChainSynchronized(final Node node) {
		return blockChain.getScore().compareTo(getMaxChainScoreAsync(node).join()) >= 0;
	}

	/**
	 * Gets the maximum block chain score for the active neighbor peers of the specified NIS nodes.
	 *
	 * @param node The node.
	 * @return The maximum block chain score.
	 */
	private CompletableFuture<BlockChainScore> getMaxChainScoreAsync(final Node node) {
		return this.connector.getKnownPeers(node)
				.thenCompose(nodes -> this.getMaxChainScoreAsync(nodes.asCollection()));
	}

	/**
	 * Gets the maximum block chain score for the specified NIS nodes.
	 *
	 * @param nodes The nodes.
	 * @return The maximum block chain score.
	 */
	private CompletableFuture<BlockChainScore> getMaxChainScoreAsync(final Collection<Node> nodes) {
		final List<CompletableFuture<BlockChainScore>> chainScoreFutures = nodes.stream()
				.map(n -> this.connector.getChainScoreAsync(n).exceptionally(e -> null))
				.collect(Collectors.toList());

		return CompletableFuture.allOf(chainScoreFutures.toArray(new CompletableFuture[chainScoreFutures.size()]))
				.thenApply(v -> {
					final Optional<BlockChainScore> maxChainScore = chainScoreFutures.stream()
							.map(CompletableFuture::join)
							.filter(cs -> null != cs)
							.max(BlockChainScore::compareTo);

					return maxChainScore.isPresent() ? maxChainScore.get() : BlockChainScore.ZERO;
				});
	}
}
