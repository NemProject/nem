package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
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
		final org.nem.nis.dbmodel.Block dbBlock = context.createDbBlock(null, null);
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
		final org.nem.nis.dbmodel.Block dbBlock = context.createDbBlock(null, null);

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
		final org.nem.nis.dbmodel.Block dbBlock = context.createDbBlock(111L, null);

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
		final org.nem.nis.dbmodel.Block dbBlock = context.createDbBlock(null, context.dbLessor);

		// Act:
		final Block model = context.mapping.map(dbBlock);

		// Assert:
		context.assertModel(model, 0L, context.lessor);
		Assert.assertThat(model.getTransactions().isEmpty(), IsEqual.equalTo(true));
	}

	//endregion

	//region transaction mapping

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

	private static void assertBlockWithTransfersCanBeMappedToModel(final TestContext.TransactionFactory factory) {
		// Arrange:
		final TestContext context = new TestContext();
		final org.nem.nis.dbmodel.Block dbBlock = context.createDbBlock(null, null);

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
		final org.nem.nis.dbmodel.Block dbBlock = context.createDbBlock(null, null);

		final Transaction transfer2 = context.addTransfer(dbBlock, 2);
		final Transaction transfer0 = context.addImportanceTransfer(dbBlock, 0);
		final Transaction transfer1 = context.addTransfer(dbBlock, 1);
		final Transaction transfer4 = context.addTransfer(dbBlock, 4);
		final Transaction transfer3 = context.addImportanceTransfer(dbBlock, 3);

		// Act:
		final Block model = context.mapping.map(dbBlock);

		// Assert:
		context.assertModel(model);
		Assert.assertThat(model.getTransactions().size(), IsEqual.equalTo(5));
		Assert.assertThat(
				model.getTransactions(),
				IsEqual.equalTo(Arrays.asList(transfer0, transfer1, transfer2, transfer3, transfer4)));
		Mockito.verify(context.mapper, Mockito.times(5)).map(Mockito.any(), Mockito.eq(Transaction.class));
	}

	//endregion

	private static class TestContext {

		@FunctionalInterface
		private static interface TransactionFactory {
			public Transaction create(final TestContext context, final org.nem.nis.dbmodel.Block block, final int blockIndex);
		}

		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final org.nem.nis.dbmodel.Account dbForger = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final org.nem.nis.dbmodel.Account dbLessor = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final Account forger = Utils.generateRandomAccount();
		private final Account lessor = Utils.generateRandomAccount();
		private final Signature signature = Utils.generateRandomSignature();
		private final Hash prevBlockHash = Utils.generateRandomHash();
		private final Hash generationBlockHash = Utils.generateRandomHash();
		private final BlockDbModelToModelMapping mapping = new BlockDbModelToModelMapping(this.mapper, new MockAccountLookup());

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbForger, Account.class)).thenReturn(this.forger);
			Mockito.when(this.mapper.map(this.dbLessor, Account.class)).thenReturn(this.lessor);
		}

		public org.nem.nis.dbmodel.Block createDbBlock(final Long difficulty, final org.nem.nis.dbmodel.Account lessor) {
			final org.nem.nis.dbmodel.Block dbBlock = new org.nem.nis.dbmodel.Block();
			dbBlock.setForger(this.dbForger);
			dbBlock.setPrevBlockHash(this.prevBlockHash);
			dbBlock.setGenerationHash(this.generationBlockHash);
			dbBlock.setTimeStamp(4444);
			dbBlock.setHeight(7L);

			dbBlock.setDifficulty(difficulty);
			dbBlock.setLessor(lessor);
			dbBlock.setForgerProof(this.signature.getBytes());

			dbBlock.setBlockTransfers(new ArrayList<>());
			dbBlock.setBlockImportanceTransfers(new ArrayList<>());
			return dbBlock;
		}

		public void assertModel(final Block model) {
			this.assertModel(model, 0L, null);
		}

		public void assertModel(final Block model, final long expectedDifficulty, final Account expectedLessor) {
			Assert.assertThat(model.getSigner(), IsEqual.equalTo(this.forger));
			Assert.assertThat(model.getPreviousBlockHash(), IsEqual.equalTo(this.prevBlockHash));
			Assert.assertThat(model.getGenerationHash(), IsEqual.equalTo(this.generationBlockHash));
			Assert.assertThat(model.getTimeStamp(), IsEqual.equalTo(new TimeInstant(4444)));
			Assert.assertThat(model.getHeight(), IsEqual.equalTo(new BlockHeight(7)));

			Assert.assertThat(model.getDifficulty(), IsEqual.equalTo(new BlockDifficulty(expectedDifficulty)));
			Assert.assertThat(model.getLessor(), IsEqual.equalTo(expectedLessor));
			Assert.assertThat(model.getSignature(), IsEqual.equalTo(this.signature));
		}

		//region add*

		public TransferTransaction addTransfer(final org.nem.nis.dbmodel.Block block, final int blockIndex) {
			return this.addTransfer(
					dbTransfer -> block.getBlockTransfers().add(dbTransfer),
					blockIndex,
					new Transfer(),
					TransferTransaction.class);
		}

		public ImportanceTransferTransaction addImportanceTransfer(final org.nem.nis.dbmodel.Block block, final int blockIndex) {
			return this.addTransfer(
					dbTransfer -> block.getBlockImportanceTransfers().add(dbTransfer),
					blockIndex,
					new ImportanceTransfer(),
					ImportanceTransferTransaction.class);
		}

		private <TDbTransfer extends AbstractBlockTransfer, TModelTransfer extends Transaction> TModelTransfer addTransfer(
				final Consumer<TDbTransfer> addTransaction,
				final int blockIndex,
				final TDbTransfer dbTransfer,
				final Class<TModelTransfer> modelClass) {
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