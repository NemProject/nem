package org.nem.nis.service;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.ncc.AccountInfo;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.Utils;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;

public class AccountInfoFactoryTest {
	private static final BlockHeight LAST_BLOCK_HEIGHT = new BlockHeight(333);
	private static final BlockHeight IMPORTANCE_BLOCK_HEIGHT = new BlockHeight(123);

	@Test
	public void factoryReturnsAppropriateInfoWhenAccountImportanceIsSetButVestedBalanceIsNotSet() {
		// Arrange:
		final TestContext context = new TestContext();
		context.accountState.getImportanceInfo().setImportance(IMPORTANCE_BLOCK_HEIGHT, 0.796);

		// Act:
		final AccountInfo info = context.factory.createInfo(context.address);

		// Assert:
		context.assertAccountInfo(info, 0.796, 0);
	}

	@Test
	public void factoryReturnsAppropriateInfoWhenNeitherAccountImportanceNorVestedBalanceIsSet() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final AccountInfo info = context.factory.createInfo(context.address);

		// Assert:
		context.assertAccountInfo(info, 0.0, 0);
	}

	@Test
	public void factoryReturnsAppropriateInfoWhenBothAccountImportanceAndVestedBalanceAreSet() {
		// Arrange:
		final TestContext context = new TestContext();
		context.accountState.getImportanceInfo().setImportance(IMPORTANCE_BLOCK_HEIGHT, 0.796);
		context.accountState.getWeightedBalances().addFullyVested(IMPORTANCE_BLOCK_HEIGHT, new Amount(515));
		context.accountState.getWeightedBalances().addFullyVested(LAST_BLOCK_HEIGHT, new Amount(727));

		// Act:
		final AccountInfo info = context.factory.createInfo(context.address);

		// Assert:
		context.assertAccountInfo(info, 0.796, 727);
	}

	@Test
	public void factoryReturnsAddressPublicKeyWhenKnown() {
		// Arrange:
		final TestContext context = new TestContext();
		final Address address = Address.fromEncoded(context.address.getEncoded());

		// Act:
		final AccountInfo info = context.factory.createInfo(address);

		// Assert:
		MatcherAssert.assertThat(address.getPublicKey(), IsNull.nullValue());
		MatcherAssert.assertThat(info.getAddress().getPublicKey(), IsNull.notNullValue());
		MatcherAssert.assertThat(info.getAddress().getPublicKey(), IsEqual.equalTo(context.address.getPublicKey()));
	}

	@Test
	public void factoryReturnsMultisigInfoWhenCosignatoriesArePresent() {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigLinks multisigLinks = context.accountState.getMultisigLinks();
		multisigLinks.addCosignatory(Utils.generateRandomAddress());
		multisigLinks.addCosignatory(Utils.generateRandomAddress());
		multisigLinks.addCosignatory(Utils.generateRandomAddress());
		multisigLinks.incrementMinCosignatoriesBy(2);

		// Act:
		final AccountInfo info = context.factory.createInfo(context.address);

		// Assert:
		MatcherAssert.assertThat(info.getMultisigInfo(), IsNull.notNullValue());
		MatcherAssert.assertThat(info.getMultisigInfo().getCosignatoriesCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(info.getMultisigInfo().getMinCosignatories(), IsEqual.equalTo(2));
	}

	private static class TestContext {
		private final Address address = Utils.generateRandomAddressWithPublicKey();
		private final Account account = new Account(this.address);
		private final AccountState accountState = new AccountState(this.address);

		private final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final BlockChainLastBlockLayer lastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		private final AccountInfoFactory factory = new AccountInfoFactory(this.accountLookup, this.accountStateCache, this.lastBlockLayer);

		public TestContext() {
			final org.nem.nis.state.AccountInfo accountInfo = this.accountState.getAccountInfo();
			accountInfo.setLabel("alpha gamma");
			accountInfo.incrementBalance(new Amount(747));
			accountInfo.incrementHarvestedBlocks();
			accountInfo.incrementHarvestedBlocks();
			accountInfo.incrementHarvestedBlocks();

			Mockito.when(this.accountLookup.findByAddress(this.address)).thenReturn(this.account);
			Mockito.when(this.accountStateCache.findStateByAddress(this.address)).thenReturn(this.accountState);
			Mockito.when(this.lastBlockLayer.getLastBlockHeight()).thenReturn(LAST_BLOCK_HEIGHT);
		}

		public void assertAccountInfo(final AccountInfo info, final double expectedImportance, final long expectedVestedBalance) {
			// Assert:
			// - values
			MatcherAssert.assertThat(info.getAddress(), IsEqual.equalTo(this.address));
			MatcherAssert.assertThat(info.getAddress().getPublicKey(), IsEqual.equalTo(this.address.getPublicKey()));
			MatcherAssert.assertThat(info.getBalance(), IsEqual.equalTo(Amount.fromMicroNem(747)));
			MatcherAssert.assertThat(info.getVestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(expectedVestedBalance)));
			MatcherAssert.assertThat(info.getNumHarvestedBlocks(), IsEqual.equalTo(new BlockAmount(3)));
			MatcherAssert.assertThat(info.getLabel(), IsEqual.equalTo("alpha gamma"));
			MatcherAssert.assertThat(info.getImportance(), IsEqual.equalTo(expectedImportance));
			MatcherAssert.assertThat(info.getMultisigInfo(), IsNull.nullValue());

			// - mocks were called
			Mockito.verify(this.accountLookup, Mockito.only()).findByAddress(this.address);
			Mockito.verify(this.accountStateCache, Mockito.only()).findStateByAddress(this.address);
			Mockito.verify(this.lastBlockLayer, Mockito.only()).getLastBlockHeight();
		}
	}
}
