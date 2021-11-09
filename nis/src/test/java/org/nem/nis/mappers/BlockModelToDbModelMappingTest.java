package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
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
import java.util.function.Supplier;
import java.util.stream.*;

@RunWith(Enclosed.class)
@SuppressWarnings("rawtypes")
public class BlockModelToDbModelMappingTest {

	public static class General {

		// region no transaction mapping

		@Test
		public void blockWithMinimalInformationCanBeMappedToDbModel() {
			// Arrange:
			final TestContext context = new TestContext();
			final Block block = context.createBlock(null);

			// Act:
			final DbBlock dbModel = context.mapping.map(block);

			// Assert:
			context.assertDbModel(dbModel, HashUtils.calculateHash(block));
			context.assertNoTransactions(dbModel);
		}

		@Test
		public void blockWithLessorCanBeMappedToDbModel() {
			// Arrange:
			final TestContext context = new TestContext();
			final Block block = context.createBlock(context.lessor);

			// Act:
			final DbBlock dbModel = context.mapping.map(block);

			// Assert:
			context.assertDbModel(dbModel, HashUtils.calculateHash(block), context.dbLessor);
			context.assertNoTransactions(dbModel);
		}

		// endregion

		// region transaction mapping

		@Test
		public void blockWithUnsupportedTransfersCannotBeMappedToDbModel() {
			// Arrange:
			final TestContext context = new TestContext();
			final Block block = context.createBlock(null);

			context.addUnsupportedTransfer(block);

			// Act:
			ExceptionAssert.assertThrows(v -> context.mapping.map(block), IllegalArgumentException.class);
		}

		@Test
		public void blockWithMixedTransfersCanBeMappedToDbModel() {
			// Arrange:
			final TestContext context = new TestContext();
			final Block block = context.createBlock(null);

			// - create two transactions for every type
			final int numTransactionsPerType = 2;
			final int numBlockEmbeddableTypes = TransactionTypes.getBlockEmbeddableTypes().size();
			final int numTransactions = numTransactionsPerType * numBlockEmbeddableTypes;
			int k = 0;
			final List<AbstractBlockTransfer> blockTransfers = new ArrayList<>();
			for (int i = 0; i < numTransactionsPerType; ++i) {
				for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
					final AbstractBlockTransfer transfer = context.addTransfer(entry, block);
					blockTransfers.add(transfer);
					++k;
				}
			}

			// Act:
			final DbBlock dbModel = context.mapping.map(block);

			// Assert:
			context.assertDbModel(dbModel, HashUtils.calculateHash(block));

			MatcherAssert.assertThat(getNumTransactions(dbModel), IsEqual.equalTo(numTransactions));

			k = 0;
			for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
				final Collection<? extends AbstractBlockTransfer> transfers = entry.getFromBlock.apply(dbModel);
				MatcherAssert.assertThat(transfers.size(), IsEqual.equalTo(numTransactionsPerType));
				MatcherAssert.assertThat(transfers,
						IsEqual.equalTo(Arrays.asList(blockTransfers.get(k), blockTransfers.get(k + numBlockEmbeddableTypes))));
				MatcherAssert.assertThat(getBlockIndexes(transfers), IsEqual.equalTo(Arrays.asList(k, k + numBlockEmbeddableTypes)));

				Mockito.verify(context.mapper, Mockito.times(numTransactionsPerType)).map(Mockito.any(), Mockito.eq(entry.dbModelClass));
				++k;
			}

			// Sanity:
			for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
				assertTransfersHaveBlockSetCorrectly(entry.getFromBlock.apply(dbModel), dbModel);

				MatcherAssert.assertThat("not all transaction types are represented", entry.getFromBlock.apply(dbModel).isEmpty(),
						IsEqual.equalTo(false));
			}
		}

		@Test
		public void innerMultisigTransferBlockRelatedPropertiesAreSetCorrectly() {
			// Arrange:
			final TestContext context = new TestContext();
			final Block block = context.createBlock(null);

			final DbTransferTransaction innerDbTransferTransaction1 = new DbTransferTransaction();
			final DbTransferTransaction innerDbTransferTransaction2 = new DbTransferTransaction();

			context.addMultisigTransfer(block, innerDbTransferTransaction1);

			for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
				context.addTransfer(entry, block);
			}

			context.addMultisigTransfer(block, innerDbTransferTransaction2);

			// Act:
			final DbBlock dbModel = context.mapping.map(block);

			// Assert: db model properties
			context.assertDbModel(dbModel, HashUtils.calculateHash(block));

			// Assert: db model transactions
			MatcherAssert.assertThat(getNumTransactions(dbModel), IsEqual.equalTo(TransactionRegistry.size() + 2));

			int k = 0;
			for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
				final Collection<? extends AbstractBlockTransfer> transfers = entry.getFromBlock.apply(dbModel);
				if (TransactionTypes.MULTISIG == entry.type) {
					MatcherAssert.assertThat(transfers.size(), IsEqual.equalTo(3));
					MatcherAssert.assertThat(getBlockIndexes(transfers),
							IsEqual.equalTo(Arrays.asList(0, k + 1, TransactionRegistry.size() + 1)));
				} else {
					MatcherAssert.assertThat(transfers.size(), IsEqual.equalTo(1));
					MatcherAssert.assertThat(getBlockIndexes(transfers), IsEqual.equalTo(Collections.singletonList(k + 1)));
				}

				++k;
			}

			for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
				assertTransfersHaveBlockSetCorrectly(entry.getFromBlock.apply(dbModel), dbModel);
			}

			// Assert: multisig inner transactions
			// - inner transaction does not belong to a block, so it won't have order id
			MatcherAssert.assertThat(innerDbTransferTransaction1.getBlkIndex(), IsEqual.equalTo(0));
			MatcherAssert.assertThat(innerDbTransferTransaction1.getBlock(), IsEqual.equalTo(dbModel));

			MatcherAssert.assertThat(innerDbTransferTransaction2.getBlkIndex(), IsEqual.equalTo(TransactionRegistry.size() + 1));
			MatcherAssert.assertThat(innerDbTransferTransaction2.getBlock(), IsEqual.equalTo(dbModel));
		}

		// endregion
	}

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
		public void blockWithTransactionsCanBeMappedToDbModel() {
			// Arrange:
			final TestContext context = new TestContext();
			final Block block = context.createBlock(null);

			final AbstractTransfer transfer0 = context.addTransfer(this.entry, block);
			final AbstractTransfer transfer1 = context.addTransfer(this.entry, block);
			final AbstractTransfer transfer2 = context.addTransfer(this.entry, block);

			// Act:
			final DbBlock dbModel = context.mapping.map(block);
			final Collection<? extends AbstractBlockTransfer> dbTransfers = this.entry.getFromBlock.apply(dbModel);

			// Assert:
			context.assertDbModel(dbModel, HashUtils.calculateHash(block));

			MatcherAssert.assertThat(getNumTransactions(dbModel), IsEqual.equalTo(3));
			MatcherAssert.assertThat(dbTransfers.size(), IsEqual.equalTo(3));

			MatcherAssert.assertThat(this.entry.getFromBlock.apply(dbModel),
					IsEqual.equalTo(Arrays.asList(transfer0, transfer1, transfer2)));
			MatcherAssert.assertThat(getBlockIndexes(dbTransfers), IsEqual.equalTo(Arrays.asList(0, 1, 2)));
			assertTransfersHaveBlockSetCorrectly(dbTransfers, dbModel);

			Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(), Mockito.eq(this.entry.dbModelClass));
		}
	}

	// endregion

	private static int getNumTransactions(final DbBlock dbBlock) {
		return StreamSupport.stream(TransactionRegistry.iterate().spliterator(), false).map(e -> e.getFromBlock.apply(dbBlock).size())
				.reduce(0, Integer::sum);
	}

	private static Collection<Integer> getBlockIndexes(final Collection<? extends AbstractBlockTransfer> dbTransfers) {
		return dbTransfers.stream().map(AbstractBlockTransfer::getBlkIndex).collect(Collectors.toList());
	}

	private static void assertTransfersHaveBlockSetCorrectly(final Collection<? extends AbstractBlockTransfer> dbTransfers,
			final DbBlock expectedBlock) {
		for (final AbstractBlockTransfer dbTransfer : dbTransfers) {
			MatcherAssert.assertThat(dbTransfer.getBlock(), IsEqual.equalTo(expectedBlock));
		}
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbForger = Mockito.mock(DbAccount.class);
		private final DbAccount dbLessor = Mockito.mock(DbAccount.class);
		private final Account harvester = Utils.generateRandomAccount();
		private final Account lessor = Utils.generateRandomAccount();
		private final Signature signature = Utils.generateRandomSignature();
		private final Hash prevBlockHash = Utils.generateRandomHash();
		private final Hash generationBlockHash = Utils.generateRandomHash();
		private final BlockDifficulty difficulty = new BlockDifficulty(BlockDifficulty.INITIAL_DIFFICULTY.getRaw() + 1234);
		private final BlockModelToDbModelMapping mapping = new BlockModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.harvester, DbAccount.class)).thenReturn(this.dbForger);
			Mockito.when(this.mapper.map(this.lessor, DbAccount.class)).thenReturn(this.dbLessor);
		}

		public Block createBlock(final Account lessor) {
			final Block block = new Block(this.harvester, this.prevBlockHash, this.generationBlockHash, new TimeInstant(4444),
					new BlockHeight(7));

			block.setDifficulty(this.difficulty);
			block.setLessor(lessor);
			block.setSignature(this.signature);
			return block;
		}

		public void assertDbModel(final DbBlock dbModel, final Hash expectedHash) {
			this.assertDbModel(dbModel, expectedHash, null);
		}

		public void assertDbModel(final DbBlock dbModel, final Hash expectedHash, final DbAccount expectedLessor) {
			MatcherAssert.assertThat(dbModel.getHarvester(), IsEqual.equalTo(this.dbForger));
			MatcherAssert.assertThat(dbModel.getPrevBlockHash(), IsEqual.equalTo(this.prevBlockHash));
			MatcherAssert.assertThat(dbModel.getGenerationHash(), IsEqual.equalTo(this.generationBlockHash));
			MatcherAssert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(4444));
			MatcherAssert.assertThat(dbModel.getHeight(), IsEqual.equalTo(7L));

			MatcherAssert.assertThat(dbModel.getDifficulty(), IsEqual.equalTo(this.difficulty.getRaw()));
			MatcherAssert.assertThat(dbModel.getLessor(), IsEqual.equalTo(expectedLessor));
			MatcherAssert.assertThat(dbModel.getHarvesterProof(), IsEqual.equalTo(this.signature.getBytes()));

			MatcherAssert.assertThat(dbModel.getBlockHash(), IsEqual.equalTo(expectedHash));
		}

		public void assertNoTransactions(final DbBlock dbModel) {
			MatcherAssert.assertThat(getNumTransactions(dbModel), IsEqual.equalTo(0));
		}

		// region add*

		public AbstractBlockTransfer addTransfer(
				final TransactionRegistry.Entry<? extends AbstractBlockTransfer, ? extends Transaction> typedEntry, final Block block) {
			@SuppressWarnings("unchecked")
			final TransactionRegistry.Entry<AbstractBlockTransfer, ? extends Transaction> entry = (TransactionRegistry.Entry<AbstractBlockTransfer, ? extends Transaction>) typedEntry;

			final Supplier<? extends Transaction> createModel = TestTransactionRegistry.findByType(typedEntry.type).createModel;
			final Transaction transaction = createModel.get();
			return this.addTransfer(block, transaction, DbTestUtils.createTransferDbModel(entry.dbModelClass), entry.dbModelClass);
		}

		public void addMultisigTransfer(final Block block, final DbTransferTransaction dbInnerTransferTransaction) {
			final DbMultisigTransaction dbMultisigTransfer = new DbMultisigTransaction();
			dbMultisigTransfer.setSenderProof(Utils.generateRandomSignature().getBytes());
			dbMultisigTransfer.setTransferTransaction(dbInnerTransferTransaction);

			final Transaction transfer = RandomTransactionFactory.createTransfer();
			final MultisigTransaction multisigTransfer = new MultisigTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(), transfer);
			this.addTransfer(block, multisigTransfer, dbMultisigTransfer, DbMultisigTransaction.class);
		}

		public void addUnsupportedTransfer(final Block block) {
			final Transaction transfer = new MockTransaction();
			this.addTransfer(block, transfer, new DbTransferTransaction(), DbTransferTransaction.class);
		}

		private <TDbTransfer extends AbstractTransfer, TModelTransfer extends Transaction> TDbTransfer addTransfer(final Block block,
				final TModelTransfer transfer, final TDbTransfer dbTransfer, final Class<TDbTransfer> dbTransferClass) {
			dbTransfer.setSenderProof(Utils.generateRandomSignature().getBytes());
			transfer.sign();

			Mockito.when(this.mapper.map(transfer, dbTransferClass)).thenReturn(dbTransfer);
			block.addTransaction(transfer);
			return dbTransfer;
		}

		// endregion
	}
}
