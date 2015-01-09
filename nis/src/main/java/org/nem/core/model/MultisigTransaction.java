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
	 *
	 * @param transaction The multisig signature transaction.
	 */
	public void addSignature(final MultisigSignatureTransaction transaction) {
		if (!this.getOtherTransactionHash().equals(transaction.getOtherTransactionHash())) {
			throw new IllegalArgumentException("trying to add a signature for another transaction to a multisig transaction");
		}

		this.signatureTransactions.add(transaction);
	}

	/**
	 * Gets list of signature transactions.
	 *
	 * @return The list of signature transactions.
	 */
	public Set<MultisigSignatureTransaction> getCosignerSignatures() {
		return Collections.unmodifiableSet(this.signatureTransactions);
	}

	/**
	 * Gets all signers.
	 *
	 * @return All signers.
	 */
	public List<Account> getSigners() {
		return Collections.unmodifiableList(this.signatureTransactions.stream().map(t -> t.getSigner()).collect(Collectors.toList()));
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.getSigner(), this.getFee()));
		this.signatureTransactions.stream().forEach(t -> t.transfer(observer));
		this.otherTransaction.transfer(observer);
	}

	@Override
	protected Amount getMinimumFee() {
		// MultisigAwareSingleTransactionValidator takes care of validating fee on inner transaction
		// TODO 20150108 J-G: i think we should come to an agreement on the fee; what do you think about a contingent fee like:
		// > 5L * this.getCosignerSignatures().size()
		// TODO 20150109 G-J: y, I was thinking exactly about it, but due to the fact that we doubled the coins, I'd make it 10
		// (actually I'd make it even higher, but we'd have to discuss that)
		return Amount.fromNem(10L * this.getCosignerSignatures().size());
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		return this.getChildTransactions().stream()
				.flatMap(t -> t.getAccounts().stream())
				.collect(Collectors.toList());
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
		return super.verify() && this.signatureTransactions.stream().allMatch(this::isSignatureMatch);
	}

	private boolean isSignatureMatch(final MultisigSignatureTransaction signatureTransaction) {
		return signatureTransaction.getOtherTransactionHash().equals(this.getOtherTransactionHash()) && signatureTransaction.verify();
	}
}