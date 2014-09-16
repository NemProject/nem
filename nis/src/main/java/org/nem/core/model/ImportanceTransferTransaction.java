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
	// TODO-CR 20140914 J-G: shouldn't this be ImportanceTransferTransactionMode instead of int?
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
	 * @return The direction.
	 */
	public int getDirection() {
		return mode;
	}

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

		checkMode();
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

		// TODO J-G 20140914 you could add a test that deserialization fails when mode is invalid
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
		// TODO 20140914 J-J: i understand what you're doing but i don't really like overloading the meaning of (nem) transfer
		// this also means that different types of transfers cannot share observers
		// a better approach might be to add a new observer interface notifyImportanceTransfer
		// but that could also get out of control if we need to add one for each transaction type
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
