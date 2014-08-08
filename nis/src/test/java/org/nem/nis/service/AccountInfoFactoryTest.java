package org.nem.nis.service;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.model.ncc.AccountInfo;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.Utils;

public class AccountInfoFactoryTest {

	@Test
	public void factoryDelegatesToAccountLookup() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final Account account = createAccount(address);
		final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
		Mockito.when(accountLookup.findByAddress(address)).thenReturn(account);
		final AccountInfoFactory factory = new AccountInfoFactory(accountLookup);

		// Act:
		factory.createInfo(address);

		// Assert:
		Mockito.verify(accountLookup, Mockito.times(1)).findByAddress(address);
	}

	@Test
	public void factoryReturnsAppropriateInfoWhenAccountImportanceIsSet() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final Account account = createAccount(address);
		account.getImportanceInfo().setImportance(new BlockHeight(123), 0.796);
		final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
		Mockito.when(accountLookup.findByAddress(address)).thenReturn(account);
		final AccountInfoFactory factory = new AccountInfoFactory(accountLookup);

		// Act:
		final AccountInfo info = factory.createInfo(address);

		// Assert:
		Assert.assertThat(info.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(info.getKeyPair().getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
		Assert.assertThat(info.getBalance(), IsEqual.equalTo(Amount.fromMicroNem(747)));
		Assert.assertThat(info.getNumForagedBlocks(), IsEqual.equalTo(new BlockAmount(3)));
		Assert.assertThat(info.getLabel(), IsEqual.equalTo("alpha gamma"));
		Assert.assertThat(info.getImportance(), IsEqual.equalTo(0.796));
	}

	@Test
	public void factoryReturnsAppropriateInfoWhenAccountImportanceIsUnset() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final Account account = createAccount(address);
		final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
		Mockito.when(accountLookup.findByAddress(address)).thenReturn(account);
		final AccountInfoFactory factory = new AccountInfoFactory(accountLookup);

		// Act:
		final AccountInfo info = factory.createInfo(address);

		// Assert:
		Assert.assertThat(info.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(info.getKeyPair().getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
		Assert.assertThat(info.getBalance(), IsEqual.equalTo(Amount.fromMicroNem(747)));
		Assert.assertThat(info.getNumForagedBlocks(), IsEqual.equalTo(new BlockAmount(3)));
		Assert.assertThat(info.getLabel(), IsEqual.equalTo("alpha gamma"));
		Assert.assertThat(info.getImportance(), IsEqual.equalTo(0.0));
	}

	private static Account createAccount(final Address address) {
		// Arrange:
		final Account account = new Account(address);
		account.setLabel("alpha gamma");
		account.incrementBalance(new Amount(747));
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		account.addMessage(new PlainMessage(new byte[] { 1, 4, 5 }));
		account.addMessage(new PlainMessage(new byte[] { 8, 12, 4 }));
		return account;
	}
}