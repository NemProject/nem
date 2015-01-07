package org.nem.core.model;

import org.nem.core.crypto.Hash;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A multisig transaction.
 */
public class MultisigTransaction extends Transaction implements SerializableEntity {
	private final Transaction otherTransaction;
	private final Hash otherTransactionHash;
	private final SortedSet<MultisigSignatureTransaction> signatureTransactions = new TreeSet<>(new MultisigSignatureTransactionComparator());

	/**
	 * Creates a multisig transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 * @param otherTransaction The other (enclosed) transaction.
	 */
	public MultisigTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Transaction otherTransaction) {
		super(TransactionTypes.MULTISIG, 1, timeStamp, sender);
		this.otherTransaction = otherTransaction;
		this.otherTransactionHash = HashUtils.calculateHash(otherTransaction.asNonVerifiable());
	}

	/**
	 * Deserializes a multisig transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public MultisigTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.MULTISIG, options, deserializer);
		this.otherTransaction = deserializer.readObject("otherTrans", TransactionFactory.NON_VERIFIABLE);
		this.otherTransactionHash = HashUtils.calculateHash(this.otherTransaction.asNonVerifiable());

		final Collection<Transaction> signatures = DeserializationOptions.VERIFIABLE == options
				? deserializer.readObjectArray("signatures", TransactionFactory.VERIFIABLE)
				: new ArrayList<>();

		signatures.forEach(o -> this.addSignature((MultisigSignatureTransaction)o));
	}

	/**
	 * Gets the other transaction.
	 *
	 * @return The other transaction.
	 */
	public Transaction getOtherTransaction() {
		return this.otherTransaction;
	}

	/**
	 * Gets the hash of the other transaction.
	 *
	 * @return The hash of the other transaction.
	 */
	public Hash getOtherTransactionHash() {
		return this.otherTransactionHash;
	}

	/**
	 * Adds a signature to this transaction.
	 * TODO this should get called by unconfirmed transactions
	 *
	 * @param transaction The multisig signature transaction.
	 */
	public void addSignature(final MultisigSignatureTransaction transaction) {
		if (!this.getOtherTransactionHash().equals(transaction.getOtherTransactionHash())) {
			throw new IllegalArgumentException("trying to add a signature for another transaction to a multisig transaction");
		}

		// TODO 20141204 G-J: where should we check for duplicate MultisigSignatures ?
		// TODO 20141221 G-J: after changing to SortedSet, changing compareTo and equals in
		// MultisigSignatureTransaction could do the trick
		// Not sure if that's a good idea, but I've changed comparator in such way
		this.signatureTransactions.add(transaction);
	}

	// TODO 20150103 J-G: should test this where getSigners is tested

	/**
	 * Gets list of signature transactions.
	 *
	 * @return The list of signature transactions.
	 */
	public SortedSet<MultisigSignatureTransaction> getCosignerSignatures() {
		return this.signatureTransactions;
	}

	/**
	 * Gets all signers.
	 * TODO this should get called by unconfirmed transactions
	 *
	 * @return All signers.
	 */
	public List<Account> getSigners() {
		return this.signatureTransactions.stream().map(t -> t.getSigner()).collect(Collectors.toList());
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.getSigner(), this.getFee()));
		this.signatureTransactions.stream().forEach(t -> t.transfer(observer));
		this.otherTransaction.transfer(observer);
	}

	@Override
	protected Amount getMinimumFee() {
		// TODO 20141201 - i'm not sure if we want to charge people extra for multisig?
		// TODO 20141202 G-J: 1) it requires a lot of additional processing, so that is a good reason
		// to require additional fee. Maybe 100 is bit too much, but it should be high.
		//
		// MultisigAwareSingleTransactionValidator takes care of validating fee on inner transaction
		return Amount.fromNem(100L);
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		// TODO 20141220 J-G: should review / test this
		// TODO 20150105 G-J: should we add other cosigners here as well?
		// > I think it's not necessary
		return this.otherTransaction.getAccounts();
	}

	@Override
	public Collection<Transaction> getChildTransactions() {
		// we want validators to run on both inner transaction and all signatures
		final List<Transaction> result = new ArrayList<>(this.getCosignerSignatures());
		result.add(this.otherTransaction);
		return result;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		// this shouldn't be called since the other overload is implemented
	}

	@Override
	protected void serializeImpl(final Serializer serializer, final boolean includeNonVerifiableData) {
		super.serializeImpl(serializer);
		serializer.writeObject("otherTrans", this.otherTransaction.asNonVerifiable());

		if (includeNonVerifiableData) {
			serializer.writeObjectArray("signatures", this.signatureTransactions);
		}
	}

	@Override
	public boolean verify() {
		if (!super.verify()) {
			return false;
		}

		// TODO 20150103 J-G: consider simplifying since you are looping twice
		return
				this.signatureTransactions.stream().allMatch(signatureTransactions -> signatureTransactions.getOtherTransactionHash().equals(this.getOtherTransactionHash())) &&
						this.signatureTransactions.stream().allMatch(signatureTransaction -> signatureTransaction.verify());
	}
}