package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.test.Utils;

public class VirtualAccountTest {

	@Test
	public void virtualAccountReadOnlyGettersInitiallyAllDelegateToRealAccount() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		account.setLabel("foo");
		final VirtualAccount virtualAccount = new VirtualAccount(account);

		// Assert:
		Assert.assertThat(virtualAccount.getKeyPair(), IsSame.sameInstance(account.getKeyPair()));
		Assert.assertThat(virtualAccount.getAddress(), IsSame.sameInstance(account.getAddress()));
		Assert.assertThat(virtualAccount.getBalance(), IsSame.sameInstance(account.getBalance()));
		Assert.assertThat(virtualAccount.getLabel(), IsSame.sameInstance(account.getLabel()));
	}

	@Test
	public void virtualAccountBalanceChangesDoNotChangeRealAccountBalance() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(new Amount(1000));
		final VirtualAccount virtualAccount = new VirtualAccount(account);

		// Act:
		virtualAccount.incrementBalance(new Amount(500));
		virtualAccount.decrementBalance(new Amount(30));

		// Assert:
		Assert.assertThat(virtualAccount.getBalance(), IsNot.not(IsEqual.equalTo(account.getBalance())));
		Assert.assertThat(virtualAccount.getBalance(), IsEqual.equalTo(new Amount(1470)));
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(new Amount(1000)));
	}

	@Test
	public void revertRevertsAllVirtualBalanceChanges() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(new Amount(1000));
		final VirtualAccount virtualAccount = new VirtualAccount(account);

		virtualAccount.incrementBalance(new Amount(500));
		virtualAccount.decrementBalance(new Amount(30));

		// Act:
		virtualAccount.revert();

		// Assert:
		Assert.assertThat(virtualAccount.getBalance(), IsEqual.equalTo(account.getBalance()));
		Assert.assertThat(virtualAccount.getBalance(), IsEqual.equalTo(new Amount(1000)));
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(new Amount(1000)));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void virtualAccountDoesNotSupportSetLabel() {
		// Arrange:
		final VirtualAccount account = new VirtualAccount(Utils.generateRandomAccount());

		// Act:
		account.setLabel("foo");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void virtualAccountDoesNotSupportGetMessages() {
		// Arrange:
		final VirtualAccount account = new VirtualAccount(Utils.generateRandomAccount());

		// Act:
		account.getMessages();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void virtualAccountDoesNotSupportAddMessage() {
		// Arrange:
		final VirtualAccount account = new VirtualAccount(Utils.generateRandomAccount());

		// Act:
		account.addMessage(new PlainMessage(new byte[] { }));
	}
}
