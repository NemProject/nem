package org.nem.core.model;

import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.TransactionObserver;
import org.nem.core.serialization.Serializer;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A transaction which provisions a namespace.
 */
public class ProvisionNamespaceTransaction extends Transaction {
	private final NamespaceIdPart newPart;
	private final NamespaceId parent;

	public ProvisionNamespaceTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final NamespaceIdPart newPart,
			final NamespaceId parent) {
		super(TransactionTypes.PROVISION_NAMESPACE, 1, timeStamp, sender);
		this.newPart = newPart;
		this.parent = parent;
	}

	public NamespaceIdPart getNewPart() {
		return this.newPart;
	}

	public NamespaceId getResultingNamespace() {
		return null == this.parent ? this.newPart.toNamespaceId() : this.parent.concat(this.newPart);
	}

	public NamespaceId getParent() {
		return this.parent;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeObject("newPart", this.newPart);
		serializer.writeObject("parent", this.parent);
	}

	@Override
	protected void transfer(final TransactionObserver observer) {

	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		return Collections.emptyList();
	}
}
