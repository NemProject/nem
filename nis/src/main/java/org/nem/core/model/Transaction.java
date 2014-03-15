package org.nem.core.model;

import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

/**
 * An abstract transaction class that serves as the base class of all NEM transactions.
 */
public abstract class Transaction extends VerifiableEntity implements Comparable<Transaction> {

	private long fee;
	private TimeInstant deadline = TimeInstant.ZERO;

	/**
	 * Creates a new transaction.
	 *
	 * @param type The transaction type.
	 * @param version The transaction version.
     * @param timestamp The transaction timestamp.
	 * @param sender The transaction sender.
	 */
	public Transaction(final int type, final int version, final TimeInstant timestamp, final Account sender) {
		super(type, version, timestamp, sender);
	}

	/**
	 * Deserializes a new transaction.
	 *
	 * @param type The transaction type.
	 * @param deserializer The deserializer to use.
	 */
	public Transaction(final int type, final DeserializationOptions options, final Deserializer deserializer) {
		super(type, options, deserializer);
		this.fee = deserializer.readLong("fee");
		this.deadline = SerializationUtils.readTimeInstant(deserializer, "deadline");
	}

	//region Setters and Getters

	/**
	 * Gets the fee.
	 *
	 * @return The fee.
	 */
	public long getFee() { return Math.max(this.getMinimumFee(), this.fee); }

	/**
	 * Sets the fee.
	 *
	 * @param fee The desired fee.
	 */
	public void setFee(final long fee) { this.fee = fee; }

	/**
	 * Gets the deadline.
	 *
	 * @return The deadline.
	 */
	public TimeInstant getDeadline() { return this.deadline; }

	/**
	 * Sets the deadline.
	 *
	 * @param deadline The desired deadline.
	 */
	public void setDeadline(final TimeInstant deadline) { this.deadline = deadline; }

	//endregion

	@Override
	public int compareTo(Transaction rhs) {
        int[] comparisonResults = new int[] {
            Integer.compare(this.getType(), rhs.getType()),
            Integer.compare(this.getVersion(), rhs.getVersion()),
            this.getTimeStamp().compareTo(rhs.getTimeStamp()),
            Long.compare(this.getFee(), rhs.getFee())
        };

        for (int result : comparisonResults) {
            if (result != 0)
                return result;
        }

        return 0;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		serializer.writeLong("fee", this.getFee());
        SerializationUtils.writeTimeInstant(serializer, "deadline", this.getDeadline());
	}

	/**
	 * Executes the transaction.
	 *
	 * TODO: not sure about this api ... what do we want to happen if execution fails?
	 */
	public abstract void execute();

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
	protected abstract long getMinimumFee();
}