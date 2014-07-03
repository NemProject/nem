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
	public ValidationResult pushTransaction(final Transaction entity, final NodeIdentity identity) {
		final ValidationResult result = this.pushEntity(
				entity,
				obj -> PushService.checkTransaction(obj),
				obj -> this.foraging.processTransaction(obj),
				transaction -> { },
				NodeApiId.REST_PUSH_TRANSACTION,
				identity);

		if (result.isFailure())
			LOGGER.info(String.format("Warning: ValidationResult=%s", result));

		return result;
	}

	private static ValidationResult checkTransaction(final Transaction transaction) {
		return !transaction.verify()
			? ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE
		    : transaction.checkValidity();
	}

	/**
	 * Pushes a block.
	 *
	 * @param entity The block.
	 * @param identity The identity of the pushing node.
	 */
	public void pushBlock(final Block entity, final NodeIdentity identity) {
		final ValidationResult result = this.pushEntity(
				entity,
				obj -> this.blockChain.checkPushedBlock(obj),
				obj -> this.blockChain.processBlock(obj),
				block -> LOGGER.info("   block height: " + entity.getHeight()),
				NodeApiId.REST_PUSH_BLOCK,
				identity);

		if (result.isFailure())
			LOGGER.info(String.format("Warning: ValidationResult=%s", result));
	}

	private <T extends VerifiableEntity & SerializableEntity> ValidationResult pushEntity(
			final T entity,
			final Function<T, ValidationResult> isValid,
			final Function<T, ValidationResult> isAccepted,
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

		final ValidationResult isValidResult = isValid.apply(entity);
		if (isValidResult.isFailure()) {
			// Bad experience with the remote node.
			updateStatus.accept(NodeInteractionResult.FAILURE);
			return isValidResult;
		}

		// validate entity and broadcast (async)
		final ValidationResult status = isAccepted.apply(entity);
		// Good or bad experience with the remote node.
		updateStatus.accept(NodeInteractionResult.fromValidationResult(status));

		if (status.isSuccess()) {
			final SecureSerializableEntity<T> secureEntity = new SecureSerializableEntity<>(
					entity,
					this.host.getNetwork().getLocalNode().getIdentity());
			network.broadcast(broadcastId, secureEntity);
		}

		return status;
	}
}
