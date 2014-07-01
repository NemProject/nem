package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.nis.*;
import org.nem.nis.mappers.ValidationResultToNodeInteractionResultMapper;
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
		ValidationResult result = this.pushEntity(
				entity,
				obj -> PushService.checkTransaction(obj),
				obj -> this.foraging.processTransaction(obj),
				transaction -> { },
				NodeApiId.REST_PUSH_TRANSACTION,
				identity);

		if (ValidationResult.SUCCESS != result &&
			ValidationResult.NEUTRAL != result)
			LOGGER.info("transfer must be valid and verifiable");

		return result;
	}

	private static ValidationResult checkTransaction(final Transaction transaction) {
		if (false == transaction.verify()) {
			return ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE;
		}
		return transaction.checkValidity();
	}

	/**
	 * Pushes a block.
	 *
	 * @param entity The block.
	 * @param identity The identity of the pushing node.
	 */
	public void pushBlock(final Block entity, final NodeIdentity identity) {
		ValidationResult result = this.pushEntity(
				entity,
				obj -> this.blockChain.checkPushedBlock(obj),
				obj -> this.blockChain.processBlock(obj),
				block -> LOGGER.info("   block height: " + entity.getHeight()),
				NodeApiId.REST_PUSH_BLOCK,
				identity);

		if (ValidationResult.SUCCESS != result &&
			ValidationResult.NEUTRAL != result)
			LOGGER.info("block must be valid and verifiable");
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

		final ValidationResult isValidStatus = isValid.apply(entity);
		if (isValidStatus != ValidationResult.SUCCESS &&
			isValidStatus != ValidationResult.NEUTRAL) {
			// Bad experience with the remote node.
			updateStatus.accept(NodeInteractionResult.FAILURE);
			return isValidStatus;
		}

		// validate entity and broadcast (async)
		final ValidationResult status = isAccepted.apply(entity);
		// Good or bad experience with the remote node.
		updateStatus.accept(ValidationResultToNodeInteractionResultMapper.map(status));

		if (status == ValidationResult.SUCCESS) {
			final SecureSerializableEntity<T> secureEntity = new SecureSerializableEntity<>(
					entity,
					this.host.getNetwork().getLocalNode().getIdentity());
			network.broadcast(broadcastId, secureEntity);
		}

		return status;
	}
}
