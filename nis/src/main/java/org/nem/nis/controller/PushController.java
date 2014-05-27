package org.nem.nis.controller;

import org.nem.core.model.*;
import org.nem.core.serialization.Deserializer;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.BlockChain;
import org.nem.nis.Foraging;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.annotations.P2PApi;
import org.nem.peer.*;
import org.nem.peer.node.Node;
import org.nem.peer.node.NodeApiId;
import org.nem.peer.trust.score.NodeExperience;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * This controller will handle data propagation:
 * * /push/transaction - for what is now model.Transaction
 * * /push/block - for model.Block
 * <p/>
 * It would probably fit better in TransferController, but this is
 * part of p2p API, so I think it should be kept separated.
 * (I think it might pay off in future, if we'd like to add restrictions to client APIs)
 */

// TODO: add tests
@RestController
public class PushController {
	private static final Logger LOGGER = Logger.getLogger(PushController.class.getName());

	private final AccountAnalyzer accountAnalyzer;
	private final Foraging foraging;
	private final BlockChain blockChain;
	private final NisPeerNetworkHost host;

	@Autowired(required = true)
	PushController(
			final AccountAnalyzer accountAnalyzer,
			final Foraging foraging,
			final BlockChain blockChain,
			final NisPeerNetworkHost host) {
		this.accountAnalyzer = accountAnalyzer;
		this.foraging = foraging;
		this.blockChain = blockChain;
		this.host = host;
	}

	@RequestMapping(value = "/push/transaction", method = RequestMethod.POST)
	@P2PApi
	public void pushTransaction(@RequestBody final Deserializer deserializer, HttpServletRequest request) {
		final Transaction transaction = TransactionFactory.VERIFIABLE.deserialize(deserializer);

		LOGGER.info("   signer: " + transaction.getSigner().getKeyPair().getPublicKey());
		LOGGER.info("   verify: " + Boolean.toString(transaction.verify()));
		LOGGER.info(request.getRemoteAddr());
		Node remoteNode = host.getNetwork().getNodes().getNode(request.getRemoteAddr());

		// transaction timestamp is checked inside processTransaction
		if (!transaction.isValid() || !transaction.verify()) {
			// Bad experience with the remote node.
			if (remoteNode != null) {
				host.getNetwork().updateExperience(remoteNode, NodeExperience.Code.FAILURE);
			}
			
			throw new IllegalArgumentException("transfer must be valid and verifiable");
		}

		final PeerNetwork network = this.host.getNetwork();

		// add to unconfirmed transactions
		if (this.foraging.processTransaction(transaction)) {
			// Good experience with the remote node.
			if (remoteNode != null) {
				host.getNetwork().updateExperience(remoteNode, NodeExperience.Code.SUCCESS);
			}
			
			// propagate transactions
			// this returns immediately, so that client who
			// actually has sent /transfer/announce won't wait for this...
			network.broadcast(NodeApiId.REST_PUSH_TRANSACTION, transaction);
		}
	}

	@RequestMapping(value = "/push/block", method = RequestMethod.POST)
	@P2PApi
	public void pushBlock(@RequestBody final Deserializer deserializer, HttpServletRequest request) {
		final Block block = BlockFactory.VERIFIABLE.deserialize(deserializer);

		// TODO: refactor logging
		LOGGER.info("   signer: " + block.getSigner().getKeyPair().getPublicKey());
		LOGGER.info("   verify: " + Boolean.toString(block.verify()));
		LOGGER.info(request.getRemoteAddr());
		Node remoteNode = host.getNetwork().getNodes().getNode(request.getRemoteAddr());
		// TODO: if the remote node is null, do we want to create a new node? I guess yes.

		if (!block.verify()) {
			// Bad experience with the remote node.
			if (remoteNode != null) {
				host.getNetwork().updateExperience(remoteNode, NodeExperience.Code.FAILURE);
			}
			
			throw new IllegalArgumentException("block must be verifiable");
		}

		// validate block and broadcast (async)
		if (this.blockChain.processBlock(block)) {
			// Good experience with the remote node.
			if (remoteNode != null) {
				host.getNetwork().updateExperience(remoteNode, NodeExperience.Code.SUCCESS);
			}
			
			this.host.getNetwork().broadcast(NodeApiId.REST_PUSH_BLOCK, block);
		}
	}
}
