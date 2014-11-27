package org.nem.nis.service;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.node.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.time.*;
import org.nem.nis.*;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.validators.SingleTransactionValidator;
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
	private static final int BLOCK_CACHE_SECONDS = 600;
	private static final int TX_CACHE_SECONDS = 10;

	private final UnconfirmedTransactions unconfirmedTransactions;
	private final SingleTransactionValidator validator;
	private final BlockChain blockChain;
	private final NisPeerNetworkHost host;
	private final HashCache transactionHashCache;
	private final HashCache blockHashCache;

	@Autowired(required = true)
	public PushService(
			final UnconfirmedTransactions unconfirmedTransactions,
			final SingleTransactionValidator validator,
			final BlockChain blockChain,
			final NisPeerNetworkHost host,
			final TimeProvider timeProvider) {
		this.unconfirmedTransactions = unconfirmedTransactions;
		this.validator = validator;
		this.blockChain = blockChain;
		this.host = host;
		this.transactionHashCache = new HashCache(timeProvider, TX_CACHE_SECONDS);
		this.blockHashCache = new HashCache(timeProvider, BLOCK_CACHE_SECONDS);
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

		final ValidationResult result = this.pushEntity(
				entity,
				transaction -> this.checkTransaction(transaction),
				transaction -> this.unconfirmedTransactions.addNew(transaction),
				transaction -> {},
				NisPeerId.REST_PUSH_TRANSACTION,
				identity);

		if (result.isFailure()) {
			this.transactionHashCache.remove(hash);
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
		final Hash hash = HashUtils.calculateHash(entity);
		if (this.blockHashCache.isKnown(hash)) {
			return;
		}
		final ValidationResult result = this.pushEntity(
				entity,
				obj -> this.blockChain.checkPushedBlock(obj),
				obj -> this.blockChain.processBlock(obj),
				block -> LOGGER.info("   block height: " + entity.getHeight()),
				NisPeerId.REST_PUSH_BLOCK,
				identity);

		if (result.isFailure()) {
			this.blockHashCache.remove(hash);
			LOGGER.info(String.format("Warning: ValidationResult=%s", result));
		}
	}

	private <T extends VerifiableEntity & SerializableEntity> ValidationResult pushEntity(
			final T entity,
			final Function<T, ValidationResult> isValid,
			final Function<T, ValidationResult> isAccepted,
			final Consumer<T> logAdditionalInfo,
			final NisPeerId broadcastId,
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

	private static class HashCache {
		private final HashMap<Hash, TimeInstant> cache;
		private final TimeProvider timeProvider;
		private final int cacheSeconds;

		private HashCache(final TimeProvider timeProvider, final int cacheSeconds) {
			this.timeProvider = timeProvider;
			this.cacheSeconds = cacheSeconds;
			this.cache = new HashMap<>();
		}

		private boolean isKnown(final Hash hash) {
			this.prune();
			if (this.cache.containsKey(hash)) {
				return true;
			}

			this.cache.putIfAbsent(hash, this.timeProvider.getCurrentTime());
			return false;
		}

		private void remove(final Hash hash) {
			this.cache.remove(hash);
		}

		private void prune() {
			final TimeInstant currentTime = this.timeProvider.getCurrentTime();
			final Iterator<Map.Entry<Hash, TimeInstant>> iterator = this.cache.entrySet().iterator();
			while (iterator.hasNext()) {
				final Map.Entry<Hash, TimeInstant> entry = iterator.next();
				if (entry.getValue().addSeconds(this.cacheSeconds).compareTo(currentTime) <= 0) {
					iterator.remove();
				}
			}
		}
	}
}
