package org.nem.core.model;

import org.nem.core.messages.MessageFactory;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.function.BiPredicate;

/**
 * A transaction that represents the exchange of funds and/or a message
 * between a sender and a recipient.
 */
public class TransferTransaction extends Transaction {
	private static final int MAX_MESSAGE_SIZE = 512;

	private Amount amount;
	private Message message;
	private Account recipient;

	/**
	 * Creates a transfer transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender    The transaction sender.
	 * @param recipient The transaction recipient.
	 * @param amount    The transaction amount.
	 * @param message   The transaction message.
	 */
	public TransferTransaction(final TimeInstant timeStamp, final Account sender, final Account recipient, final Amount amount, final Message message) {
		super(TransactionTypes.TRANSFER, 1, timeStamp, sender);
		this.recipient = recipient;
		this.amount = amount;
		this.message = message;

		if (null == this.recipient)
			throw new IllegalArgumentException("recipient is required");
	}

	/**
	 * Deserializes a transfer transaction.
	 *
	 * @param deserializer The deserializer.
	 */
	public TransferTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.TRANSFER, options, deserializer);
		this.recipient = Account.readFrom(deserializer, "recipient");
		this.amount = Amount.readFrom(deserializer, "amount");
		this.message = deserializer.readOptionalObject("message", MessageFactory.DESERIALIZER);
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

	private int getMessageLength() {
		return null == this.message ? 0 : this.message.getEncodedPayload().length;
	}


	@Override
	public ValidationResult checkDerivedValidity(final BiPredicate<Account, Amount> canDebitPredicate) {
		if (!canDebitPredicate.test(this.getSigner(), this.amount.add(this.getFee())))
			return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;

		if (this.getMessageLength() > MAX_MESSAGE_SIZE)
			return ValidationResult.FAILURE_MESSAGE_TOO_LARGE;

		return ValidationResult.SUCCESS;
	}

	@Override
	protected Amount getMinimumFee() {
		if (NemesisBlock.ADDRESS.equals(this.getSigner().getAddress()))
			return Amount.ZERO;

		return Amount.fromNem(this.getMinimumTransferFee() + this.getMinimumMessageFee());
	}

	private long getMinimumTransferFee() {
		double microNemAmount = this.amount.getNumNem();
		return Math.max(1, (long)Math.ceil(microNemAmount / 25000 + Math.log(microNemAmount) / 5));
	}

	private long getMinimumMessageFee() {
		return 0 == this.getMessageLength()
				? 0
				: Math.max(1, 5 * this.getMessageLength() / 256);
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		Account.writeTo(serializer, "recipient", this.recipient);
		Amount.writeTo(serializer, "amount", this.amount);
		serializer.writeObject("message", this.message);
	}

	@Override
	protected void executeCommit() {
		if (0 != this.getMessageLength())
			this.recipient.addMessage(this.message);
	}

	@Override
	protected void undoCommit() {
		if (0 != this.getMessageLength())
			this.recipient.removeMessage(this.message);
	}

	@Override
	protected void transfer(final TransferObserver observer) {
		observer.notifyTransfer(this.getSigner(), this.recipient, this.amount);
		observer.notifyDebit(this.getSigner(), this.getFee());
	}
}