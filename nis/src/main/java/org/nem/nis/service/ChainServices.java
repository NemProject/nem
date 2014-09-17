package org.nem.nis.service;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.Node;
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
 * TODO 20140917 BR -> J: switched from chain score to chain height.
 */
public class ChainServices {
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final HttpConnectorPool connectorPool;

	/**
	 * Creates a new chain service instance.
	 *
	 * @param blockChainLastBlockLayer The block chain last block layer.
	 */
	@Autowired(required = true)
	public ChainServices(final BlockChainLastBlockLayer blockChainLastBlockLayer, final HttpConnectorPool connectorPool) {
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.connectorPool = connectorPool;
	}

	/**
	 * Gets a value indicating whether or not the local chain is synchronized with the network.
	 *
	 * @return true if the local chain is synchronized, false otherwise.
	 */
	public boolean isChainSynchronized(final Node node) {
		final BlockHeight maxHeight = getMaxChainHeightAsync(node).join();
		return new BlockHeight(this.blockChainLastBlockLayer.getLastBlockHeight()).compareTo(maxHeight) >= 0;
	}

	/**
	 * Gets the maximum block chain height for the active neighbor peers of the specified NIS nodes.
	 *
	 * @param node The node.
	 * @return The maximum block chain height.
	 */
	public CompletableFuture<BlockHeight> getMaxChainHeightAsync(final Node node) {
		return this.connectorPool.getPeerConnector(null).getKnownPeers(node)
				.thenCompose(nodes -> this.getMaxChainHeightAsync(nodes.asCollection()));
	}

	/**
	 * Gets the maximum block chain height for the specified NIS nodes.
	 *
	 * @param nodes The nodes.
	 * @return The maximum block chain height.
	 */
	private CompletableFuture<BlockHeight> getMaxChainHeightAsync(final Collection<Node> nodes) {
		final List<CompletableFuture<BlockHeight>> chainHeightFutures = nodes.stream()
				.map(n -> this.connectorPool.getSyncConnector(null).getChainHeightAsync(n).exceptionally(e -> null))
				.collect(Collectors.toList());

		return CompletableFuture.allOf(chainHeightFutures.toArray(new CompletableFuture[chainHeightFutures.size()]))
				.thenApply(v -> {
					final Optional<BlockHeight> maxChainHeight = chainHeightFutures.stream()
							.map(CompletableFuture::join)
							.filter(h -> null != h)
							.max(BlockHeight::compareTo);

					return maxChainHeight.isPresent() ? maxChainHeight.get() : BlockHeight.ONE;
				});
	}
}
