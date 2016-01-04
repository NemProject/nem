package org.nem.core.model;

import org.nem.core.model.mosaic.MosaicConstants;
import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A transaction that provisions a namespace.
 */
public class ProvisionNamespaceTransaction extends Transaction {
	private final Account rentalFeeSink;
	private final Amount rentalFee;
	private final NamespaceIdPart newPart;
	private final NamespaceId parent;

	/**
	 * Creates a new provision namespace transaction.
	 * The parent parameter is allowed to be null.
	 *
	 * @param timeStamp The timestamp.
	 * @param sender The sender.
	 * @param newPart The new namespace id part.
	 * @param parent The parent namespace id.
	 */
	public ProvisionNamespaceTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final NamespaceIdPart newPart,
			final NamespaceId parent) {
		this(timeStamp, sender, MosaicConstants.NAMESPACE_OWNER_NEM, null == parent ? Amount.fromNem(50_000) : Amount.fromNem(5_000), newPart, parent);
	}

	/**
	 * Creates a new provision namespace transaction.
	 * The parent parameter is allowed to be null.
	 *
	 * @param timeStamp The timestamp.
	 * @param sender The sender.
	 * @param rentalFeeSink The rental fee sink.
	 * @param rentalFee The rental fee.
	 * @param newPart The new namespace id part.
	 * @param parent The parent namespace id.
	 */
	public ProvisionNamespaceTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Account rentalFeeSink,
			final Amount rentalFee,
			final NamespaceIdPart newPart,
			final NamespaceId parent) {
		super(TransactionTypes.PROVISION_NAMESPACE, 1, timeStamp, sender);

		this.rentalFeeSink = rentalFeeSink;
		this.rentalFee = rentalFee;
		this.newPart = newPart;
		this.parent = parent;
	}

	/**
	 * Deserializes a provision namespace transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public ProvisionNamespaceTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.PROVISION_NAMESPACE, options, deserializer);
		this.rentalFeeSink = Account.readFrom(deserializer, "rentalFeeSink");
		this.rentalFee = Amount.readFrom(deserializer, "rentalFee");
		this.newPart = new NamespaceIdPart(deserializer.readString("newPart"));
		final String parent = deserializer.readOptionalString("parent");
		this.parent = null == parent ? null : new NamespaceId(parent);
	}

	/**
	 * Gets the namespace rental fee sink.
	 *
	 * @return The rental fee sink.
	 */
	public Account getRentalFeeSink() {
		return this.rentalFeeSink;
	}

	/**
	 * Gets the rental fee.
	 *
	 * @return The rental fee.
	 */
	public Amount getRentalFee() {
		return this.rentalFee;
	}

	/**
	 * Gets the new part for the namespace id.
	 *
	 * @return The new namespace id part.
	 */
	public NamespaceIdPart getNewPart() {
		return this.newPart;
	}

	/**
	 * Gets the parent namespace id.
	 *
	 * @return The parent namespace id.
	 */
	public NamespaceId getParent() {
		return this.parent;
	}

	/**
	 * Gets the resulting namespace id after appending the new part to the parent.
	 *
	 * @return The resulting namespace id.
	 */
	public NamespaceId getResultingNamespaceId() {
		return null == this.parent ? new NamespaceId(this.newPart.toString()) : this.parent.concat(this.newPart);
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		return Collections.singletonList(this.rentalFeeSink);
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		Account.writeTo(serializer, "rentalFeeSink", this.rentalFeeSink);
		Amount.writeTo(serializer, "rentalFee", this.rentalFee);
		serializer.writeString("newPart", this.newPart.toString());
		serializer.writeString("parent", null == this.parent ? null : this.parent.toString());
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		observer.notify(new AccountNotification(this.rentalFeeSink));
		observer.notify(new BalanceTransferNotification(this.getSigner(), this.rentalFeeSink, this.rentalFee));
		observer.notify(new ProvisionNamespaceNotification(this.getSigner(), this.getResultingNamespaceId()));
		super.transfer(observer);
	}
}
