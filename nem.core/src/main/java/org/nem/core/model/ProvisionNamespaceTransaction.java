package org.nem.core.model;

import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A transaction which provisions a namespace.
 */
public class ProvisionNamespaceTransaction extends Transaction {
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
		super(TransactionTypes.PROVISION_NAMESPACE, 1, timeStamp, sender);
		this.newPart = newPart;
		this.parent = parent;
	}

	/**
	 * Deserializes a multisig signature transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public ProvisionNamespaceTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.PROVISION_NAMESPACE, options, deserializer);
		this.newPart = deserializer.readObject("newPart", NamespaceIdPart::new);
		this.parent = deserializer.readObject("parent", NamespaceId::new);
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
	 * Gets the resulting namespace id after appending the new part to the parent
	 * .
	 * @return The resulting namespace id.
	 */
	public NamespaceId getResultingNamespaceId() {
		return null == this.parent ? this.newPart.toNamespaceId() : this.parent.concat(this.newPart);
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		return Collections.emptyList();
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeObject("newPart", this.newPart);
		serializer.writeObject("parent", this.parent);
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		observer.notify(new ProvisionNamespaceNotification(this.getSigner(), this.getResultingNamespaceId()));
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.getDebtor(), this.getFee()));
	}
}
