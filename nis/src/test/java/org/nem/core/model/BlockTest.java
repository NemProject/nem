package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.function.Consumer;

public class BlockTest {

	final static Hash DUMMY_PREVIOUS_HASH = Utils.generateRandomHash();
	final static Hash DUMMY_GENERATION_HASH = Utils.generateRandomHash();

	//region Constructors

	@Test
	public void ctorCanCreateBlockForAccountWithSignerPrivateKey() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();

		// Act:
		final Block block = createBlock(signer);

		// Assert:
		Assert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(block.getType(), IsEqual.equalTo(1));
		Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(7)));

		Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(DUMMY_PREVIOUS_HASH));
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(3)));
		Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(0));

		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(BlockDifficulty.INITIAL_DIFFICULTY));
		Assert.assertThat(block.getGenerationHash(), IsEqual.equalTo(DUMMY_GENERATION_HASH));
	}

	@Test
	public void ctorCanCreateBlockAroundPreviousBlock() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Block previousBlock = createBlock(signer);

		// Act:
		final Block block = new Block(signer, previousBlock, new TimeInstant(11));

		// Assert:
		Assert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(block.getType(), IsEqual.equalTo(1));
		Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(11)));

		Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(HashUtils.calculateHash(previousBlock)));
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(4)));
		Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(0));

		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(BlockDifficulty.INITIAL_DIFFICULTY));
		final Hash expectedGenerationHash = HashUtils.nextHash(
				previousBlock.getGenerationHash(),
				signer.getKeyPair().getPublicKey());
		Assert.assertThat(block.getGenerationHash(), IsEqual.equalTo(expectedGenerationHash));
	}

	//endregion

	//region Setters

	@Test
	public void previousBlockCanSet() {
		// Arrange:
		final Block previousBlock = createBlock(Utils.generateRandomAccount());
		final Block block = createBlock(Utils.generateRandomAccount());

		// Act:
		block.setPrevious(previousBlock);

		// Assert:
		Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(HashUtils.calculateHash(previousBlock)));
		final Hash expectedGenerationHash = HashUtils.nextHash(
				previousBlock.getGenerationHash(),
				block.getSigner().getKeyPair().getPublicKey());
		Assert.assertThat(block.getGenerationHash(), IsEqual.equalTo(expectedGenerationHash));
	}

	@Test
	public void blockDifficultyCanBeSet() {
		// Arrange:
		final Block block = createBlock(Utils.generateRandomAccount());
		final BlockDifficulty blockDifficulty = new BlockDifficulty(44_444_444_444L);

		// Act:
		block.setDifficulty(blockDifficulty);

		// Assert:
		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(blockDifficulty));
	}

	//endregion

	//region Serialization

	@Test
	public void blockCanBeRoundTripped() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();

		// Act:
		final Block block = createBlockForRoundTripTests(true, signer);

		// Assert:
		Assert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(block.getType(), IsEqual.equalTo(1));
		Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(7)));

		// (t1 has a fee of 1 and t2 has a fee of 2)
		Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(Amount.fromNem(3L)));
		Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(DUMMY_PREVIOUS_HASH));
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(3)));

		final List<Transaction> transactions = block.getTransactions();
		Assert.assertThat(transactions.size(), IsEqual.equalTo(2));

		final TransferTransaction transaction1 = (TransferTransaction)transactions.get(0);
		Assert.assertThat(transaction1.getAmount(), IsEqual.equalTo(Amount.fromNem(17)));

		final TransferTransaction transaction2 = (TransferTransaction)transactions.get(1);
		Assert.assertThat(transaction2.getAmount(), IsEqual.equalTo(Amount.fromNem(290)));
	}

	@Test
	public void blockDifficultyIsNotRoundTripped() {
		// Act:
		final Block block = createBlockForRoundTripTests(true, null);

		// Assert:
		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(BlockDifficulty.INITIAL_DIFFICULTY));
	}

	@Test
	public void changingDifficultyDoesNotInvalidateBlock() {
		// Arrange:
		final Block block = createBlockForRoundTripTests(true, null);

		// Act
		boolean originalVerifyResult = block.verify();
		block.setDifficulty(new BlockDifficulty(55_444_333_222_111L));

		// Assert:
		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(new BlockDifficulty(55_444_333_222_111L)));
		Assert.assertThat(originalVerifyResult, IsEqual.equalTo(true));
		Assert.assertThat(block.verify(), IsEqual.equalTo(true));
	}

	@Test
	public void changingPreviousBlockInvalidatesBlock() {
		// Arrange:
		final Block previousBlock = createBlock(Utils.generateRandomAccount());
		final Block block = createBlockForRoundTripTests(true, null);

		// Act
		boolean originalVerifyResult = block.verify();
		block.setPrevious(previousBlock);

		// Assert:
		Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(HashUtils.calculateHash(previousBlock)));
		Assert.assertThat(originalVerifyResult, IsEqual.equalTo(true));
		Assert.assertThat(block.verify(), IsEqual.equalTo(false));
	}

	@Test
	public void blockAndTransactionsCanBeVerifiedAfterVerifiableRoundTrip() {
		// Act:
		final Block block = createBlockForRoundTripTests(true, null);

		// Assert:
		Assert.assertThat(block.verify(), IsEqual.equalTo(true));

		final List<Transaction> transactions = block.getTransactions();
		Assert.assertThat(transactions.size(), IsEqual.equalTo(2));

		final TransferTransaction transaction1 = (TransferTransaction)transactions.get(0);
		Assert.assertThat(transaction1.verify(), IsEqual.equalTo(true));

		final TransferTransaction transaction2 = (TransferTransaction)transactions.get(1);
		Assert.assertThat(transaction2.verify(), IsEqual.equalTo(true));
	}

	@Test
	public void transactionsCanBeVerifiedAfterNonVerifiableRoundTrip() {
		// Act:
		final Block block = createBlockForRoundTripTests(false, null);

		// Assert:
		Assert.assertThat(block.getSignature(), IsNull.nullValue());

		final List<Transaction> transactions = block.getTransactions();
		Assert.assertThat(transactions.size(), IsEqual.equalTo(2));

		final TransferTransaction transaction1 = (TransferTransaction)transactions.get(0);
		Assert.assertThat(transaction1.verify(), IsEqual.equalTo(true));

		final TransferTransaction transaction2 = (TransferTransaction)transactions.get(1);
		Assert.assertThat(transaction2.verify(), IsEqual.equalTo(true));
	}

	private Block createBlockForRoundTripTests(boolean verifiable, final Account signer) {
		// Arrange:
		final Block originalBlock = createBlock(null == signer ? Utils.generateRandomAccount() : signer);
		final TransferTransaction transaction1 = createSignedTransactionWithAmount(17);
		originalBlock.addTransaction(transaction1);

		final TransferTransaction transaction2 = createSignedTransactionWithAmount(290);
		originalBlock.addTransaction(transaction2);
		originalBlock.setDifficulty(new BlockDifficulty(22_222_222_222L));
		originalBlock.sign();

		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();
		accountLookup.setMockAccount(Utils.createPublicOnlyKeyAccount(originalBlock.getSigner()));
		accountLookup.setMockAccount(Utils.createPublicOnlyKeyAccount(transaction1.getSigner()));
		accountLookup.setMockAccount(Utils.createPublicOnlyKeyAccount(transaction2.getSigner()));

		// Act:
		final SerializableEntity entity = verifiable ? originalBlock : originalBlock.asNonVerifiable();
		final VerifiableEntity.DeserializationOptions options = verifiable
				? VerifiableEntity.DeserializationOptions.VERIFIABLE
				: VerifiableEntity.DeserializationOptions.NON_VERIFIABLE;

		final Deserializer deserializer = Utils.roundtripSerializableEntity(entity, accountLookup);
		return new Block(deserializer.readInt("type"), options, deserializer);
	}

	private TransferTransaction createSignedTransactionWithAmount(long amount) {
		final TransferTransaction transaction = new TransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Amount.fromNem(amount),
				null);
		transaction.sign();
		return transaction;
	}

	//endregion

	//region Transaction

	@Test
	public void singleTransactionCanBeAddedToBlock() {
		// Arrange:
		final Block block = createBlock();
		final Transaction transaction = createTransactionWithFee(17);

		// Act:
		block.addTransaction(transaction);

		// Assert:
		Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(1));
		Assert.assertThat((block.getTransactions().get(0)), IsEqual.equalTo(transaction));
	}

	@Test
	public void multipleTransactionsCanBeAddedToBlock() {
		// Arrange:
		final Block block = createBlock();
		final List<Transaction> transactions = new ArrayList<>();
		transactions.add(createTransactionWithFee(17));
		transactions.add(createTransactionWithFee(11));

		// Act:
		block.addTransactions(transactions);

		// Assert:
		Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(2));
		Assert.assertThat((block.getTransactions().get(0)), IsEqual.equalTo(transactions.get(0)));
		Assert.assertThat((block.getTransactions().get(1)), IsEqual.equalTo(transactions.get(1)));
	}

	//endregion

	//region Fee

	@Test
	public void blockFeeIsSumOfTransactionFees() {
		// Arrange:
		final Block block = createBlock();
		block.addTransaction(createTransactionWithFee(17));
		block.addTransaction(createTransactionWithFee(11));
		block.addTransactions(Arrays.asList((Transaction) createTransactionWithFee(3), createTransactionWithFee(50)));
		block.addTransaction(createTransactionWithFee(22));

		// Assert:
		Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(new Amount(103L)));
	}

	//endregion

 	//region execute

	@Test
	public void executeIncrementsForagedBlocks() {
		// Arrange: initial foraged blocks = 3
		final UndoExecuteTestContext context = new UndoExecuteTestContext();

		// Act:
		context.block.execute();

		// Assert:
		Assert.assertThat(context.account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(4)));
	}

	@Test
	public void executeIncrementsForagerBalanceByTotalFee() {
		// Arrange: initial balance = 100, total fee = 28
		final UndoExecuteTestContext context = new UndoExecuteTestContext();

		// Act:
		context.block.execute();

		// Assert:
		Assert.assertThat(context.account.getBalance(), IsEqual.equalTo(new Amount(128)));
	}

	@Test
	public void executeCallsExecuteOnAllTransactionsInForwardOrder() {
		// Arrange:
		final UndoExecuteTestContext context = new UndoExecuteTestContext();

		// Act:
		context.block.execute();

		// Assert:
		Assert.assertThat(context.executeList, IsEquivalent.equivalentTo(new Integer[] { 1, 2 }));
	}

	@Test
	public void executeDelegatesToSubscribedObserver() {
		// Arrange:
		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);

		// Assert:
		assertExecuteNotificationForObservers(Arrays.asList(observer), b -> b.subscribe(observer));
	}

	@Test
	public void executeDelegatesToAllSubscribedObservers() {
		// Arrange:
		final List<BlockTransferObserver> observers = Arrays.asList(
				Mockito.mock(BlockTransferObserver.class),
				Mockito.mock(BlockTransferObserver.class),
				Mockito.mock(BlockTransferObserver.class));

		// Assert:
		assertExecuteNotificationForObservers(observers, b -> observers.stream().forEach(b::subscribe));
	}

	@Test
	public void executeDoesNotDelegateToUnsubscribedObserver() {
		// Arrange:
		final BlockTransferObserver observer1 = Mockito.mock(BlockTransferObserver.class);
		final BlockTransferObserver observer2 = Mockito.mock(BlockTransferObserver.class);
		final BlockTransferObserver observer3 = Mockito.mock(BlockTransferObserver.class);

		// Assert:
		assertExecuteNotificationForObservers(Arrays.asList(observer1, observer3), b -> {
			b.subscribe(observer1);
			b.subscribe(observer2);
			b.subscribe(observer3);
			b.unsubscribe(observer2);
		});

		verifyCallCounts(observer2, 0, 0, 0, 0);
	}

	private static void assertExecuteNotificationForObservers(
			final List<BlockTransferObserver> observers,
			final Consumer<Block> registerObservers) {

		// Arrange:
		final Account account1 = Utils.generateRandomAccount();
		account1.incrementBalance(Amount.fromNem(25));
		account1.getWeightedBalances().addReceive(BlockHeight.ONE, Amount.fromNem(25));
		final Account account2 = Utils.generateRandomAccount();
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);
		transaction.setFee(Amount.fromNem(7));
		transaction.setTransferAction(to -> {
			to.notifyTransfer(account1, account2, Amount.fromNem(12));
			to.notifyCredit(account1, Amount.fromNem(9));
			to.notifyDebit(account1, Amount.fromNem(11));
		});

		final BlockHeight height = new BlockHeight(11);
		final Block block = createBlockWithHeight(height);
		block.addTransaction(transaction);
		registerObservers.accept(block);

		// Act:
		block.execute();

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

	//endregion

	//region undo

	@Test
	public void undoDecrementsForagedBlocks() {
		// Arrange: initial foraged blocks = 3
		final UndoExecuteTestContext context = new UndoExecuteTestContext();

		// Act:
		context.block.undo();

		// Assert:
		Assert.assertThat(context.account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(2)));
	}

	@Test
	public void undoDecrementsForagerBalanceByTotalFee() {
		// Arrange: initial balance = 100, total fee = 28
		final UndoExecuteTestContext context = new UndoExecuteTestContext();

		// Act:
		context.block.undo();

		// Assert:
		Assert.assertThat(context.account.getBalance(), IsEqual.equalTo(new Amount(72)));
	}

	@Test
	public void undoCallsUndoOnAllTransactionsInReverseOrder() {
		// Arrange:
		final UndoExecuteTestContext context = new UndoExecuteTestContext();

		// Act:
		context.block.undo();

		// Assert:
		Assert.assertThat(context.undoList, IsEquivalent.equivalentTo(new Integer[] { 2, 1 }));
	}

	@Test
	public void undoDelegatesToSubscribedObserver() {
		// Arrange:
		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);

		// Assert:
		assertUndoNotificationForObservers(Arrays.asList(observer), b -> b.subscribe(observer));
	}

	@Test
	public void undoDelegatesToAllSubscribedObservers() {
		// Arrange:
		final List<BlockTransferObserver> observers = Arrays.asList(
				Mockito.mock(BlockTransferObserver.class),
				Mockito.mock(BlockTransferObserver.class),
				Mockito.mock(BlockTransferObserver.class));

		// Assert:
		assertUndoNotificationForObservers(observers, b -> observers.stream().forEach(b::subscribe));
	}

	@Test
	public void undoDoesNotDelegateToUnsubscribedObserver() {
		// Arrange:
		final BlockTransferObserver observer1 = Mockito.mock(BlockTransferObserver.class);
		final BlockTransferObserver observer2 = Mockito.mock(BlockTransferObserver.class);
		final BlockTransferObserver observer3 = Mockito.mock(BlockTransferObserver.class);

		// Assert:
		assertUndoNotificationForObservers(Arrays.asList(observer1, observer3), b -> {
			b.subscribe(observer1);
			b.subscribe(observer2);
			b.subscribe(observer3);
			b.unsubscribe(observer2);
		});

		verifyCallCounts(observer2, 0, 0, 0, 0);
	}

	private static void assertUndoNotificationForObservers(
			final List<BlockTransferObserver> observers,
			final Consumer<Block> registerObservers) {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount();
		account1.incrementBalance(Amount.fromNem(25));
		account1.getWeightedBalances().addReceive(BlockHeight.ONE, Amount.fromNem(25));

		final Account account2 = Utils.generateRandomAccount();
		// this might look strange, but we won't be able to undo if weighted balances doesn't have
		// knowledge, that somewhere in the past we received something
		account2.getWeightedBalances().addReceive(BlockHeight.ONE, Amount.fromNem(12));
		// we won't be able to undo if an original outlink doesn't exist
		account2.getImportanceInfo().addOutlink(
				new AccountLink(new BlockHeight(11), Amount.fromNem(12), account1.getAddress()));

		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);
		transaction.setFee(Amount.fromNem(7));
		transaction.setTransferAction(to -> {
			to.notifyTransfer(account2, account1, Amount.fromNem(12));
			to.notifyDebit(account1, Amount.fromNem(9));
			to.notifyCredit(account1, Amount.fromNem(11));
		});

		final BlockHeight height = new BlockHeight(11);
		final Block block = createBlockWithHeight(height);
		block.getSigner().incrementBalance(Amount.fromNem(7));
		block.getSigner().getWeightedBalances().addReceive(BlockHeight.ONE, Amount.fromNem(7));
		block.addTransaction(transaction);

		registerObservers.accept(block);

		// Act:
		block.undo();

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

	private final class UndoExecuteTestContext {
		private final Account account;
		private final Block block;
		private final MockTransaction transaction1;
		private final MockTransaction transaction2;
		private final List<Integer> executeList = new ArrayList<>();
		private final List<Integer> undoList = new ArrayList<>();

		public UndoExecuteTestContext() {
			this.account = Utils.generateRandomAccount();
			this.account.incrementBalance(new Amount(100));
			for (int i = 0; i < 3; ++i)
				this.account.incrementForagedBlocks();

			this.transaction1 = createTransaction(1, 17);
			this.transaction2 = createTransaction(2, 11);

			this.block = createBlock(this.account);
			this.block.addTransaction(this.transaction1);
			this.block.addTransaction(this.transaction2);

			this.account.getWeightedBalances().addReceive(BlockHeight.ONE, new Amount(100));
		}

		private MockTransaction createTransaction(final int customField, final long fee) {
			final MockTransaction transaction = createTransactionWithFee(customField, fee);
			transaction.setExecuteList(this.executeList);
			transaction.setUndoList(this.undoList);
			return transaction;
		}
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsAppropriateRepresentation() {
		// Arrange:
		final Block block = createBlock();
		block.addTransaction(createTransactionWithFee(1));
		block.addTransaction(createTransactionWithFee(7));

		// Assert:
		Assert.assertThat(block.toString(), IsEqual.equalTo("height: 3, #tx: 2"));
	}

	//endregion

	private static MockTransaction createTransactionWithFee(final long fee) {
		// Arrange:
		return createTransactionWithFee(127, fee);
	}

	private static MockTransaction createTransactionWithFee(final int customField, final long fee) {
		// Arrange:
		Account sender = Utils.generateRandomAccount();
		MockTransaction transaction = new MockTransaction(sender, customField);
		transaction.setFee(new Amount(fee));
		return transaction;
	}

	private static Block createBlock(final Account forger) {
		// Arrange:
		return new Block(forger, DUMMY_PREVIOUS_HASH, DUMMY_GENERATION_HASH, new TimeInstant(7), new BlockHeight(3));
	}

	private static Block createBlockWithHeight(final BlockHeight height) {
		// Arrange:
		final Account forger = Utils.generateRandomAccount();
		final Block block = new Block(
				forger,
				DUMMY_PREVIOUS_HASH,
				DUMMY_GENERATION_HASH,
				new TimeInstant(7),
				height);
		forger.incrementForagedBlocks();
		return block;
	}

	private static Block createBlock() {
		// Arrange:
		return createBlock(Utils.generateRandomAccount());
	}
}
