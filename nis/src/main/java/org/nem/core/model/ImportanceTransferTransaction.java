package org.nem.core.model;

import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.function.BiPredicate;

/**
 * A transaction which describes cancellation or creation of
 * transfer of importance from signer to remote account.
 */
public class ImportanceTransferTransaction extends Transaction {
	private final int mode;
	private final Account remoteAccount;

	/**
	 * Creates an importance transfer transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 * @param mode The transaction importance transfer mode.
	 * @param remoteAccount The remote account.
	 */
	public ImportanceTransferTransaction(final TimeInstant timeStamp, final Account sender, final int mode, final Account remoteAccount) {
		super(TransactionTypes.IMPORTANCE_TRANSFER, 1, timeStamp, sender);
		this.mode = mode;
		this.remoteAccount = remoteAccount;

		if (null == this.remoteAccount) {
			throw new IllegalArgumentException("remoteAccount is required");
		}

		if (!isModeValid(this.mode)) {
			throw new IllegalArgumentException("invalid mode");
		}
	}

	/**
	 * Deserializes an importance transfer transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public ImportanceTransferTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.IMPORTANCE_TRANSFER, options, deserializer);
		this.mode = deserializer.readInt("mode");
		this.remoteAccount = Account.readFrom(deserializer, "remoteAccount", AddressEncoding.PUBLIC_KEY);

		if (!isModeValid(this.mode)) {
			throw new TypeMismatchException("mode");
		}
	}

	private static boolean isModeValid(final int mode) {
		switch (mode) {
			case ImportanceTransferTransactionMode.Activate:
			case ImportanceTransferTransactionMode.Deactivate:
				return true;
		}

		return false;
	}

	/**
	 * Gets remote account.
	 *
	 * @return The remote account.
	 */
	public Account getRemote() {
		return this.remoteAccount;
	}

	/**
	 * Gets direction of importance transfer
	 *
	 * @return The direction.
	 */
	// TODO-CR: rename to getMode?
	public int getDirection() {
		return this.mode;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeInt("mode", this.mode);
		Account.writeTo(serializer, "remoteAccount", this.remoteAccount, AddressEncoding.PUBLIC_KEY);
	}

	@Override
	protected void executeCommit() {
		// empty
	}

	@Override
	protected void undoCommit() {
		// empty
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		observer.notify(new AccountNotification(this.getRemote()));
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.getSigner(), this.getFee()));
		observer.notify(new ImportanceTransferNotification(this.getSigner(), this.getRemote(), this.getDirection()));
	}

	@Override
	protected ValidationResult checkDerivedValidity(final BiPredicate<Account, Amount> canDebitPredicate) {
		if (!canDebitPredicate.test(this.getSigner(), this.getFee())) {
			return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
		}

		return ValidationResult.SUCCESS;
	}

	@Override
	protected Amount getMinimumFee() {
		return Amount.fromNem(1);
	}
}
