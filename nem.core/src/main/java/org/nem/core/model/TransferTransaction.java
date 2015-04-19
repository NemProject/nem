package org.nem.core.model;

import org.nem.core.messages.MessageFactory;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A transaction that represents the exchange of funds and/or a message
 * between a sender and a recipient.
 */
public class TransferTransaction extends Transaction {
	private final Amount amount;
	private final Message message;
	private final Account recipient;

	/**
	 * Creates a transfer transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 * @param recipient The transaction recipient.
	 * @param amount The transaction amount.
	 * @param message The transaction message.
	 */
	public TransferTransaction(final TimeInstant timeStamp, final Account sender, final Account recipient, final Amount amount, final Message message) {
		super(TransactionTypes.TRANSFER, 1, timeStamp, sender);
		this.recipient = recipient;
		this.amount = amount;
		this.message = message;

		if (null == this.recipient) {
			throw new IllegalArgumentException("recipient is required");
		}
	}

	/**
	 * Deserializes a transfer transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public TransferTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.TRANSFER, options, deserializer);
		this.recipient = Account.readFrom(deserializer, "recipient");
		this.amount = Amount.readFrom(deserializer, "amount");
		final Message message = deserializer.readOptionalObject(
				"message",
				messageDeserializer -> MessageFactory.deserialize(messageDeserializer, this.getSigner(), this.getRecipient()));
		this.message = normalizeMessage(message);
	}

	private static Message normalizeMessage(final Message message) {
		// don't charge for empty messages
		return null == message || 0 == message.getEncodedPayload().length ? null : message;
	}

	/**
	 * Gets the recipient.
	 *
	 * @return The recipient.
	 */
	public Account getRecipient() {
		return this.recipient;
	}

	/**
	 * Gets the transaction amount.
	 *
	 * @return The transaction amount.
	 */
	public Amount getAmount() {
		return this.amount;
	}

	/**
	 * Gets the transaction message.
	 *
	 * @return The transaction message.
	 */
	public Message getMessage() {
		return this.message;
	}

	/**
	 * Gets the transaction message length.
	 *
	 * @return The transaction message length.
	 */
	public int getMessageLength() {
		return null == this.message ? 0 : this.message.getEncodedPayload().length;
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		return Collections.singletonList(this.recipient);
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		Account.writeTo(serializer, "recipient", this.recipient);
		Amount.writeTo(serializer, "amount", this.amount);
		serializer.writeObject("message", this.message);
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		final TransferObserver transferObserver = new TransactionObserverToTransferObserverAdapter(observer);
		transferObserver.notifyTransfer(this.getSigner(), this.recipient, this.amount);
		transferObserver.notifyDebit(this.getSigner(), this.getFee());
	}
}