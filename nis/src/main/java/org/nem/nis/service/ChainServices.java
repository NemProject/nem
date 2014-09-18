package org.nem.nis.service;

import org.nem.core.model.primitive.BlockChainScore;
import org.nem.core.node.Node;
import org.nem.nis.BlockChain;
import org.nem.peer.connect.HttpConnectorPool;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * This class provides higher-level functions around accessing information about the NIS block chain of other nodes.
 * TODO 20140909 J-B: what is the relation between this class and the one in NCC?
 * (i do like moving getMaxChainScoreAsync to NIS where i think it is more natural anyway)
 * TODO 20140910 BR -> J: isChainSynchronized only returns boolean, therefore no need to move blocks across the wire.
 * TODO 20140910          NCC needs more information because it estimates how far behind NIS is. Maybe we should have a
 * TODO 20140910          lastBlockHeight request which has the advantage of
 * TODO 20140910          1) not requesting complete blocks (which could possibly contain many transactions)
 * TODO 20140910          2) holding all the information needed by NCC and this class.
 * TODO 20140910          Not sure about you last comment, getMaxChainScoreAsync is in NIS, did you mean the NCC getMaxBlockHeightAsync?
 * TODO 20140910 i was wondering does it make sense for NIS to estimate how far behind it is
 */
public class ChainServices {
	private final BlockChain blockChain;
	private final HttpConnectorPool connectorPool;

	/**
	 * Creates a new chain service instance.
	 *
	 * @param blockChain The block chain.
	 */
	@Autowired(required = true)
	public ChainServices(final BlockChain blockChain, final HttpConnectorPool connectorPool) {
		this.blockChain = blockChain;
		this.connectorPool = connectorPool;
	}

	/**
	 * Gets a value indicating whether or not the local chain is synchronized with the network.
	 *
	 * @return true if the local chain is synchronized, false otherwise.
	 */
	public boolean isChainSynchronized(final Node node) {
		return this.blockChain.getScore().compareTo(getMaxChainScoreAsync(node).join()) >= 0;
	}

	/**
	 * Gets the maximum block chain score for the active neighbor peers of the specified NIS nodes.
	 *
	 * @param node The node.
	 * @return The maximum block chain score.
	 */
	private CompletableFuture<BlockChainScore> getMaxChainScoreAsync(final Node node) {
		return this.connectorPool.getPeerConnector(null).getKnownPeers(node)
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
				.map(n -> this.connectorPool.getSyncConnector(null).getChainScoreAsync(n).exceptionally(e -> null))
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
