package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
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
	private final ReadOnlyAccountStateCache stateCache;
	private final ReadOnlyNamespaceCache namespaceCache;

	/**
	 * Creates a new validator.
	 *
	 * @param stateCache The account state cache.
	 * @param namespaceCache The namespace cache.
	 */
	public SmartTileBagValidator(final ReadOnlyAccountStateCache stateCache, final ReadOnlyNamespaceCache namespaceCache) {
		this.stateCache = stateCache;
		this.namespaceCache = namespaceCache;
	}

	@Override
	public ValidationResult validate(final TransferTransaction transaction, final ValidationContext context) {
		final SmartTileBag bag = transaction.getSmartTileBag();
		if (bag.isEmpty()) {
			return ValidationResult.SUCCESS;
		}

		for (final SmartTile smartTile : bag.getSmartTiles()) {
			final ReadOnlyMosaicEntry mosaicEntry = this.getMosaicEntry(smartTile.getMosaicId());
			if (null == mosaicEntry) {
				return ValidationResult.FAILURE_MOSAIC_UNKNOWN;
			}

			// TODO 20150715 J-B: can't hurt to check, but i don't think this will ever happen
			final Mosaic mosaic = mosaicEntry.getMosaic();
			if (null == mosaic) {
				return ValidationResult.FAILURE_MOSAIC_UNKNOWN;
			}

			// TODO 20150715 J-B: speaking with makoto seems we need to allow if creator is recipient too
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

			// TODO 20150715 J-B: i'm not sure if i follow this check
			final long oneMillion = 1_000_000L;
			if ((quantity % oneMillion) != 0) {
				return ValidationResult.FAILURE_MOSAIC_DIVISIBILITY_VIOLATED;
			}

			quantity /= oneMillion;
			final ReadOnlyAccountState state = this.stateCache.findStateByAddress(transaction.getSigner().getAddress());
			// TODO 20150715 J-B: so this validator can replace the balance validator for xem (assuming xem is pacakged in a smart tile, which is not true for V1s)?
			if (smartTile.getMosaicId().equals(NamespaceConstants.MOSAIC_XEM.getId())) {
				if (quantity > state.getAccountInfo().getBalance().getNumMicroNem()) {
					return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
				}
			} else {
				final SmartTile signerSmartTile = state.getSmartTileMap().get(smartTile.getMosaicId());
				if (null == signerSmartTile || signerSmartTile.getQuantity().compareTo(smartTile.getQuantity()) < 0) {
					return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
				}
			}
		}

		return ValidationResult.SUCCESS;
	}

	// TODO 20150715 J-B: probably can change to getMosaic
	private ReadOnlyMosaicEntry getMosaicEntry(final MosaicId mosaicId) {
		final ReadOnlyNamespaceEntry namespaceEntry = this.namespaceCache.get(mosaicId.getNamespaceId());
		if (null == namespaceEntry) {
			return null;
		}

		final ReadOnlyMosaics mosaics = namespaceEntry.getMosaics();
		if (null == mosaics) {
			return null;
		}

		return mosaics.get(mosaicId);
	}
}
