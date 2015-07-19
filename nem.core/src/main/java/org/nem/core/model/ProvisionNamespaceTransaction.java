package org.nem.core.model;

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
	private final Account lessor;
	private final Amount rentalFee;
	private final NamespaceIdPart newPart;
	private final NamespaceId parent;

	/**
	 * Creates a new provision namespace transaction.
	 * The parent parameter is allowed to be null.
	 *
	 * @param timeStamp The timestamp.
	 * @param sender The sender.
	 * @param lessor The lessor.
	 * @param rentalFee The rental fee.
	 * @param newPart The new namespace id part.
	 * @param parent The parent namespace id.
	 */
	public ProvisionNamespaceTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Account lessor,
			final Amount rentalFee,
			final NamespaceIdPart newPart,
			final NamespaceId parent) {
		super(TransactionTypes.PROVISION_NAMESPACE, 1, timeStamp, sender);
		if (!lessor.hasPublicKey()) {
			throw new IllegalArgumentException("lessor public key required");
		}

		this.lessor = lessor;
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
		this.lessor = Account.readFrom(deserializer, "lessor", AddressEncoding.PUBLIC_KEY);
		this.rentalFee = Amount.readFrom(deserializer, "rentalFee");
		this.newPart = new NamespaceIdPart(deserializer.readString("newPart"));
		final String parent = deserializer.readOptionalString("parent");
		this.parent = null == parent ? null : new NamespaceId(parent);
	}

	/**
	 * Gets the namespace lessor.
	 *
	 * @return The lessor.
	 */
	public Account getLessor() {
		return this.lessor;
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
		return Collections.singletonList(this.lessor);
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		Account.writeTo(serializer, "lessor", this.lessor, AddressEncoding.PUBLIC_KEY);
		Amount.writeTo(serializer, "rentalFee", this.rentalFee);
		serializer.writeString("newPart", this.newPart.toString());
		serializer.writeString("parent", null == this.parent ? null : this.parent.toString());
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		final TransferObserver transferObserver = new TransactionObserverToTransferObserverAdapter(observer);
		transferObserver.notifyTransfer(this.getSigner(), this.lessor, this.rentalFee);
		observer.notify(new ProvisionNamespaceNotification(this.getSigner(), this.getResultingNamespaceId()));
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.getDebtor(), this.getFee()));
	}
}
