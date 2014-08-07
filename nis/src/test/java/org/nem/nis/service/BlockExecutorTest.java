package org.nem.nis.service;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.secret.*;

import java.util.*;
import java.util.function.Consumer;

public class BlockExecutorTest {


	//region execute / undo basic updates

	@Test
	public void executeIncrementsForagedBlocks() {
		// Arrange: initial foraged blocks = 3
		final UndoExecuteTestContext context = new UndoExecuteTestContext();

		// Act:
		context.execute();

		// Assert:
		Assert.assertThat(context.account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(4)));
	}

	@Test
	public void executeIncrementsForagerBalanceByTotalFee() {
		// Arrange: initial balance = 100, total fee = 28
		final UndoExecuteTestContext context = new UndoExecuteTestContext();

		// Act:
		context.execute();

		// Assert:
		Assert.assertThat(context.account.getBalance(), IsEqual.equalTo(new Amount(128)));
	}

	@Test
	public void executeCallsExecuteOnAllTransactionsInForwardOrder() {
		// Arrange:
		final UndoExecuteTestContext context = new UndoExecuteTestContext();

		// Act:
		context.execute();

		// Assert:
		Assert.assertThat(context.executeList, IsEquivalent.equivalentTo(new Integer[]{ 1, 2 }));
	}

	@Test
	public void undoDecrementsForagedBlocks() {
		// Arrange: initial foraged blocks = 3
		final UndoExecuteTestContext context = new UndoExecuteTestContext();

		// Act:
		context.execute();
		context.undo();

		// Assert:
		Assert.assertThat(context.account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(3)));
	}

	@Test
	public void undoDecrementsForagerBalanceByTotalFee() {
		// Arrange: initial balance = 100, total fee = 28
		final UndoExecuteTestContext context = new UndoExecuteTestContext();

		// Act:
		context.execute();
		context.undo();

		// Assert:
		Assert.assertThat(context.account.getBalance(), IsEqual.equalTo(new Amount(100)));
	}

	@Test
	public void undoCallsUndoOnAllTransactionsInReverseOrder() {
		// Arrange:
		final UndoExecuteTestContext context = new UndoExecuteTestContext();

		// Act:
		context.execute();
		context.undo();

		// Assert:
		Assert.assertThat(context.undoList, IsEquivalent.equivalentTo(new Integer[] { 2, 1 }));
	}

	private final class UndoExecuteTestContext {
		private final Account account;
		private final Block block;
		private final MockTransaction transaction1;
		private final MockTransaction transaction2;
		private final List<Integer> executeList = new ArrayList<>();
		private final List<Integer> undoList = new ArrayList<>();
		private final BlockExecutor executor = new BlockExecutor();

		public UndoExecuteTestContext() {
			this.account = Utils.generateRandomAccount();
			this.account.incrementBalance(new Amount(100));
			for (int i = 0; i < 3; ++i)
				this.account.incrementForagedBlocks();

			this.transaction1 = createTransaction(1, 17);
			this.transaction2 = createTransaction(2, 11);

			this.block = BlockUtils.createBlock(this.account);
			this.block.addTransaction(this.transaction1);
			this.block.addTransaction(this.transaction2);

			this.account.getWeightedBalances().addReceive(BlockHeight.ONE, new Amount(100));
		}

		private void execute() {
			this.executor.execute(this.block);
		}

		private void undo() {
			this.executor.undo(this.block);
		}

		private MockTransaction createTransaction(final int customField, final long fee) {
			final MockTransaction transaction = BlockUtils.createTransactionWithFee(customField, fee);
			transaction.setExecuteList(this.executeList);
			transaction.setUndoList(this.undoList);
			return transaction;
		}
	}

	//endregion

	//region execute / undo notifications

	@Test
	public void executeDelegatesToSubscribedObserver() {
		// Arrange:
		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);

		// Assert:
		assertExecuteNotificationForObservers(Arrays.asList(observer));
	}

	@Test
	public void executeDelegatesToAllSubscribedObservers() {
		// Arrange:
		final List<BlockTransferObserver> observers = Arrays.asList(
				Mockito.mock(BlockTransferObserver.class),
				Mockito.mock(BlockTransferObserver.class),
				Mockito.mock(BlockTransferObserver.class));

		// Assert:
		assertExecuteNotificationForObservers(observers);
	}

	@Test
	public void undoDelegatesToSubscribedObserver() {
		// Arrange:
		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);

		// Assert:
		assertUndoNotificationForObservers(Arrays.asList(observer));
	}

	@Test
	public void undoDelegatesToAllSubscribedObservers() {
		// Arrange:
		final List<BlockTransferObserver> observers = Arrays.asList(
				Mockito.mock(BlockTransferObserver.class),
				Mockito.mock(BlockTransferObserver.class),
				Mockito.mock(BlockTransferObserver.class));

		// Assert:
		assertUndoNotificationForObservers(observers);
	}

	private static void assertExecuteNotificationForObservers(final List<BlockTransferObserver> observers) {
		// Arrange:
		final Account account1 = new MockAccountContext().account;
		final Account account2 = new MockAccountContext().account;

		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);
		transaction.setFee(Amount.fromNem(7));
		transaction.setTransferAction(to -> {
			to.notifyTransfer(account1, account2, Amount.fromNem(12));
			to.notifyCredit(account1, Amount.fromNem(9));
			to.notifyDebit(account1, Amount.fromNem(11));
		});

		final BlockHeight height = new BlockHeight(11);
		final Block block = createBlockWithTransaction(height, Amount.fromNem(7), transaction);
		final BlockExecutor executor = createBlockExecutor();

		// Act:
		executor.execute(block, observers);

		// Assert:
		Assert.assertThat(observers.size() > 0, IsEqual.equalTo(true));
		for (final BlockTransferObserver observer : observers) {
			// transaction transfer action
			Mockito.verify(observer, Mockito.times(1)).notifySend(height, account1, Amount.fromNem(12));
			Mockito.verify(observer, Mockito.times(1)).notifyReceive(height, account2, Amount.fromNem(12));
			Mockito.verify(observer, Mockito.times(1)).notifySend(height, account1, Amount.fromNem(11));
			Mockito.verify(observer, Mockito.times(1)).notifyReceive(height, account1, Amount.fromNem(9));

			// signer fee
			Mockito.verify(observer, Mockito.times(1)).notifyReceive(height, block.getSigner(), Amount.fromNem(7));

			// total call counts
			verifyCallCounts(observer, 2, 3, 0, 0);
		}
	}

	private static void assertUndoNotificationForObservers(final List<BlockTransferObserver> observers) {
		// Arrange:
		final Account account1 = new MockAccountContext().account;
		final Account account2 = new MockAccountContext().account;

		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);
		transaction.setFee(Amount.fromNem(7));
		transaction.setTransferAction(to -> {
			to.notifyTransfer(account2, account1, Amount.fromNem(12));
			to.notifyDebit(account1, Amount.fromNem(9));
			to.notifyCredit(account1, Amount.fromNem(11));
		});

		final BlockHeight height = new BlockHeight(11);
		final Block block = createBlockWithTransaction(height, Amount.fromNem(7), transaction);
		final BlockExecutor executor = createBlockExecutor();
		executor.execute(block);

		// Act:
		executor.undo(block, observers);

		// Assert:
		Assert.assertThat(observers.size() > 0, IsEqual.equalTo(true));
		for (final BlockTransferObserver observer : observers) {
			// transaction transfer action
			Mockito.verify(observer, Mockito.times(1)).notifyReceiveUndo(height, account1, Amount.fromNem(12));
			Mockito.verify(observer, Mockito.times(1)).notifySendUndo(height, account2, Amount.fromNem(12));
			Mockito.verify(observer, Mockito.times(1)).notifyReceiveUndo(height, account1, Amount.fromNem(11));
			Mockito.verify(observer, Mockito.times(1)).notifySendUndo(height, account1, Amount.fromNem(9));

			// signer fee
			Mockito.verify(observer, Mockito.times(1)).notifyReceiveUndo(height, block.getSigner(), Amount.fromNem(7));

			// total call counts
			verifyCallCounts(observer, 0, 0, 2, 3);
		}
	}

	private static void verifyCallCounts(
			final BlockTransferObserver observer,
			final int notifySendCounts,
			final int notifyReceiveCounts,
			final int notifySendUndoCounts,
			final int notifyReceiveUndoCounts) {
		Mockito.verify(observer, Mockito.times(notifySendCounts)).notifySend(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.times(notifyReceiveCounts)).notifyReceive(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.times(notifySendUndoCounts)).notifySendUndo(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.times(notifyReceiveUndoCounts)).notifyReceiveUndo(Mockito.any(), Mockito.any(), Mockito.any());
	}

	private static Block createBlockWithTransaction(final BlockHeight height, final Amount amount, final Transaction transaction) {
		final Block block = BlockUtils.createBlockWithHeight(height);
		block.getSigner().incrementBalance(amount);
		block.getSigner().getWeightedBalances().addReceive(BlockHeight.ONE, amount);
		block.addTransaction(transaction);
		return block;
	}

	private static Block createBlockWithDefaultTransaction(
			final BlockHeight height,
			final Account account1,
			final Account account2) {
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);
		transaction.setFee(Amount.fromNem(7));
		transaction.setTransferAction(to -> {
			to.notifyTransfer(account1, account2, Amount.fromNem(12));
			to.notifyDebit(account1, Amount.fromNem(9));
			to.notifyCredit(account1, Amount.fromNem(11));
		});

		return createBlockWithTransaction(height, Amount.fromNem(7), transaction);
	}

	//endregion

	//region execute / undo notification updates

	@Test
	public void executeUpdatesOutlinks() {
		// Arrange:
		final MockAccountContext accountContext1 = new MockAccountContext();
		final MockAccountContext accountContext2 = new MockAccountContext();

		final BlockHeight height = new BlockHeight(11);
		final Block block = createBlockWithDefaultTransaction(height, accountContext1.account, accountContext2.account);
		final BlockExecutor executor = createBlockExecutor();

		// Act:
		executor.execute(block);

		// Assert:
		Mockito.verify(accountContext1.importance, Mockito.times(1)).addOutlink(Mockito.any());
		Mockito.verify(accountContext2.importance, Mockito.times(0)).addOutlink(Mockito.any());
	}

	@Test
	public void undoUpdatesOutlinks() {
		// Arrange:
		final MockAccountContext accountContext1 = new MockAccountContext();
		final MockAccountContext accountContext2 = new MockAccountContext();

		final BlockHeight height = new BlockHeight(11);
		final Block block = createBlockWithDefaultTransaction(height, accountContext1.account, accountContext2.account);
		final BlockExecutor executor = createBlockExecutor();

		// Act:
		executor.execute(block);
		executor.undo(block);

		// Assert:
		Mockito.verify(accountContext1.importance, Mockito.times(1)).removeOutlink(Mockito.any());
		Mockito.verify(accountContext2.importance, Mockito.times(0)).removeOutlink(Mockito.any());
	}

	@Test
	public void executeUpdatesWeightedBalances() {
		// Arrange:
		final MockAccountContext accountContext1 = new MockAccountContext();
		final MockAccountContext accountContext2 = new MockAccountContext();

		final BlockHeight height = new BlockHeight(11);
		final Block block = createBlockWithDefaultTransaction(height, accountContext1.account, accountContext2.account);
		final BlockExecutor executor = createBlockExecutor();

		// Act:
		executor.execute(block);

		// Assert:
		Mockito.verify(accountContext1.balances, Mockito.times(2)).addSend(Mockito.any(), Mockito.any());
		Mockito.verify(accountContext2.balances, Mockito.times(1)).addReceive(Mockito.any(), Mockito.any());
	}

	@Test
	public void undoUpdatesWeightedBalances() {
		// Arrange:
		final MockAccountContext accountContext1 = new MockAccountContext();
		final MockAccountContext accountContext2 = new MockAccountContext();

		final BlockHeight height = new BlockHeight(11);
		final Block block = createBlockWithDefaultTransaction(height, accountContext1.account, accountContext2.account);
		final BlockExecutor executor = createBlockExecutor();

		// Act:
		executor.execute(block);
		executor.undo(block);

		// Assert:
		Mockito.verify(accountContext1.balances, Mockito.times(2)).undoSend(Mockito.any(), Mockito.any());
		Mockito.verify(accountContext2.balances, Mockito.times(1)).undoReceive(Mockito.any(), Mockito.any());
	}

	private static class MockAccountContext {
		private final Account account;
		private final AccountImportance importance;
		private final WeightedBalances balances;
		private final Address address;

		public MockAccountContext() {
			this.account = Mockito.mock(Account.class);
			this.importance = Mockito.mock(AccountImportance.class);
			this.balances = Mockito.mock(WeightedBalances.class);
			this.address = Mockito.mock(Address.class);

			Mockito.when(this.account.getAddress()).thenReturn(Utils.generateRandomAddress());
			Mockito.when(this.account.getImportanceInfo()).thenReturn(this.importance);
			Mockito.when(this.account.getWeightedBalances()).thenReturn(this.balances);
			Mockito.when(this.account.getAddress()).thenReturn(this.address);
		}
	}

	//endregion

	//region execute / undo outlink update ordering

	@Test
	public void executeAddsOutlinkAfterUpdatingBalances() {
		// Arrange:
		final Account foragerAccount = Utils.generateRandomAccount();
		foragerAccount.incrementBalance(Amount.fromNem(50));
		foragerAccount.getWeightedBalances().addReceive(BlockHeight.ONE, Amount.fromNem(50));
		final Block block = BlockUtils.createBlock(foragerAccount);
		block.addTransaction(BlockUtils.createTransactionWithFee(Amount.fromNem(2)));
		final BlockExecutor executor = createBlockExecutor();

		final Amount[] balanceInObserver = new Amount[1];
		final BlockTransferObserver observer = new BlockTransferObserver() {
			@Override
			public void notifySend(final BlockHeight height, final Account account, final Amount amount) {
			}

			@Override
			public void notifyReceive(final BlockHeight height, final Account account, final Amount amount) {
				if (foragerAccount.getAddress().equals(foragerAccount.getAddress()))
					balanceInObserver[0] = account.getBalance();
			}

			@Override
			public void notifySendUndo(final BlockHeight height, final Account account, final Amount amount) {
			}

			@Override
			public void notifyReceiveUndo(final BlockHeight height, final Account account, final Amount amount) {
			}
		};

		// Act:
		executor.execute(block, Arrays.asList(observer));

		// Assert:
		Assert.assertThat(balanceInObserver[0], IsEqual.equalTo(Amount.fromNem(52)));
		Assert.assertThat(foragerAccount.getBalance(), IsEqual.equalTo(Amount.fromNem(52)));
	}

	@Test
	public void undoRemovesOutlinkBeforeUpdatingBalances() {
		// Arrange:
		final Account foragerAccount = Utils.generateRandomAccount();
		foragerAccount.incrementBalance(Amount.fromNem(50));
		foragerAccount.getWeightedBalances().addReceive(BlockHeight.ONE, Amount.fromNem(50));
		final Block block = BlockUtils.createBlock(foragerAccount);
		block.addTransaction(BlockUtils.createTransactionWithFee(Amount.fromNem(2)));
		final BlockExecutor executor = createBlockExecutor();

		final Amount[] balanceInObserver = new Amount[1];
		final BlockTransferObserver observer = new BlockTransferObserver() {
			@Override
			public void notifySend(final BlockHeight height, final Account account, final Amount amount) {
			}

			@Override
			public void notifyReceive(final BlockHeight height, final Account account, final Amount amount) {

			}

			@Override
			public void notifySendUndo(final BlockHeight height, final Account account, final Amount amount) {
			}

			@Override
			public void notifyReceiveUndo(final BlockHeight height, final Account account, final Amount amount) {
				if (foragerAccount.getAddress().equals(foragerAccount.getAddress()))
					balanceInObserver[0] = account.getBalance();
			}
		};

		// Act:
		executor.execute(block, Arrays.asList(observer));
		executor.undo(block, Arrays.asList(observer));

		// Assert:
		Assert.assertThat(balanceInObserver[0], IsEqual.equalTo(Amount.fromNem(52)));
		Assert.assertThat(foragerAccount.getBalance(), IsEqual.equalTo(Amount.fromNem(50)));
	}

	//endregion

	private static BlockExecutor createBlockExecutor() {
		return new BlockExecutor();
	}
}