package org.nem.core.model;

import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * An abstract transaction class that serves as the base class of all NEM transactions.
 */
public abstract class Transaction extends VerifiableEntity implements Comparable<Transaction> {
	private Amount fee = Amount.ZERO;
	private TimeInstant deadline = TimeInstant.ZERO;

	/**
	 * Creates a new transaction.
	 *
	 * @param type The transaction type.
	 * @param version The transaction version.
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 */
	public Transaction(final int type, final int version, final TimeInstant timeStamp, final Account sender) {
		super(type, version, timeStamp, sender);
	}

	/**
	 * Deserializes a new transaction.
	 *
	 * @param type The transaction type.
	 * @param options The deserializer options.
	 * @param deserializer The deserializer to use.
	 */
	public Transaction(final int type, final DeserializationOptions options, final Deserializer deserializer) {
		super(type, options, deserializer);
		this.fee = Amount.readFrom(deserializer, "fee");
		this.deadline = TimeInstant.readFrom(deserializer, "deadline");
	}

	//region Setters and Getters

	/**
	 * Gets the fee.
	 *
	 * @return The fee.
	 */
	public Amount getFee() {
		return this.fee.compareTo(this.getMinimumFee()) < 0
				? this.getMinimumFee()
				: this.fee;
	}

	/**
	 * Sets the fee.
	 *
	 * @param fee The desired fee.
	 */
	public void setFee(final Amount fee) {
		this.fee = fee;
	}

	/**
	 * Gets the deadline.
	 *
	 * @return The deadline.
	 */
	public TimeInstant getDeadline() {
		return this.deadline;
	}

	/**
	 * Sets the deadline.
	 *
	 * @param deadline The desired deadline.
	 */
	public void setDeadline(final TimeInstant deadline) {
		this.deadline = deadline;
	}

	/**
	 * Gets all accounts that are affected by this transaction.
	 *
	 * @return The accounts.
	 */
	public Collection<Account> getAccounts() {
		final Set<Account> accounts = new HashSet<>();
		accounts.add(this.getSigner());
		accounts.addAll(this.getOtherAccounts());
		return accounts;
	}

	//endregion

	@Override
	public int compareTo(final Transaction rhs) {
		// first sort by fees (lowest first) and then timestamps (newest first)
		final int[] comparisonResults = new int[] {
				this.getFee().compareTo(rhs.getFee()),
				-1 * this.getTimeStamp().compareTo(rhs.getTimeStamp()),
		};

		for (final int result : comparisonResults) {
			if (result != 0) {
				return result;
			}
		}

		return 0;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		Amount.writeTo(serializer, "fee", this.getFee());
		TimeInstant.writeTo(serializer, "deadline", this.getDeadline());
	}

	/**
	 * Executes the transaction using the specified observer.
	 *
	 * @param observer The observer to use.
	 */
	public final void execute(final TransactionObserver observer) {
		this.transfer(observer);
	}

	/**
	 * Undoes the transaction using the specified observer.
	 *
	 * @param observer The observer to use.
	 */
	public final void undo(final TransactionObserver observer) {
		final ReverseTransactionObserver reverseObserver = new ReverseTransactionObserver(observer);
		this.transfer(reverseObserver);
		reverseObserver.commit();
	}

	/**
	 * Gets all transactions that are children of this transaction.
	 * In other words, this transaction is an aggregate of the child transactions.
	 *
	 * @return The child transactions.
	 */
	public Collection<Transaction> getChildTransactions() {
		return Collections.emptyList();
	}

	/**
	 * Executes all transfers using the specified observer.
	 *
	 * @param observer The transfer observer.
	 */
	protected abstract void transfer(final TransactionObserver observer);

	/**
	 * Gets the minimum fee for this transaction.
	 *
	 * @return The minimum fee.
	 */
	protected abstract Amount getMinimumFee();

	/**
	 * Gets all accounts (excluding the signer) that are affected by this transaction.
	 *
	 * @return The accounts.
	 */
	protected abstract Collection<Account> getOtherAccounts();
}