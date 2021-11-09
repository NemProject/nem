package org.nem.nis.controller;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.ncc.AccountInfo;
import org.nem.core.model.primitive.*;
import org.nem.core.node.NodeFeature;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.controller.requests.*;
import org.nem.nis.controller.viewmodels.AccountHistoricalDataViewModel;
import org.nem.nis.harvesting.*;
import org.nem.nis.pox.poi.GroupedHeight;
import org.nem.nis.service.*;
import org.nem.nis.state.*;
import org.nem.nis.test.NisUtils;
import org.nem.specific.deploy.NisConfiguration;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

@RunWith(Enclosed.class)
public class AccountInfoControllerTest {

	private static abstract class AccountStatusTestBase {

		// region account status

		@Test
		public void accountStatusDelegatesToUnlockedAccountsForUnlockedAccountStatus() {
			this.assertAccountStatusDelegatesToUnlockedAccounts(true, AccountStatus.UNLOCKED);
		}

		@Test
		public void accountStatusDelegatesToUnlockedAccountsForLockedAccountStatus() {
			this.assertAccountStatusDelegatesToUnlockedAccounts(false, AccountStatus.LOCKED);
		}

		private void assertAccountStatusDelegatesToUnlockedAccounts(final boolean isAccountUnlockedResult,
				final AccountStatus expectedStatus) {
			// Arrange:
			final TestContext context = new TestContext();
			context.setUnlocked(isAccountUnlockedResult);

			// Act:
			final AccountMetaData accountMetaData = this.getAccountInfo(context);

			// Assert:
			context.assertUnlocked(accountMetaData, expectedStatus);
		}

		// endregion

		// region remote status

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
			this.assertUnconfirmedImportanceTransferOverridesAccountRemoteStatus(AccountRemoteStatus.INACTIVE,
					AccountRemoteStatus.ACTIVATING);
		}

		@Test
		public void accountStatusOverridesActiveRemoteStatusIfUnconfirmedImportanceTransferIsPending() {
			// Assert:
			this.assertUnconfirmedImportanceTransferOverridesAccountRemoteStatus(AccountRemoteStatus.ACTIVE,
					AccountRemoteStatus.DEACTIVATING);
		}

		private void assertUnconfirmedImportanceTransferOverridesAccountRemoteStatus(final AccountRemoteStatus remoteStatus,
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
			ExceptionAssert.assertThrows(v -> this.getAccountInfo(context), IllegalStateException.class);
		}

		// endregion

		// region multisig accounts

		@Test
		public void accountStatusCanReturnAccountWithNoAssociatedMultisigAccountsAndNotBeingAnyCosignatory() {
			// Arrange:
			final TestContext context = new TestContext();

			// Act:
			final AccountMetaData accountMetaData = this.getAccountInfo(context);

			// Assert:
			MatcherAssert.assertThat(accountMetaData.getCosignatoryOf().size(), IsEqual.equalTo(0));
			MatcherAssert.assertThat(accountMetaData.getCosignatories().size(), IsEqual.equalTo(0));
		}

		@Test
		public void accountStatusDelegatesToAccountInfoFactoryForAccountInfoOfAssociatedMultisigAccounts() {
			this.assertAccountStatusDelegatesToAccountInfoFactory(AccountMetaData::getCosignatoryOf, MultisigLinks::addCosignatoryOf);
		}

		@Test
		public void accountStatusDelegatesToAccountInfoFactoryForAccountInfoOfCosignatories() {
			this.assertAccountStatusDelegatesToAccountInfoFactory(AccountMetaData::getCosignatories, MultisigLinks::addCosignatory);
		}

		private void assertAccountStatusDelegatesToAccountInfoFactory(final Function<AccountMetaData, List<AccountInfo>> getAccountInfos,
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
			MatcherAssert.assertThat(getAccountInfos.apply(accountMetaData).size(), IsEqual.equalTo(3));
			MatcherAssert.assertThat(getAccountInfos.apply(accountMetaData), IsEquivalent.equivalentTo(accountInfos));
			for (final Address address : addresses) {
				Mockito.verify(context.accountInfoFactory, Mockito.times(1)).createInfo(address);
			}
		}

		// endregion

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
			MatcherAssert.assertThat(metaDataPair.getEntity(), IsSame.sameInstance(accountInfo));
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
			MatcherAssert.assertThat(metaDataPair.getEntity(), IsSame.sameInstance(accountInfo));
			Mockito.verify(context.accountInfoFactory, Mockito.only()).createInfo(context.address);
			Mockito.verify(context.accountStateCache, Mockito.times(1)).findLatestForwardedStateByAddress(this.delegatingAddress);
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
			MatcherAssert.assertThat(pairs.size(), IsEqual.equalTo(1));
			return pairs.get(0);
		}

		@Test
		@SuppressWarnings("rawtypes")
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

			final Deserializer deserializer = NisUtils.getAccountIdsDeserializer(accountIds);

			// Act:
			final SerializableList<AccountMetaDataPair> pairs = context.controller.accountGetBatch(deserializer);

			// Assert:
			MatcherAssert.assertThat(pairs.size(), IsEqual.equalTo(3));
			MatcherAssert.assertThat(pairs.asCollection().stream().map(AbstractMetaDataPair::getEntity).collect(Collectors.toList()),
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
			final List<SerializableAccountId> accountIds = Collections.singletonList(new SerializableAccountId(context.address));
			final BlockHeight height = new BlockHeight(625);
			context.prepareHistoricalData(accountIds, new BlockHeight[]{
					height
			}, new Amount[]{
					Amount.fromNem(234)
			}, new Amount[]{
					Amount.fromNem(345)
			}, new Double[]{
					0.456
			}, new Double[]{
					0.567
			});
			final AccountHistoricalDataRequestBuilder builder = new AccountHistoricalDataRequestBuilder();
			builder.setAddress(context.address.toString());
			builder.setStartHeight("625");
			builder.setEndHeight("625");
			builder.setIncrement("1");

			// Act:
			final SerializableList<AccountHistoricalDataViewModel> viewModels = context.controller.accountHistoricalDataGet(builder);

			// Assert:
			MatcherAssert.assertThat(viewModels.size(), IsEqual.equalTo(1));
			final AccountHistoricalDataViewModel viewModel = viewModels.get(0);
			MatcherAssert.assertThat(viewModel.getHeight(), IsEqual.equalTo(new BlockHeight(625)));
			MatcherAssert.assertThat(viewModel.getAddress(), IsEqual.equalTo(context.address));
			MatcherAssert.assertThat(viewModel.getBalance(), IsEqual.equalTo(Amount.fromNem(234 + 345)));
			MatcherAssert.assertThat(viewModel.getVestedBalance(), IsEqual.equalTo(Amount.fromNem(234)));
			MatcherAssert.assertThat(viewModel.getUnvestedBalance(), IsEqual.equalTo(Amount.fromNem(345)));
			MatcherAssert.assertThat(viewModel.getImportance(), IsEqual.equalTo(0.456));
			MatcherAssert.assertThat(viewModel.getPageRank(), IsEqual.equalTo(0.567));
		}

		@Test
		public void accountHistoricalDataGetCanReturnMultipleHistoricalDataPoints() {
			// Arrange:
			final TestContext context = new TestContext();
			Mockito.when(context.nisConfiguration.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA)).thenReturn(true);
			final List<SerializableAccountId> accountIds = Collections.singletonList(new SerializableAccountId(context.address));
			final BlockHeight[] heights = {
					new BlockHeight(200), new BlockHeight(500), new BlockHeight(800)
			};
			final Amount[] vestedBalances = {
					Amount.fromNem(234), Amount.fromNem(342), Amount.fromNem(423)
			};
			final Amount[] unvestedBalances = {
					Amount.fromNem(345), Amount.fromNem(453), Amount.fromNem(534)
			};
			final Double[] importances = {
					0.456, 0.564, 0.645
			};
			final Double[] pageRanks = {
					0.567, 0.675, 0.756
			};
			context.prepareHistoricalData(accountIds, heights, vestedBalances, unvestedBalances, importances, pageRanks);
			final AccountHistoricalDataRequestBuilder builder = new AccountHistoricalDataRequestBuilder();
			builder.setAddress(context.address.toString());
			builder.setStartHeight("200");
			builder.setEndHeight("800");
			builder.setIncrement("300");

			// Act:
			final SerializableList<AccountHistoricalDataViewModel> viewModels = context.controller.accountHistoricalDataGet(builder);

			// Assert:
			MatcherAssert.assertThat(viewModels.size(), IsEqual.equalTo(heights.length));
			for (int i = 0; i < heights.length; i++) {
				final AccountHistoricalDataViewModel viewModel = viewModels.get(i);
				MatcherAssert.assertThat(viewModel.getHeight(), IsEqual.equalTo(heights[i]));
				MatcherAssert.assertThat(viewModel.getAddress(), IsEqual.equalTo(context.address));
				MatcherAssert.assertThat(viewModel.getBalance(), IsEqual.equalTo(vestedBalances[i].add(unvestedBalances[i])));
				MatcherAssert.assertThat(viewModel.getVestedBalance(), IsEqual.equalTo(vestedBalances[i]));
				MatcherAssert.assertThat(viewModel.getUnvestedBalance(), IsEqual.equalTo(unvestedBalances[i]));
				MatcherAssert.assertThat(viewModel.getImportance(), IsEqual.equalTo(importances[i]));
				MatcherAssert.assertThat(viewModel.getPageRank(), IsEqual.equalTo(pageRanks[i]));
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

	public static class AccountHistoricalDataGetBatchTest {
		private static JSONObject createValidJsonObject(final Collection<SerializableAccountId> accountIds, long startHeight,
				long endHeight, long increment) {
			final JsonSerializer serializer = new JsonSerializer();
			serializer.writeObjectArray("accounts", accountIds);
			BlockHeight.writeTo(serializer, "startHeight", new BlockHeight(startHeight));
			BlockHeight.writeTo(serializer, "endHeight", new BlockHeight(endHeight));
			serializer.writeLong("incrementBy", increment);

			return serializer.getObject();
		}

		private static void prepareHistoricalData(final TestContext context, final List<SerializableAccountId> accountIds,
				final BlockHeight[] heights) {
			final Amount[] vestedBalances = new Amount[accountIds.size() * heights.length];
			final Amount[] unvestedBalances = new Amount[accountIds.size() * heights.length];
			final Double[] importances = new Double[accountIds.size() * heights.length];
			final Double[] pageRanks = new Double[accountIds.size() * heights.length];
			for (int i = 0; i < accountIds.size() * heights.length; ++i) {
				vestedBalances[i] = Amount.fromNem(2 * i);
				unvestedBalances[i] = Amount.fromNem(3 * i);
				importances[i] = 4.0 * i;
				pageRanks[i] = 5.0 * i;
			}

			context.prepareHistoricalData(accountIds, heights, vestedBalances, unvestedBalances, importances, pageRanks);
		}

		private static void assertHistoricalData(final SerializableList<SerializableList<AccountHistoricalDataViewModel>> viewModelsList,
				final List<SerializableAccountId> accountIds, final BlockHeight[] heights) {
			// Assert:
			MatcherAssert.assertThat(viewModelsList.size(), IsEqual.equalTo(accountIds.size()));
			for (int i = 0; i < accountIds.size(); ++i) {
				final SerializableList<AccountHistoricalDataViewModel> viewModels = viewModelsList.get(i);
				for (int j = 0; j < heights.length; ++j) {
					final int index = i * heights.length + j;
					final AccountHistoricalDataViewModel viewModel = viewModels.get(j);
					MatcherAssert.assertThat(viewModel.getHeight(), IsEqual.equalTo(heights[j]));
					MatcherAssert.assertThat(viewModel.getAddress(), IsEqual.equalTo(accountIds.get(i).getAddress()));
					MatcherAssert.assertThat(viewModel.getBalance(), IsEqual.equalTo(Amount.fromNem(5 * index)));
					MatcherAssert.assertThat(viewModel.getVestedBalance(), IsEqual.equalTo(Amount.fromNem(2 * index)));
					MatcherAssert.assertThat(viewModel.getUnvestedBalance(), IsEqual.equalTo(Amount.fromNem(3 * index)));
					MatcherAssert.assertThat(viewModel.getImportance(), IsEqual.equalTo(4.0 * index));
					MatcherAssert.assertThat(viewModel.getPageRank(), IsEqual.equalTo(5.0 * index));
				}
			}
		}

		@Test
		public void accountHistoricalDataGetBatchReturnsNoHistoricalDataWhenNoAccountsAreSupplied() {
			// Arrange:
			final TestContext context = new TestContext();
			Mockito.when(context.nisConfiguration.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA)).thenReturn(true);
			final BlockHeight[] heights = new BlockHeight[]{
					new BlockHeight(625)
			};
			final List<SerializableAccountId> accountIds = Collections.emptyList();
			prepareHistoricalData(context, accountIds, heights);
			final Deserializer deserializer = new JsonDeserializer(createValidJsonObject(accountIds, 625, 625, 1), null);

			// Act:
			final SerializableList<SerializableList<AccountHistoricalDataViewModel>> viewModelsList = context.controller
					.accountHistoricalDataGetBatch(deserializer);

			// Assert:
			MatcherAssert.assertThat(viewModelsList.size(), IsEqual.equalTo(0));
		}
		@Test
		public void accountHistoricalDataGetBatchCanReturnHistoricalDataForSingleAccount_SingleHeight() {
			// Arrange:
			final TestContext context = new TestContext();
			Mockito.when(context.nisConfiguration.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA)).thenReturn(true);
			final BlockHeight[] heights = new BlockHeight[]{
					new BlockHeight(625)
			};
			final List<SerializableAccountId> accountIds = Collections
					.singletonList(new SerializableAccountId(Utils.generateRandomAddress()));
			prepareHistoricalData(context, accountIds, heights);
			final Deserializer deserializer = new JsonDeserializer(createValidJsonObject(accountIds, 625, 625, 1), null);

			// Act:
			final SerializableList<SerializableList<AccountHistoricalDataViewModel>> viewModelsList = context.controller
					.accountHistoricalDataGetBatch(deserializer);

			// Assert:
			assertHistoricalData(viewModelsList, accountIds, heights);
		}

		@Test
		public void accountHistoricalDataGetBatchCanReturnHistoricalDataForMultipleAccounts_SingleHeight() {
			// Arrange:
			final TestContext context = new TestContext();
			Mockito.when(context.nisConfiguration.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA)).thenReturn(true);
			final BlockHeight[] heights = new BlockHeight[]{
					new BlockHeight(625)
			};
			final List<SerializableAccountId> accountIds = Arrays.asList(new SerializableAccountId(Utils.generateRandomAddress()),
					new SerializableAccountId(Utils.generateRandomAddress()));
			prepareHistoricalData(context, accountIds, heights);
			final Deserializer deserializer = new JsonDeserializer(createValidJsonObject(accountIds, 625, 625, 1), null);

			// Act:
			final SerializableList<SerializableList<AccountHistoricalDataViewModel>> viewModelsList = context.controller
					.accountHistoricalDataGetBatch(deserializer);

			// Assert:
			assertHistoricalData(viewModelsList, accountIds, heights);
		}

		@Test
		public void accountHistoricalDataGetBatchCanReturnHistoricalDataForSingleAccount_MultipleHeights() {
			// Arrange:
			final TestContext context = new TestContext();
			Mockito.when(context.nisConfiguration.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA)).thenReturn(true);
			final BlockHeight[] heights = new BlockHeight[]{
					new BlockHeight(5), new BlockHeight(5 + 360), new BlockHeight(5 + 2 * 360)
			};
			final List<SerializableAccountId> accountIds = Collections
					.singletonList(new SerializableAccountId(Utils.generateRandomAddress()));
			prepareHistoricalData(context, accountIds, heights);
			final Deserializer deserializer = new JsonDeserializer(createValidJsonObject(accountIds, 5, 5 + 2 * 360, 360), null);

			// Act:
			final SerializableList<SerializableList<AccountHistoricalDataViewModel>> viewModelsList = context.controller
					.accountHistoricalDataGetBatch(deserializer);

			// Assert:
			assertHistoricalData(viewModelsList, accountIds, heights);
		}

		@Test
		public void accountHistoricalDataGetBatchCanReturnHistoricalDataForMultipleAccounts_MultipleHeights() {
			// Arrange:
			final TestContext context = new TestContext();
			Mockito.when(context.nisConfiguration.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA)).thenReturn(true);
			final BlockHeight[] heights = new BlockHeight[]{
					new BlockHeight(5), new BlockHeight(5 + 360), new BlockHeight(5 + 2 * 360)
			};
			final List<SerializableAccountId> accountIds = Arrays.asList(new SerializableAccountId(Utils.generateRandomAddress()),
					new SerializableAccountId(Utils.generateRandomAddress()));
			prepareHistoricalData(context, accountIds, heights);
			final Deserializer deserializer = new JsonDeserializer(createValidJsonObject(accountIds, 5, 5 + 2 * 360, 360), null);

			// Act:
			final SerializableList<SerializableList<AccountHistoricalDataViewModel>> viewModelsList = context.controller
					.accountHistoricalDataGetBatch(deserializer);

			// Assert:
			assertHistoricalData(viewModelsList, accountIds, heights);
		}

		@Test
		public void accountHistoricalDataGetFailsIfNodeFeatureIsNotSupported() {
			// Arrange:
			final TestContext context = new TestContext();
			Mockito.when(context.nisConfiguration.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA)).thenReturn(false);
			final List<SerializableAccountId> accountIds = Collections
					.singletonList(new SerializableAccountId(Utils.generateRandomAddress()));
			final Deserializer deserializer = new JsonDeserializer(createValidJsonObject(accountIds, 5, 5 + 2 * 360, 360), null);

			// Assert:
			ExceptionAssert.assertThrows(v -> context.controller.accountHistoricalDataGetBatch(deserializer),
					UnsupportedOperationException.class);
		}
	}

	public static class AccountStatusTest extends AccountStatusTestBase {

		@Override
		protected AccountMetaData getAccountInfo(final TestContext context) {
			return context.controller.accountStatus(context.getBuilder());
		}
	}

	// endregion

	private static Transaction createTransfer(final Address address) {
		return new TransferTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(), new Account(address), Amount.fromNem(1), null);
	}

	private static Transaction createImportanceTransfer(final Address address) {
		return new ImportanceTransferTransaction(TimeInstant.ZERO, new Account(address), ImportanceTransferMode.Activate,
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
		private final AccountMetaDataFactory accountMetaDataFactory;

		public TestContext() {
			final UnconfirmedTransactionsFilter unconfirmedTransactions = Mockito.mock(UnconfirmedTransactionsFilter.class);
			Mockito.when(unconfirmedTransactions.getMostRecentTransactionsForAccount(Mockito.any(), Mockito.eq(Integer.MAX_VALUE)))
					.thenReturn(this.filteredTransactions);

			accountMetaDataFactory = new AccountMetaDataFactory(this.accountInfoFactory, this.unlockedAccounts, unconfirmedTransactions,
					this.blockChainLastBlockLayer, this.accountStateCache);

			this.setRemoteStatus(AccountRemoteStatus.ACTIVATING, 1);
			Mockito.when(this.accountInfoFactory.createInfo(this.address)).thenReturn(Mockito.mock(AccountInfo.class));

			this.controller = new AccountInfoController(this.blockChainLastBlockLayer, this.accountInfoFactory, this.accountMetaDataFactory,
					this.accountStateCache, this.nisConfiguration);
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
			return NisUtils.getAccountIdsDeserializer(Collections.singletonList(new AccountId(this.address.getEncoded())));
		}

		private void setRemoteStatus(final AccountRemoteStatus accountRemoteStatus, final long blockHeight) {
			this.setRemoteStatus(this.address, accountRemoteStatus, blockHeight);
		}

		private void setRemoteStatus(final Address address, final AccountRemoteStatus accountRemoteStatus, final long blockHeight) {
			Mockito.when(this.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(new BlockHeight(blockHeight));

			final ReadOnlyRemoteLinks remoteLinks = Mockito.mock(RemoteLinks.class);
			Mockito.when(remoteLinks.getRemoteStatus(new BlockHeight(blockHeight))).thenReturn(getRemoteStatus(accountRemoteStatus));

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

				default :
					return RemoteStatus.REMOTE_ACTIVE;
			}
		}

		private void setUnlocked(final boolean isAccountUnlockedResult) {
			Mockito.when(this.unlockedAccounts.isAccountUnlocked(this.address)).thenReturn(isAccountUnlockedResult);
			Mockito.when(this.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(new BlockHeight(17L));

			// set the remote status to avoid NullPointerException
			this.setRemoteStatus(AccountRemoteStatus.INACTIVE, 17L);
		}

		private void assertRemoteStatus(final AccountMetaData accountMetaData, final AccountRemoteStatus remoteStatus,
				final long blockHeight) {
			MatcherAssert.assertThat(accountMetaData.getRemoteStatus(), IsEqual.equalTo(remoteStatus));
			Mockito.verify(this.accountStateCache, Mockito.only()).findStateByAddress(this.address);
			final ReadOnlyRemoteLinks remoteLinks = this.accountStateCache.findStateByAddress(this.address).getRemoteLinks();
			Mockito.verify(remoteLinks, Mockito.only()).getRemoteStatus(new BlockHeight(blockHeight));
			Mockito.verify(this.blockChainLastBlockLayer, Mockito.only()).getLastBlockHeight();
		}

		private void assertUnlocked(final AccountMetaData accountMetaData, final AccountStatus status) {
			MatcherAssert.assertThat(accountMetaData.getStatus(), IsEqual.equalTo(status));
			Mockito.verify(this.unlockedAccounts, Mockito.times(1)).isAccountUnlocked(this.address);
		}

		private void prepareHistoricalData(final Collection<SerializableAccountId> accountIds, final BlockHeight[] heights,
				final Amount[] vestedBalances, final Amount[] unvestedBalances, final Double[] importances, final Double[] pageRanks) {
			int counter = 0;
			for (final AccountId accountId : accountIds) {
				final ReadOnlyAccountState accountState = Mockito.mock(AccountState.class);
				final WeightedBalances weightedBalances = Mockito.mock(WeightedBalances.class);
				final HistoricalImportances historicalImportances = Mockito.mock(HistoricalImportances.class);
				for (int i = 0; i < heights.length; i++) {
					final BlockHeight groupedHeight = GroupedHeight.fromHeight(heights[i]);
					Mockito.when(this.accountStateCache.findStateByAddress(accountId.getAddress())).thenReturn(accountState);
					Mockito.when(accountState.getWeightedBalances()).thenReturn(weightedBalances);
					Mockito.when(weightedBalances.getVested(heights[i])).thenReturn(vestedBalances[i + counter * heights.length]);
					Mockito.when(weightedBalances.getUnvested(heights[i])).thenReturn(unvestedBalances[i + counter * heights.length]);
					Mockito.when(accountState.getHistoricalImportances()).thenReturn(historicalImportances);
					Mockito.when(historicalImportances.getHistoricalImportance(groupedHeight))
							.thenReturn(importances[i + counter * heights.length]);
					Mockito.when(historicalImportances.getHistoricalPageRank(groupedHeight))
							.thenReturn(pageRanks[i + counter * heights.length]);
				}

				counter++;
			}

			// set the last block height to the maximum end height used by the tests that call this
			Mockito.when(this.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(new BlockHeight(800));
		}
	}
}
