package org.nem.core.model;

import org.nem.core.crypto.Hash;
import org.nem.core.model.observers.BalanceAdjustmentNotification;
import org.nem.core.model.observers.NotificationType;
import org.nem.core.model.observers.TransactionObserver;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.ArrayUtils;

import java.util.*;

/**
 * A multisig signature transaction.
 */
public class MultisigSignatureTransaction extends Transaction implements SerializableEntity {
	private final Hash otherTransactionHash;

	/**
	 * Creates a multisig signature transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 * @param otherTransactionHash The hash of the other transaction.
	 */
	public MultisigSignatureTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Hash otherTransactionHash) {
		super(TransactionTypes.MULTISIG_SIGNATURE, 1, timeStamp, sender);
		this.otherTransactionHash = otherTransactionHash;
	}

	/**
	 * Deserializes a multisig signature transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public MultisigSignatureTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.MULTISIG_SIGNATURE, options, deserializer);
		this.otherTransactionHash = deserializer.readObject("otherHash", Hash::new);
	}

	/**
	 * Gets the hash of the other transaction.
	 *
	 * @return The hash of the other transaction.
	 */
	public Hash getOtherTransactionHash() {
		return this.otherTransactionHash;
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.getSigner(), this.getFee()));
	}

	@Override
	protected Amount getMinimumFee() {
		return Amount.ZERO;
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		// TODO 20141220 J-G: should test this
		return new ArrayList<>();
	}

	@Override
	public int hashCode() {
		// TODO 20150104 J-G: i think we need this (and need tests for it)
		return this.getSigner().getAddress().hashCode() ^ this.getOtherTransactionHash().hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof MultisigSignatureTransaction)) {
			return false;
		}

		final MultisigSignatureTransaction rhs = (MultisigSignatureTransaction)obj;
		return (0 == rhs.compareTo(rhs));
	}

	/**
	 * For MultisigSignature We take into consideration only signer and other transaction hash.
	 *
	 * @param rhs Transaction to compare to
	 * @return -1, 0 or 1
	 */
	@Override
	public int compareTo(final Transaction rhs) {
		if (!(rhs instanceof  MultisigSignatureTransaction)) {
			// TODO 20140103 J-G: i think this is wrong, why are you always prioritizing multisig transactions?
			// TODO 20150105 G-J: I wanted MultisigSignatures inside SortedSet in MultisigTransaction
			// > to be sorted in some sensible manner (they need to be sorted, otherwise we'd need kind of "orderId" field,
			// > we've discussed that some time ago)
			// > MultisigSignatures will never be returned from UnconfirmedTransactions, they cannot exist without associated
			// > MultisigTransaction (getTransactionsBefore() filters them out),
			// > so I think it should be ok, to override compareTo(), or is there something I'm missing?
			return -1;
		}

		int result = this.getSigner().getAddress().getEncoded().compareTo(rhs.getSigner().getAddress().getEncoded());
		if (result != 0) {
			return result;
		}

		return ArrayUtils.compare(this.getOtherTransactionHash().getRaw(), ((MultisigSignatureTransaction)rhs).getOtherTransactionHash().getRaw());
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeObject("otherHash", this.getOtherTransactionHash());
	}
}
