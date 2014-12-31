package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class BlockModelToDbModelMappingTest {

	//region no transaction mapping

	@Test
	public void blockWithMinimalInformationCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = context.createBlock(null);

		// Act:
		final org.nem.nis.dbmodel.Block dbModel = context.mapping.map(block);

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
		final org.nem.nis.dbmodel.Block dbModel = context.mapping.map(block);

		// Assert:
		context.assertDbModel(dbModel, HashUtils.calculateHash(block), context.dbLessor);
		context.assertNoTransactions(dbModel);
	}

	//endregion

	//region transaction mapping

	@Test
	public void blockWithTransfersCanBeMappedToDbModel() {
		// Assert:
		assertBlockWithTransfersCanBeMappedToDbModel(
				TestContext::addTransfer,
				org.nem.nis.dbmodel.Block::getBlockTransfers,
				Transfer.class);
	}

	@Test
	public void blockWithImportanceTransfersCanBeMappedToDbModel() {
		// Assert:
		assertBlockWithTransfersCanBeMappedToDbModel(
				TestContext::addImportanceTransfer,
				org.nem.nis.dbmodel.Block::getBlockImportanceTransfers,
				ImportanceTransfer.class);
	}

	private static void assertBlockWithTransfersCanBeMappedToDbModel(
			final BiFunction<TestContext, Block, AbstractTransfer> factory,
			final Function<org.nem.nis.dbmodel.Block, Collection<? extends AbstractTransfer>> getMatchingTransactions,
			final Class<?> expectedClass) {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = context.createBlock(null);

		final AbstractTransfer transfer0 = factory.apply(context, block);
		final AbstractTransfer transfer1 = factory.apply(context, block);
		final AbstractTransfer transfer2 = factory.apply(context, block);

		// Act:
		final org.nem.nis.dbmodel.Block dbModel = context.mapping.map(block);
		final Collection<? extends AbstractTransfer> dbTransfers = getMatchingTransactions.apply(dbModel);

		// Assert:
		context.assertDbModel(dbModel, HashUtils.calculateHash(block));

		Assert.assertThat(getNumTransactions(dbModel), IsEqual.equalTo(3));
		Assert.assertThat(dbTransfers.size(), IsEqual.equalTo(3));

		Assert.assertThat(getMatchingTransactions.apply(dbModel), IsEqual.equalTo(Arrays.asList(transfer0, transfer1, transfer2)));
		Assert.assertThat(getBlockIndexes(dbTransfers), IsEqual.equalTo(Arrays.asList(0, 1, 2)));
		Assert.assertThat(getOrderIndexes(dbTransfers), IsEqual.equalTo(Arrays.asList(0, 1, 2)));

		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(), Mockito.eq(expectedClass));
	}

	@Test
	public void blockWithMixedTransfersCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = context.createBlock(null);

		final AbstractTransfer transfer0 = context.addTransfer(block);
		final AbstractTransfer transfer1 = context.addImportanceTransfer(block);
		final AbstractTransfer transfer2 = context.addTransfer(block);
		final AbstractTransfer transfer3 = context.addTransfer(block);
		final AbstractTransfer transfer4 = context.addImportanceTransfer(block);

		// Act:
		final org.nem.nis.dbmodel.Block dbModel = context.mapping.map(block);

		// Assert:
		context.assertDbModel(dbModel, HashUtils.calculateHash(block));

		Assert.assertThat(getNumTransactions(dbModel), IsEqual.equalTo(5));

		Collection<? extends AbstractTransfer> transfers = dbModel.getBlockTransfers();
		Assert.assertThat(transfers.size(), IsEqual.equalTo(3));
		Assert.assertThat(transfers, IsEqual.equalTo(Arrays.asList(transfer0, transfer2, transfer3)));
		Assert.assertThat(getBlockIndexes(transfers), IsEqual.equalTo(Arrays.asList(0, 2, 3)));
		Assert.assertThat(getOrderIndexes(transfers), IsEqual.equalTo(Arrays.asList(0, 1, 2)));

		transfers = dbModel.getBlockImportanceTransfers();
		Assert.assertThat(transfers.size(), IsEqual.equalTo(2));
		Assert.assertThat(transfers, IsEqual.equalTo(Arrays.asList(transfer1, transfer4)));
		Assert.assertThat(getBlockIndexes(transfers), IsEqual.equalTo(Arrays.asList(1, 4)));
		Assert.assertThat(getOrderIndexes(transfers), IsEqual.equalTo(Arrays.asList(0, 1)));

		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(), Mockito.eq(Transfer.class));
		Mockito.verify(context.mapper, Mockito.times(2)).map(Mockito.any(), Mockito.eq(ImportanceTransfer.class));
	}

	//endregion

	private static int getNumTransactions(final org.nem.nis.dbmodel.Block block) {
		return block.getBlockTransfers().size() + block.getBlockImportanceTransfers().size();
	}

	private static Collection<Integer> getBlockIndexes(final Collection<? extends AbstractTransfer> transfers) {
		return transfers.stream().map(AbstractTransfer::getBlkIndex).collect(Collectors.toList());
	}

	private static Collection<Integer> getOrderIndexes(final Collection<? extends AbstractTransfer> transfers) {
		return transfers.stream().map(AbstractTransfer::getOrderId).collect(Collectors.toList());
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final org.nem.nis.dbmodel.Account dbForger = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final org.nem.nis.dbmodel.Account dbLessor = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final Account forger = Utils.generateRandomAccount();
		private final Account lessor = Utils.generateRandomAccount();
		private final Signature signature = Utils.generateRandomSignature();
		private final Hash prevBlockHash = Utils.generateRandomHash();
		private final Hash generationBlockHash = Utils.generateRandomHash();
		private final BlockDifficulty difficulty = new BlockDifficulty(BlockDifficulty.INITIAL_DIFFICULTY.getRaw() + 1234);
		private final BlockModelToDbModelMapping mapping = new BlockModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.forger, org.nem.nis.dbmodel.Account.class)).thenReturn(this.dbForger);
			Mockito.when(this.mapper.map(this.lessor, org.nem.nis.dbmodel.Account.class)).thenReturn(this.dbLessor);
		}

		public Block createBlock(final Account lessor) {
			final Block block = new Block(
					this.forger,
					this.prevBlockHash,
					this.generationBlockHash,
					new TimeInstant(4444),
					new BlockHeight(7));

			block.setDifficulty(this.difficulty);
			block.setLessor(lessor);
			block.setSignature(this.signature);
			return block;
		}

		public void assertDbModel(final org.nem.nis.dbmodel.Block dbModel, final Hash expectedHash) {
			this.assertDbModel(dbModel, expectedHash, null);
		}

		public void assertDbModel(final org.nem.nis.dbmodel.Block dbModel, final Hash expectedHash, final org.nem.nis.dbmodel.Account expectedLessor) {
			Assert.assertThat(dbModel.getForger(), IsEqual.equalTo(this.dbForger));
			Assert.assertThat(dbModel.getPrevBlockHash(), IsEqual.equalTo(this.prevBlockHash));
			Assert.assertThat(dbModel.getGenerationHash(), IsEqual.equalTo(this.generationBlockHash));
			Assert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(4444));
			Assert.assertThat(dbModel.getHeight(), IsEqual.equalTo(7L));

			Assert.assertThat(dbModel.getDifficulty(), IsEqual.equalTo(this.difficulty.getRaw()));
			Assert.assertThat(dbModel.getLessor(), IsEqual.equalTo(expectedLessor));
			Assert.assertThat(dbModel.getForgerProof(), IsEqual.equalTo(this.signature.getBytes()));

			Assert.assertThat(dbModel.getBlockHash(), IsEqual.equalTo(expectedHash));
		}

		public void assertNoTransactions(final org.nem.nis.dbmodel.Block dbModel) {
			Assert.assertThat(getNumTransactions(dbModel), IsEqual.equalTo(0));
		}

		//region add*

		public Transfer addTransfer(final Block block) {
			final TransferTransaction transfer = new TransferTransaction(
					TimeInstant.ZERO,
					Utils.generateRandomAccount(),
					Utils.generateRandomAccount(),
					Amount.fromNem(111),
					null);
			return this.addTransfer(block, transfer, new Transfer(), Transfer.class);
		}

		public ImportanceTransfer addImportanceTransfer(final Block block) {
			final ImportanceTransferTransaction transfer = new ImportanceTransferTransaction(
					TimeInstant.ZERO,
					Utils.generateRandomAccount(),
					ImportanceTransferTransaction.Mode.Activate,
					Utils.generateRandomAccount());
			return this.addTransfer(block, transfer, new ImportanceTransfer(), ImportanceTransfer.class);
		}

		private <TDbTransfer extends AbstractTransfer, TModelTransfer extends Transaction> TDbTransfer addTransfer(
				final Block block,
				final TModelTransfer transfer,
				final TDbTransfer dbTransfer,
				final Class<TDbTransfer> dbTransferClass) {
			transfer.sign();

			Mockito.when(this.mapper.map(transfer, dbTransferClass)).thenReturn(dbTransfer);
			block.addTransaction(transfer);
			return dbTransfer;
		}

		//endregion
	}
}