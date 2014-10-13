package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.nis.controller.requests.AccountIdBuilder;
import org.nem.nis.harvesting.*;
import org.nem.nis.service.*;

public class AccountInfoControllerTest {

	//region accountGet

	@Test
	public void accountGetDelegatesToAccountInfoFactory() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final AccountIdBuilder builder = new AccountIdBuilder();
		builder.setAddress(address.getEncoded());
		final AccountInfo accountInfo = Mockito.mock(AccountInfo.class);
		final AccountRemoteStatus accountRemoteStatus = Mockito.mock(AccountRemoteStatus.class);

		final TestContext context = new TestContext();
		Mockito.when(context.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(1L);
		Mockito.when(context.accountInfoFactory.createInfo(address)).thenReturn(accountInfo);
		Mockito.when(context.accountInfoFactory.getRemoteStatus(address, BlockHeight.ONE)).thenReturn(accountRemoteStatus);

		// Act:
		final AccountMetaDataPair metaDataPair = context.controller.accountGet(builder);

		// Assert:
		Mockito.verify(context.accountInfoFactory, Mockito.times(1)).createInfo(address);
		Mockito.verify(context.accountInfoFactory, Mockito.times(1)).getRemoteStatus(address, BlockHeight.ONE);
		Assert.assertThat(metaDataPair.getAccount(), IsSame.sameInstance(accountInfo));
	}

	@Test
	public void accountGetDelegatesToUnlockedAccounts() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final AccountIdBuilder builder = new AccountIdBuilder();
		builder.setAddress(address.getEncoded());

		final TestContext context = new TestContext();
		Mockito.when(context.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(1L);
		Mockito.when(context.accountInfoFactory.createInfo(address)).thenReturn(Mockito.mock(AccountInfo.class));
		Mockito.when(context.accountInfoFactory.getRemoteStatus(address, BlockHeight.ONE)).thenReturn(Mockito.mock(AccountRemoteStatus.class));
		Mockito.when(context.unlockedAccounts.isAccountUnlocked(address)).thenReturn(true);

		// Act:
		final AccountMetaDataPair metaDataPair = context.controller.accountGet(builder);

		// Assert:
		Mockito.verify(context.unlockedAccounts, Mockito.times(1)).isAccountUnlocked(address);
		Assert.assertThat(metaDataPair.getMetaData().getStatus(), IsEqual.equalTo(AccountStatus.UNLOCKED));
	}

	//endregion

	//region accountStatus
	@Test
	public void accountStatusDelegatesToUnlockedAccounts() {
		this.assertAccountStatusDelegatesToUnlockedAccounts(true, AccountStatus.UNLOCKED);
		this.assertAccountStatusDelegatesToUnlockedAccounts(false, AccountStatus.LOCKED);
	}

	private void assertAccountStatusDelegatesToUnlockedAccounts(final boolean returned, final AccountStatus expectedStatus) {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final AccountIdBuilder builder = new AccountIdBuilder();
		builder.setAddress(address.getEncoded());

		final TestContext context = new TestContext();
		Mockito.when(context.unlockedAccounts.isAccountUnlocked(address)).thenReturn(returned);

		// Act:
		final AccountMetaData accountMetaData = context.controller.accountStatus(builder);

		// Assert:
		Mockito.verify(context.unlockedAccounts, Mockito.times(1)).isAccountUnlocked(address);
		Assert.assertThat(accountMetaData.getStatus(), IsEqual.equalTo(expectedStatus));
	}
	//endregion

	private static class TestContext {
		private final AccountInfoController controller;
		private final UnlockedAccounts unlockedAccounts = Mockito.mock(UnlockedAccounts.class);
		private final AccountInfoFactory accountInfoFactory = Mockito.mock(AccountInfoFactory.class);
		private final BlockChainLastBlockLayer blockChainLastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);

		public TestContext() {
			this.controller = new AccountInfoController(
					this.unlockedAccounts,
					this.blockChainLastBlockLayer,
					this.accountInfoFactory);
		}
	}
}
