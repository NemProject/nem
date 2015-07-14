package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceConstants;
import org.nem.nis.cache.*;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.validators.ValidationContext;

/**
 * Single transaction validator that validates a bag of smart tiles. For all smart tiles in the bag:
 * - the mosaic id must be known.
 * - if the transaction signer is not the mosaic creator then the mosaic must have the property 'transferable'.
 * - the product of transaction amount and smart tile quantity should not exceed the mosaic quantity property.
 * - the transaction signer must have enough smart tiles.
 * - the smart tile portion transferred should not violate the divisibility property.
 */
public class SmartTileBagValidator implements TSingleTransactionValidator<TransferTransaction> {
	private static long ONE_MILLION = 1_000_000L;
	private final ReadOnlyAccountStateCache stateCache;
	private final ReadOnlyMosaicCache mosaicCache;

	/**
	 * Creates a new validator.
	 *
	 * @param stateCache The account state cache.
	 */
	public SmartTileBagValidator(final ReadOnlyAccountStateCache stateCache, final ReadOnlyMosaicCache mosaicCache) {
		this.stateCache = stateCache;
		this.mosaicCache = mosaicCache;
	}

	@Override
	public ValidationResult validate(final TransferTransaction transaction, final ValidationContext context) {
		final SmartTileBag bag = transaction.getSmartTileBag();
		if (bag.isEmpty()) {
			return ValidationResult.SUCCESS;
		}

		for (SmartTile smartTile : bag.getSmartTiles()) {
			final Mosaic mosaic = this.mosaicCache.get(smartTile.getMosaicId());
			if (null == mosaic) {
				return ValidationResult.FAILURE_MOSAIC_UNKNOWN;
			}

			final MosaicProperties properties = mosaic.getProperties();
			if (!mosaic.getCreator().equals(transaction.getSigner()) && !properties.isTransferable()) {
				return ValidationResult.FAILURE_MOSAIC_NOT_TRANSFERABLE;
			}

			long quantity;
			try {
				// TODO 20150714 BR -> all: should change getNumMicroNem to getRaw imo.
				quantity = Math.multiplyExact(smartTile.getQuantity().getRaw(), transaction.getAmount().getNumMicroNem());
			} catch (ArithmeticException e) {
				return ValidationResult.FAILURE_MOSAIC_MAX_QUANTITY_EXCEEDED;
			}

			if ((quantity % ONE_MILLION) != 0) {
				return ValidationResult.FAILURE_MOSAIC_DIVISIBILITY_VIOLATED;
			}

			quantity /= ONE_MILLION;
			if (quantity > properties.getQuantity()) {
				return ValidationResult.FAILURE_MOSAIC_MAX_QUANTITY_EXCEEDED;
			}

			final ReadOnlyAccountState state = this.stateCache.findStateByAddress(transaction.getSigner().getAddress());
			if (smartTile.getMosaicId().equals(new MosaicId(NamespaceConstants.NAMESPACE_ID_NEM, "xem")) &&
					quantity > state.getAccountInfo().getBalance().getNumMicroNem()) {
				return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
			} else {
				final SmartTile signerSmartTile = state.getSmartTileMap().get(smartTile.getMosaicId());
				if (null == signerSmartTile || signerSmartTile.getQuantity().compareTo(smartTile.getQuantity()) < 0) {
					return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
				}
			}
		}

		return ValidationResult.SUCCESS;
	}
}
