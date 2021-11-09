package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.ReadOnlyMosaicEntry;
import org.nem.nis.validators.ValidationContext;

/**
 * A single transaction validator implementation that validates mosaic supply change transactions:<br>
 * - [mosaic] underlying mosaic definition must be known<br>
 * - [namespace] underlying namespace must be active at the context height<br>
 * - [mosaic] transaction signer must be the creator of the mosaic<br>
 * - [mosaic] quantity is mutable<br>
 * - [mosaic] the max quantity is not exceeded<br>
 * - [mosaic] only existing mosaics owned by the creator can be deleted
 */
public class MosaicSupplyChangeTransactionValidator implements TSingleTransactionValidator<MosaicSupplyChangeTransaction> {
	private final ReadOnlyNamespaceCache namespaceCache;

	/**
	 * Creates a new validator.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public MosaicSupplyChangeTransactionValidator(final ReadOnlyNamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public ValidationResult validate(final MosaicSupplyChangeTransaction transaction, final ValidationContext context) {
		final ReadOnlyMosaicEntry mosaicEntry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, transaction.getMosaicId());
		if (null == mosaicEntry) {
			return ValidationResult.FAILURE_MOSAIC_UNKNOWN;
		}

		final MosaicDefinition mosaicDefinition = mosaicEntry.getMosaicDefinition();
		if (!this.namespaceCache.isActive(mosaicDefinition.getId().getNamespaceId(), context.getBlockHeight())) {
			return ValidationResult.FAILURE_NAMESPACE_EXPIRED;
		}

		if (!mosaicDefinition.getCreator().equals(transaction.getSigner())) {
			return ValidationResult.FAILURE_MOSAIC_CREATOR_CONFLICT;
		}

		final MosaicProperties properties = mosaicDefinition.getProperties();
		if (!properties.isSupplyMutable()) {
			return ValidationResult.FAILURE_MOSAIC_SUPPLY_IMMUTABLE;
		}

		return this.validateQuantityChange(mosaicEntry, transaction);
	}

	private ValidationResult validateQuantityChange(final ReadOnlyMosaicEntry mosaicEntry,
			final MosaicSupplyChangeTransaction transaction) {
		final int divisibility = mosaicEntry.getMosaicDefinition().getProperties().getDivisibility();
		final Supply existingSupply = mosaicEntry.getSupply();
		final Supply delta = transaction.getDelta();
		switch (transaction.getSupplyType()) {
			case Create:
				if (null == MosaicUtils.tryAdd(divisibility, existingSupply, delta)) {
					return ValidationResult.FAILURE_MOSAIC_MAX_SUPPLY_EXCEEDED;
				}
				break;

			case Delete:
				final Quantity existingBalance = mosaicEntry.getBalances().getBalance(transaction.getSigner().getAddress());
				final Supply existingBalanceAsSupply = MosaicUtils.toSupply(existingBalance, divisibility);
				if (existingBalanceAsSupply.compareTo(delta) < 0) {
					return ValidationResult.FAILURE_MOSAIC_SUPPLY_NEGATIVE;
				}
				break;
			default :
				break;
		}

		return ValidationResult.SUCCESS;
	}
}
