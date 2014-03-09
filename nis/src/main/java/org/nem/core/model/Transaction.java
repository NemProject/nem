package org.nem.core.model;

import org.nem.core.crypto.Hashes;
import org.nem.core.serialization.*;

/**
 * An abstract transaction class that serves as the base class of all NEM transactions.
 */
public abstract class Transaction extends VerifiableEntity {

	private long fee;
	private int timestamp;
	private int deadline;

	/**
	 * Creates a new transaction.
	 *
	 * @param type The transaction type.
	 * @param version The transaction version.
	 * @param sender The transaction sender.
	 */
	public Transaction(final int type, final int version, final Account sender) {
		super(type, version, sender);
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
		this.timestamp = deserializer.readInt("timestamp");
		this.deadline = deserializer.readInt("deadline");
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
	 * Gets transaction timestamp
	 *
	 * @return transaction timestamp
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * Sets transaction timestamp (in seconds since NEM EPOCH)
	 *
	 * @param timestamp
	 */
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Gets transaction deadline (in seconds since NEM EPOCH)
	 *
	 * @return
	 */
	public int getDeadline() {
		return deadline;
	}

	/**
	 * Sets transaction deadline (in seconds since NEM EPOCH)
	 *
	 * @param deadline
	 */
	public void setDeadline(int deadline) {
		this.deadline = deadline;
	}
	//endregion

	@Override
	protected void serializeImpl(final Serializer serializer) {
		serializer.writeLong("fee", this.getFee());
		serializer.writeInt("timestamp", this.getTimestamp());
		serializer.writeInt("deadline", this.getDeadline());
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
		return this.timestamp >= 0 && this.deadline > this.timestamp && this.deadline < 24*60*60;
	}

	/**
	 * Gets the minimum fee for this transaction.
	 *
	 * @return The minimum fee.
	 */
	protected abstract long getMinimumFee();
}