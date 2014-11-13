package org.nem.core.model;

import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

/**
 * A transaction which describes association of cosignatory with a multisig account.
 * <br/>
 * First such transaction converts account to multisig account.
 */
public class MultisigSignerModificationTransaction extends Transaction {

	//region ModificationType

	/**
	 * Enum containing types of multisig modifications.
	 */
	public static enum ModificationType {
		/**
		 * An unknown mode.
		 */
		Unknown(0),

		/**
		 * When adding cosignatory to multisig account.
		 */
		Add(1);

		/**
		 * For now we WON'T allow removal...
		 * TODO 20141112 J-G: do you have a concern in mind or are just dropping it for expediency?
		 * TODO 20131113 G-J,B: actually it's not about implementation part, but about real life part.
		 *  1. Let's say I create multisig acct for some company and add 3 addresses mine X, and A, B, C
		 *  2. Company adds fund to the account
		 *  3. Now I remove A, B and C, and withdraw funds from an account and go to Bahamas...
		 *
		 *  I'm not sure how removal of account should be done...
		 *
		 * TODO 20131113 G-J,B: my idea (may be flawed), was as follows:
		 * 22:57 <@gimre> 1. making "add cosignatory" transaction (i.e. add X as cosigner of M), changes account M to multisig
		 * 22:58 <@gimre> and than you simply add next accounts
		 * 22:58 <@gimre> and than transfer from M to anywhere
		 * 22:59 <@gimre> requires signatures from all cosignatories
		 * 22:59 <@gimre> also, there would probably have to be "wait" time, before cosignatory becomes valid...
		 * 23:00 <@gimre> (similar to that with activation of remote harvesting account)
		 * 23:02 <@BloodyRookie> is it really needed to change a multisig account once it is created? (like adding new cosigners)
		 * 23:03 <@gimre> BloodyRookie: most likely not, but what I wanted was to have "add cosigner" as a separate transactions,
		 * as that most likely will be easier to handle
		 */
		// Del(2)

		private final int value;

		private ModificationType(final int value) {
			this.value = value;
		}

		private boolean isValid() {
			switch (this) {
				case Add:
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
		public static ModificationType fromValueOrDefault(final int value) {
			for (final ModificationType modificationType : values()) {
				if (modificationType.value() == value) {
					return modificationType;
				}
			}

			return ModificationType.Unknown;
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

	private final ModificationType modificationType;
	private final Account cosignatoryAccount;

	/**
	 * Creates an multisig signer modification transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender (multisig account).
	 * @param modificationType The type of signer modification transaction.
	 * @param cosignatoryAccount The cosignatory account.
	 */
	public MultisigSignerModificationTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final ModificationType modificationType,
			final Account cosignatoryAccount) {
		super(TransactionTypes.MULTISIG_SIGNER_MODIFY, 1, timeStamp, sender);
		this.modificationType = modificationType;
		this.cosignatoryAccount = cosignatoryAccount;

		if (null == this.cosignatoryAccount) {
			throw new IllegalArgumentException("cosignatoryAccount is required");
		}

		if (!this.modificationType.isValid()) {
			throw new IllegalArgumentException("invalid mode");
		}
	}

	/**
	 * Deserializes a multisig signer modification transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public MultisigSignerModificationTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.MULTISIG_SIGNER_MODIFY, options, deserializer);
		this.modificationType = ModificationType.fromValueOrDefault(deserializer.readInt("modificationType"));
		this.cosignatoryAccount = Account.readFrom(deserializer, "cosignatoryAccount", AddressEncoding.PUBLIC_KEY);

		if (!this.modificationType.isValid()) {
			throw new TypeMismatchException("mode");
		}
	}

	/**
	 * Gets cosignatory account.
	 *
	 * @return The cosignatory account.
	 */
	public Account getCosignatory() {
		return this.cosignatoryAccount;
	}

	/**
	 * Gets type of multisig signer modification transaction.
	 *
	 * @return The modification type.
	 */
	public ModificationType getModificationType() {
		return this.modificationType;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeInt("modificationType", this.modificationType.value());
		Account.writeTo(serializer, "cosignatoryAccount", this.cosignatoryAccount, AddressEncoding.PUBLIC_KEY);
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		observer.notify(new AccountNotification(this.getCosignatory()));
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.getSigner(), this.getFee()));
		observer.notify(new CosignatoryModificationNotification(this.getSigner(), this.getCosignatory(), this.modificationType.value()));
	}

	@Override
	protected Amount getMinimumFee() {
		// TODO 20141111: decide, but I believe this should be high
		return Amount.fromNem(1000);
	}
}
