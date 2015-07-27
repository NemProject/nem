package org.nem.nis.secret;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.*;

import java.util.*;
import java.util.function.Function;

public class BlockTransactionObserverFactoryTest {
	private static final EnumSet<ObserverOption> OPTIONS_NO_INCREMENTAL_POI = EnumSet.of(ObserverOption.NoIncrementalPoi);

	//region basic

	@Test
	public void createExecuteCommitObserverReturnsValidObserver() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory();

		// Act:
		final BlockTransactionObserver observer = factory.createExecuteCommitObserver(context.nisCache);

		// Assert:
		Assert.assertThat(observer, IsNull.notNullValue());
		assertAreEquivalent(observer.getName(), getDefaultObserverNames());
	}

	@Test
	public void createExecuteCommitObserverWithNoIncrementalPoiReturnsValidObserver() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory(OPTIONS_NO_INCREMENTAL_POI);

		// Act:
		final BlockTransactionObserver observer = factory.createExecuteCommitObserver(context.nisCache);

		// Assert:
		Assert.assertThat(observer, IsNull.notNullValue());
		assertAreEquivalent(observer.getName(), getBaseObserverNames());
	}

	@Test
	public void createUndoCommitObserverReturnsValidObserver() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory();

		// Act:
		final BlockTransactionObserver observer = factory.createUndoCommitObserver(context.nisCache);

		// Assert:
		Assert.assertThat(observer, IsNull.notNullValue());
		assertAreEquivalent(observer.getName(), getDefaultObserverNames());
	}

	@Test
	public void createUndoCommitObserverWithNoIncrementalPoiReturnsValidObserver() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory(OPTIONS_NO_INCREMENTAL_POI);

		// Act:
		final BlockTransactionObserver observer = factory.createUndoCommitObserver(context.nisCache);

		// Assert:
		Assert.assertThat(observer, IsNull.notNullValue());
		assertAreEquivalent(observer.getName(), getBaseObserverNames());
	}

	private static Collection<String> getDefaultObserverNames() {
		final Collection<String> expectedClasses = getBaseObserverNames();
		expectedClasses.add("RecalculateImportancesObserver");
		return expectedClasses;
	}

	private static Collection<String> getBaseObserverNames() {
		return new ArrayList<String>() {
			{
				this.add("WeightedBalancesObserver");
				this.add("AccountsHeightObserver");
				this.add("BalanceCommitTransferObserver");
				this.add("HarvestRewardCommitObserver");
				this.add("RemoteObserver");
				this.add("MultisigCosignatoryModificationObserver");
				this.add("MultisigMinCosignatoriesModificationObserver");
				this.add("OutlinkObserver");
				this.add("TransactionHashesObserver");
				this.add("ProvisionNamespaceObserver");
				this.add("MosaicDefinitionCreationObserver");
				this.add("MosaicSupplyChangeObserver");
				this.add("MosaicTransferObserver");
				this.add("AccountInfoMosaicIdsObserver");

				this.add("AccountStateCachePruningObserver");
				this.add("NamespaceCachePruningObserver");
				this.add("TransactionHashCachePruningObserver");
			}
		};
	}

	private static void assertAreEquivalent(final String name, final Collection<String> expectedSubObserverNames) {
		// Act:
		final List<String> subObserverNames = Arrays.asList(name.split(","));

		// Assert:
		Assert.assertThat(subObserverNames, IsEquivalent.equivalentTo(expectedSubObserverNames));
	}

	//endregion

	//region options

	@Test
	public void createExecuteDoesPerformIncrementalPoiWhenEnabled() {
		// Arrange:
		final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory();

		// Assert:
		assertRecalculateImportancesIsCalled(factory::createExecuteCommitObserver, NotificationTrigger.Execute);
	}

	@Test
	public void createExecuteDoesNotPerformIncrementalPoiWhenDisabled() {
		// Arrange:
		final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory(OPTIONS_NO_INCREMENTAL_POI);

		// Assert:
		assertRecalculateImportancesIsNotCalled(factory::createExecuteCommitObserver, NotificationTrigger.Execute);
	}

	@Test
	public void createUndoDoesPerformIncrementalPoiWhenEnabled() {
		// Arrange:
		final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory();

		// Assert:
		assertRecalculateImportancesIsCalled(factory::createUndoCommitObserver, NotificationTrigger.Undo);
	}

	@Test
	public void createUndoDoesNotPerformIncrementalPoiWhenDisabled() {
		// Arrange:
		final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory(OPTIONS_NO_INCREMENTAL_POI);

		// Assert:
		assertRecalculateImportancesIsNotCalled(factory::createUndoCommitObserver, NotificationTrigger.Undo);
	}

	private static void assertRecalculateImportancesIsCalled(
			final Function<NisCache, BlockTransactionObserver> createObserver,
			final NotificationTrigger trigger) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final BlockTransactionObserver observer = createObserver.apply(context.nisCache);
		notifyHarvestReward(observer, context.accountContext1.account, trigger);

		// Assert:
		Mockito.verify(context.poiFacade, Mockito.only()).recalculateImportances(Mockito.any(), Mockito.any());
	}

	private static void assertRecalculateImportancesIsNotCalled(
			final Function<NisCache, BlockTransactionObserver> createObserver,
			final NotificationTrigger trigger) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final BlockTransactionObserver observer = createObserver.apply(context.nisCache);
		notifyHarvestReward(observer, context.accountContext1.account, trigger);

		// Assert:
		Mockito.verify(context.poiFacade, Mockito.never()).recalculateImportances(Mockito.any(), Mockito.any());
	}

	private static void notifyHarvestReward(final BlockTransactionObserver observer, final Account account, final NotificationTrigger trigger) {
		observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BlockHarvest, account, Amount.ZERO),
				new BlockNotificationContext(BlockHeight.ONE, TimeInstant.ZERO, trigger));
	}

	//endregion

	//region outlink side effects

	@Test
	public void executeUpdatesOutlinks() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserver observer = context.factory.createExecuteCommitObserver(context.nisCache);

		// Act:
		observer.notify(
				new BalanceTransferNotification(context.accountContext1.account, context.accountContext2.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.accountContext1.importance, Mockito.times(1)).addOutlink(Mockito.any());
		Mockito.verify(context.accountContext2.importance, Mockito.times(0)).addOutlink(Mockito.any());
	}

	@Test
	public void undoUpdatesOutlinks() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserver observer = context.factory.createUndoCommitObserver(context.nisCache);

		// Act:
		observer.notify(
				new BalanceTransferNotification(context.accountContext2.account, context.accountContext1.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Undo));

		// Assert:
		Mockito.verify(context.accountContext1.importance, Mockito.times(1)).removeOutlink(Mockito.any());
		Mockito.verify(context.accountContext2.importance, Mockito.times(0)).removeOutlink(Mockito.any());
	}

	//endregion

	//region weighted balance side effects

	@Test
	public void executeUpdatesWeightedBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserver observer = context.factory.createExecuteCommitObserver(context.nisCache);

		// Act:
		observer.notify(
				new BalanceTransferNotification(context.accountContext1.account, context.accountContext2.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));
		observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceDebit, context.accountContext1.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.accountContext1.balances, Mockito.times(2)).addSend(Mockito.any(), Mockito.any());
		Mockito.verify(context.accountContext2.balances, Mockito.times(1)).addReceive(Mockito.any(), Mockito.any());
	}

	@Test
	public void undoUpdatesWeightedBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserver observer = context.factory.createUndoCommitObserver(context.nisCache);

		// Act:
		observer.notify(
				new BalanceTransferNotification(context.accountContext2.account, context.accountContext1.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Undo));
		observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceCredit, context.accountContext1.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Undo));

		// Assert:
		Mockito.verify(context.accountContext1.balances, Mockito.times(2)).undoSend(Mockito.any(), Mockito.any());
		Mockito.verify(context.accountContext2.balances, Mockito.times(1)).undoReceive(Mockito.any(), Mockito.any());
	}

	//endregion

	//region execute / undo outlink update ordering

	@Test
	public void executeAddsOutlinkAfterUpdatingBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<String> breadcrumbs = new ArrayList<>();
		final Function<String, Answer> createAnswer = breadcrumb -> invocationOnMock -> {
			breadcrumbs.add(breadcrumb);
			return null;
		};

		Mockito.doAnswer(createAnswer.apply("outlink")).when(context.accountContext1.importance).addOutlink(Mockito.any());
		Mockito.doAnswer(createAnswer.apply("balance")).when(context.accountContext1.accountInfo).decrementBalance(Mockito.any());
		Mockito.doAnswer(createAnswer.apply("weighted-balance")).when(context.accountContext1.balances).addSend(Mockito.any(), Mockito.any());

		// Act:
		final BlockTransactionObserver observer = context.factory.createExecuteCommitObserver(context.nisCache);
		observer.notify(
				new BalanceTransferNotification(context.accountContext1.account, context.accountContext2.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		Assert.assertThat(breadcrumbs, IsEqual.equalTo(Arrays.asList("weighted-balance", "balance", "outlink")));
	}

	@Test
	public void undoRemovesOutlinkBeforeUpdatingBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<String> breadcrumbs = new ArrayList<>();
		final Function<String, Answer> createAnswer = breadcrumb -> invocationOnMock -> {
			breadcrumbs.add(breadcrumb);
			return null;
		};

		Mockito.doAnswer(createAnswer.apply("outlink")).when(context.accountContext1.importance).removeOutlink(Mockito.any());
		Mockito.doAnswer(createAnswer.apply("balance")).when(context.accountContext1.accountInfo).incrementBalance(Mockito.any());
		Mockito.doAnswer(createAnswer.apply("weighted-balance")).when(context.accountContext1.balances).undoSend(Mockito.any(), Mockito.any());

		// Act:
		final BlockTransactionObserver observer = context.factory.createUndoCommitObserver(context.nisCache);
		observer.notify(
				new BalanceTransferNotification(context.accountContext2.account, context.accountContext1.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Undo));

		// Assert:
		Assert.assertThat(breadcrumbs, IsEqual.equalTo(Arrays.asList("outlink", "balance", "weighted-balance")));
	}

	//endregion

	private static class MockAccountContext {
		private final Account account = Mockito.mock(Account.class);
		private final AccountInfo accountInfo = Mockito.mock(AccountInfo.class);
		private final AccountImportance importance = Mockito.mock(AccountImportance.class);
		private final WeightedBalances balances = Mockito.mock(WeightedBalances.class);
		private final Address address = Utils.generateRandomAddress();

		public MockAccountContext(final AccountStateCache accountStateCache) {
			Mockito.when(this.account.getAddress()).thenReturn(this.address);

			final AccountState accountState = Mockito.mock(AccountState.class);
			Mockito.when(accountState.getAccountInfo()).thenReturn(this.accountInfo);
			Mockito.when(accountState.getWeightedBalances()).thenReturn(this.balances);
			Mockito.when(accountState.getImportanceInfo()).thenReturn(this.importance);
			Mockito.when(accountState.getImportanceInfo()).thenReturn(this.importance);
			Mockito.when(accountStateCache.mutableContents()).thenReturn(new CacheContents<>(new ArrayList<>()));

			Mockito.when(accountStateCache.findStateByAddress(this.address)).thenReturn(accountState);
		}
	}

	private static class TestContext {
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final DefaultPoiFacade poiFacade = Mockito.mock(DefaultPoiFacade.class);
		private final MockAccountContext accountContext1 = this.addAccount();
		private final MockAccountContext accountContext2 = this.addAccount();
		private final NisCache nisCache = NisCacheFactory.create(this.accountStateCache, this.poiFacade);
		private final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory();

		private MockAccountContext addAccount() {
			return new MockAccountContext(this.accountStateCache);
		}
	}
}