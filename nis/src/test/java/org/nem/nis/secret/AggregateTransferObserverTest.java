package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;

import java.util.*;

public class AggregateTransferObserverTest {

	@Test
	public void notifyTransferDelegatesToSubObservers() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);

		final List<TransferObserver> transferObservers = createTransferObservers();
		final TransferObserver aggregateObserver = new AggregateTransferObserver(transferObservers);

		// Act:
		aggregateObserver.notifyTransfer(account1, account2, amount);

		// Assert:
		for (final TransferObserver transferObserver : transferObservers) {
			Mockito.verify(transferObserver, Mockito.times(1)).notifyTransfer(account1, account2, amount);

			Mockito.verify(transferObserver, Mockito.times(1)).notifyTransfer(Mockito.any(), Mockito.any(), Mockito.any());
			Mockito.verify(transferObserver, Mockito.times(0)).notifyCredit(Mockito.any(), Mockito.any());
			Mockito.verify(transferObserver, Mockito.times(0)).notifyDebit(Mockito.any(), Mockito.any());
		}
	}

	@Test
	public void notifyCreditDelegatesToSubObservers() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);

		final List<TransferObserver> transferObservers = createTransferObservers();
		final TransferObserver aggregateObserver = new AggregateTransferObserver(transferObservers);

		// Act:
		aggregateObserver.notifyCredit(account, amount);

		// Assert:
		for (final TransferObserver transferObserver : transferObservers) {
			Mockito.verify(transferObserver, Mockito.times(1)).notifyCredit(account, amount);

			Mockito.verify(transferObserver, Mockito.times(0)).notifyTransfer(Mockito.any(), Mockito.any(), Mockito.any());
			Mockito.verify(transferObserver, Mockito.times(1)).notifyCredit(Mockito.any(), Mockito.any());
			Mockito.verify(transferObserver, Mockito.times(0)).notifyDebit(Mockito.any(), Mockito.any());
		}
	}

	@Test
	public void notifyDebitDelegatesToSubObservers() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);

		final List<TransferObserver> transferObservers = createTransferObservers();
		final TransferObserver aggregateObserver = new AggregateTransferObserver(transferObservers);

		// Act:
		aggregateObserver.notifyDebit(account, amount);

		// Assert:
		for (final TransferObserver transferObserver : transferObservers) {
			Mockito.verify(transferObserver, Mockito.times(1)).notifyDebit(account, amount);

			Mockito.verify(transferObserver, Mockito.times(0)).notifyTransfer(Mockito.any(), Mockito.any(), Mockito.any());
			Mockito.verify(transferObserver, Mockito.times(0)).notifyCredit(Mockito.any(), Mockito.any());
			Mockito.verify(transferObserver, Mockito.times(1)).notifyDebit(Mockito.any(), Mockito.any());
		}
	}

	private static List<TransferObserver> createTransferObservers() {
		return Arrays.asList(
				Mockito.mock(TransferObserver.class),
				Mockito.mock(TransferObserver.class),
				Mockito.mock(TransferObserver.class));
	}
}