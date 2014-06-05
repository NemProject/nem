package org.nem.core.model;

import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.function.BiPredicate;

/**
 * An abstract transaction class that serves as the base class of all NEM transactions.
 */
public abstract class Transaction extends VerifiableEntity implements Comparable<Transaction> {

	private Amount fee = Amount.ZERO;
	private TimeInstant deadline = TimeInstant.ZERO;

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
	 */
	public final void execute() {
		this.execute(new CommitTransferObserver());
		this.executeCommit();
	}

	/**
	 * Executes the transaction using the specified observer.
	 *
	 * @param observer The observer to use.
	 */
	public final void execute(final TransferObserver observer) {
		this.transfer(observer, Direction.Execute);
	}

	/**
	 * Performs any other actions required to commit the transaction.
	 */
	protected abstract void executeCommit();


	/**
	 * Undoes the transaction.
	 */
	public final void undo() {
		this.undo(new CommitTransferObserver());
		this.undoCommit();
	}

	/**
	 * Undoes the transaction using the specified observer.
	 *
	 * @param observer The observer to use.
	 */
	public final void undo(final TransferObserver observer) {
		this.transfer(new ReverseTransferObserver(observer), Direction.Undo);
	}

	/**
	 * Performs any other actions required to undo the transaction.
	 */
	protected abstract void undoCommit();

	public enum Direction {
		Execute,
		Undo
	}
	/**
	 * Executes all transfers using the specified observer.
	 *
	 * @param observer The transfer observer.
	 */
	protected abstract void transfer(final TransferObserver observer, final Direction direction);

	/**
	 * Determines if this transaction is valid using a custom can-debit predicate.
	 *
	 * @param canDebitPredicate A predicate that should return true if the first parameter (account)
	 *                          has a balance of at least the second parameter (amount).
	 * @return true if this transaction is valid.
	 */
	public final ValidationResult checkValidity(final BiPredicate<Account, Amount> canDebitPredicate) {
		if (this.getTimeStamp().compareTo(this.deadline) >= 0)
			return ValidationResult.FAILURE_PAST_DEADLINE;

		if (this.deadline.compareTo(this.getTimeStamp().addDays(1)) > 0)
			return ValidationResult.FAILURE_FUTURE_DEADLINE;

		return this.checkDerivedValidity(canDebitPredicate);
	}

	/**
	 * Checks the validity of this transaction.
	 *
	 * @return The validation result.
	 */
	public final ValidationResult checkValidity() {
		return this.checkValidity((account, amount) -> account.getBalance().compareTo(amount) >= 0);
	}

	/**
	 * Determines if this transaction is valid using a custom can-debit predicate
	 * by performing custom, implementation-specific validation checks.
	 *
	 * @param canDebitPredicate A predicate that should return true if the first parameter (account)
	 *                          has a balance of at least the second parameter (amount).
	 * @return true if this transaction is valid.
	 */
	protected abstract ValidationResult checkDerivedValidity(final BiPredicate<Account, Amount> canDebitPredicate);

	/**
	 * Gets the minimum fee for this transaction.
	 *
	 * @return The minimum fee.
	 */
	protected abstract Amount getMinimumFee();

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
}