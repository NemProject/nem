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
	public ValidationResult validate(final Transaction transaction, final DebitPredicate predicate) {
		if (TransactionTypes.IMPORTANCE_TRANSFER != transaction.getType()){
			return ValidationResult.SUCCESS;
		}

		return this.validate((ImportanceTransferTransaction)transaction);
	}

	// TODO 20140920 J-G: should we have more specific results?
	private ValidationResult validate(final ImportanceTransferTransaction transaction) {
		final PoiAccountState state = this.poiFacade.findStateByAddress(transaction.getSigner().getAddress());
		switch (transaction.getMode()) {
			case Activate:
				return ValidationResult.SUCCESS;

			case Deactivate:
				return ValidationResult.SUCCESS;
		}

		return ValidationResult.FAILURE_ENTITY_UNUSABLE;

		//final int direction = transaction.getMode().value();

		//if (direction == ImportanceTransferTransaction.Mode.Activate.value()) {
		//	if (state.hasRemoteState() && state.getRemoteState().getDirection() == direction) {
		//		return false;
		//	}
		//	// means there is previous state, which was "Deactivate" (due to check above)
		//	if (state.hasRemoteState() && height.subtract(state.getRemoteState().getRemoteHeight()) < BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY) {
		//		return false;
		//	}
		//	return true;
		//} else if (direction == ImportanceTransferTransaction.Mode.Deactivate.value()) {
		//	if (!state.hasRemoteState() || state.getRemoteState().getDirection() == direction) {
		//		return false;
		//	}
		//	// means there is previous state which was "Activate"
		//	if (state.hasRemoteState() && height.subtract(state.getRemoteState().getRemoteHeight()) < BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY) {
		//		return false;
		//	}
		//	return true;
		//}
		//return false;
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
