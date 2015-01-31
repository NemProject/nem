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

	//region Mode

	/**
	 * Static class containing modes of ImportanceTransferTransaction.
	 */
	public static enum Mode {
		/**
		 * An unknown mode.
		 */
		Unknown(0),

		/**
		 * When announcing importance transfer.
		 */
		Activate(1),

		/**
		 * When canceling association between account and importance transfer.
		 */
		Deactivate(2);

		private final int value;

		private Mode(final int value) {
			this.value = value;
		}

		private boolean isValid() {
			switch (this) {
				case Activate:
				case Deactivate:
					return true;
			}

			return false;
		}

		/**
		 * Creates a mode given a raw value.
		 *
		 * @param value The value.
		 * @return The mode if the value is known or Unknown if it was not.
		 */
		public static Mode fromValueOrDefault(final int value) {
			for (final Mode mode : values()) {
				if (mode.value() == value) {
					return mode;
				}
			}

			return Mode.Unknown;
		}

		/**
		 * Gets the underlying integer representation of the mode.
		 *
		 * @return The underlying value.
		 */
		public int value() {
			return this.value;
		}
	}

	//endregion

	private final Mode mode;
	private final Account remoteAccount;

	/**
	 * Creates an importance transfer transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 * @param mode The transaction importance transfer mode.
	 * @param remoteAccount The remote account.
	 */
	public ImportanceTransferTransaction(final TimeInstant timeStamp, final Account sender, final Mode mode, final Account remoteAccount) {
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
		this.mode = Mode.fromValueOrDefault(deserializer.readInt("mode"));
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
	public Mode getMode() {
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
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.getSigner(), this.getFee()));
		observer.notify(new ImportanceTransferNotification(this.getSigner(), this.getRemote(), this.mode.value()));
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		return Arrays.asList(this.remoteAccount);
	}
}
