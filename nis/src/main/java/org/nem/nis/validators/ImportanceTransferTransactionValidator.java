package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.poi.*;
import org.nem.nis.secret.BlockChainConstants;

/**
 * A TransferTransactionValidator implementation that applies to importance transfer transactions.
 */
public class ImportanceTransferTransactionValidator implements SingleTransactionValidator {
	private final PoiFacade poiFacade;
	private final Amount minHarvesterBalance;

	/**
	 * Creates a new validator.
	 *
	 * @param poiFacade The poi facade.
	 * @param minHarvesterBalance The minimum balance required for a harvester.
	 */
	public ImportanceTransferTransactionValidator(
			final PoiFacade poiFacade,
			final Amount minHarvesterBalance) {
		this.poiFacade = poiFacade;
		this.minHarvesterBalance = minHarvesterBalance;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (TransactionTypes.IMPORTANCE_TRANSFER != transaction.getType()) {
			return ValidationResult.SUCCESS;
		}

		return this.validate(context.getBlockHeight(), (ImportanceTransferTransaction)transaction, context.getDebitPredicate());
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
	private ValidationResult validate(final BlockHeight height, final ImportanceTransferTransaction transaction, final DebitPredicate predicate) {
		final RemoteLinks remoteLinks = this.poiFacade.findStateByAddress(transaction.getSigner().getAddress()).getRemoteLinks();
		if (isRemoteChangeWithinOneDay(remoteLinks, height)) {
			return ValidationResult.FAILURE_ENTITY_UNUSABLE;
		}

		switch (transaction.getMode()) {
			case Activate:
				if (!predicate.canDebit(transaction.getSigner(), this.minHarvesterBalance.add(transaction.getFee()))) {
					return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
				}

				// TODO 20141109 J-G: i guess you did this so that someone doesn't automatically set their real account as the remote?
				// TODO 20141110 G-J: my idea was as follows (we've talked a bit about it on Sun) remote account should be used
				// ONLY for remote harvesting, so we should probably block any incoming or outgoing transfers, additionally we
				// shouldn't allow setting an account that already have some balance on it.
				// but right now I'm not sure if it's a good idea or not, and I was thinking about dropping this whole branch.
				// 1. from user perspective it's not that important, as "remote private key" is not easily accessible.
				// 2. adding such checks will also add additional work, as we would have to add some unconfirmed transactions purging
				if (0 != transaction.getRemote().getBalance().compareTo(Amount.ZERO)) {
					return ValidationResult.FAILURE_DESTINATION_ACCOUNT_NOT_EMPTY;
				}

				// if a remote is already activated, it needs to be deactivated first
				return !isRemoteActivated(remoteLinks) ? ValidationResult.SUCCESS : ValidationResult.FAILURE_ENTITY_UNUSABLE;

			case Deactivate:
			default:
				// if a remote is already deactivated, it needs to be activated first
				return !isRemoteDeactivated(remoteLinks) ? ValidationResult.SUCCESS : ValidationResult.FAILURE_ENTITY_UNUSABLE;
		}
	}
}
