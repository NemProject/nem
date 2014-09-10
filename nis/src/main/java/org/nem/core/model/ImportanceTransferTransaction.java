package org.nem.core.model;

import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.AddressEncoding;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.Serializer;
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
	 * Gets remote account.
	 *
	 * @return The remote account.
	 */
	public Account getRemote() {
		return remoteAccount;
	}

	/**
	 * Gets direction of importance transfer
	 *
	 * @return ImportanceTransferTransactionMode
	 */
	public int getDirection() {
		return mode;
	}

	/**
	 * Creates Importance transfer transaction.
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

		checkMode();
	}

	/**
	 * Deserializes importance transfer transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public ImportanceTransferTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.IMPORTANCE_TRANSFER, options, deserializer);
		this.mode = deserializer.readInt("mode");
		this.remoteAccount = Account.readFrom(deserializer, "remoteAccount", AddressEncoding.PUBLIC_KEY);

		checkMode();
	}

	private void checkMode() {
		if (this.mode != ImportanceTransferTransactionMode.Activate && this.mode != ImportanceTransferTransactionMode.Deactivate) {
			throw new IllegalArgumentException("invalid mode");
		}
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
	protected void transfer(final TransferObserver observer) {
		// this might look dumb, but it's essential to trigger proper observers
		// TODO 20140909 J-G: please elaborate :)
		// We need to trigger AccountsHeightObserver (which will add "recipient" to account analyzer)
		observer.notifyTransfer(this.getSigner(), this.getRemote(), Amount.ZERO);
		observer.notifyDebit(this.getSigner(), this.getFee());
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
