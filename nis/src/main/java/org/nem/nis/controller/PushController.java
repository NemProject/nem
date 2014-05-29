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
import org.nem.peer.node.NodeEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.function.*;
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
	public void pushTransaction(@RequestBody final Deserializer deserializer, final HttpServletRequest request) {
		boolean result = this.pushEntity(
				TransactionFactory.VERIFIABLE.deserialize(deserializer),
				transaction -> transaction.isValid() && transaction.verify(),
				this.foraging::processTransaction,
				NodeApiId.REST_PUSH_TRANSACTION,
				request);

		if (!result)
			throw new IllegalArgumentException("transfer must be valid and verifiable");
	}

	@RequestMapping(value = "/push/block", method = RequestMethod.POST)
	@P2PApi
	public void pushBlock(@RequestBody final Deserializer deserializer, final HttpServletRequest request) {
		boolean result = this.pushEntity(
				BlockFactory.VERIFIABLE.deserialize(deserializer),
				block -> this.blockChain.isNextBlock(block) && block.verify(),
				this.blockChain::processBlock,
				NodeApiId.REST_PUSH_BLOCK,
				request);

		if (!result)
			throw new IllegalArgumentException("block must be verifiable");
	}

	private <T extends VerifiableEntity> boolean pushEntity(
			final T entity,
			final Predicate<T> isValid,
			final Function<T, NodeInteractionResult> isAccepted,
			final NodeApiId broadcastId,
			final HttpServletRequest request) {
		LOGGER.info(String.format("   received: %s from %s", entity.getType(), request.getRemoteAddr()));
		LOGGER.info("   signer: " + entity.getSigner().getKeyPair().getPublicKey());
		LOGGER.info("   verify: " + Boolean.toString(entity.verify()));

		final PeerNetwork network = this.host.getNetwork();
		Node remoteNode = network.getNodes().getNode(request.getRemoteAddr());
		if (null == remoteNode)
			remoteNode = Node.fromHost(request.getRemoteAddr());

		if (!isValid.test(entity)) {
			// Bad experience with the remote node.
			network.updateExperience(remoteNode, NodeInteractionResult.FAILURE);
			return false;
		}

		// validate block and broadcast (async)
		final NodeInteractionResult status = isAccepted.apply(entity);
		if (NodeInteractionResult.NEUTRAL != status) {
			// Good experience with the remote node.
			network.updateExperience(remoteNode, status);
		}

		if (status == NodeInteractionResult.SUCCESS) {
			network.broadcast(broadcastId, entity);
		}

		return true;
	}
}
