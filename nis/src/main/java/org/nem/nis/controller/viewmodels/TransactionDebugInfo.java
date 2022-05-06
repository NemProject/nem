package org.nem.nis.controller.viewmodels;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.*;

/**
 * Debug information about a transaction.
 */
public class TransactionDebugInfo implements SerializableEntity {

	private final TimeInstant timeStamp;
	private final TimeInstant deadline;
	private final Address sender;
	private final Address recipient;
	private final Amount amount;
	private final Amount fee;
	private final String message;

	/**
	 * Creates a new transaction debug info.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param deadline The transaction deadline.
	 * @param sender The transaction sender.
	 * @param recipient The transaction recipient.
	 * @param amount The transaction amount.
	 * @param fee The transaction fee.
	 * @param message The transaction message.
	 */
	public TransactionDebugInfo(final TimeInstant timeStamp, final TimeInstant deadline, final Address sender, final Address recipient,
			final Amount amount, final Amount fee, final String message) {
		this.timeStamp = timeStamp;
		this.deadline = deadline;
		this.sender = sender;
		this.recipient = recipient;
		this.amount = amount;
		this.fee = fee;
		this.message = message;
	}

	/**
	 * Deserializes a transaction debug info object.
	 *
	 * @param deserializer The deserializer.
	 */
	public TransactionDebugInfo(final Deserializer deserializer) {
		this.timeStamp = readTimeStringAsTimeInstant(deserializer, "timeStamp");
		this.deadline = readTimeStringAsTimeInstant(deserializer, "deadline");
		this.sender = Address.readFrom(deserializer, "sender");
		this.recipient = Address.readFrom(deserializer, "recipient");
		this.amount = Amount.readFrom(deserializer, "amount");
		this.fee = Amount.readFrom(deserializer, "fee");
		this.message = deserializer.readString("message");
	}

	/**
	 * Returns the timestamp of the transaction.
	 *
	 * @return The timestamp.
	 */
	public TimeInstant getTimeStamp() {
		return this.timeStamp;
	}

	/**
	 * Returns the deadline of the transaction.
	 *
	 * @return The deadline.
	 */
	public TimeInstant getDeadline() {
		return this.deadline;
	}

	/**
	 * Returns the address of the sender of the transaction.
	 *
	 * @return The address.
	 */
	public Address getSender() {
		return this.sender;
	}

	/**
	 * Returns the address of the recipient of the transaction.
	 *
	 * @return The address.
	 */
	public Address getRecipient() {
		return this.recipient;
	}

	/**
	 * Returns the transferred amount of the transaction.
	 *
	 * @return The amount.
	 */
	public Amount getAmount() {
		return this.amount;
	}

	/**
	 * Returns the fee of the transaction.
	 *
	 * @return The fee.
	 */
	public Amount getFee() {
		return this.fee;
	}

	/**
	 * Returns the message of the transaction.
	 *
	 * @return The message.
	 */
	public String getMessage() {
		return this.message;
	}

	@Override
	public void serialize(final Serializer serializer) {
		writeTimeInstantAsTimeString(serializer, "timeStamp", this.timeStamp);
		writeTimeInstantAsTimeString(serializer, "deadline", this.deadline);
		Address.writeTo(serializer, "sender", this.sender);
		Address.writeTo(serializer, "recipient", this.recipient);
		Amount.writeTo(serializer, "amount", this.amount);
		Amount.writeTo(serializer, "fee", this.fee);
		serializer.writeString("message", this.message);
	}

	private static TimeInstant readTimeStringAsTimeInstant(final Deserializer deserializer, final String name) {
		return UnixTime.fromDateString(deserializer.readString(name), TimeInstant.ZERO).getTimeInstant();
	}

	private static void writeTimeInstantAsTimeString(final Serializer serializer, final String label, final TimeInstant timeInstant) {
		serializer.writeString(label, UnixTime.fromTimeInstant(timeInstant).getDateString());
	}
}
