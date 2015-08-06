package org.nem.core.model;

import org.nem.core.messages.MessageFactory;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.MustBe;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A transaction that represents the exchange of funds/mosaics and/or a message
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
		MustBe.notNull(recipient, "recipient");

		this.recipient = recipient;
		this.amount = amount;
		this.attachment = null == attachment ? new TransferTransactionAttachment() : attachment;
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
			final Collection<Mosaic> mosaics = deserializer.readObjectArray("mosaics", Mosaic::new);
			mosaics.forEach(this.attachment::addMosaic);
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

	/**
	 * Gets all mosaics (excluding xem transfers).
	 *
	 * @return The mosaics.
	 */
	public Collection<Mosaic> getMosaics() {
		return this.getAttachment().getMosaics().stream()
				.filter(p -> !isMosaicXem(p))
				.map(p -> new Mosaic(p.getMosaicId(), Quantity.fromValue(this.getRawQuantity(p.getQuantity()))))
				.collect(Collectors.toList());
	}

	/**
	 * Gets the (optional) xem transfer amount.
	 *
	 * @return The amount or null if no xem should be transferred.
	 */
	public Amount getXemTransferAmount() {
		if (this.getAttachment().getMosaics().isEmpty()) {
			return this.amount;
		}

		return this.getAttachment().getMosaics().stream()
				.filter(TransferTransaction::isMosaicXem)
				.map(p -> Amount.fromMicroNem(this.getRawQuantity(p.getQuantity())))
				.findFirst()
				.orElse(null);
	}

	private long getRawQuantity(final Quantity quantity) {
		return this.amount.getNumMicroNem() * quantity.getRaw() / Amount.MICRONEMS_IN_NEM;
	}

	private static boolean isMosaicXem(final Mosaic mosaic) {
		return mosaic.getMosaicId().equals(MosaicConstants.MOSAIC_DEFINITION_XEM.getId());
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
			serializer.writeObjectArray("mosaics", this.attachment.getMosaics());
		}
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		final List<Notification> notifications = new ArrayList<>();
		notifications.add(new AccountNotification(this.getRecipient()));

		final Amount amount = this.getXemTransferAmount();
		if (null != amount) {
			notifications.add(new BalanceTransferNotification(this.getSigner(), this.getRecipient(), amount));
		}

		this.getMosaics().stream()
				.map(pair -> new MosaicTransferNotification(this.getSigner(), this.getRecipient(), pair.getMosaicId(), pair.getQuantity()))
				.forEach(notifications::add);

		notifications.forEach(observer::notify);
		super.transfer(observer);
	}
}