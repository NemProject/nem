package org.nem.nis.websocket;

import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockChainScore;

import java.util.Collection;

public interface BlockListener {
	/**
	 * Publishes blocks added to the chain to BlockListener.
	 *
	 * @param peerChain Collection of blocks added.
	 * @param peerScore Score of a chain.
	 */
	void pushBlocks(final Collection<Block> peerChain, final BlockChainScore peerScore);
}
