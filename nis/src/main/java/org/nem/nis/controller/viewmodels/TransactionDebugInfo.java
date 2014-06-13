package org.nem.nis.controller.viewmodels;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;
import org.nem.core.time.SystemTimeProvider;
import org.nem.core.time.TimeInstant;

/**
 * Debug information about a transaction.
 */
public class TransactionDebugInfo implements SerializableEntity {

	private TimeInstant timestamp;
	private TimeInstant deadline;
	private final Address sender;
	private final Address recipient;
	private final Amount amount;
	private final Amount fee;
	private final String message;
	

	/**
	 * Creates a new transaction debug info.
	 *
	 * @param timestamp The transaction timestamp.
	 * @param timestamp The transaction deadline.
	 * @param sender The transaction sender.
	 * @param recipient The transaction recipient.
	 * @param amount The transaction amount.
	 * @param fee The transaction fee.
	 * @param message The transaction message.
	 */
	public TransactionDebugInfo(
			final TimeInstant timestamp,
			final TimeInstant deadline,
			final Address sender,
			final Address recipient,
			final Amount amount,
			final Amount fee,
			final String message) {
		this.timestamp = timestamp;
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
	 * @throws ParseException 
	 */
	public TransactionDebugInfo(final Deserializer deserializer){
		try {
			Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(deserializer.readString("timestamp"));
			this.timestamp = new TimeInstant(SystemTimeProvider.getTime(date.getTime()));
			date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(deserializer.readString("deadline"));
			this.deadline = new TimeInstant(SystemTimeProvider.getTime(date.getTime()));
		}
		catch (ParseException e) {
			this.timestamp = new TimeInstant(0);
			this.deadline = new TimeInstant(0);
		}
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
	public TimeInstant getTimestamp() {
		return this.timestamp;
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
		Date date = new Date(SystemTimeProvider.getEpochTimeMillis() + this.timestamp.getRawTime() * 1000);
		String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
		serializer.writeString("timestamp", dateString);
		date = new Date(SystemTimeProvider.getEpochTimeMillis() + this.deadline.getRawTime() * 1000);
		dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
		serializer.writeString("deadline", dateString);
		Address.writeTo(serializer, "sender", this.sender);
		Address.writeTo(serializer, "recipient", this.recipient);
		Amount.writeTo(serializer, "amount", this.amount);
		Amount.writeTo(serializer, "fee", this.fee);
		serializer.writeString("message", this.message);
	}

}
