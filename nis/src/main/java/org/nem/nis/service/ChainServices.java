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
 */
public class ChainServices {
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final HttpConnectorPool connectorPool;

	/**
	 * Creates a new chain service instance.
	 *
	 * @param blockChainLastBlockLayer The block chain last block layer.
	 * @param connectorPool The factory of http connectors.
	 */
	@Autowired(required = true)
	public ChainServices(final BlockChainLastBlockLayer blockChainLastBlockLayer, final HttpConnectorPool connectorPool) {
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.connectorPool = connectorPool;
	}

	/**
	 * Gets a value indicating whether or not the local chain is synchronized with respect to the specified nodes.
	 *
	 * @param nodes The nodes.
	 * @return true if the local chain is synchronized, false otherwise.
	 */
	public CompletableFuture<Boolean> isChainSynchronized(final Collection<Node> nodes) {
		return this.getMaxChainHeightAsync(nodes)
				.thenApply(maxHeight -> {
					final BlockHeight localBlockHeight = this.blockChainLastBlockLayer.getLastBlockHeight();
					return localBlockHeight.compareTo(maxHeight) >= 0;
				});
	}

	/**
	 * Gets the maximum block chain height for the specified NIS nodes.
	 *
	 * @param nodes The nodes.
	 * @return The maximum block chain height.
	 */
	public CompletableFuture<BlockHeight> getMaxChainHeightAsync(final Collection<Node> nodes) {
		final List<CompletableFuture<BlockHeight>> chainHeightFutures = nodes.stream()
				.map(n -> this.connectorPool.getSyncConnector(null).getChainHeightAsync(n).exceptionally(e -> null))
				.collect(Collectors.toList());

		return CompletableFuture.allOf(chainHeightFutures.toArray(new CompletableFuture[chainHeightFutures.size()]))
				.thenApply(v -> {
					final Optional<BlockHeight> maxChainHeight = chainHeightFutures.stream()
							.map(cf -> cf.join())
							.filter(h -> null != h)
							.max(BlockHeight::compareTo);

					return maxChainHeight.isPresent() ? maxChainHeight.get() : BlockHeight.ONE;
				});
	}
}
