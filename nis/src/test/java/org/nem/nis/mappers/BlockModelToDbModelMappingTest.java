package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.test.RandomTransactionFactory;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class BlockModelToDbModelMappingTest {

	//region no transaction mapping

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

	//endregion

	//region transaction mapping

	@Test
	public void oneBlockWithTransfersCanBeMappedToDbModelTestExistsForEachRegisteredTransactionType() {
		// Assert:
		Assert.assertThat(
				6, // the number of blockWith*CanBeMappedToDbModel tests
				IsEqual.equalTo(TransactionRegistry.size()));
	}

	@Test
	public void blockWithTransfersCanBeMappedToDbModel() {
		// Assert:
		assertBlockWithTransfersCanBeMappedToDbModel(
				TestContext::addTransfer,
				DbBlock::getBlockTransferTransactions,
				DbTransferTransaction.class);
	}

	@Test
	public void blockWithImportanceTransfersCanBeMappedToDbModel() {
		// Assert:
		assertBlockWithTransfersCanBeMappedToDbModel(
				TestContext::addImportanceTransfer,
				DbBlock::getBlockImportanceTransferTransactions,
				DbImportanceTransferTransaction.class);
	}

	@Test
	public void blockWithMultisigModificationsCanBeMappedToDbModel() {
		// Assert:
		assertBlockWithTransfersCanBeMappedToDbModel(
				TestContext::addMultisigModification,
				DbBlock::getBlockMultisigAggregateModificationTransactions,
				DbMultisigAggregateModificationTransaction.class);
	}

	@Test
	public void blockWithMultisigTransfersCanBeMappedToDbModel() {
		// Assert:
		assertBlockWithTransfersCanBeMappedToDbModel(
				TestContext::addMultisigTransfer,
				DbBlock::getBlockMultisigTransactions,
				DbMultisigTransaction.class);
	}

	@Test
	public void blockWithProvisionNamespaceTransactionCanBeMappedToDbModel() {
		// Assert:
		assertBlockWithTransfersCanBeMappedToDbModel(
				TestContext::addProvisionNamespaceTransaction,
				DbBlock::getBlockProvisionNamespaceTransactions,
				DbProvisionNamespaceTransaction.class);
	}

	@Test
	public void blockWithMosaicCreationTransactionCanBeMappedToDbModel() {
		// Assert:
		assertBlockWithTransfersCanBeMappedToDbModel(
				TestContext::addMosaicCreationTransaction,
				DbBlock::getBlockMosaicCreationTransactions,
				DbMosaicCreationTransaction.class);
	}

	private static void assertBlockWithTransfersCanBeMappedToDbModel(
			final BiFunction<TestContext, Block, AbstractTransfer> factory,
			final Function<DbBlock, Collection<? extends AbstractBlockTransfer>> getMatchingTransactions,
			final Class<?> expectedClass) {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = context.createBlock(null);

		final AbstractTransfer transfer0 = factory.apply(context, block);
		final AbstractTransfer transfer1 = factory.apply(context, block);
		final AbstractTransfer transfer2 = factory.apply(context, block);

		// Act:
		final DbBlock dbModel = context.mapping.map(block);
		final Collection<? extends AbstractBlockTransfer> dbTransfers = getMatchingTransactions.apply(dbModel);

		// Assert:
		context.assertDbModel(dbModel, HashUtils.calculateHash(block));

		Assert.assertThat(getNumTransactions(dbModel), IsEqual.equalTo(3));
		Assert.assertThat(dbTransfers.size(), IsEqual.equalTo(3));

		Assert.assertThat(getMatchingTransactions.apply(dbModel), IsEqual.equalTo(Arrays.asList(transfer0, transfer1, transfer2)));
		Assert.assertThat(getBlockIndexes(dbTransfers), IsEqual.equalTo(Arrays.asList(0, 1, 2)));
		assertTransfersHaveBlockSetCorrectly(dbTransfers, dbModel);

		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(), Mockito.eq(expectedClass));
	}

	@Test
	public void blockWithUnsupportedTransfersCannotBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = context.createBlock(null);

		context.addUnsupportedTransfer(block);

		// Act:
		ExceptionAssert.assertThrows(
				v -> context.mapping.map(block),
				IllegalArgumentException.class);
	}

	@Test
	public void blockWithMixedTransfersCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = context.createBlock(null);

		final AbstractTransfer transfer0 = context.addTransfer(block);
		final AbstractTransfer transfer1 = context.addImportanceTransfer(block);
		final AbstractTransfer transfer2 = context.addTransfer(block);
		final AbstractTransfer transfer3 = context.addMosaicCreationTransaction(block);
		final AbstractTransfer transfer4 = context.addTransfer(block);
		final AbstractTransfer transfer5 = context.addProvisionNamespaceTransaction(block);
		final AbstractTransfer transfer6 = context.addImportanceTransfer(block);
		final AbstractTransfer transfer7 = context.addMosaicCreationTransaction(block);
		final AbstractTransfer transfer8 = context.addMultisigModification(block);
		final AbstractTransfer transfer9 = context.addProvisionNamespaceTransaction(block);
		final AbstractTransfer transfer10 = context.addMultisigTransfer(block);
		final AbstractTransfer transfer11 = context.addMultisigModification(block);
		final AbstractTransfer transfer12 = context.addMultisigTransfer(block);

		// Act:
		final DbBlock dbModel = context.mapping.map(block);

		// Assert:
		context.assertDbModel(dbModel, HashUtils.calculateHash(block));

		Assert.assertThat(getNumTransactions(dbModel), IsEqual.equalTo(13));

		Collection<? extends AbstractBlockTransfer> transfers = dbModel.getBlockTransferTransactions();
		Assert.assertThat(transfers.size(), IsEqual.equalTo(3));
		Assert.assertThat(transfers, IsEqual.equalTo(Arrays.asList(transfer0, transfer2, transfer4)));
		Assert.assertThat(getBlockIndexes(transfers), IsEqual.equalTo(Arrays.asList(0, 2, 4)));

		transfers = dbModel.getBlockImportanceTransferTransactions();
		Assert.assertThat(transfers.size(), IsEqual.equalTo(2));
		Assert.assertThat(transfers, IsEqual.equalTo(Arrays.asList(transfer1, transfer6)));
		Assert.assertThat(getBlockIndexes(transfers), IsEqual.equalTo(Arrays.asList(1, 6)));

		transfers = dbModel.getBlockMultisigAggregateModificationTransactions();
		Assert.assertThat(transfers.size(), IsEqual.equalTo(2));
		Assert.assertThat(transfers, IsEqual.equalTo(Arrays.asList(transfer8, transfer11)));
		Assert.assertThat(getBlockIndexes(transfers), IsEqual.equalTo(Arrays.asList(8, 11)));

		transfers = dbModel.getBlockMultisigTransactions();
		Assert.assertThat(transfers.size(), IsEqual.equalTo(2));
		Assert.assertThat(transfers, IsEqual.equalTo(Arrays.asList(transfer10, transfer12)));
		Assert.assertThat(getBlockIndexes(transfers), IsEqual.equalTo(Arrays.asList(10, 12)));

		transfers = dbModel.getBlockProvisionNamespaceTransactions();
		Assert.assertThat(transfers.size(), IsEqual.equalTo(2));
		Assert.assertThat(transfers, IsEqual.equalTo(Arrays.asList(transfer5, transfer9)));
		Assert.assertThat(getBlockIndexes(transfers), IsEqual.equalTo(Arrays.asList(5, 9)));

		transfers = dbModel.getBlockMosaicCreationTransactions();
		Assert.assertThat(transfers.size(), IsEqual.equalTo(2));
		Assert.assertThat(transfers, IsEqual.equalTo(Arrays.asList(transfer3, transfer7)));
		Assert.assertThat(getBlockIndexes(transfers), IsEqual.equalTo(Arrays.asList(3, 7)));

		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(), Mockito.eq(DbTransferTransaction.class));
		Mockito.verify(context.mapper, Mockito.times(2)).map(Mockito.any(), Mockito.eq(DbImportanceTransferTransaction.class));
		Mockito.verify(context.mapper, Mockito.times(2)).map(Mockito.any(), Mockito.eq(DbMultisigAggregateModificationTransaction.class));
		Mockito.verify(context.mapper, Mockito.times(2)).map(Mockito.any(), Mockito.eq(DbMultisigTransaction.class));
		Mockito.verify(context.mapper, Mockito.times(2)).map(Mockito.any(), Mockito.eq(DbProvisionNamespaceTransaction.class));
		Mockito.verify(context.mapper, Mockito.times(2)).map(Mockito.any(), Mockito.eq(DbMosaicCreationTransaction.class));

		// Sanity:
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			assertTransfersHaveBlockSetCorrectly(entry.getFromBlock.apply(dbModel), dbModel);

			Assert.assertThat(
					"not all transaction types are represented",
					entry.getFromBlock.apply(dbModel).isEmpty(),
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

		final AbstractTransfer transfer0 = context.addTransfer(block);
		final AbstractTransfer transfer1 = context.addMultisigTransfer(block, innerDbTransferTransaction1);
		final AbstractTransfer transfer2 = context.addTransfer(block);
		final AbstractTransfer transfer3 = context.addTransfer(block);
		final AbstractTransfer transfer4 = context.addImportanceTransfer(block);
		final AbstractTransfer transfer5 = context.addProvisionNamespaceTransaction(block);
		final AbstractTransfer transfer6 = context.addMultisigTransfer(block, innerDbTransferTransaction2);
		final AbstractTransfer transfer7 = context.addMosaicCreationTransaction(block);

		// Act:
		final DbBlock dbModel = context.mapping.map(block);

		// Assert: db model properties
		context.assertDbModel(dbModel, HashUtils.calculateHash(block));

		// Assert: db model transactions
		Assert.assertThat(getNumTransactions(dbModel), IsEqual.equalTo(8));

		Collection<? extends AbstractBlockTransfer> transfers = dbModel.getBlockTransferTransactions();
		Assert.assertThat(transfers.size(), IsEqual.equalTo(3));
		Assert.assertThat(transfers, IsEqual.equalTo(Arrays.asList(transfer0, transfer2, transfer3)));
		Assert.assertThat(getBlockIndexes(transfers), IsEqual.equalTo(Arrays.asList(0, 2, 3)));

		transfers = dbModel.getBlockImportanceTransferTransactions();
		Assert.assertThat(transfers.size(), IsEqual.equalTo(1));
		Assert.assertThat(transfers, IsEqual.equalTo(Collections.singletonList(transfer4)));
		Assert.assertThat(getBlockIndexes(transfers), IsEqual.equalTo(Collections.singletonList(4)));

		transfers = dbModel.getBlockMultisigTransactions();
		Assert.assertThat(transfers.size(), IsEqual.equalTo(2));
		Assert.assertThat(transfers, IsEqual.equalTo(Arrays.asList(transfer1, transfer6)));
		Assert.assertThat(getBlockIndexes(transfers), IsEqual.equalTo(Arrays.asList(1, 6)));

		transfers = dbModel.getBlockProvisionNamespaceTransactions();
		Assert.assertThat(transfers.size(), IsEqual.equalTo(1));
		Assert.assertThat(transfers, IsEqual.equalTo(Collections.singletonList(transfer5)));
		Assert.assertThat(getBlockIndexes(transfers), IsEqual.equalTo(Collections.singletonList(5)));

		transfers = dbModel.getBlockMosaicCreationTransactions();
		Assert.assertThat(transfers.size(), IsEqual.equalTo(1));
		Assert.assertThat(transfers, IsEqual.equalTo(Collections.singletonList(transfer7)));
		Assert.assertThat(getBlockIndexes(transfers), IsEqual.equalTo(Collections.singletonList(7)));

		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			assertTransfersHaveBlockSetCorrectly(entry.getFromBlock.apply(dbModel), dbModel);
		}

		// Assert: multisig inner transactions
		// inner transaction does not belong to a block, so it won't have order id
		Assert.assertThat(innerDbTransferTransaction1.getBlkIndex(), IsEqual.equalTo(1));
		Assert.assertThat(innerDbTransferTransaction1.getBlock(), IsEqual.equalTo(dbModel));

		Assert.assertThat(innerDbTransferTransaction2.getBlkIndex(), IsEqual.equalTo(6));
		Assert.assertThat(innerDbTransferTransaction2.getBlock(), IsEqual.equalTo(dbModel));
	}

	//endregion

	private static int getNumTransactions(final DbBlock dbBlock) {
		return StreamSupport.stream(TransactionRegistry.iterate().spliterator(), false)
				.map(e -> e.getFromBlock.apply(dbBlock).size())
				.reduce(0, Integer::sum);
	}

	private static Collection<Integer> getBlockIndexes(final Collection<? extends AbstractBlockTransfer> dbTransfers) {
		return dbTransfers.stream().map(AbstractBlockTransfer::getBlkIndex).collect(Collectors.toList());
	}

	private static void assertTransfersHaveBlockSetCorrectly(
			final Collection<? extends AbstractBlockTransfer> dbTransfers,
			final DbBlock expectedBlock) {
		for (final AbstractBlockTransfer dbTransfer : dbTransfers) {
			Assert.assertThat(dbTransfer.getBlock(), IsEqual.equalTo(expectedBlock));
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
			final Block block = new Block(
					this.harvester,
					this.prevBlockHash,
					this.generationBlockHash,
					new TimeInstant(4444),
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
			Assert.assertThat(dbModel.getHarvester(), IsEqual.equalTo(this.dbForger));
			Assert.assertThat(dbModel.getPrevBlockHash(), IsEqual.equalTo(this.prevBlockHash));
			Assert.assertThat(dbModel.getGenerationHash(), IsEqual.equalTo(this.generationBlockHash));
			Assert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(4444));
			Assert.assertThat(dbModel.getHeight(), IsEqual.equalTo(7L));

			Assert.assertThat(dbModel.getDifficulty(), IsEqual.equalTo(this.difficulty.getRaw()));
			Assert.assertThat(dbModel.getLessor(), IsEqual.equalTo(expectedLessor));
			Assert.assertThat(dbModel.getHarvesterProof(), IsEqual.equalTo(this.signature.getBytes()));

			Assert.assertThat(dbModel.getBlockHash(), IsEqual.equalTo(expectedHash));
		}

		public void assertNoTransactions(final DbBlock dbModel) {
			Assert.assertThat(getNumTransactions(dbModel), IsEqual.equalTo(0));
		}

		//region add*

		public DbTransferTransaction addTransfer(final Block block) {
			final Transaction transfer = RandomTransactionFactory.createTransfer();
			return this.addTransfer(block, transfer, new DbTransferTransaction(), DbTransferTransaction.class);
		}

		public DbImportanceTransferTransaction addImportanceTransfer(final Block block) {
			final Transaction transfer = RandomTransactionFactory.createImportanceTransfer();
			return this.addTransfer(block, transfer, new DbImportanceTransferTransaction(), DbImportanceTransferTransaction.class);
		}

		public DbMultisigAggregateModificationTransaction addMultisigModification(final Block block) {
			final Transaction transfer = RandomTransactionFactory.createMultisigModification();
			return this.addTransfer(block, transfer, new DbMultisigAggregateModificationTransaction(), DbMultisigAggregateModificationTransaction.class);
		}

		public DbMultisigTransaction addMultisigTransfer(final Block block) {
			final Transaction transfer = RandomTransactionFactory.createTransfer();
			final MultisigTransaction multisigTransfer = new MultisigTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(), transfer);
			return this.addTransfer(block, multisigTransfer, new DbMultisigTransaction(), DbMultisigTransaction.class);
		}

		public DbMultisigTransaction addMultisigTransfer(final Block block, final DbTransferTransaction dbInnerTransferTransaction) {
			final DbMultisigTransaction dbMultisigTransfer = new DbMultisigTransaction();
			dbMultisigTransfer.setSenderProof(Utils.generateRandomSignature().getBytes());
			dbMultisigTransfer.setTransferTransaction(dbInnerTransferTransaction);

			final Transaction transfer = RandomTransactionFactory.createTransfer();
			final MultisigTransaction multisigTransfer = new MultisigTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(), transfer);
			return this.addTransfer(block, multisigTransfer, dbMultisigTransfer, DbMultisigTransaction.class);
		}

		public DbProvisionNamespaceTransaction addProvisionNamespaceTransaction(final Block block) {
			final Transaction transfer = RandomTransactionFactory.createProvisionNamespaceTransaction();
			final DbProvisionNamespaceTransaction dbTransfer = this.addTransfer(
					block,
					transfer,
					new DbProvisionNamespaceTransaction(),
					DbProvisionNamespaceTransaction.class);
			dbTransfer.setNamespace(new DbNamespace());
			return dbTransfer;
		}

		public DbMosaicCreationTransaction addMosaicCreationTransaction(final Block block) {
			final Transaction transfer = RandomTransactionFactory.createMosaicCreationTransaction();
			final DbMosaicCreationTransaction dbTransfer = this.addTransfer(
					block,
					transfer,
					new DbMosaicCreationTransaction(),
					DbMosaicCreationTransaction.class);
			dbTransfer.setMosaic(new DbMosaic());
			return dbTransfer;
		}

		public DbTransferTransaction addUnsupportedTransfer(final Block block) {
			final Transaction transfer = new MockTransaction();
			return this.addTransfer(block, transfer, new DbTransferTransaction(), DbTransferTransaction.class);
		}

		private <TDbTransfer extends AbstractTransfer, TModelTransfer extends Transaction> TDbTransfer addTransfer(
				final Block block,
				final TModelTransfer transfer,
				final TDbTransfer dbTransfer,
				final Class<TDbTransfer> dbTransferClass) {
			dbTransfer.setSenderProof(Utils.generateRandomSignature().getBytes());
			transfer.sign();

			Mockito.when(this.mapper.map(transfer, dbTransferClass)).thenReturn(dbTransfer);
			block.addTransaction(transfer);
			return dbTransfer;
		}

		//endregion
	}
}