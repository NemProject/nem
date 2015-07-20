package org.nem.core.model;

import org.nem.core.messages.MessageFactory;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A transaction that represents the exchange of funds/smart tiles and/or a message
 * between a sender and a recipient.
 */
public class TransferTransaction extends Transaction {
	private static final int CURRENT_VERSION = 2;
	private final Amount amount;
	private final Account recipient;
	private final TransferTransactionAttachment attachment;

	/**
	 * Creates a transfer transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 * @param recipient The transaction recipient.
	 * @param amount The transaction amount.
	 * @param attachment The transaction attachment.
	 */
	public TransferTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Account recipient,
			final Amount amount,
			final TransferTransactionAttachment attachment) {
		this(CURRENT_VERSION, timeStamp, sender, recipient, amount, attachment);
	}

	/**
	 * Creates a transfer transaction.
	 *
	 * @param version The transaction version.
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 * @param recipient The transaction recipient.
	 * @param amount The transaction amount.
	 * @param attachment The transaction attachment.
	 */
	public TransferTransaction(
			final int version,
			final TimeInstant timeStamp,
			final Account sender,
			final Account recipient,
			final Amount amount,
			final TransferTransactionAttachment attachment) {
		super(TransactionTypes.TRANSFER, version, timeStamp, sender);
		this.recipient = recipient;
		this.amount = amount;
		this.attachment = attachment;
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

		this.attachment = new TransferTransactionAttachment();
		final Message message = deserializer.readOptionalObject(
				"message",
				messageDeserializer -> MessageFactory.deserialize(messageDeserializer, this.getSigner(), this.getRecipient()));
		this.attachment.setMessage(normalizeMessage(message));

		if (this.getEntityVersion() >= CURRENT_VERSION) {
			final Collection<MosaicTransferPair> transferPairs = deserializer.readOptionalObjectArray("smartTiles", MosaicTransferPair::new);
			if (null != transferPairs) {
				transferPairs.forEach(p -> this.attachment.addMosaicTransfer(p.getMosaicId(), p.getQuantity()));
			}
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
		return this.attachment.getMessage();
	}

	/**
	 * Gets the transaction message length.
	 *
	 * @return The transaction message length.
	 */
	public int getMessageLength() {
		return null == this.getMessage() ? 0 : this.getMessage().getEncodedPayload().length;
	}

	/**
	 * Gets the attachment.
	 *
	 * @return The attachment.
	 */
	public TransferTransactionAttachment getAttachment() {
		return this.attachment;
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
		serializer.writeObject("message", this.getMessage());
		if (this.getEntityVersion() >= CURRENT_VERSION) {
			serializer.writeObjectArray("smartTiles", this.attachment.getMosaicTransfers());
		}
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		// TODO 20150720 J-J temporarily disable!
		final TransferObserver transferObserver = new TransactionObserverToTransferObserverAdapter(observer);
//		if (this.smartTileBag.isEmpty()) {
//			transferObserver.notifyTransfer(this.getSigner(), this.recipient, this.amount);
//		} else {
//			final Quantity quantity = Quantity.fromValue(this.amount.getNumMicroNem());
//			for (SmartTile smartTile : this.smartTileBag.getSmartTiles()) {
//				// TODO 20150716 J-J: not sure if it makes sense to pass a smart tile here; might be better to pass mosaic + quantity
//				final Quantity effectiveQuantity = Quantity.fromValue((quantity.getRaw() * smartTile.getQuantity().getRaw()) / 1_000_000L);
//				final SmartTile effectiveSmartTile = new SmartTile(smartTile.getMosaicId(), effectiveQuantity);
//				transferObserver.notifyTransfer(this.getSigner(), this.recipient, effectiveSmartTile);
//			}
//		}

		transferObserver.notifyDebit(this.getSigner(), this.getFee());
	}

	private void raiseTransferNotification(final TransactionObserver observer) {

	}
}