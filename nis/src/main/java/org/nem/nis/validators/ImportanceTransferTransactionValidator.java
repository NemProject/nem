package org.nem.nis.validators;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.dbmodel.ImportanceTransfer;
import org.nem.nis.poi.*;
import org.nem.nis.secret.BlockChainConstants;

/**
 * A TransferTransactionValidator implementation that applies to importance transfer transactions.
 */
public class ImportanceTransferTransactionValidator implements TransactionValidator {
	private final PoiFacade poiFacade;

	/**
	 * Creates a new validator.
	 *
	 * @param poiFacade The poi facade.
	 */
	public ImportanceTransferTransactionValidator(final PoiFacade poiFacade) {
		this.poiFacade = poiFacade;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (TransactionTypes.IMPORTANCE_TRANSFER != transaction.getType()){
			return ValidationResult.SUCCESS;
		}

		return this.validate(context.getBlockHeight(), (ImportanceTransferTransaction)transaction)
				? ValidationResult.SUCCESS
				: ValidationResult.FAILURE_ENTITY_UNUSABLE;
	}

	private static boolean isRemoteActivated(final RemoteLinks remoteLinks) {
		return !remoteLinks.isEmpty() && ImportanceTransferTransaction.Mode.Activate.value() == remoteLinks.getCurrent().getMode();
	}

	private static boolean isRemoteDeactivated(final RemoteLinks remoteLinks) {
		return remoteLinks.isEmpty() || ImportanceTransferTransaction.Mode.Deactivate.value() == remoteLinks.getCurrent().getMode();
	}

	private static boolean isRemoteChangeWithinOneDay(final RemoteLinks remoteLinks, final BlockHeight height) {
		return !remoteLinks.isEmpty() && height.subtract(remoteLinks.getCurrent().getEffectiveHeight()) < BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	}

	// TODO 20140920 J-G: should we have more specific results?
	private boolean validate(final BlockHeight height, final ImportanceTransferTransaction transaction) {
		final RemoteLinks remoteLinks = this.poiFacade.findStateByAddress(transaction.getSigner().getAddress()).getRemoteLinks();
		if (isRemoteChangeWithinOneDay(remoteLinks, height)) {
			return false;
		}

		switch (transaction.getMode()) {
			case Activate:
				// if a remote is already activated, it needs to be deactivated first
				return !isRemoteActivated(remoteLinks);

			case Deactivate:
			default:
				// if a remote is already deactivated, it needs to be activated first
				return !isRemoteDeactivated(remoteLinks);
		}
	}

	//public static boolean canAccountForageAtHeight(final RemoteState rState, final BlockHeight height) {
	//	final ImportanceTransferTransaction.Mode mode = ImportanceTransferTransaction.Mode.fromValueOrDefault(rState.getDirection());
	//	switch (mode) {
	//		case Activate:
	//
	//	}
	//	boolean activate = (mode == ImportanceTransferTransaction.Mode.Activate);
	//	boolean deactivate = (mode == ImportanceTransferTransaction.Mode.Deactivate);
	//
	//	if (!activate && !deactivate) {
	//		throw new IllegalStateException("unhandled importance transfer mode");
	//	}
	//	long settingHeight = height.subtract(rState.getRemoteHeight());
	//
	//	// remote already activated, or already deactivated
	//	if (((rState.isOwner() && activate) || (!rState.isOwner() && deactivate)) && (settingHeight >= BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY)) {
	//		return false;
	//	}
	//
	//	// remote already activated, or already deactivated
	//	if (((rState.isOwner() && deactivate) || (!rState.isOwner() && activate)) && (settingHeight < BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY)) {
	//		return false;
	//	}
	//	return true;
	//}
//
//	private boolean couldSignerForage(final Block block) {
//		final PoiAccountState accountState = this.poiFacade.findStateByAddress(block.getSigner().getAddress());
//		if (accountState.hasRemoteState()) {
//			if (!canAccountForageAtHeight(accountState.getRemoteState(), block.getHeight())) {
//				return false;
//			}
//		}
//
//		return true;
//	}
//
//	if (!couldSignerForage(block)) {
//		return false;
//	}



//		if (transaction.getType() == TransactionTypes.IMPORTANCE_TRANSFER) {
//			if (!verifyImportanceTransfer(this.poiFacade, block.getHeight(), (ImportanceTransferTransaction)transaction))
//			{
//				return false;
//			}
//		}
}
