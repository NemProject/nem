package org.nem.core.model;

import org.nem.core.model.observers.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A transaction which describes cancellation or creation of
 * transfer of importance from signer to remote account.
 */
public class ImportanceTransferTransaction extends Transaction {

	private final ImportanceTransferMode mode;
	private final Account remoteAccount;

	/**
	 * Creates an importance transfer transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 * @param mode The transaction importance transfer mode.
	 * @param remoteAccount The remote account.
	 */
	public ImportanceTransferTransaction(final TimeInstant timeStamp, final Account sender, final ImportanceTransferMode mode, final Account remoteAccount) {
		super(TransactionTypes.IMPORTANCE_TRANSFER, 1, timeStamp, sender);
		this.mode = mode;
		this.remoteAccount = remoteAccount;

		if (null == this.remoteAccount) {
			throw new IllegalArgumentException("remoteAccount is required");
		}

		if (!this.mode.isValid()) {
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
		this.mode = ImportanceTransferMode.fromValueOrDefault(deserializer.readInt("mode"));
		this.remoteAccount = Account.readFrom(deserializer, "remoteAccount", AddressEncoding.PUBLIC_KEY);

		if (!this.mode.isValid()) {
			throw new TypeMismatchException("mode");
		}
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
	public ImportanceTransferMode getMode() {
		return this.mode;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeInt("mode", this.mode.value());
		Account.writeTo(serializer, "remoteAccount", this.remoteAccount, AddressEncoding.PUBLIC_KEY);
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		observer.notify(new AccountNotification(this.getRemote()));
		observer.notify(new ImportanceTransferNotification(this.getSigner(), this.getRemote(), this.mode));
		super.transfer(observer);
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		return Collections.singletonList(this.remoteAccount);
	}
}
