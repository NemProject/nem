package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.ncc.*;
import org.nem.core.model.ncc.AccountInfo;
import org.nem.core.model.primitive.*;
import org.nem.core.node.NodeFeature;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.controller.requests.AccountHistoricalDataRequestBuilder;
import org.nem.nis.controller.viewmodels.AccountHistoricalDataViewModel;
import org.nem.nis.harvesting.*;
import org.nem.nis.poi.GroupedHeight;
import org.nem.nis.service.*;
import org.nem.nis.state.*;
import org.nem.specific.deploy.NisConfiguration;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

@RunWith(Enclosed.class)
public class AccountInfoControllerTest {

	private static abstract class AccountStatusTestBase {

		//region account status

		@Test
		public void accountStatusDelegatesToUnlockedAccountsForUnlockedAccountStatus() {
			this.assertAccountStatusDelegatesToUnlockedAccounts(true, AccountStatus.UNLOCKED);
		}

		@Test
		public void accountStatusDelegatesToUnlockedAccountsForLockedAccountStatus() {
			this.assertAccountStatusDelegatesToUnlockedAccounts(false, AccountStatus.LOCKED);
		}

		private void assertAccountStatusDelegatesToUnlockedAccounts(
				final boolean isAccountUnlockedResult,
				final AccountStatus expectedStatus) {
			// Arrange:
			final TestContext context = new TestContext();
			context.setUnlocked(isAccountUnlockedResult);

			// Act:
			final AccountMetaData accountMetaData = this.getAccountInfo(context);

			// Assert:
			context.assertUnlocked(accountMetaData, expectedStatus);
		}

		//endregion

		//region remote status

		@Test
		public void accountStatusDelegatesToAccountInfoFactoryForRemoteStatus() {
			// Arrange:
			final TestContext context = new TestContext();
			context.setRemoteStatus(AccountRemoteStatus.ACTIVATING, 17);

			// Act:
			final AccountMetaData accountMetaData = this.getAccountInfo(context);

			// Assert:
			context.assertRemoteStatus(accountMetaData, AccountRemoteStatus.ACTIVATING, 17);
		}

		@Test
		public void accountStatusOverridesInactiveRemoteStatusIfUnconfirmedImportanceTransferIsPending() {
			// Assert:
			this.assertUnconfirmedImportanceTransferOverridesAccountRemoteStatus(
					AccountRemoteStatus.INACTIVE,
					AccountRemoteStatus.ACTIVATING);
		}

		@Test
		public void accountStatusOverridesActiveRemoteStatusIfUnconfirmedImportanceTransferIsPending() {
			// Assert:
			this.assertUnconfirmedImportanceTransferOverridesAccountRemoteStatus(
					AccountRemoteStatus.ACTIVE,
					AccountRemoteStatus.DEACTIVATING);
		}

		private void assertUnconfirmedImportanceTransferOverridesAccountRemoteStatus(
				final AccountRemoteStatus remoteStatus,
				final AccountRemoteStatus expectedRemoteStatus) {
			// Arrange:
			final TestContext context = new TestContext();
			context.setRemoteStatus(remoteStatus, 17);
			context.filteredTransactions.add(createImportanceTransfer(context.address));

			// Act:
			final AccountMetaData accountMetaData = this.getAccountInfo(context);

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
			final AccountMetaData accountMetaData = this.getAccountInfo(context);

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
					v -> this.getAccountInfo(context),
					IllegalStateException.class);
		}

		//endregion

		//region multisig accounts

		@Test
		public void accountStatusCanReturnAccountWithNoAssociatedMultisigAccountsAndNotBeingAnyCosignatory() {
			// Arrange:
			final TestContext context = new TestContext();

			// Act:
			final AccountMetaData accountMetaData = this.getAccountInfo(context);

			// Assert:
			Assert.assertThat(accountMetaData.getCosignatoryOf().size(), IsEqual.equalTo(0));
			Assert.assertThat(accountMetaData.getCosignatories().size(), IsEqual.equalTo(0));
		}

		@Test
		public void accountStatusDelegatesToAccountInfoFactoryForAccountInfoOfAssociatedMultisigAccounts() {
			this.assertAccountStatusDelegatesToAccountInfoFactory(AccountMetaData::getCosignatoryOf, MultisigLinks::addCosignatoryOf);
		}

		@Test
		public void accountStatusDelegatesToAccountInfoFactoryForAccountInfoOfCosignatories() {
			this.assertAccountStatusDelegatesToAccountInfoFactory(AccountMetaData::getCosignatories, MultisigLinks::addCosignatory);
		}

		private void assertAccountStatusDelegatesToAccountInfoFactory(
				final Function<AccountMetaData, List<AccountInfo>> getAccountInfos,
				final BiConsumer<MultisigLinks, Address> addAccountInfo) {
			// Arrange:
			final TestContext context = new TestContext();

			// - set up three account infos (of cosignatoryOf or cosignatory)
			final MultisigLinks multisigLinks = new MultisigLinks();
			final List<Address> addresses = new ArrayList<>();
			final List<AccountInfo> accountInfos = new ArrayList<>();
			for (int i = 0; i < 3; ++i) {
				final Address address = Utils.generateRandomAddress();
				final AccountInfo accountInfo = Mockito.mock(AccountInfo.class);
				Mockito.when(context.accountInfoFactory.createInfo(address)).thenReturn(accountInfo);

				addAccountInfo.accept(multisigLinks, address);
				addresses.add(address);
				accountInfos.add(accountInfo);
			}

			// - set the original account's multisig links
			final ReadOnlyAccountState state = context.accountStateCache.findStateByAddress(context.address);
			Mockito.when(state.getMultisigLinks()).thenReturn(multisigLinks);

			// Act:
			final AccountMetaData accountMetaData = this.getAccountInfo(context);

			// Assert:
			// - each account info was queried and returned
			Assert.assertThat(getAccountInfos.apply(accountMetaData).size(), IsEqual.equalTo(3));
			Assert.assertThat(getAccountInfos.apply(accountMetaData), IsEquivalent.equivalentTo(accountInfos));
			for (final Address address : addresses) {
				Mockito.verify(context.accountInfoFactory, Mockito.times(1)).createInfo(address);
			}
		}

		//endregion

		protected abstract AccountMetaData getAccountInfo(final TestContext context);
	}

	private static abstract class AccountGetTestBase extends AccountStatusTestBase {

		@Test
		public void accountGetDelegatesToAccountInfoFactoryForAccountInfo() {
			// Arrange:
			final AccountInfo accountInfo = Mockito.mock(AccountInfo.class);
			final TestContext context = new TestContext();
			Mockito.when(context.accountInfoFactory.createInfo(context.address)).thenReturn(accountInfo);

			// Act:
			final AccountMetaDataPair metaDataPair = this.getAccountMetaDataPair(context);

			// Assert:
			Assert.assertThat(metaDataPair.getEntity(), IsSame.sameInstance(accountInfo));
			Mockito.verify(context.accountInfoFactory, Mockito.only()).createInfo(context.address);
		}

		@Override
		protected final AccountMetaData getAccountInfo(final TestContext context) {
			return this.getAccountMetaDataPair(context).getMetaData();
		}

		protected abstract AccountMetaDataPair getAccountMetaDataPair(final TestContext context);
	}

	public static class AccountGetTest extends AccountGetTestBase {

		@Override
		protected AccountMetaDataPair getAccountMetaDataPair(final TestContext context) {
			return context.controller.accountGet(context.getBuilder());
		}
	}

	public static class AccountGetFromPublicKeyTest extends AccountGetTestBase {

		@Override
		protected AccountMetaDataPair getAccountMetaDataPair(final TestContext context) {
			return context.controller.accountGetFromPublicKey(context.getPublicKeyBuilder());
		}
	}

	public static class AccountGetMosaicDefinitionsTest {
		@Test
		public void accountGetMosaicDefinitionsDelegatesToNamespaceCache() {
			// Arrange:
			final TestContext context = new TestContext();
			final MosaicId mosaicId1 = context.createMosaicId("gimre.games.pong", "Paddle");
			final MosaicId mosaicId2 = context.createMosaicId("gimre.games.pong", "Ball");
			final MosaicId mosaicId3 = context.createMosaicId("gimre.games.pong", "Goals");
			context.prepareMosaics(Arrays.asList(mosaicId1, mosaicId2, mosaicId3));
			context.ownsMosaic(context.address, Arrays.asList(mosaicId1, mosaicId2));
			final Address another = Utils.generateRandomAddressWithPublicKey();
			context.ownsMosaic(another, Arrays.asList(mosaicId2, mosaicId3));

			// Act:
			final SerializableList<MosaicDefinition> returnedMosaicDefinitions1 = this.getAccountMosaicDefinitions(context, context.address);
			final SerializableList<MosaicDefinition> returnedMosaicDefinitions2 = this.getAccountMosaicDefinitions(context, another);

			// Assert:
			Mockito.verify(context.accountStateCache, Mockito.times(1)).findStateByAddress(context.address);
			Mockito.verify(context.accountStateCache, Mockito.times(1)).findStateByAddress(another);
			context.assertMosaicsOwned(returnedMosaicDefinitions1.asCollection(), Arrays.asList(mosaicId1, mosaicId2));
			context.assertMosaicsOwned(returnedMosaicDefinitions2.asCollection(), Arrays.asList(mosaicId2, mosaicId3));
		}

		@Test
		public void accountGetMosaicDefinitionsBatchDelegatesToNamespaceCache() {
			// Arrange:
			final TestContext context = new TestContext();
			final MosaicId mosaicId1 = context.createMosaicId("gimre.games.pong", "Paddle");
			final MosaicId mosaicId2 = context.createMosaicId("gimre.games.pong", "Ball");
			final MosaicId mosaicId3 = context.createMosaicId("gimre.games.pong", "Goals");
			context.prepareMosaics(Arrays.asList(mosaicId1, mosaicId2, mosaicId3));
			context.ownsMosaic(context.address, Arrays.asList(mosaicId1, mosaicId2));
			final Address another = Utils.generateRandomAddressWithPublicKey();
			context.ownsMosaic(another, Arrays.asList(mosaicId2, mosaicId3));

			// Act:
			final SerializableList<MosaicDefinition> returnedMosaicDefinitions = this.getAccountMosaicDefinitionsBatch(
					context,
					Arrays.asList(context.address, another));

			// Assert:
			Mockito.verify(context.accountStateCache, Mockito.times(1)).findStateByAddress(context.address);
			Mockito.verify(context.accountStateCache, Mockito.times(1)).findStateByAddress(another);
			context.assertMosaicsOwned(returnedMosaicDefinitions.asCollection(), Arrays.asList(mosaicId1, mosaicId2, mosaicId3));
		}

		protected SerializableList<MosaicDefinition> getAccountMosaicDefinitions(final TestContext context, final Address address) {
			return context.controller.accountGetMosaicDefinitions(context.getBuilder(address));
		}

		protected SerializableList<MosaicDefinition> getAccountMosaicDefinitionsBatch(final TestContext context, final List<Address> addresses) {
			return context.controller.accountGetMosaicDefinitionsBatch(
					getAccountIdsDeserializer(addresses.stream().map(a -> new AccountId(a.getEncoded())).collect(Collectors.toList()))
			);
		}
	}

	private static abstract class AccountForwardedGetTestBase {
		private final Address delegatingAddress = Utils.generateRandomAddressWithPublicKey();

		protected Address getDelegatingAddress() {
			return this.delegatingAddress;
		}

		@Test
		public void accountGetForwardedDelegatesToAccountInfoFactoryForAccountInfo() {
			// Arrange:
			final AccountInfo accountInfo = Mockito.mock(AccountInfo.class);
			final TestContext context = new TestContext();
			Mockito.when(accountInfo.getAddress()).thenReturn(context.address);
			Mockito.when(context.accountStateCache.findLatestForwardedStateByAddress(this.delegatingAddress))
					.thenReturn(new AccountState(context.address));
			Mockito.when(context.accountInfoFactory.createInfo(context.address)).thenReturn(accountInfo);

			// Act:
			final AccountMetaDataPair metaDataPair = this.getAccountMetaDataPair(context);

			// Assert:
			Assert.assertThat(metaDataPair.getEntity(), IsSame.sameInstance(accountInfo));
			Mockito.verify(context.accountInfoFactory, Mockito.only()).createInfo(context.address);
			Mockito.verify(context.accountStateCache, Mockito.times(1)).findLatestForwardedStateByAddress(this.delegatingAddress);
		}

		protected final AccountMetaData getAccountInfo(final TestContext context) {
			return this.getAccountMetaDataPair(context).getMetaData();
		}

		protected abstract AccountMetaDataPair getAccountMetaDataPair(final TestContext context);
	}

	public static class AccountGetForwardedTest extends AccountForwardedGetTestBase {

		@Override
		protected AccountMetaDataPair getAccountMetaDataPair(final TestContext context) {
			// Act:
			final AccountIdBuilder builder = new AccountIdBuilder();
			builder.setAddress(this.getDelegatingAddress().getEncoded());
			return context.controller.accountGetForwarded(builder);
		}
	}

	public static class AccountGetForwardedFromPublicKeyTest extends AccountForwardedGetTestBase {

		@Override
		protected AccountMetaDataPair getAccountMetaDataPair(final TestContext context) {
			// Act:
			final PublicKeyBuilder builder = new PublicKeyBuilder();
			builder.setPublicKey(this.getDelegatingAddress().getPublicKey().toString());
			return context.controller.accountGetForwardedFromPublicKey(builder);
		}
	}

	public static class AccountGetBatchTest extends AccountGetTestBase {

		@Override
		protected AccountMetaDataPair getAccountMetaDataPair(final TestContext context) {
			final SerializableList<AccountMetaDataPair> pairs = context.controller.accountGetBatch(context.getAccountIdListDeserializer());
			Assert.assertThat(pairs.size(), IsEqual.equalTo(1));
			return pairs.get(0);
		}

		@Test
		public void accountGetBatchCanReturnInformationAboutMultipleAccounts() {
			// Arrange:
			final TestContext context = new TestContext();
			final List<AccountId> accountIds = new ArrayList<>();
			final List<AccountInfo> accountInfos = new ArrayList<>();
			for (int i = 0; i < 3; ++i) {
				final Address address = Utils.generateRandomAddress();
				accountIds.add(new AccountId(address.getEncoded()));
				accountInfos.add(Mockito.mock(AccountInfo.class));
				context.setRemoteStatus(address, AccountRemoteStatus.ACTIVATING, 1);
				Mockito.when(context.accountInfoFactory.createInfo(address)).thenReturn(accountInfos.get(i));
			}

			final Deserializer deserializer = getAccountIdsDeserializer(accountIds);

			// Act:
			final SerializableList<AccountMetaDataPair> pairs = context.controller.accountGetBatch(deserializer);

			// Assert:
			Assert.assertThat(pairs.size(), IsEqual.equalTo(3));
			Assert.assertThat(
					pairs.asCollection().stream().map(AbstractMetaDataPair::getEntity).collect(Collectors.toList()),
					IsEquivalent.equivalentTo(accountInfos));
			Mockito.verify(context.accountInfoFactory, Mockito.times(3)).createInfo(Mockito.any());
		}
	}

	public static class AccountHistoricalDataGetTest {

		@Test
		public void accountHistoricalDataGetCanReturnSingleHistoricalDataPoint() {
			// Arrange:
			final TestContext context = new TestContext();
			Mockito.when(context.nisConfiguration.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA)).thenReturn(true);
			final BlockHeight height = new BlockHeight(625);
			context.prepareHistoricalData(
					new BlockHeight[] { height },
					new Amount[] { Amount.fromNem(234) },
					new Amount[] { Amount.fromNem(345) },
					new Double[] { 0.456 },
					new Double[] { 0.567 });
			final AccountHistoricalDataRequestBuilder builder = new AccountHistoricalDataRequestBuilder();
			builder.setAddress(context.address.toString());
			builder.setStartHeight("625");
			builder.setEndHeight("625");
			builder.setIncrement("1");

			// Act:
			final SerializableList<AccountHistoricalDataViewModel> viewModels = context.controller.accountHistoricalDataGet(builder);

			// Assert:
			Assert.assertThat(viewModels.size(), IsEqual.equalTo(1));
			final AccountHistoricalDataViewModel viewModel = viewModels.get(0);
			Assert.assertThat(viewModel.getHeight(), IsEqual.equalTo(new BlockHeight(625)));
			Assert.assertThat(viewModel.getAddress(), IsEqual.equalTo(context.address));
			Assert.assertThat(viewModel.getBalance(), IsEqual.equalTo(Amount.fromNem(234 + 345)));
			Assert.assertThat(viewModel.getVestedBalance(), IsEqual.equalTo(Amount.fromNem(234)));
			Assert.assertThat(viewModel.getUnvestedBalance(), IsEqual.equalTo(Amount.fromNem(345)));
			Assert.assertThat(viewModel.getImportance(), IsEqual.equalTo(0.456));
			Assert.assertThat(viewModel.getPageRank(), IsEqual.equalTo(0.567));
		}

		@Test
		public void accountHistoricalDataGetCanReturnMultipleHistoricalDataPoints() {
			// Arrange:
			final TestContext context = new TestContext();
			Mockito.when(context.nisConfiguration.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA)).thenReturn(true);
			final BlockHeight[] heights = { new BlockHeight(200), new BlockHeight(500), new BlockHeight(800) };
			final Amount[] vestedBalances = { Amount.fromNem(234), Amount.fromNem(342), Amount.fromNem(423) };
			final Amount[] unvestedBalances = { Amount.fromNem(345), Amount.fromNem(453), Amount.fromNem(534) };
			final Double[] importances = { 0.456, 0.564, 0.645 };
			final Double[] pageRanks = { 0.567, 0.675, 0.756 };
			context.prepareHistoricalData(
					heights,
					vestedBalances,
					unvestedBalances,
					importances,
					pageRanks);
			final AccountHistoricalDataRequestBuilder builder = new AccountHistoricalDataRequestBuilder();
			builder.setAddress(context.address.toString());
			builder.setStartHeight("200");
			builder.setEndHeight("800");
			builder.setIncrement("300");

			// Act:
			final SerializableList<AccountHistoricalDataViewModel> viewModels = context.controller.accountHistoricalDataGet(builder);

			// Assert:
			Assert.assertThat(viewModels.size(), IsEqual.equalTo(heights.length));
			for (int i = 0; i < heights.length; i++) {
				final AccountHistoricalDataViewModel viewModel = viewModels.get(i);
				Assert.assertThat(viewModel.getHeight(), IsEqual.equalTo(heights[i]));
				Assert.assertThat(viewModel.getAddress(), IsEqual.equalTo(context.address));
				Assert.assertThat(viewModel.getBalance(), IsEqual.equalTo(vestedBalances[i].add(unvestedBalances[i])));
				Assert.assertThat(viewModel.getVestedBalance(), IsEqual.equalTo(vestedBalances[i]));
				Assert.assertThat(viewModel.getUnvestedBalance(), IsEqual.equalTo(unvestedBalances[i]));
				Assert.assertThat(viewModel.getImportance(), IsEqual.equalTo(importances[i]));
				Assert.assertThat(viewModel.getPageRank(), IsEqual.equalTo(pageRanks[i]));
			}
		}

		@Test
		public void accountHistoricalDataGetFailsIfNodeFeatureIsNotSupported() {
			// Arrange:
			final TestContext context = new TestContext();
			Mockito.when(context.nisConfiguration.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA)).thenReturn(false);
			final AccountHistoricalDataRequestBuilder builder = new AccountHistoricalDataRequestBuilder();

			// Assert:
			ExceptionAssert.assertThrows(v -> context.controller.accountHistoricalDataGet(builder), UnsupportedOperationException.class);
		}
	}

	public static class AccountStatusTest extends AccountStatusTestBase {

		@Override
		protected AccountMetaData getAccountInfo(final TestContext context) {
			return context.controller.accountStatus(context.getBuilder());
		}
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
				ImportanceTransferMode.Activate,
				Utils.generateRandomAccount());
	}

	private static class TestContext {
		private final Address address = Utils.generateRandomAddressWithPublicKey();

		private final AccountInfoController controller;
		private final UnlockedAccounts unlockedAccounts = Mockito.mock(UnlockedAccounts.class);
		private final List<Transaction> filteredTransactions = new ArrayList<>();
		private final AccountInfoFactory accountInfoFactory = Mockito.mock(AccountInfoFactory.class);
		private final BlockChainLastBlockLayer blockChainLastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		private final ReadOnlyAccountStateCache accountStateCache = Mockito.mock(ReadOnlyAccountStateCache.class);
		private final NisConfiguration nisConfiguration = Mockito.mock(NisConfiguration.class);
		private final ReadOnlyNamespaceCache namespaceCache = Mockito.mock(ReadOnlyNamespaceCache.class);
		private final HashMap<MosaicId, MosaicDefinition> mosaicDefinitions = new HashMap<>();

		public TestContext() {
			final UnconfirmedTransactionsFilter unconfirmedTransactions = Mockito.mock(UnconfirmedTransactionsFilter.class);
			Mockito.when(unconfirmedTransactions.getMostRecentTransactionsForAccount(Mockito.any(), Mockito.eq(Integer.MAX_VALUE)))
					.thenReturn(this.filteredTransactions);

			this.setRemoteStatus(AccountRemoteStatus.ACTIVATING, 1);
			Mockito.when(this.accountInfoFactory.createInfo(this.address)).thenReturn(Mockito.mock(AccountInfo.class));

			this.controller = new AccountInfoController(
					this.unlockedAccounts,
					unconfirmedTransactions,
					this.blockChainLastBlockLayer,
					this.accountInfoFactory,
					this.accountStateCache,
					this.nisConfiguration,
					this.namespaceCache);
		}

		private AccountIdBuilder getBuilder() {
			return this.getBuilder(this.address);
		}

		private AccountIdBuilder getBuilder(final Address address) {
			final AccountIdBuilder builder = new AccountIdBuilder();
			builder.setAddress(address.getEncoded());
			return builder;
		}

		private PublicKeyBuilder getPublicKeyBuilder() {
			final PublicKeyBuilder builder = new PublicKeyBuilder();
			builder.setPublicKey(this.address.getPublicKey().toString());
			return builder;
		}

		private Deserializer getAccountIdListDeserializer() {
			return getAccountIdsDeserializer(Collections.singletonList(new AccountId(this.address.getEncoded())));
		}

		private void setRemoteStatus(final AccountRemoteStatus accountRemoteStatus, final long blockHeight) {
			this.setRemoteStatus(this.address, accountRemoteStatus, blockHeight);
		}

		private void setRemoteStatus(final Address address, final AccountRemoteStatus accountRemoteStatus, final long blockHeight) {
			Mockito.when(this.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(new BlockHeight(blockHeight));

			final ReadOnlyRemoteLinks remoteLinks = Mockito.mock(RemoteLinks.class);
			Mockito.when(remoteLinks.getRemoteStatus(new BlockHeight(blockHeight)))
					.thenReturn(getRemoteStatus(accountRemoteStatus));

			final ReadOnlyAccountState accountState = Mockito.mock(AccountState.class);
			Mockito.when(accountState.getRemoteLinks()).thenReturn(remoteLinks);
			Mockito.when(accountState.getMultisigLinks()).thenReturn(new MultisigLinks());

			Mockito.when(this.accountStateCache.findStateByAddress(address)).thenReturn(accountState);
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
			Mockito.when(this.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(new BlockHeight(17L));

			// set the remote status to avoid NullPointerException
			this.setRemoteStatus(AccountRemoteStatus.INACTIVE, 17L);
		}

		private void assertRemoteStatus(
				final AccountMetaData accountMetaData,
				final AccountRemoteStatus remoteStatus,
				final long blockHeight) {
			Assert.assertThat(accountMetaData.getRemoteStatus(), IsEqual.equalTo(remoteStatus));
			Mockito.verify(this.accountStateCache, Mockito.only()).findStateByAddress(this.address);
			final ReadOnlyRemoteLinks remoteLinks = this.accountStateCache.findStateByAddress(this.address).getRemoteLinks();
			Mockito.verify(remoteLinks, Mockito.only()).getRemoteStatus(new BlockHeight(blockHeight));
			Mockito.verify(this.blockChainLastBlockLayer, Mockito.only()).getLastBlockHeight();
		}

		private void assertUnlocked(
				final AccountMetaData accountMetaData,
				final AccountStatus status) {
			Assert.assertThat(accountMetaData.getStatus(), IsEqual.equalTo(status));
			Mockito.verify(this.unlockedAccounts, Mockito.times(1)).isAccountUnlocked(this.address);
		}

		private void prepareHistoricalData(
				final BlockHeight[] heights,
				final Amount[] vestedBalances,
				final Amount[] unvestedBalances,
				final Double[] importances,
				final Double[] pageRanks) {
			final ReadOnlyAccountState accountState = Mockito.mock(AccountState.class);
			final WeightedBalances weightedBalances = Mockito.mock(WeightedBalances.class);
			final HistoricalImportances historicalImportances = Mockito.mock(HistoricalImportances.class);
			for (int i = 0; i < heights.length; i++) {
				final BlockHeight groupedHeight = GroupedHeight.fromHeight(heights[i]);
				Mockito.when(this.accountStateCache.findStateByAddress(this.address)).thenReturn(accountState);
				Mockito.when(accountState.getWeightedBalances()).thenReturn(weightedBalances);
				Mockito.when(weightedBalances.getVested(heights[i])).thenReturn(vestedBalances[i]);
				Mockito.when(weightedBalances.getUnvested(heights[i])).thenReturn(unvestedBalances[i]);
				Mockito.when(accountState.getHistoricalImportances()).thenReturn(historicalImportances);
				Mockito.when(historicalImportances.getHistoricalImportance(groupedHeight)).thenReturn(importances[i]);
				Mockito.when(historicalImportances.getHistoricalPageRank(groupedHeight)).thenReturn(pageRanks[i]);
			}

			// set the last block height to the maximum end height used by the tests that call this
			Mockito.when(this.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(new BlockHeight(800));
		}

		public MosaicId createMosaicId(final String namespaceName, final String mosaicName) {
			final MosaicId mosaicId = Utils.createMosaicId(namespaceName, mosaicName);
			this.mosaicDefinitions.put(mosaicId, Mockito.mock(MosaicDefinition.class));
			return mosaicId;
		}

		public void prepareMosaics(final List<MosaicId> mosaicIds) {
			final Set<NamespaceId> uniqueNamespaces = mosaicIds.stream().map(MosaicId::getNamespaceId).collect(Collectors.toSet());
			for (final NamespaceId namespaceId : uniqueNamespaces) {
				final NamespaceEntry namespaceEntry = Mockito.mock(NamespaceEntry.class);
				Mockito.when(this.namespaceCache.get(namespaceId)).thenReturn(namespaceEntry);

				final Mosaics mosaics = Mockito.mock(Mosaics.class);
				Mockito.when(namespaceEntry.getMosaics()).thenReturn(mosaics);
			}
			for (final MosaicId mosaicId : mosaicIds) {
				final ReadOnlyMosaics mosaics = this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics();

				final MosaicEntry mosaicEntry = Mockito.mock(MosaicEntry.class);
				Mockito.when(mosaics.get(mosaicId)).thenReturn(mosaicEntry);

				final MosaicDefinition mosaicDefinition = this.mosaicDefinitions.get(mosaicId);
				Mockito.when(mosaicEntry.getMosaicDefinition()).thenReturn(mosaicDefinition);
			}
		}

		public void ownsMosaic(final Address address, final List<MosaicId> mosaicIds) {
			final ReadOnlyAccountState accountState = Mockito.mock(AccountState.class);
			final org.nem.nis.state.AccountInfo accountInfo = Mockito.mock(org.nem.nis.state.AccountInfo.class);

			Mockito.when(accountState.getAccountInfo()).thenReturn(accountInfo);
			Mockito.when(accountInfo.getMosaicIds()).thenReturn(mosaicIds);
			Mockito.when(this.accountStateCache.findStateByAddress(address)).thenReturn(accountState);
		}

		public void assertMosaicsOwned(final Collection<MosaicDefinition> returnedMosaicDefinitions, final List<MosaicId> expected) {
			Assert.assertThat(returnedMosaicDefinitions.size(), IsEqual.equalTo(expected.size()));

			final Set<MosaicDefinition> definitions = expected.stream().map(this.mosaicDefinitions::get).collect(Collectors.toSet());
			Assert.assertThat(returnedMosaicDefinitions, IsEquivalent.equivalentTo(definitions));
		}
	}

	private static Deserializer getAccountIdsDeserializer(final Collection<AccountId> accountIds) {
		final List<SerializableEntity> serializableAccountIds = accountIds.stream()
				.map(aid -> (SerializableEntity)serializer -> Address.writeTo(serializer, "account", aid.getAddress()))
				.collect(Collectors.toList());
		return Utils.roundtripSerializableEntity(
				new SerializableList<>(serializableAccountIds),
				null);
	}
}
