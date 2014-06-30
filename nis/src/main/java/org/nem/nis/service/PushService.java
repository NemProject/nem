package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.nis.*;
import org.nem.peer.*;
import org.nem.peer.node.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
	 * @param identity The identity of the pushing node.
	 */
	public boolean pushTransaction(final Transaction entity, final NodeIdentity identity) {
		boolean result = this.pushEntity(
				entity,
				obj -> PushService.checkTransaction(obj),
				obj -> this.foraging.processTransaction(obj),
				transaction -> { },
				NodeApiId.REST_PUSH_TRANSACTION,
				identity);

		if (!result)
			throw new IllegalArgumentException("transfer must be valid and verifiable");

		// TODO: this function always returns true
		return true;
	}

	private static NodeInteractionResult checkTransaction(final Transaction transaction) {
		final boolean isValid = ValidationResult.SUCCESS == transaction.checkValidity() && transaction.verify();
		return isValid ? NodeInteractionResult.SUCCESS : NodeInteractionResult.FAILURE;
	}

	/**
	 * Pushes a block.
	 *
	 * @param entity The block.
	 * @param identity The identity of the pushing node.
	 */
	public void pushBlock(final Block entity, final NodeIdentity identity) {
		boolean result = this.pushEntity(
				entity,
				obj -> this.blockChain.checkPushedBlock(obj),
				obj -> this.blockChain.processBlock(obj),
				block -> LOGGER.info("   block height: " + entity.getHeight()),
				NodeApiId.REST_PUSH_BLOCK,
				identity);

		if (!result)
			throw new IllegalArgumentException("block must be verifiable");
	}

	private <T extends VerifiableEntity & SerializableEntity> boolean pushEntity(
			final T entity,
			final Function<T, NodeInteractionResult> isValid,
			final Function<T, NodeInteractionResult> isAccepted,
			final Consumer<T> logAdditionalInfo,
			final NodeApiId broadcastId,
			final NodeIdentity identity) {
		LOGGER.info(String.format("   received: %s from %s", entity.getType(), identity));
		LOGGER.info("   signer: " + entity.getSigner().getKeyPair().getPublicKey());
		LOGGER.info("   verify: " + Boolean.toString(entity.verify()));
		logAdditionalInfo.accept(entity);

		final PeerNetwork network = this.host.getNetwork();
		final Node remoteNode = null == identity ? null : network.getNodes().findNodeByIdentity(identity);
		final Consumer<NodeInteractionResult> updateStatus = status -> {
			if (null != remoteNode)
				network.updateExperience(remoteNode, status);
		};

		final NodeInteractionResult isValidStatus = isValid.apply(entity);
		if (isValidStatus == NodeInteractionResult.FAILURE) {
			// Bad experience with the remote node.
			updateStatus.accept(isValidStatus);
			return false;
		}

		// validate block and broadcast (async)
		final NodeInteractionResult status = isAccepted.apply(entity);
		// Good or bad experience with the remote node.
		updateStatus.accept(status);

		if (status == NodeInteractionResult.SUCCESS) {
			final SecureSerializableEntity<T> secureEntity = new SecureSerializableEntity<>(
					entity,
					this.host.getNetwork().getLocalNode().getIdentity());
			network.broadcast(broadcastId, secureEntity);
		}

		return true;
	}
}
