package org.nem.core.test;

import org.nem.core.model.Account;
import org.nem.core.model.Amount;
import org.nem.core.model.NemTransferSimulate;

public class MockSimulateTransfer implements NemTransferSimulate {
	private Account simulateSubAccount;
	private Account simulateAddAccount;
	private Amount simulateSubAmount;
	private Amount simulateAddAmount;

	@Override
	public boolean sub(Account account, Amount amount) {
		simulateSubAccount = account;
		simulateSubAmount = amount;
		return true;
	}

	@Override
	public void add(Account account, Amount amount) {
		simulateAddAccount = account;
		simulateAddAmount = amount;
	}

	public Account getSimulateSubAccount() {
		return simulateSubAccount;
	}

	public Account getSimulateAddAccount() {
		return simulateAddAccount;
	}

	public Amount getSimulateSubAmount() {
		return simulateSubAmount;
	}

	public Amount getSimulateAddAmount() {
		return simulateAddAmount;
	}
}
