package org.nem.nis.websocket;

import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockChainScore;

import java.util.Collection;

public interface BlockListener {
	void pushBlocks(final Collection<Block> peerChain, final BlockChainScore peerScore);
}
