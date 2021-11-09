package org.nem.nis.harvesting;

import org.nem.core.model.Block;
import org.nem.core.node.NisPeerId;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockChain;
import org.nem.peer.*;

/**
 * A harvesting task.
 */
public class HarvestingTask {
	private final BlockChain blockChain;
	private final Harvester harvester;
	private final UnconfirmedTransactions unconfirmedTransactions;

	/**
	 * Creates a new task.
	 *
	 * @param blockChain The block chain.
	 * @param harvester The harvester.
	 * @param unconfirmedTransactions The unconfirmed transactions.
	 */
	public HarvestingTask(final BlockChain blockChain, final Harvester harvester, final UnconfirmedTransactions unconfirmedTransactions) {
		this.blockChain = blockChain;
		this.harvester = harvester;
		this.unconfirmedTransactions = unconfirmedTransactions;
	}

	/**
	 * Executes the harvesting task.
	 *
	 * @param network The network.
	 * @param currentTime The current time.
	 */
	public void harvest(final PeerNetwork network, final TimeInstant currentTime) {
		this.unconfirmedTransactions.dropExpiredTransactions(currentTime);
		final Block block = this.harvester.harvestBlock();
		if (null == block || !this.blockChain.processBlock(block).isSuccess()) {
			return;
		}

		final SecureSerializableEntity<?> secureBlock = new SecureSerializableEntity<>(block, network.getLocalNode().getIdentity());
		network.broadcast(NisPeerId.REST_PUSH_BLOCK, secureBlock);
	}
}
