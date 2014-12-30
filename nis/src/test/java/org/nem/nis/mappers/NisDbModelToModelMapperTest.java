package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.Block;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.function.*;

public class NisDbModelToModelMapperTest {

	@Test
	public void mapBlockDelegatesToInnerMapper() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Block result = context.nisMapper.map(context.dbBlock);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(context.block));
		Mockito.verify(context.mapper, Mockito.only()).map(context.dbBlock, Block.class);
	}

	@Test
	public void mapTransferTransactionsDelegatesToInnerMapper() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setTransactions(Transfer::new, context.dbBlock::setBlockTransfers, TransferTransaction.class, 3);
		final List<AbstractTransfer> dbTransferTransactions = new ArrayList<>(context.dbTransfers);
		final List<Transaction> transferTransactions = new ArrayList<>(context.transfers);
		context.setTransactions(ImportanceTransfer::new, context.dbBlock::setBlockImportanceTransfers, ImportanceTransferTransaction.class, 2);

		// Act:
		final Collection<Transaction> transfers = context.nisMapper.mapTransferTransactions(context.dbBlock);

		// Assert:
		final int numExpectedTransfers = 3;
		Assert.assertThat(transfers.size(), IsEqual.equalTo(numExpectedTransfers));
		Assert.assertThat(transfers, IsEquivalent.equivalentTo(transferTransactions));

		for (int i = 0; i < numExpectedTransfers; ++i) {
			Mockito.verify(context.mapper, Mockito.times(1)).map(Mockito.eq(dbTransferTransactions.get(i)), Mockito.any());
		}
	}

	@Test
	public void mapTransactionsDelegatesToInnerMapper() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setTransactions(Transfer::new, context.dbBlock::setBlockTransfers, TransferTransaction.class, 3);
		context.setTransactions(ImportanceTransfer::new, context.dbBlock::setBlockImportanceTransfers, ImportanceTransferTransaction.class, 2);

		// Act:
		final Collection<Transaction> transfers = context.nisMapper.mapTransactions(context.dbBlock);

		// Assert:
		final int numExpectedTransfers = 5;
		Assert.assertThat(transfers.size(), IsEqual.equalTo(numExpectedTransfers));
		Assert.assertThat(transfers, IsEquivalent.equivalentTo(context.transfers));

		for (int i = 0; i < numExpectedTransfers; ++i) {
			Mockito.verify(context.mapper, Mockito.times(1)).map(Mockito.eq(context.dbTransfers.get(i)), Mockito.any());
		}
	}

	private static class TestContext {
		private final Block block = Mockito.mock(Block.class);
		private final org.nem.nis.dbmodel.Block dbBlock = new org.nem.nis.dbmodel.Block();

		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final NisDbModelToModelMapper nisMapper = new NisDbModelToModelMapper(this.mapper);

		private final List<AbstractTransfer> dbTransfers = new ArrayList<>();
		private final List<Transaction> transfers = new ArrayList<>();

		public TestContext() {
			// set up block mapping
			Mockito.when(this.mapper.map(this.dbBlock, Block.class)).thenReturn(this.block);
		}

		private <TDbModel extends AbstractTransfer, TModel extends Transaction> Collection<TModel> setTransactions(
				final Supplier<TDbModel> createDbModel,
				final Consumer<List<TDbModel>> setTransactions,
				final Class<TModel> modelClass,
				final int num) {
			final List<TDbModel> dbTransfers = new ArrayList<>();
			final List<TModel> transfers = new ArrayList<>();
			for (int i = 0; i < num; ++i) {
				final TDbModel dbTransfer = createDbModel.get();
				dbTransfers.add(dbTransfer);

				final TModel transfer = Mockito.mock(modelClass);
				transfers.add(transfer);

				Mockito.when(this.mapper.map(dbTransfer, modelClass)).thenReturn(transfer);
			}

			setTransactions.accept(dbTransfers);

			this.dbTransfers.addAll(dbTransfers);
			this.transfers.addAll(transfers);
			return transfers;
		}
	}
}