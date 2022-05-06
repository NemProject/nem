package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.test.DbTestUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.*;

@RunWith(Enclosed.class)
@SuppressWarnings("rawtypes")
public class BlockDbModelToModelMappingTest {

	// region General

	public static class General {

		// region nemesis block mapping

		@Test
		public void nemesisDbModelCanBeMappedToNemesisModel() {
			// Arrange:
			final TestContext context = new TestContext();
			final DbBlock dbBlock = context.createDbBlock();
			dbBlock.setHeight(1L);

			// Act:
			final Block model = context.mapping.map(dbBlock);

			// Assert:
			context.assertNemesisModel(model);
			MatcherAssert.assertThat(model.getTransactions().isEmpty(), IsEqual.equalTo(true));
		}

		// endregion

		// region no transaction mapping

		@Test
		public void blockWithMinimalInformationCanBeMappedToModel() {
			// Arrange:
			final TestContext context = new TestContext();
			final DbBlock dbBlock = context.createDbBlock();

			// Act:
			final Block model = context.mapping.map(dbBlock);

			// Assert:
			context.assertModel(model);
			MatcherAssert.assertThat(model.getTransactions().isEmpty(), IsEqual.equalTo(true));
		}

		@Test
		public void blockWithDifficultyCanBeMappedToModel() {
			// Arrange:
			final TestContext context = new TestContext();
			final DbBlock dbBlock = context.createDbBlock(111L, null);

			// Act:
			final Block model = context.mapping.map(dbBlock);

			// Assert:
			context.assertModel(model, 111L, null);
			MatcherAssert.assertThat(model.getTransactions().isEmpty(), IsEqual.equalTo(true));
		}

		@Test
		public void blockWithLessorCanBeMappedToModel() {
			// Arrange:
			final TestContext context = new TestContext();
			final DbBlock dbBlock = context.createDbBlock(null, context.dbLessor);

			// Act:
			final Block model = context.mapping.map(dbBlock);

			// Assert:
			context.assertModel(model, 0L, context.lessor);
			MatcherAssert.assertThat(model.getTransactions().isEmpty(), IsEqual.equalTo(true));
		}

		// endregion

		// region transaction mapping

		@Test
		public void blockWithMixedTransfersCanBeMappedToModel() {
			// Arrange:
			final TestContext context = new TestContext();
			final DbBlock dbBlock = context.createDbBlock();

			// - randomly shuffle the transactions
			final int numTransactionsPerType = 2;
			final int numTransactions = numTransactionsPerType * TransactionTypes.getBlockEmbeddableTypes().size();
			final List<Integer> indexes = IntStream.range(0, numTransactions).boxed().collect(Collectors.toList());
			Collections.shuffle(indexes);

			// - create two transactions for every type
			int k = 0;
			final List<Transaction> transactions = new ArrayList<>();
			final List<Transaction> orderedTransactions = new ArrayList<>(numTransactions);
			orderedTransactions.addAll(indexes.stream().map(i -> (Transaction) null).collect(Collectors.toList()));
			for (int i = 0; i < numTransactionsPerType; ++i) {
				for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
					final int index = indexes.get(k);
					final Transaction transaction = context.addTransfer(entry, dbBlock, index);
					transactions.add(transaction);
					orderedTransactions.set(index, transaction);
					++k;
				}
			}

			// Act:
			final Block model = context.mapping.map(dbBlock);

			// Assert:
			context.assertModel(model);
			MatcherAssert.assertThat(model.getTransactions().size(), IsEqual.equalTo(numTransactions));
			MatcherAssert.assertThat(model.getTransactions(), IsEqual.equalTo(orderedTransactions));
			Mockito.verify(context.mapper, Mockito.times(numTransactions)).map(Mockito.any(), Mockito.eq(Transaction.class));

			// Sanity:
			MatcherAssert.assertThat(transactions, IsNot.not(IsEqual.equalTo(orderedTransactions)));
			for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
				MatcherAssert.assertThat("not all transaction types are represented", entry.getFromBlock.apply(dbBlock).isEmpty(),
						IsEqual.equalTo(false));
			}
		}

		@Test
		public void innerMultisigTransfersAreNotIncludedDirectlyInModelBlock() {
			// Arrange:
			final TestContext context = new TestContext();
			final DbBlock dbBlock = context.createDbBlock();

			// - create one transactions for every type with bookend multisig transactions
			final int numTransactionsPerType = 1;
			final int numTransactions = numTransactionsPerType * TransactionTypes.getBlockEmbeddableTypes().size() + 2;

			int k = 0;
			context.addMultisigTransferWithInnerTransfer(dbBlock, k++);

			for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
				context.addTransfer(entry, dbBlock, k++);
			}

			context.addMultisigTransferWithInnerTransfer(dbBlock, k);

			// Act:
			final Block model = context.mapping.map(dbBlock);

			// Assert:
			context.assertModel(model);
			MatcherAssert.assertThat(model.getTransactions().size(), IsEqual.equalTo(numTransactions));
			Mockito.verify(context.mapper, Mockito.times(numTransactions)).map(Mockito.any(), Mockito.eq(Transaction.class));

			for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
				final int numExpectedTransactions = TransactionTypes.MULTISIG == entry.type
						? numTransactionsPerType + 2
						: numTransactionsPerType;
				MatcherAssert.assertThat(
						String.format("transaction type %d should have %d transactions in block", entry.type, numExpectedTransactions),
						entry.getFromBlock.apply(dbBlock).size(), IsEqual.equalTo(numExpectedTransactions));
			}
		}
	}

	// endregion

	// region PerTransaction

	@RunWith(Parameterized.class)
	public static class PerTransaction {
		private final TransactionRegistry.Entry<? extends AbstractTransfer, ? extends Transaction> entry;

		public PerTransaction(final int type) {
			this.entry = TransactionRegistry.findByType(type);
		}

		@Parameterized.Parameters
		public static Collection<Object[]> data() {
			return ParameterizedUtils.wrap(TransactionTypes.getBlockEmbeddableTypes());
		}

		@Test
		public void blockWithTransactionsCanBeMappedToModel() {
			// Arrange:
			final TestContext context = new TestContext();
			final DbBlock dbBlock = context.createDbBlock();

			final Transaction transfer2 = context.addTransfer(this.entry, dbBlock, 2);
			final Transaction transfer0 = context.addTransfer(this.entry, dbBlock, 0);
			final Transaction transfer1 = context.addTransfer(this.entry, dbBlock, 1);

			// Act:
			final Block model = context.mapping.map(dbBlock);

			// Assert:
			context.assertModel(model);
			MatcherAssert.assertThat(model.getTransactions().size(), IsEqual.equalTo(3));
			MatcherAssert.assertThat(model.getTransactions(), IsEqual.equalTo(Arrays.asList(transfer0, transfer1, transfer2)));
			Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(), Mockito.eq(Transaction.class));
		}
	}

	// endregion

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbHarvester = Mockito.mock(DbAccount.class);
		private final DbAccount dbLessor = Mockito.mock(DbAccount.class);
		private final Account harvester = Utils.generateRandomAccount();
		private final Account lessor = Utils.generateRandomAccount();
		private final Signature signature = Utils.generateRandomSignature();
		private final Hash prevBlockHash = Utils.generateRandomHash();
		private final Hash generationBlockHash = Utils.generateRandomHash();
		private final BlockDbModelToModelMapping mapping = new BlockDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbHarvester, Account.class)).thenReturn(this.harvester);
			Mockito.when(this.mapper.map(this.dbLessor, Account.class)).thenReturn(this.lessor);
		}

		public DbBlock createDbBlock() {
			return this.createDbBlock(null, null);
		}

		public DbBlock createDbBlock(final Long difficulty, final DbAccount lessor) {
			final DbBlock dbBlock = new DbBlock();
			dbBlock.setHarvester(this.dbHarvester);
			dbBlock.setPrevBlockHash(this.prevBlockHash);
			dbBlock.setGenerationHash(this.generationBlockHash);
			dbBlock.setTimeStamp(4444);
			dbBlock.setHeight(7L);

			dbBlock.setDifficulty(difficulty);
			dbBlock.setLessor(lessor);
			dbBlock.setHarvesterProof(this.signature.getBytes());

			for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
				entry.setInBlock.accept(dbBlock, new ArrayList<>());
			}

			return dbBlock;
		}

		public void assertModel(final Block model) {
			this.assertModel(model, 0L, null);
		}

		public void assertNemesisModel(final Block model) {
			this.assertModelInternal(model, 0L, null);
			MatcherAssert.assertThat(model.getHeight(), IsEqual.equalTo(BlockHeight.ONE));
			MatcherAssert.assertThat(model.getType(), IsEqual.equalTo(-1));
		}

		public void assertModel(final Block model, final long expectedDifficulty, final Account expectedLessor) {
			this.assertModelInternal(model, expectedDifficulty, expectedLessor);
			MatcherAssert.assertThat(model.getHeight(), IsEqual.equalTo(new BlockHeight(7)));
			MatcherAssert.assertThat(model.getType(), IsEqual.equalTo(1));
		}

		private void assertModelInternal(final Block model, final long expectedDifficulty, final Account expectedLessor) {
			MatcherAssert.assertThat(model.getSigner(), IsEqual.equalTo(this.harvester));
			MatcherAssert.assertThat(model.getPreviousBlockHash(), IsEqual.equalTo(this.prevBlockHash));
			MatcherAssert.assertThat(model.getGenerationHash(), IsEqual.equalTo(this.generationBlockHash));
			MatcherAssert.assertThat(model.getTimeStamp(), IsEqual.equalTo(new TimeInstant(4444)));

			MatcherAssert.assertThat(model.getDifficulty(), IsEqual.equalTo(new BlockDifficulty(expectedDifficulty)));
			MatcherAssert.assertThat(model.getLessor(), IsEqual.equalTo(expectedLessor));
			MatcherAssert.assertThat(model.getSignature(), IsEqual.equalTo(this.signature));
		}

		// region add*

		public Transaction addTransfer(final TransactionRegistry.Entry<? extends AbstractBlockTransfer, ? extends Transaction> typedEntry,
				final DbBlock block, final int blockIndex) {
			@SuppressWarnings("unchecked")
			final TransactionRegistry.Entry<AbstractBlockTransfer, ? extends Transaction> entry = (TransactionRegistry.Entry<AbstractBlockTransfer, ? extends Transaction>) typedEntry;
			return this.addTransfer(dbTransfer -> {
				final List<AbstractBlockTransfer> transactions = entry.getFromBlock.apply(block);
				transactions.add(dbTransfer);
				entry.setInBlock.accept(block, transactions);
			}, blockIndex, DbTestUtils.createTransferDbModel(entry.dbModelClass), entry.modelClass);
		}

		public void addMultisigTransferWithInnerTransfer(final DbBlock block, final int blockIndex) {
			final DbTransferTransaction dbInnerTransfer = new DbTransferTransaction();
			this.addTransfer(block::addTransferTransaction, blockIndex, dbInnerTransfer, TransferTransaction.class);
			dbInnerTransfer.setSenderProof(null);

			final DbMultisigTransaction dbMultisigTransfer = new DbMultisigTransaction();
			dbMultisigTransfer.setTransferTransaction(dbInnerTransfer);
			this.addTransfer(block::addMultisigTransaction, blockIndex, dbMultisigTransfer, MultisigTransaction.class);
		}

		private <TDbTransfer extends AbstractBlockTransfer, TModelTransfer extends Transaction> TModelTransfer addTransfer(
				final Consumer<TDbTransfer> addTransaction, final int blockIndex, final TDbTransfer dbTransfer,
				final Class<TModelTransfer> modelClass) {
			dbTransfer.setSenderProof(Utils.generateRandomSignature().getBytes());
			dbTransfer.setBlkIndex(blockIndex);
			dbTransfer.setReferencedTransaction(0L);
			final TModelTransfer transfer = Mockito.mock(modelClass);
			Mockito.when(this.mapper.map(dbTransfer, Transaction.class)).thenReturn(transfer);
			addTransaction.accept(dbTransfer);
			return transfer;
		}

		// endregion
	}
}
