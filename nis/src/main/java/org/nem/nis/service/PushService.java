package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.nis.*;
import org.nem.peer.*;
import org.nem.peer.node.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.function.*;
import java.util.logging.Logger;

// TODO: add unit tests for this

/**
 * Spring service that provides functions for pushing entities
 */
@Service
public class PushService {
	private static final Logger LOGGER = Logger.getLogger(PushService.class.getName());

	private final Foraging foraging;
	private final BlockChain blockChain;
	private final NisPeerNetworkHost host;

	@Autowired(required = true)
	public PushService(
			final Foraging foraging,
			final BlockChain blockChain,
			final NisPeerNetworkHost host) {
		this.foraging = foraging;
		this.blockChain = blockChain;
		this.host = host;
	}

	/**
	 * Pushes a transaction.
	 *
	 * @param entity The transaction.
	 * @param request The servlet request.
	 */
	public void pushTransaction(final Transaction entity, final HttpServletRequest request) {
		boolean result = this.pushEntity(
				entity,
				transaction -> (transaction.isValid() && transaction.verify()) ? NodeInteractionResult.SUCCESS : NodeInteractionResult.FAILURE,
				this.foraging::processTransaction,
				NodeApiId.REST_PUSH_TRANSACTION,
				request);

		if (!result)
			throw new IllegalArgumentException("transfer must be valid and verifiable");
	}

	/**
	 * Pushes a block.
	 *
	 * @param entity The block.
	 * @param request The servlet request.
	 */
	public void pushBlock(final Block entity, final HttpServletRequest request) {
		boolean result = this.pushEntity(
				entity,
				this.blockChain::checkPushedBlock,
				this.blockChain::processBlock,
				NodeApiId.REST_PUSH_BLOCK,
				request);

		if (!result)
			throw new IllegalArgumentException("block must be verifiable");
	}

	private <T extends VerifiableEntity> boolean pushEntity(
			final T entity,
			final Function<T, NodeInteractionResult> isValid,
			final Function<T, NodeInteractionResult> isAccepted,
			final NodeApiId broadcastId,
			final HttpServletRequest request) {
		LOGGER.info(String.format("   received: %s from %s", entity.getType(), request.getRemoteAddr()));
		LOGGER.info("   signer: " + entity.getSigner().getKeyPair().getPublicKey());
		LOGGER.info("   verify: " + Boolean.toString(entity.verify()));

		final PeerNetwork network = this.host.getNetwork();
		Node remoteNode = network.getNodes().findNodeByEndpoint(NodeEndpoint.fromHost(request.getRemoteAddr()));
		if (null == remoteNode)
			remoteNode = Node.fromHost(request.getRemoteAddr());

		final NodeInteractionResult isValidStatus = isValid.apply(entity);
		if (isValidStatus != NodeInteractionResult.SUCCESS) {
			// Bad experience with the remote node.
			network.updateExperience(remoteNode, isValidStatus);
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
