package org.nem.nis.service;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.model.ncc.AccountInfo;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.Utils;
import org.nem.nis.poi.*;

public class AccountInfoFactoryTest {

	@Test
	public void factoryDelegatesToAccountLookup() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.factory.createInfo(context.address);

		// Assert:
		Mockito.verify(context.accountLookup, Mockito.times(1)).findByAddress(context.address);
	}

	@Test
	public void factoryDelegatesToPoiFacade() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.factory.createInfo(context.address);

		// Assert:
		Mockito.verify(context.poiFacade, Mockito.times(1)).findStateByAddress(context.address);
	}

	@Test
	public void factoryReturnsAppropriateInfoWhenAccountImportanceIsSet() {
		// Arrange:
		final TestContext context = new TestContext();
		context.accountState.getImportanceInfo().setImportance(new BlockHeight(123), 0.796);

		// Act:
		final AccountInfo info = context.factory.createInfo(context.address);

		// Assert:
		assertAccountInfo(info, context.address, 0.796);
	}

	@Test
	public void factoryReturnsAppropriateInfoWhenAccountImportanceIsUnset() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final AccountInfo info = context.factory.createInfo(context.address);

		// Assert:
		assertAccountInfo(info, context.address, 0.0);
	}

	private static Account createAccount(final Address address) {
		// Arrange:
		final Account account = new Account(address);
		account.setLabel("alpha gamma");
		account.incrementBalance(new Amount(747));
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		return account;
	}

	private static void assertAccountInfo(final AccountInfo info, final Address address, final double expectedImportance) {
		Assert.assertThat(info.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(info.getKeyPair().getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
		Assert.assertThat(info.getBalance(), IsEqual.equalTo(Amount.fromMicroNem(747)));
		Assert.assertThat(info.getNumForagedBlocks(), IsEqual.equalTo(new BlockAmount(3)));
		Assert.assertThat(info.getLabel(), IsEqual.equalTo("alpha gamma"));
		Assert.assertThat(info.getImportance(), IsEqual.equalTo(expectedImportance));
	}

	private static class TestContext {
		private final Address address = Utils.generateRandomAddressWithPublicKey();
		private final Account account = createAccount(this.address);
		private final PoiAccountState accountState = new PoiAccountState(this.address);

		private final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final AccountInfoFactory factory = new AccountInfoFactory(this.accountLookup, this.poiFacade);

		private TestContext() {
			Mockito.when(this.accountLookup.findByAddress(this.address)).thenReturn(this.account);
			Mockito.when(this.poiFacade.findStateByAddress(this.address)).thenReturn(this.accountState);
		}
	}
}