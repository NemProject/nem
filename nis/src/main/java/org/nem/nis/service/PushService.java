package org.nem.nis.service;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.node.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.time.*;
import org.nem.nis.*;
import org.nem.nis.boot.NisPeerNetworkHost;
import org.nem.nis.harvesting.UnconfirmedTransactions;
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
	private static final int BLOCK_CACHE_SECONDS = 6000;
	private static final int TX_CACHE_SECONDS = 1800;

	private final UnconfirmedTransactions unconfirmedTransactions;
	private final BlockChain blockChain;
	private final NisPeerNetworkHost host;
	private final HashCache transactionHashCache;
	private final HashCache blockHashCache;

	@Autowired(required = true)
	public PushService(
			final UnconfirmedTransactions unconfirmedTransactions,
			final BlockChain blockChain,
			final NisPeerNetworkHost host,
			final TimeProvider timeProvider) {
		this.unconfirmedTransactions = unconfirmedTransactions;
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
	 * @return The result of transaction validation
	 */
	public ValidationResult pushTransaction(final Transaction entity, final NodeIdentity identity) {
		if (!isSameNetwork(entity)) {
			// this will happen a lot in the beginning because testnet nodes still have mainnet nodes in the peer list.
			return ValidationResult.FAILURE_WRONG_NETWORK;
		}

		// not nice but can the transaction be rejected elsewhere?
		if (BlockMarkerConstants.MULTISIG_M_OF_N_FORK > this.blockChain.getHeight().getRaw()) {
			if (TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION == entity.getType() &&
				entity.getEntityVersion() != 1) {
				return ValidationResult.FAILURE_MULTISIG_V2_AGGREGATE_MODIFICATION_BEFORE_FORK;
			}

			if (TransactionTypes.MULTISIG == entity.getType()) {
				final Transaction innerTransaction = ((MultisigTransaction)entity).getOtherTransaction();
				if (TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION == innerTransaction.getType() &&
					innerTransaction.getEntityVersion() != 1) {
					return ValidationResult.FAILURE_MULTISIG_V2_AGGREGATE_MODIFICATION_BEFORE_FORK;
				}
			}
		}

		final PushContext<Transaction> context = new PushContext<>(entity, identity, NisPeerId.REST_PUSH_TRANSACTION);
		context.isAccepted = this.unconfirmedTransactions::addNew;
		return this.pushEntityWithCache(context, this.transactionHashCache);
	}

	/**
	 * Pushes a block.
	 *
	 * @param entity The block.
	 * @param identity The identity of the pushing node.
	 */
	public ValidationResult pushBlock(final Block entity, final NodeIdentity identity) {
		if (!isSameNetwork(entity)) {
			// this will happen a lot in the beginning because testnet nodes still have mainnet nodes in the peer list.
			return ValidationResult.FAILURE_WRONG_NETWORK;
		}

		final PushContext<Block> context = new PushContext<>(entity, identity, NisPeerId.REST_PUSH_BLOCK);
		context.isValid = this.blockChain::checkPushedBlock;
		context.isAccepted = this.blockChain::processBlock;
		context.logAdditionalInfo = block -> LOGGER.info("   block height: " + block.getHeight());
		return this.pushEntityWithCache(context, this.blockHashCache);
	}

	private static class PushContext<T> {
		public final T entity;
		public final NodeIdentity identity;
		public final NisPeerId broadcastId;
		public Function<T, ValidationResult> isValid;
		public Function<T, ValidationResult> isAccepted;
		public Consumer<T> logAdditionalInfo;

		public PushContext(final T entity, final NodeIdentity identity, final NisPeerId broadcastId) {
			this.entity = entity;
			this.identity = identity;
			this.broadcastId = broadcastId;

			this.isValid = e -> ValidationResult.SUCCESS;
			this.isAccepted = e -> ValidationResult.SUCCESS;
			this.logAdditionalInfo = e -> {};
		}
	}

	private <T extends VerifiableEntity & SerializableEntity> ValidationResult pushEntityWithCache(
			final PushContext<T> context,
			final HashCache hashCache) {
		final Hash hash = HashUtils.calculateHash(context.entity);
		final ValidationResult cachedResult = hashCache.getCachedResult(hash);
		if (null != cachedResult) {
			return cachedResult;
		}

		// cache immediately neutral result which gets updated below
		// the reason is that pushEntity() includes broadcast() and can take some time
		// meanwhile the same transaction could be processed many times
		hashCache.setCachedResult(hash, ValidationResult.NEUTRAL);

		final ValidationResult result = this.pushEntity(context);
		if (result.isFailure()) {
			LOGGER.info(String.format("Warning: ValidationResult=%s", result));
		}

		hashCache.updateCachedResult(hash, result);
		return result;
	}

	private <T extends VerifiableEntity & SerializableEntity> ValidationResult pushEntity(final PushContext<T> context) {
		final String message = String.format("   received: %s from %s  (signer: %s)",
				context.entity.getType(),
				context.identity,
				context.entity.getSigner().getAddress());
		LOGGER.info(message);
		context.logAdditionalInfo.accept(context.entity);

		final PeerNetwork network = this.host.getNetwork();
		final Node remoteNode = null == context.identity ? null : network.getNodes().findNodeByIdentity(context.identity);
		final Consumer<NodeInteractionResult> updateStatus = status -> {
			if (null != remoteNode) {
				network.updateExperience(remoteNode, status);
			}
		};

		final ValidationResult isValidResult = context.isValid.apply(context.entity);
		if (isValidResult.isFailure()) {
			if (ValidationResult.FAILURE_ENTITY_UNUSABLE_OUT_OF_SYNC != isValidResult) {
				// Bad experience with the remote node.
				updateStatus.accept(NodeInteractionResult.FAILURE);
			}

			return isValidResult;
		}

		// validate entity and broadcast (async)
		final ValidationResult status = context.isAccepted.apply(context.entity);
		// Good or bad experience with the remote node.
		updateStatus.accept(NodeInteractionResult.fromValidationResult(status));

		if (status.isSuccess()) {
			final SecureSerializableEntity<T> secureEntity = new SecureSerializableEntity<>(
					context.entity,
					this.host.getNetwork().getLocalNode().getIdentity());
			network.broadcast(context.broadcastId, secureEntity);
		}

		return status;
	}

	private static boolean isSameNetwork(final VerifiableEntity entity) {
		final byte ourNetworkVersion = NetworkInfos.getDefault().getVersion();
		final byte remoteNetworkVersion = (byte)(entity.getVersion() >>> 24);
		return ourNetworkVersion == remoteNetworkVersion;
	}

	private static class HashCache {
		private final HashMap<Hash, HashCacheValue> cache;
		private final TimeProvider timeProvider;
		private final int cacheSeconds;

		private HashCache(final TimeProvider timeProvider, final int cacheSeconds) {
			this.timeProvider = timeProvider;
			this.cacheSeconds = cacheSeconds;
			this.cache = new HashMap<>();
		}

		private ValidationResult getCachedResult(final Hash hash) {
			this.prune();
			final HashCacheValue cachedValue = this.cache.getOrDefault(hash, null);
			return null == cachedValue ? null : cachedValue.result;
		}

		private void setCachedResult(final Hash hash, final ValidationResult result) {
			this.updateCachedResult(hash, result);
		}

		private void updateCachedResult(final Hash hash, final ValidationResult result) {
			final HashCacheValue value = this.cache.getOrDefault(hash, new HashCacheValue());
			if (null == value.timeStamp) {
				value.timeStamp = this.timeProvider.getCurrentTime();
			}

			value.result = ValidationResult.SUCCESS == result ? ValidationResult.NEUTRAL : result;
			this.cache.put(hash, value);
		}

		private void prune() {
			final TimeInstant currentTime = this.timeProvider.getCurrentTime();
			final Iterator<Map.Entry<Hash, HashCacheValue>> iterator = this.cache.entrySet().iterator();
			while (iterator.hasNext()) {
				final Map.Entry<Hash, HashCacheValue> entry = iterator.next();
				if (entry.getValue().timeStamp.addSeconds(this.cacheSeconds).compareTo(currentTime) <= 0) {
					iterator.remove();
				}
			}
		}
	}

	private static class HashCacheValue {
		TimeInstant timeStamp;
		ValidationResult result;
	}
}
