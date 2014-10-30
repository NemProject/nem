package org.nem.nis.service;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.node.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.time.*;
import org.nem.nis.*;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.validators.TransactionValidator;
import org.nem.peer.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.*;
import java.util.logging.Logger;

/**
 * Spring service that provides functions for pushing entities
 */
@Service
public class PushService {
	private static final Logger LOGGER = Logger.getLogger(PushService.class.getName());

	private final UnconfirmedTransactions unconfirmedTransactions;
	private final TransactionValidator validator;
	private final BlockChain blockChain;
	private final NisPeerNetworkHost host;
	private final TransactionHashCache transactionHashCache;

	@Autowired(required = true)
	public PushService(
			final UnconfirmedTransactions unconfirmedTransactions,
			final TransactionValidator validator,
			final BlockChain blockChain,
			final NisPeerNetworkHost host,
			final TimeProvider timeProvider) {
		this.unconfirmedTransactions = unconfirmedTransactions;
		this.validator = validator;
		this.blockChain = blockChain;
		this.host = host;
		this.transactionHashCache = new TransactionHashCache(timeProvider);
	}

	/**
	 * Pushes a transaction.
	 *
	 * @param entity The transaction.
	 * @param identity The identity of the pushing node.
	 */
	public ValidationResult pushTransaction(final Transaction entity, final NodeIdentity identity) {
		final Hash hash = HashUtils.calculateHash(entity);
		if (this.transactionHashCache.isKnown(hash)) {
			return ValidationResult.NEUTRAL;
		}

		this.transactionHashCache.add(hash);
		final ValidationResult result = this.pushEntity(
				entity,
				transaction -> this.checkTransaction(transaction),
				transaction -> this.unconfirmedTransactions.addNew(transaction),
				transaction -> {},
				NodeApiId.REST_PUSH_TRANSACTION,
				identity);

		if (result.isFailure()) {
			LOGGER.info(String.format("Warning: ValidationResult=%s", result));
		}

		return result;
	}

	private ValidationResult checkTransaction(final Transaction transaction) {
		return !transaction.verify()
				? ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE
				: this.validator.validate(transaction);
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

		if (result.isFailure()) {
			LOGGER.info(String.format("Warning: ValidationResult=%s", result));
		}
	}

	private <T extends VerifiableEntity & SerializableEntity> ValidationResult pushEntity(
			final T entity,
			final Function<T, ValidationResult> isValid,
			final Function<T, ValidationResult> isAccepted,
			final Consumer<T> logAdditionalInfo,
			final NodeApiId broadcastId,
			final NodeIdentity identity) {
		final String message = String.format("   received: %s from %s%s   signer: %s%sverify: %s",
				entity.getType(),
				identity,
				System.lineSeparator(),
				entity.getSigner().getKeyPair().getPublicKey(),
				System.lineSeparator(),
				entity.verify());
		LOGGER.info(message);
		logAdditionalInfo.accept(entity);

		final PeerNetwork network = this.host.getNetwork();
		final Node remoteNode = null == identity ? null : network.getNodes().findNodeByIdentity(identity);
		final Consumer<NodeInteractionResult> updateStatus = status -> {
			if (null != remoteNode) {
				network.updateExperience(remoteNode, status);
			}
		};

		final ValidationResult isValidResult = isValid.apply(entity);
		if (isValidResult.isFailure()) {
			if (ValidationResult.FAILURE_ENTITY_UNUSABLE != isValidResult) {
				// Bad experience with the remote node.
				updateStatus.accept(NodeInteractionResult.FAILURE);
			}

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

	private class TransactionHashCache {
		private final HashMap<Hash, TimeInstant> cache;
		private final TimeProvider timeProvider;

		private TransactionHashCache(final TimeProvider timeProvider) {
			this.timeProvider = timeProvider;
			this.cache = new HashMap<>();
		}

		private boolean isKnown(final Hash hash) {
			this.prune();
			return this.cache.containsKey(hash);
		}

		private void prune() {
			final TimeInstant currentTime = this.timeProvider.getCurrentTime();
			Iterator<Map.Entry<Hash, TimeInstant>> iterator = this.cache.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<Hash, TimeInstant> entry = iterator.next();
				if (entry.getValue().addSeconds(10).compareTo(currentTime) <= 0) {
					iterator.remove();
				}
			}
		}

		private void add(final Hash hash) {
			this.cache.putIfAbsent(hash, this.timeProvider.getCurrentTime());
		}
	}
}
