package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.test.*;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class NisDbModelToModelMapperTest {

	// region delegation

	@Test
	public void mapBlockDelegatesToInnerMapper() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Block result = context.nisMapper.map(context.dbBlock);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(context.block));
		Mockito.verify(context.mapper, Mockito.only()).map(context.dbBlock, Block.class);
	}

	@Test
	public void mapTransferTransactionDelegatesToInnerMapper() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbTransferTransaction dbTransferTransaction = new DbTransferTransaction();
		final TransferTransaction transfer = Mockito.mock(TransferTransaction.class);
		Mockito.when(context.mapper.map(dbTransferTransaction, Transaction.class)).thenReturn(transfer);

		// Act:
		final Transaction result = context.nisMapper.map(dbTransferTransaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(transfer));
		Mockito.verify(context.mapper, Mockito.only()).map(dbTransferTransaction, Transaction.class);
	}

	@Test
	public void mapTransactionsDelegatesToInnerMapper() {
		// Arrange:
		final TestContext context = new TestContext();
		setTransactionsForMapTransactionsTests(context);

		// Act:
		final Collection<Transaction> transfers = context.nisMapper.mapTransactions(context.dbBlock);

		// Assert:
		context.assertMappedTransfers(transfers, Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
	}

	@Test
	public void mapTransactionsIfDelegatesToInnerMapper() {
		// Arrange:
		final TestContext context = new TestContext();
		setTransactionsForMapTransactionsTests(context);

		// Act:
		final int[] count = {
				0
		};
		final Collection<Transaction> transfers = context.nisMapper.mapTransactionsIf(context.dbBlock, t -> 0 == count[0]++ % 2);

		// Assert:
		context.assertMappedTransfers(transfers, Arrays.asList(0, 2, 4, 6));
	}

	@Test
	public void mapDelegatesToInnerMapper() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Namespace result = context.nisMapper.map(context.dbNamespace, Namespace.class);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(context.namespace));
		Mockito.verify(context.mapper, Mockito.only()).map(context.dbNamespace, Namespace.class);
	}

	// endregion

	private static void setTransactionsForMapTransactionsTests(final TestContext context) {
		context.setTransactions(DbTransferTransaction::new, context.dbBlock::setBlockTransferTransactions, TransferTransaction.class, 3);
		context.setTransactions(DbImportanceTransferTransaction::new, context.dbBlock::setBlockImportanceTransferTransactions,
				ImportanceTransferTransaction.class, 2);
		context.setTransactions(DbMultisigAggregateModificationTransaction::new,
				context.dbBlock::setBlockMultisigAggregateModificationTransactions, MultisigAggregateModificationTransaction.class, 1);
		context.setTransactions(DbMultisigTransaction::new, context.dbBlock::setBlockMultisigTransactions, MultisigTransaction.class, 2);
	}

	private static class TestContext {
		private final Block block = Mockito.mock(Block.class);
		private final DbBlock dbBlock = new DbBlock();

		private final Namespace namespace = Mockito.mock(Namespace.class);
		private final DbNamespace dbNamespace = new DbNamespace();

		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final NisDbModelToModelMapper nisMapper = new NisDbModelToModelMapper(this.mapper);

		private final List<AbstractTransfer> dbTransfers = new ArrayList<>();
		private final List<Transaction> transfers = new ArrayList<>();

		public TestContext() {
			// set up mapping
			Mockito.when(this.mapper.map(this.dbBlock, Block.class)).thenReturn(this.block);
			Mockito.when(this.mapper.map(this.dbNamespace, Namespace.class)).thenReturn(this.namespace);
		}

		private <TDbModel extends AbstractTransfer, TModel extends Transaction> void setTransactions(final Supplier<TDbModel> createDbModel,
				final Consumer<List<TDbModel>> setTransactions, final Class<TModel> modelClass, final int num) {
			final List<TDbModel> dbTransfers = new ArrayList<>();
			final List<TModel> transfers = new ArrayList<>();
			for (int i = 0; i < num; ++i) {
				final TDbModel dbTransfer = createDbModel.get();
				dbTransfer.setSenderProof(Utils.generateRandomSignature().getBytes());
				dbTransfers.add(dbTransfer);

				final TModel transfer = Mockito.mock(modelClass);
				transfers.add(transfer);

				Mockito.when(this.mapper.map(dbTransfer, Transaction.class)).thenReturn(transfer);
			}

			setTransactions.accept(dbTransfers);

			this.dbTransfers.addAll(dbTransfers);
			this.transfers.addAll(transfers);
		}

		private void assertMappedTransfers(final Collection<Transaction> transfers, final Collection<Integer> expectedTransferIndexes) {
			final int numExpectedTransfers = expectedTransferIndexes.size();
			final Collection<Transaction> expectedTransfers = expectedTransferIndexes.stream().map(this.transfers::get)
					.collect(Collectors.toList());
			final List<AbstractTransfer> expectedDbTransfers = expectedTransferIndexes.stream().map(this.dbTransfers::get)
					.collect(Collectors.toList());

			MatcherAssert.assertThat(transfers.size(), IsEqual.equalTo(numExpectedTransfers));
			MatcherAssert.assertThat(transfers, IsEquivalent.equivalentTo(expectedTransfers));

			for (int i = 0; i < numExpectedTransfers; ++i) {
				Mockito.verify(this.mapper, Mockito.times(1)).map(Mockito.eq(expectedDbTransfers.get(i)), Mockito.any());
			}
		}
	}
}
