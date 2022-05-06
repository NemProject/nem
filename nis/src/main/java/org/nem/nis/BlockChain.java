package org.nem.nis;

import org.nem.core.connect.FatalPeerException;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.Node;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.websocket.BlockListener;
import org.nem.nis.sync.BlockChainUpdater;
import org.nem.peer.*;
import org.nem.peer.connect.SyncConnectorPool;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.logging.Logger;

public class BlockChain implements BlockSynchronizer {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());

	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final BlockChainUpdater updater;

	@Autowired(required = true)
	public BlockChain(final BlockChainLastBlockLayer blockChainLastBlockLayer, final BlockChainUpdater updater) {
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.updater = updater;
	}

	/**
	 * Gets the current height of the block chain.
	 *
	 * @return The block chain height.
	 */
	public BlockHeight getHeight() {
		return this.blockChainLastBlockLayer.getLastBlockHeight();
	}

	/**
	 * Checks if given block follows last block in the chain.
	 *
	 * @param block The block to check.
	 * @return true if block can be next in chain.
	 */
	private boolean isLastBlockParent(final Block block) {
		final DbBlock lastDbBlock = this.blockChainLastBlockLayer.getLastDbBlock();
		final boolean result = lastDbBlock.getBlockHash().equals(block.getPreviousBlockHash());
		LOGGER.info(String.format("isLastBlockParent? %s; last block height: %s; hash: %s", result, lastDbBlock.getHeight(),
				lastDbBlock.getBlockHash()));
		return result;
	}

	/**
	 * Checks if given block has the same parent as last block in the chain.
	 *
	 * @param block The block to check.
	 * @return true if block is a sibling of the last block in the chain.
	 */
	private boolean isLastBlockSibling(final Block block) {
		final boolean result;
		// it's better to base it on hash of previous block instead of height
		result = this.blockChainLastBlockLayer.getLastDbBlock().getPrevBlockHash().equals(block.getPreviousBlockHash());
		return result;
	}

	/**
	 * Checks a block that was received by a peer.
	 *
	 * @param block The block.
	 * @return An appropriate interaction result.
	 */
	public ValidationResult checkPushedBlock(final Block block) {
		if (!this.isLastBlockParent(block)) {
			// if peer tried to send us block that we also generated, there is no sense to punish him
			return this.isLastBlockSibling(block) ? ValidationResult.NEUTRAL : ValidationResult.FAILURE_ENTITY_UNUSABLE_OUT_OF_SYNC;
		}

		// the peer returned a block that can be added to our chain
		return block.verify() ? ValidationResult.SUCCESS : ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE;
	}

	/**
	 * Synch algorithm:<br>
	 * 1. Get peer's last block compare with ours, assuming it's ok<br>
	 * 2. Take hashes of last blocks - at most DEFAULT_REWRITE_LIMIT hashes, compare with proper hashes of peer, to find last common and
	 * first different block. If all peer's hashes has been checked we have nothing to do<br>
	 * 3. if we have some blocks left AFTER common blocks, we'll need to revert those transactions, but before that we'll do some simple
	 * check, to see if peer's chain is actually better<br>
	 * 4. Now we can get peer's chain and verify it<br>
	 * 5. Once we've verified it, we can apply it (all-or-nothing policy, if verification failed, we won't try to apply part of it)
	 *
	 * @param connectorPool The sync connector pool.
	 * @param node The other node.
	 */
	@Override
	public NodeInteractionResult synchronizeNode(final SyncConnectorPool connectorPool, final Node node) {
		try {
			return this.synchronizeNodeInternal(connectorPool, node);
		} catch (final FatalPeerException ex) {
			LOGGER.info(String.format("failed to synchronize with %s: %s", node, ex));
			return NodeInteractionResult.FAILURE;
		}
	}

	private NodeInteractionResult synchronizeNodeInternal(final SyncConnectorPool connectorPool, final Node node) {
		return this.updater.updateChain(connectorPool, node);
	}

	/**
	 * Checks if passed receivedBlock is correct, and if eligible adds it to db
	 *
	 * @param receivedBlock - receivedBlock that's going to be processed
	 * @return Node experience code which indicates the status of the operation
	 */
	public ValidationResult processBlock(final Block receivedBlock) {
		return this.updater.updateBlock(receivedBlock);
	}

	/**
	 * Adds a block listener to the block updater mechanism. Listener will be informed about blocks that were successfully added to the
	 * chain.
	 *
	 * @param blockListener The block listener.
	 */
	public void addListener(final BlockListener blockListener) {
		this.updater.addListener(blockListener);
	}
}
