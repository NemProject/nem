package org.nem.nis.service;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.ncc.AccountInfo;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.Utils;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.AccountState;

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
		Mockito.verify(context.accountStateCache, Mockito.times(1)).findStateByAddress(context.address);
	}

	@Test
	public void factoryReturnsAppropriateInfoWhenAccountImportanceIsSetButVestedBalanceIsNotSet() {
		// Arrange:
		final TestContext context = new TestContext();
		context.accountState.getImportanceInfo().setImportance(new BlockHeight(123), 0.796);

		// Act:
		final AccountInfo info = context.factory.createInfo(context.address);

		// Assert:
		assertAccountInfo(info, context.address, 0.796, 0);
	}

	@Test
	public void factoryReturnsAppropriateInfoWhenNeitherAccountImportanceNorVestedBalanceIsSet() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final AccountInfo info = context.factory.createInfo(context.address);

		// Assert:
		assertAccountInfo(info, context.address, 0.0, 0);
	}

	@Test
	public void factoryReturnsAppropriateInfoWhenBothAccountImportanceAndVestedBalanceAreSet() {
		// Arrange:
		final TestContext context = new TestContext();
		context.accountState.getImportanceInfo().setImportance(new BlockHeight(123), 0.796);
		context.accountState.getWeightedBalances().addFullyVested(new BlockHeight(123), new Amount(727));

		// Act:
		final AccountInfo info = context.factory.createInfo(context.address);

		// Assert:
		assertAccountInfo(info, context.address, 0.796, 727);
	}

	private static void assertAccountInfo(final AccountInfo info, final Address address, final double expectedImportance, long expectedVestedBalance) {
		Assert.assertThat(info.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(info.getAddress().getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
		Assert.assertThat(info.getBalance(), IsEqual.equalTo(Amount.fromMicroNem(747)));
		Assert.assertThat(info.getVestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(expectedVestedBalance)));
		Assert.assertThat(info.getNumHarvestedBlocks(), IsEqual.equalTo(new BlockAmount(3)));
		Assert.assertThat(info.getLabel(), IsEqual.equalTo("alpha gamma"));
		Assert.assertThat(info.getImportance(), IsEqual.equalTo(expectedImportance));
	}

	private static class TestContext {
		private final Address address = Utils.generateRandomAddressWithPublicKey();
		private final Account account = new Account(this.address);
		private final AccountState accountState = new AccountState(this.address);

		private final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final AccountInfoFactory factory = new AccountInfoFactory(this.accountLookup, this.accountStateCache);

		private TestContext() {
			final org.nem.nis.state.AccountInfo accountInfo = this.accountState.getAccountInfo();
			accountInfo.setLabel("alpha gamma");
			accountInfo.incrementBalance(new Amount(747));
			accountInfo.incrementHarvestedBlocks();
			accountInfo.incrementHarvestedBlocks();
			accountInfo.incrementHarvestedBlocks();

			Mockito.when(this.accountLookup.findByAddress(this.address)).thenReturn(this.account);
			Mockito.when(this.accountStateCache.findStateByAddress(this.address)).thenReturn(this.accountState);
		}
	}
}