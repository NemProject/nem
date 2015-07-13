package org.nem.core.model;

import org.nem.core.messages.MessageFactory;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.MustBe;

import java.util.*;

/**
 * A transaction that represents the exchange of funds/smart tiles and/or a message
 * between a sender and a recipient.
 */
public class TransferTransaction extends Transaction {
	private static final int CURRENT_VERSION = 2;
	private final Amount amount;
	private final Message message;
	private final Account recipient;
	private final SmartTileBag smartTileBag;

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
		this(timeStamp, sender, recipient, amount, message, null);
	}

	/**
	 * Creates a transfer transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 * @param recipient The transaction recipient.
	 * @param amount The transaction amount.
	 * @param message The transaction message.
	 * @param smartTileBag The bag of smart tiles.
	 */
	public TransferTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Account recipient,
			final Amount amount,
			final Message message,
			final SmartTileBag smartTileBag) {
		super(TransactionTypes.TRANSFER, CURRENT_VERSION, timeStamp, sender);
		this.recipient = recipient;
		this.amount = amount;
		this.message = message;
		this.smartTileBag = null == smartTileBag ? new SmartTileBag(Collections.emptyList()) : smartTileBag;

		MustBe.notNull(this.recipient, "recipient");
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
		if (this.getEntityVersion() >= CURRENT_VERSION) {
			final Collection<SmartTile> smartTiles = deserializer.readOptionalObjectArray("smartTiles", SmartTile::new);
			this.smartTileBag = smartTiles == null ? new SmartTileBag(Collections.emptyList()) : new SmartTileBag(smartTiles);
		} else {
			this.smartTileBag = new SmartTileBag(Collections.emptyList());
		}
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

	/**
	 * Gets the bag of smart tiles.
	 *
	 * @return The bag of smart tiles.
	 */
	public SmartTileBag getSmartTileBag() {
		return this.smartTileBag;
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
		if (this.getEntityVersion() >= CURRENT_VERSION) {
			serializer.writeObjectArray("smartTiles", this.smartTileBag.getSmartTiles());
		}
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		final TransferObserver transferObserver = new TransactionObserverToTransferObserverAdapter(observer);
		transferObserver.notifyTransfer(this.getSigner(), this.recipient, this.amount);
		transferObserver.notifyDebit(this.getSigner(), this.getFee());
	}
}