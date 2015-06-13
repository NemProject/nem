package org.nem.core.model;

import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.observers.TransactionObserver;
import org.nem.core.serialization.Serializer;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A transaction which provisions a namespace.
 */
public class ProvisionNamespaceTransaction extends Transaction {
	private final String name;
	private final NamespaceId parent;

	public ProvisionNamespaceTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final String name,
			final NamespaceId parent) {
		super(TransactionTypes.PROVISION_NAMESPACE, 1, timeStamp, sender);
		this.name = name;
		this.parent = parent;
		// TODO 20150613 BR -> all: validate name?
	}

	public String getName() {
		return this.name;
	}

	public String getFullName() {
		return null == this.parent ? this.name : this.parent + "." + this.name;
	}

	public NamespaceId getParent() {
		return this.parent;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeString("name", this.name);
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
