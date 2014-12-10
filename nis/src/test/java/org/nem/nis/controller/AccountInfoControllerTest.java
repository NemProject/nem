package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.controller.requests.AccountIdBuilder;
import org.nem.nis.harvesting.*;
import org.nem.nis.poi.*;
import org.nem.nis.remote.*;
import org.nem.nis.service.*;

import java.util.*;

public class AccountInfoControllerTest {

	//region accountGet

	@Test
	public void accountGetDelegatesToAccountInfoFactoryForAccountInfo() {
		// Arrange:
		final AccountInfo accountInfo = Mockito.mock(AccountInfo.class);
		final TestContext context = new TestContext();
		context.setRemoteStatus(AccountRemoteStatus.ACTIVATING, 1);
		Mockito.when(context.accountInfoFactory.createInfo(context.address)).thenReturn(accountInfo);

		// Act:
		final AccountMetaDataPair metaDataPair = context.controller.accountGet(context.getBuilder());

		// Assert:
		Assert.assertThat(metaDataPair.getAccount(), IsSame.sameInstance(accountInfo));
		Mockito.verify(context.accountInfoFactory, Mockito.times(1)).createInfo(context.address);
	}

	@Test
	public void accountGetDelegatesToAccountInfoFactoryForRemoteStatus() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setRemoteStatus(AccountRemoteStatus.ACTIVATING, 1);

		// Act:
		final AccountMetaDataPair metaDataPair = context.controller.accountGet(context.getBuilder());

		// Assert:
		context.assertRemoteStatus(metaDataPair.getMetaData(), AccountRemoteStatus.ACTIVATING, 1);
	}

	@Test
	public void accountGetDelegatesToUnlockedAccountsForOverriddenRemoteStatus() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setRemoteStatus(AccountRemoteStatus.ACTIVE, 1);
		context.filteredTransactions.add(createImportanceTransfer(context.address));

		// Act:
		final AccountMetaDataPair metaDataPair = context.controller.accountGet(context.getBuilder());

		// Assert:
		context.assertRemoteStatus(metaDataPair.getMetaData(), AccountRemoteStatus.DEACTIVATING, 1);
	}

	@Test
	public void accountGetDelegatesToUnlockedAccountsForAccountStatus() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setUnlocked(false);

		// Act:
		final AccountMetaDataPair metaDataPair = context.controller.accountGet(context.getBuilder());

		// Assert:
		context.assertUnlocked(metaDataPair.getMetaData(), AccountStatus.LOCKED);
	}

	//endregion

	//region accountStatus

	@Test
	public void accountStatusDelegatesToUnlockedAccountsForUnlockedAccountStatus() {
		assertAccountStatusDelegatesToUnlockedAccounts(true, AccountStatus.UNLOCKED);
	}

	@Test
	public void accountStatusDelegatesToUnlockedAccountsForLockedAccountStatus() {
		assertAccountStatusDelegatesToUnlockedAccounts(false, AccountStatus.LOCKED);
	}

	private static void assertAccountStatusDelegatesToUnlockedAccounts(
			final boolean isAccountUnlockedResult,
			final AccountStatus expectedStatus) {
		// Arrange:
		final TestContext context = new TestContext();
		context.setUnlocked(isAccountUnlockedResult);

		// Act:
		final AccountMetaData accountMetaData = context.controller.accountStatus(context.getBuilder());

		// Assert:
		context.assertUnlocked(accountMetaData, expectedStatus);
	}

	@Test
	public void accountStatusDelegatesToAccountInfoFactoryForRemoteStatus() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setRemoteStatus(AccountRemoteStatus.ACTIVATING, 17);

		// Act:
		final AccountMetaData accountMetaData = context.controller.accountStatus(context.getBuilder());

		// Assert:
		context.assertRemoteStatus(accountMetaData, AccountRemoteStatus.ACTIVATING, 17);
	}

	@Test
	public void accountStatusOverridesInactiveRemoteStatusIfUnconfirmedImportanceTransferIsPending() {
		// Assert:
		assertUnconfirmedImportanceTransferOverridesAccountRemoteStatus(
				AccountRemoteStatus.INACTIVE,
				AccountRemoteStatus.ACTIVATING);
	}

	@Test
	public void accountStatusOverridesActiveRemoteStatusIfUnconfirmedImportanceTransferIsPending() {
		// Assert:
		assertUnconfirmedImportanceTransferOverridesAccountRemoteStatus(
				AccountRemoteStatus.ACTIVE,
				AccountRemoteStatus.DEACTIVATING);
	}

	private static void assertUnconfirmedImportanceTransferOverridesAccountRemoteStatus(
			final AccountRemoteStatus remoteStatus,
			final AccountRemoteStatus expectedRemoteStatus) {
		// Arrange:
		final TestContext context = new TestContext();
		context.setRemoteStatus(remoteStatus, 17);
		context.filteredTransactions.add(createImportanceTransfer(context.address));

		// Act:
		final AccountMetaData accountMetaData = context.controller.accountStatus(context.getBuilder());

		// Assert:
		context.assertRemoteStatus(accountMetaData, expectedRemoteStatus, 17);
	}

	@Test
	public void accountStatusDoesNotOverrideRemoteStatusIfOtherUnconfirmedTransferIsPending() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setRemoteStatus(AccountRemoteStatus.ACTIVE, 17);
		context.filteredTransactions.add(createTransfer(context.address));

		// Act:
		final AccountMetaData accountMetaData = context.controller.accountStatus(context.getBuilder());

		// Assert:
		context.assertRemoteStatus(accountMetaData, AccountRemoteStatus.ACTIVE, 17);
	}

	@Test
	public void accountStatusFailsIfUnconfirmedImportanceTransferIsPendingWithUnexpectedRemoteStatus() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setRemoteStatus(AccountRemoteStatus.ACTIVATING, 17);
		context.filteredTransactions.add(createImportanceTransfer(context.address));

		// Act:
		ExceptionAssert.assertThrows(
				v -> context.controller.accountStatus(context.getBuilder()),
				IllegalStateException.class);
	}

	//endregion

	private static Transaction createTransfer(final Address address) {
		return new TransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				new Account(address),
				Amount.fromNem(1),
				null);
	}

	private static Transaction createImportanceTransfer(final Address address) {
		return new ImportanceTransferTransaction(
				TimeInstant.ZERO,
				new Account(address),
				ImportanceTransferTransaction.Mode.Activate,
				Utils.generateRandomAccount());
	}

	private static class TestContext {
		private final Address address = Utils.generateRandomAddressWithPublicKey();

		private final AccountInfoController controller;
		private final UnlockedAccounts unlockedAccounts = Mockito.mock(UnlockedAccounts.class);
		private final List<Transaction> filteredTransactions = new ArrayList<>();
		private final AccountInfoFactory accountInfoFactory = Mockito.mock(AccountInfoFactory.class);
		private final BlockChainLastBlockLayer blockChainLastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);

		public TestContext() {
			final UnconfirmedTransactions unconfirmedTransactions = Mockito.mock(UnconfirmedTransactions.class);
			final UnconfirmedTransactions filteredUnconfirmedTransactions = Mockito.mock(UnconfirmedTransactions.class);
			Mockito.when(unconfirmedTransactions.getTransactionsForAccount(Mockito.any()))
					.thenReturn(filteredUnconfirmedTransactions);
			Mockito.when(filteredUnconfirmedTransactions.getAll()).thenReturn(this.filteredTransactions);

			this.controller = new AccountInfoController(
					this.unlockedAccounts,
					unconfirmedTransactions,
					this.blockChainLastBlockLayer,
					this.accountInfoFactory,
					this.poiFacade);
		}

		private AccountIdBuilder getBuilder() {
			final AccountIdBuilder builder = new AccountIdBuilder();
			builder.setAddress(this.address.getEncoded());
			return builder;
		}

		private void setRemoteStatus(final AccountRemoteStatus accountRemoteStatus, final long blockHeight) {
			Mockito.when(this.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(blockHeight);

			final RemoteLinks remoteLinks = Mockito.mock(RemoteLinks.class);
			Mockito.when(remoteLinks.getRemoteStatus(new BlockHeight(blockHeight)))
					.thenReturn(getRemoteStatus(accountRemoteStatus));

			final PoiAccountState accountState = Mockito.mock(PoiAccountState.class);
			Mockito.when(accountState.getRemoteLinks()).thenReturn(remoteLinks);

			Mockito.when(this.poiFacade.findStateByAddress(this.address)).thenReturn(accountState);
		}

		private static RemoteStatus getRemoteStatus(final AccountRemoteStatus accountRemoteStatus) {
			switch (accountRemoteStatus) {
				case INACTIVE:
					return RemoteStatus.OWNER_INACTIVE;

				case ACTIVATING:
					return RemoteStatus.OWNER_ACTIVATING;

				case ACTIVE:
					return RemoteStatus.OWNER_ACTIVE;

				case DEACTIVATING:
					return RemoteStatus.OWNER_DEACTIVATING;

				default:
					return RemoteStatus.REMOTE_ACTIVE;
			}
		}

		private void setUnlocked(final boolean isAccountUnlockedResult) {
			Mockito.when(this.unlockedAccounts.isAccountUnlocked(this.address)).thenReturn(isAccountUnlockedResult);
			Mockito.when(this.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(17L);

			// set the remote status to avoid NullPointerException
			this.setRemoteStatus(AccountRemoteStatus.INACTIVE, 17L);
		}

		private void assertRemoteStatus(
				final AccountMetaData accountMetaData,
				final AccountRemoteStatus remoteStatus,
				final long blockHeight) {
			Assert.assertThat(accountMetaData.getRemoteStatus(), IsEqual.equalTo(remoteStatus));
			Mockito.verify(this.poiFacade, Mockito.only()).findStateByAddress(this.address);
			final RemoteLinks remoteLinks = this.poiFacade.findStateByAddress(this.address).getRemoteLinks();
			Mockito.verify(remoteLinks, Mockito.only()).getRemoteStatus(new BlockHeight(blockHeight));
			Mockito.verify(this.blockChainLastBlockLayer, Mockito.only()).getLastBlockHeight();
		}

		private void assertUnlocked(
				final AccountMetaData accountMetaData,
				final AccountStatus status) {
			Assert.assertThat(accountMetaData.getStatus(), IsEqual.equalTo(status));
			Mockito.verify(this.unlockedAccounts, Mockito.times(1)).isAccountUnlocked(this.address);
		}
	}
}
