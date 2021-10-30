package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.test.DbTestUtils;

import java.util.*;
import java.util.function.*;

@RunWith(Enclosed.class)
@SuppressWarnings("rawtypes")
public class MultisigTransactionDbModelToModelMappingTest {

	// region General

	public static class General extends AbstractTransferDbModelToModelMappingTest<DbMultisigTransaction, MultisigTransaction> {

		@Test
		public void cannotMapOtherMultisigTransferToModel() {
			// Arrange:
			final TestContext context = new TestContext();
			final DbMultisigTransaction dbTransfer = context.createDbModel();

			// Act:
			ExceptionAssert.assertThrows(v -> context.mapping.map(dbTransfer), IllegalArgumentException.class);
		}

		@Test
		public void canMapMultisigWithSingleSignatureToModel() {
			// Arrange:
			final TestContext context = new TestContext();
			context.addTransfer();
			context.addSignature();
			final DbMultisigTransaction dbTransfer = context.createDbModel();

			// Act:
			final MultisigTransaction model = context.mapping.map(dbTransfer);

			// Assert:
			context.assertModel(model, 1);
		}

		@Test
		public void canMapMultisigWithMultipleSignaturesToModel() {
			// Arrange:
			final TestContext context = new TestContext();
			context.addTransfer();
			context.addSignature();
			context.addSignature();
			context.addSignature();
			final DbMultisigTransaction dbTransfer = context.createDbModel();

			// Act:
			final MultisigTransaction model = context.mapping.map(dbTransfer);

			// Assert:
			context.assertModel(model, 3);
		}

		@Override
		protected DbMultisigTransaction createDbModel() {
			final DbImportanceTransferTransaction dbTransfer = new DbImportanceTransferTransaction();
			dbTransfer.setMode(1);

			final DbMultisigTransaction dbMultisigTransfer = new DbMultisigTransaction();
			dbMultisigTransfer.setImportanceTransferTransaction(dbTransfer);
			dbMultisigTransfer.setMultisigSignatureTransactions(new HashSet<>());
			return dbMultisigTransfer;
		}

		@Override
		protected IMapping<DbMultisigTransaction, MultisigTransaction> createMapping(final IMapper mapper) {
			// ugly, but the passed in IMapper is a mock object, and we need to set it up to return a non-null transaction
			// for the inner transaction
			final ImportanceTransferTransaction transfer = RandomTransactionFactory.createImportanceTransfer();
			Mockito.when(mapper.map(Mockito.any(), Mockito.eq(Transaction.class))).thenReturn(transfer);

			return new MultisigTransactionDbModelToModelMapping(mapper);
		}
	}

	// endregion

	// region PerTransaction

	@RunWith(Parameterized.class)
	public static class PerTransaction {
		private final TransactionRegistry.Entry<? extends AbstractTransfer, ? extends Transaction> entry;
		private final Supplier<? extends Transaction> createModel;

		public PerTransaction(final int type) {
			this.entry = TransactionRegistry.findByType(type);
			this.createModel = TestTransactionRegistry.findByType(type).createModel;
		}

		@Parameterized.Parameters
		public static Collection<Object[]> data() {
			return ParameterizedUtils.wrap(TransactionTypes.getMultisigEmbeddableTypes());
		}

		@Test
		public void canMapMultisigWithInnerTransactionToModel() {
			// Assert:
			assertCanMapMultisigWithInnerTransaction(context -> {
				final AbstractBlockTransfer dbModel = DbTestUtils.createTransferDbModel(this.entry.dbModelClass);
				final Transaction model = this.createModel.get();
				context.addTransfer(dbModel, model, this.entry.setInMultisig);
			});
		}

		private static void assertCanMapMultisigWithInnerTransaction(final Consumer<TestContext> addInnerTransaction) {
			// Arrange:
			final TestContext context = new TestContext();
			addInnerTransaction.accept(context);
			final DbMultisigTransaction dbTransfer = context.createDbModel();

			// Act:
			final MultisigTransaction model = context.mapping.map(dbTransfer);

			// Assert:
			context.assertModel(model, 0);
		}
	}

	// endregion

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final org.nem.core.model.Account sender = Utils.generateRandomAccount();
		private final Set<DbMultisigSignatureTransaction> dbSignatures = new HashSet<>();
		private final Set<MultisigSignatureTransaction> expectedSignatures = new HashSet<>();
		private final DbMultisigTransaction dbTransfer = new DbMultisigTransaction();
		private Transaction expectedOtherTransaction;

		private final MultisigTransactionDbModelToModelMapping mapping = new MultisigTransactionDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbSender, org.nem.core.model.Account.class)).thenReturn(this.sender);
		}

		private void addSignature() {
			final DbMultisigSignatureTransaction dbSignature = Mockito.mock(DbMultisigSignatureTransaction.class);
			final MultisigSignatureTransaction signature = new MultisigSignatureTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(),
					this.expectedOtherTransaction.getSigner(), HashUtils.calculateHash(this.expectedOtherTransaction));
			Mockito.when(this.mapper.map(dbSignature, MultisigSignatureTransaction.class)).thenReturn(signature);

			this.dbSignatures.add(dbSignature);
			this.expectedSignatures.add(signature);
		}

		public void addTransfer() {
			this.addTransfer(new DbTransferTransaction(), RandomTransactionFactory.createTransfer(),
					DbMultisigTransaction::setTransferTransaction);
		}

		private <TDbTransfer extends AbstractBlockTransfer, TModelTransfer extends Transaction> void addTransfer(
				final TDbTransfer dbTransfer, final TModelTransfer transfer,
				final BiConsumer<DbMultisigTransaction, TDbTransfer> setTransferInMultisig) {
			Mockito.when(this.mapper.map(dbTransfer, Transaction.class)).thenReturn(transfer);

			setTransferInMultisig.accept(this.dbTransfer, dbTransfer);
			this.expectedOtherTransaction = transfer;
		}

		public DbMultisigTransaction createDbModel() {
			this.dbTransfer.setTimeStamp(4444);
			this.dbTransfer.setSender(this.dbSender);
			this.dbTransfer.setMultisigSignatureTransactions(this.dbSignatures);

			// zero out required fields
			this.dbTransfer.setFee(0L);
			this.dbTransfer.setDeadline(0);
			return this.dbTransfer;
		}

		public void assertModel(final MultisigTransaction model, final int numExpectedSignatures) {
			MatcherAssert.assertThat(model.getOtherTransaction(), IsEqual.equalTo(this.expectedOtherTransaction));
			MatcherAssert.assertThat(model.getCosignerSignatures().size(), IsEqual.equalTo(numExpectedSignatures));

			MatcherAssert.assertThat(model.getCosignerSignatures(), IsEqual.equalTo(this.expectedSignatures));
		}
	}
}
