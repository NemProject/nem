package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.function.Consumer;

public class BlockDbModelToModelMappingTest {

	//region nemesis block mapping

	@Test
	public void nemesisDbModelCanBeMappedToNemesisModel() {
		// Arrange:
		final DeserializationContext deserializationContext = new DeserializationContext(new MockAccountLookup());
		final TestContext context = new TestContext();
		final DbBlock dbBlock = context.createDbBlock(null, null);
		dbBlock.setHeight(1L);

		// Act:
		final Block model = context.mapping.map(dbBlock);

		// Assert:
		Assert.assertThat(model, IsInstanceOf.instanceOf(NemesisBlock.class));
		Assert.assertThat(
				HashUtils.calculateHash(model),
				IsEqual.equalTo(HashUtils.calculateHash(NemesisBlock.fromResource(deserializationContext))));
	}

	//endregion

	//region no transaction mapping

	@Test
	public void blockWithMinimalInformationCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbBlock dbBlock = context.createDbBlock(null, null);

		// Act:
		final Block model = context.mapping.map(dbBlock);

		// Assert:
		context.assertModel(model);
		Assert.assertThat(model.getTransactions().isEmpty(), IsEqual.equalTo(true));
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
		Assert.assertThat(model.getTransactions().isEmpty(), IsEqual.equalTo(true));
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
		Assert.assertThat(model.getTransactions().isEmpty(), IsEqual.equalTo(true));
	}

	//endregion

	//region transaction mapping

	@Test
	public void oneBlockWithTransfersCanBeMappedToModelTestExistsForEachRegisteredTransactionType() {
		// Assert:
		Assert.assertThat(
				4, // the number of blockWith*CanBeMappedToModel tests
				IsEqual.equalTo(TransactionRegistry.size()));
	}

	@Test
	public void blockWithTransfersCanBeMappedToModel() {
		// Assert:
		assertBlockWithTransfersCanBeMappedToModel(TestContext::addTransfer);
	}

	@Test
	public void blockWithImportanceTransfersCanBeMappedToModel() {
		// Assert:
		assertBlockWithTransfersCanBeMappedToModel(TestContext::addImportanceTransfer);
	}

	@Test
	public void blockWithMultisigModificationsCanBeMappedToModel() {
		// Assert:
		assertBlockWithTransfersCanBeMappedToModel(TestContext::addMultisigModification);
	}

	@Test
	public void blockWithMultisigTransfersCanBeMappedToModel() {
		// Assert:
		assertBlockWithTransfersCanBeMappedToModel(TestContext::addMultisigTransfer);
	}

	private static void assertBlockWithTransfersCanBeMappedToModel(final TestContext.TransactionFactory factory) {
		// Arrange:
		final TestContext context = new TestContext();
		final DbBlock dbBlock = context.createDbBlock(null, null);

		final Transaction transfer2 = factory.create(context, dbBlock, 2);
		final Transaction transfer0 = factory.create(context, dbBlock, 0);
		final Transaction transfer1 = factory.create(context, dbBlock, 1);

		// Act:
		final Block model = context.mapping.map(dbBlock);

		// Assert:
		context.assertModel(model);
		Assert.assertThat(model.getTransactions().size(), IsEqual.equalTo(3));
		Assert.assertThat(model.getTransactions(), IsEqual.equalTo(Arrays.asList(transfer0, transfer1, transfer2)));
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(), Mockito.eq(Transaction.class));
	}

	@Test
	public void blockWithMixedTransfersCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbBlock dbBlock = context.createDbBlock(null, null);

		final Transaction transfer2 = context.addTransfer(dbBlock, 2);
		final Transaction transfer0 = context.addImportanceTransfer(dbBlock, 0);
		final Transaction transfer1 = context.addTransfer(dbBlock, 1);
		final Transaction transfer4 = context.addTransfer(dbBlock, 4);
		final Transaction transfer3 = context.addImportanceTransfer(dbBlock, 3);
		final Transaction transfer7 = context.addMultisigModification(dbBlock, 7);
		final Transaction transfer5 = context.addMultisigTransfer(dbBlock, 5);
		final Transaction transfer6 = context.addMultisigModification(dbBlock, 6);

		// Act:
		final Block model = context.mapping.map(dbBlock);

		// Assert:
		final int numTransactions = 8;
		final List<Transaction> orderedTransactions = Arrays.asList(
				transfer0, transfer1, transfer2, transfer3, transfer4, transfer5, transfer6, transfer7);

		context.assertModel(model);
		Assert.assertThat(model.getTransactions().size(), IsEqual.equalTo(numTransactions));
		Assert.assertThat(model.getTransactions(), IsEqual.equalTo(orderedTransactions));
		Mockito.verify(context.mapper, Mockito.times(numTransactions)).map(Mockito.any(), Mockito.eq(Transaction.class));

		// Sanity:
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			Assert.assertThat(
					"not all transaction types are represented",
					entry.getFromBlock.apply(dbBlock).isEmpty(),
					IsEqual.equalTo(false));
		}
	}

	@Test
	public void innerMultisigTransfersAreNotIncludedDirectlyInModelBlock() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbBlock dbBlock = context.createDbBlock(null, null);

		final Transaction transfer0 = context.addTransfer(dbBlock, 0);
		final Transaction transfer1 = context.addMultisigTransferWithInnerTransfer(dbBlock, 1);
		final Transaction transfer2 = context.addTransfer(dbBlock, 2);
		final Transaction transfer3 = context.addImportanceTransfer(dbBlock, 3);
		final Transaction transfer4 = context.addMultisigTransferWithInnerTransfer(dbBlock, 4);

		// Act:
		final Block model = context.mapping.map(dbBlock);

		// Assert:
		final int numTransactions = 5;
		final List<Transaction> orderedTransactions = Arrays.asList(transfer0, transfer1, transfer2, transfer3, transfer4);

		context.assertModel(model);
		Assert.assertThat(model.getTransactions().size(), IsEqual.equalTo(numTransactions));
		Assert.assertThat(model.getTransactions(), IsEqual.equalTo(orderedTransactions));
		Mockito.verify(context.mapper, Mockito.times(numTransactions)).map(Mockito.any(), Mockito.eq(Transaction.class));

		// Sanity:
		Assert.assertThat(dbBlock.getBlockTransferTransactions().size(), IsEqual.equalTo(4));
		Assert.assertThat(dbBlock.getBlockImportanceTransferTransactions().size(), IsEqual.equalTo(1));
		Assert.assertThat(dbBlock.getBlockMultisigTransactions().size(), IsEqual.equalTo(2));
	}

	//endregion

	private static class TestContext {

		@FunctionalInterface
		private static interface TransactionFactory {
			public Transaction create(final TestContext context, final DbBlock block, final int blockIndex);
		}

		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbHarvester = Mockito.mock(DbAccount.class);
		private final DbAccount dbLessor = Mockito.mock(DbAccount.class);
		private final Account harvester = Utils.generateRandomAccount();
		private final Account lessor = Utils.generateRandomAccount();
		private final Signature signature = Utils.generateRandomSignature();
		private final Hash prevBlockHash = Utils.generateRandomHash();
		private final Hash generationBlockHash = Utils.generateRandomHash();
		private final BlockDbModelToModelMapping mapping = new BlockDbModelToModelMapping(this.mapper, new MockAccountLookup());

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbHarvester, Account.class)).thenReturn(this.harvester);
			Mockito.when(this.mapper.map(this.dbLessor, Account.class)).thenReturn(this.lessor);
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

		public void assertModel(final Block model, final long expectedDifficulty, final Account expectedLessor) {
			Assert.assertThat(model.getSigner(), IsEqual.equalTo(this.harvester));
			Assert.assertThat(model.getPreviousBlockHash(), IsEqual.equalTo(this.prevBlockHash));
			Assert.assertThat(model.getGenerationHash(), IsEqual.equalTo(this.generationBlockHash));
			Assert.assertThat(model.getTimeStamp(), IsEqual.equalTo(new TimeInstant(4444)));
			Assert.assertThat(model.getHeight(), IsEqual.equalTo(new BlockHeight(7)));

			Assert.assertThat(model.getDifficulty(), IsEqual.equalTo(new BlockDifficulty(expectedDifficulty)));
			Assert.assertThat(model.getLessor(), IsEqual.equalTo(expectedLessor));
			Assert.assertThat(model.getSignature(), IsEqual.equalTo(this.signature));
		}

		//region add*

		public TransferTransaction addTransfer(final DbBlock block, final int blockIndex) {
			return this.addTransfer(
					dbTransfer -> block.getBlockTransferTransactions().add(dbTransfer),
					blockIndex,
					new DbTransferTransaction(),
					TransferTransaction.class);
		}

		public ImportanceTransferTransaction addImportanceTransfer(final DbBlock block, final int blockIndex) {
			return this.addTransfer(
					dbTransfer -> block.getBlockImportanceTransferTransactions().add(dbTransfer),
					blockIndex,
					new DbImportanceTransferTransaction(),
					ImportanceTransferTransaction.class);
		}

		public MultisigAggregateModificationTransaction addMultisigModification(final DbBlock block, final int blockIndex) {
			return this.addTransfer(
					dbTransfer -> block.getBlockMultisigAggregateModificationTransactions().add(dbTransfer),
					blockIndex,
					new DbMultisigAggregateModificationTransaction(),
					MultisigAggregateModificationTransaction.class);
		}

		public MultisigTransaction addMultisigTransfer(final DbBlock block, final int blockIndex) {
			return this.addTransfer(
					dbTransfer -> block.getBlockMultisigTransactions().add(dbTransfer),
					blockIndex,
					new DbMultisigTransaction(),
					org.nem.core.model.MultisigTransaction.class);
		}

		public MultisigTransaction addMultisigTransferWithInnerTransfer(final DbBlock block, final int blockIndex) {
			final DbTransferTransaction dbInnerTransfer = new DbTransferTransaction();
			this.addTransfer(
					dbTransfer -> block.getBlockTransferTransactions().add(dbTransfer),
					blockIndex,
					dbInnerTransfer,
					TransferTransaction.class);
			dbInnerTransfer.setSenderProof(null);

			final DbMultisigTransaction dbMultisigTransfer = new DbMultisigTransaction();
			dbMultisigTransfer.setTransferTransaction(dbInnerTransfer);
			return this.addTransfer(
					dbTransfer -> block.getBlockMultisigTransactions().add(dbTransfer),
					blockIndex,
					dbMultisigTransfer,
					org.nem.core.model.MultisigTransaction.class);
		}

		private <TDbTransfer extends AbstractBlockTransfer, TModelTransfer extends Transaction> TModelTransfer addTransfer(
				final Consumer<TDbTransfer> addTransaction,
				final int blockIndex,
				final TDbTransfer dbTransfer,
				final Class<TModelTransfer> modelClass) {
			dbTransfer.setSenderProof(Utils.generateRandomSignature().getBytes());
			dbTransfer.setBlkIndex(blockIndex);
			dbTransfer.setReferencedTransaction(0L);
			final TModelTransfer transfer = Mockito.mock(modelClass);
			Mockito.when(this.mapper.map(dbTransfer, Transaction.class)).thenReturn(transfer);
			addTransaction.accept(dbTransfer);
			return transfer;
		}

		//endregion
	}
}