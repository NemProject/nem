package org.nem.core.model;

import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * An abstract transaction class that serves as the base class of all NEM transactions.
 */
public abstract class Transaction extends VerifiableEntity implements Comparable<Transaction> {

	private Amount fee = Amount.ZERO;
	private TimeInstant deadline = TimeInstant.ZERO;
	private final List<TransferObserver> transferObservers = new ArrayList<>();
	private final TransferObserver transferObserver = new TransferObserverAggregate();

	/**
	 * Creates a new transaction.
	 *
	 * @param type      The transaction type.
	 * @param version   The transaction version.
	 * @param timestamp The transaction timestamp.
	 * @param sender    The transaction sender.
	 */
	public Transaction(final int type, final int version, final TimeInstant timestamp, final Account sender) {
		super(type, version, timestamp, sender);
	}

	/**
	 * Deserializes a new transaction.
	 *
	 * @param type         The transaction type.
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

	//endregion

	@Override
	public int compareTo(Transaction rhs) {
		int[] comparisonResults = new int[] {
				Integer.compare(this.getType(), rhs.getType()),
				Integer.compare(this.getVersion(), rhs.getVersion()),
				this.getTimeStamp().compareTo(rhs.getTimeStamp()),
				this.getFee().compareTo(rhs.getFee())
		};

		for (int result : comparisonResults) {
			if (result != 0)
				return result;
		}

		return 0;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		Amount.writeTo(serializer, "fee", this.getFee());
		TimeInstant.writeTo(serializer, "deadline", this.getDeadline());
	}

	/**
	 * Executes the transaction.
	 *
	 * @param commit true if changes should be committed by the default action.
	 */
	public final void execute(boolean commit) {
		this.transfer(this.transferObserver);
		if (!commit)
			return;

		this.transfer(new CommitTransferObserver());
		this.executeCommit();
	}

	/**
	 * Performs any other actions required to commit the transaction.
	 */
	protected abstract void executeCommit();

	/**
	 * Performs any other actions required to undo the transaction
	 *
	 * @param commit true if changes should be committed by the default action.
	 */
	public final void undo(boolean commit) {
		this.transfer(new ReverseTransferObserver(this.transferObserver));
		if (!commit)
			return;

		this.transfer(new ReverseTransferObserver(new CommitTransferObserver()));
		this.undoCommit();
	}

	/**
	 * Performs any other actions required to undo the transaction.
	 */
	protected abstract void undoCommit();

	/**
	 * Executes all transfers using the specified observer.
	 *
	 * @param observer The transfer observer.
	 */
	protected abstract void transfer(final TransferObserver observer);

	/**
	 * Determine if transaction is valid using given transaction validator
	 * @param transactionValidator transaction validator to use for validation
	 * @return true if transaction is valid
	 */
	public abstract boolean isValid(final TransactionValidator transactionValidator);

	/**
	 * Determines if this transaction is valid.
	 *
	 * @return true if this transaction is valid.
	 */
	public boolean isValid() {
		return this.deadline.compareTo(this.getTimeStamp()) > 0
				&& this.deadline.compareTo(this.getTimeStamp().addDays(1)) < 1;
	}

	/**
	 * Gets the minimum fee for this transaction.
	 *
	 * @return The minimum fee.
	 */
	protected abstract Amount getMinimumFee();

	/**
	 * Gets a value indicated whether or not the specified observer is subscribed to this object.
	 *
	 * @param observer The observer.
	 * @return true if the observer is subscribed to this object.
	 */
	public boolean isSubscribed(final TransferObserver observer) {
		return this.transferObservers.contains(observer);
	}

	/**
	 * Subscribes the observer to transfers initiated by this transaction.
	 *
	 * @param observer The observer.
	 */
	public void subscribe(final TransferObserver observer) {
		this.transferObservers.add(observer);
	}

	/**
	 * Unsubscribes the observer from transfers initiated by this transaction.
	 *
	 * @param observer The observer.
	 */
	public void unsubscribe(final TransferObserver observer) {
		this.transferObservers.remove(observer);
	}

	private static class ReverseTransferObserver implements TransferObserver {

		private final TransferObserver observer;

		public ReverseTransferObserver(final TransferObserver observer) {
			this.observer = observer;
		}

		@Override
		public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
			this.observer.notifyTransfer(recipient, sender, amount);
		}

		@Override
		public void notifyCredit(final Account account, final Amount amount) {
			this.observer.notifyDebit(account, amount);
		}

		@Override
		public void notifyDebit(final Account account, final Amount amount) {
			this.observer.notifyCredit(account, amount);
		}
	}

	private static class CommitTransferObserver implements TransferObserver {

		@Override
		public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
			this.notifyDebit(sender, amount);
			this.notifyCredit(recipient, amount);
		}

		@Override
		public void notifyCredit(final Account account, final Amount amount) {
			account.incrementBalance(amount);
		}

		@Override
		public void notifyDebit(final Account account, final Amount amount) {
			account.decrementBalance(amount);
		}
	}

	private class TransferObserverAggregate implements TransferObserver {

		@Override
		public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
			transferObservers.stream().forEach(o -> o.notifyTransfer(sender, recipient, amount));
		}

		@Override
		public void notifyCredit(final Account account, final Amount amount) {
			transferObservers.stream().forEach(o -> o.notifyCredit(account, amount));
		}

		@Override
		public void notifyDebit(final Account account, final Amount amount) {
			transferObservers.stream().forEach(o -> o.notifyDebit(account, amount));
		}
	}
}