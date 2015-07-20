package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.nis.NamespaceConstants;
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
 * TODO 20150720 J-J: temporarily disable!
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
//		final SmartTileBag bag = transaction.getSmartTileBag();
//		if (bag.isEmpty()) {
//			return ValidationResult.SUCCESS;
//		}
//
//		for (final SmartTile smartTile : bag.getSmartTiles()) {
//			final Mosaic mosaic = NamespaceCacheUtils.getMosaic(this.namespaceCache, smartTile.getMosaicId());
//			if (null == mosaic) {
//				return ValidationResult.FAILURE_MOSAIC_UNKNOWN;
//			}
//
//			final MosaicProperties properties = mosaic.getProperties();
//			if (!mosaic.getCreator().equals(transaction.getSigner()) &&
//				!mosaic.getCreator().equals(transaction.getRecipient()) &&
//				!properties.isTransferable()) {
//				return ValidationResult.FAILURE_MOSAIC_NOT_TRANSFERABLE;
//			}
//
//			long quantity;
//			try {
//				// TODO 20150714 BR -> all: should change getNumMicroNem to getRaw imo.
//				quantity = Math.multiplyExact(smartTile.getQuantity().getRaw(), transaction.getAmount().getNumMicroNem());
//			} catch (ArithmeticException e) {
//				return ValidationResult.FAILURE_MOSAIC_MAX_QUANTITY_EXCEEDED;
//			}
//
//			// TODO 20150715 J-B: i'm not sure if i follow this check
//			// TODO 20150715 BR -> J: see trello mosaic card.
//			final long oneMillion = 1_000_000L;
//			if ((quantity % oneMillion) != 0) {
//				return ValidationResult.FAILURE_MOSAIC_DIVISIBILITY_VIOLATED;
//			}
//
//			quantity /= oneMillion;
//			final ReadOnlyAccountState state = this.stateCache.findStateByAddress(transaction.getSigner().getAddress());
//			// TODO 20150715 J-B: so this validator can replace the balance validator for xem (assuming xem is packaged in a smart tile, which is not true for V1s)?
//			// TODO 20150716 BR -> J: in principle we could do that. But since we already got V1 transactions i would just leave it like it is.
//			// > Aside from that, this validation is not enough, see trello mosaic card.
//			if (smartTile.getMosaicId().equals(NamespaceConstants.MOSAIC_XEM.getId())) {
//				if (quantity > state.getAccountInfo().getBalance().getNumMicroNem()) {
//					return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
//				}
//			} else {
//				final SmartTile signerSmartTile = state.getSmartTileMap().get(smartTile.getMosaicId());
//				if (null == signerSmartTile || signerSmartTile.getQuantity().compareTo(smartTile.getQuantity()) < 0) {
//					return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
//				}
//			}
//		}

		return ValidationResult.SUCCESS;
	}
}
