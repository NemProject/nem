package org.nem.nis;

import org.nem.core.model.*;

import java.util.List;

/**
 * A virtual account decorator that supports reverting.
 */
public class VirtualAccount extends Account {

	private final Account account;
	private Amount balance;

	/**
	 * Creates a new virtual account.
	 *
	 * @param account The real account.
	 */
	public VirtualAccount(final Account account) {
		super(account.getKeyPair(), account.getAddress());
		this.account = account;
		this.balance = account.getBalance();
	}

	@Override
	public Amount getBalance() {
		return this.balance;
	}

	@Override
	public void incrementBalance(final Amount amount) {
		this.balance = this.balance.add(amount);
	}

	@Override
	public void decrementBalance(final Amount amount) {
		this.balance = this.balance.subtract(amount);
	}

	@Override
	public String getLabel() {
		return this.account.getLabel();
	}

	@Override
	public void setLabel(final String label) {
		throw new UnsupportedOperationException("cannot set virtual account label");
	}

	@Override
	public List<Message> getMessages() {
		throw new UnsupportedOperationException("cannot retrieve virtual account message");
	}

	@Override
	public void addMessage(final Message message) {
		throw new UnsupportedOperationException("cannot add virtual account message");
	}

	/**
	 * Reverts all uncommitted changes.
	 */
	public void revert() {
		this.balance = this.account.getBalance();
	}
}
