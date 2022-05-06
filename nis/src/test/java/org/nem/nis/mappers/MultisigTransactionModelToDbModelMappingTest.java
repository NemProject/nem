package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
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
public class MultisigTransactionModelToDbModelMappingTest {

	// region General

	public static class General extends AbstractTransferModelToDbModelMappingTest<MultisigTransaction, DbMultisigTransaction> {

		@Test
		public void cannotMapMultisigTransferWithUnregisteredInnerTransactionToDbModel() {
			// Arrange:
			final TestContext context = new TestContext();
			final MultisigTransaction model = context.createModel();

			// Act:
			ExceptionAssert.assertThrows(v -> context.mapping.map(model), IllegalArgumentException.class);
		}

		@Test
		public void cannotMapMultisigTransferWithMultisigInnerTransactionToDbModel() {
			// Arrange:
			final TestContext context = new TestContext();
			context.otherTransaction = RandomTransactionFactory.createMultisigTransfer();
			final MultisigTransaction model = context.createModel();

			// Act:
			ExceptionAssert.assertThrows(v -> context.mapping.map(model), IllegalArgumentException.class);
		}

		@Test
		public void canMapMultisigWithSingleSignatureToDbModel() {
			// Arrange:
			final TestContext context = new TestContext();
			context.addTransfer();
			context.addSignature();
			final MultisigTransaction model = context.createModel();

			// Act:
			final DbMultisigTransaction dbModel = context.mapping.map(model);

			// Assert:
			context.assertDbModel(dbModel, 1);
		}

		@Test
		public void canMapMultisigWithMultipleSignaturesToDbModel() {
			// Arrange:
			final TestContext context = new TestContext();
			context.addTransfer();
			context.addSignature();
			context.addSignature();
			context.addSignature();
			final MultisigTransaction model = context.createModel();

			// Act:
			final DbMultisigTransaction dbModel = context.mapping.map(model);

			// Assert:
			context.assertDbModel(dbModel, 3);
		}

		@Override
		protected MultisigTransaction createModel(final TimeInstant timeStamp, final Account sender) {
			return new MultisigTransaction(timeStamp, sender, RandomTransactionFactory.createImportanceTransfer());
		}

		@Override
		protected IMapping<MultisigTransaction, DbMultisigTransaction> createMapping(final IMapper mapper) {
			return new MultisigTransactionModelToDbModelMapping(mapper);
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
		public void canMapMultisigWithInnerTransactionToDbModel() {
			// Assert:
			assertCanMapMultisigWithInnerTransaction(context -> {
				context.otherTransaction = this.createModel.get();
				final AbstractBlockTransfer dbInner = DbTestUtils.createTransferDbModel(this.entry.dbModelClass);
				Mockito.when(context.mapper.map(context.otherTransaction, this.entry.dbModelClass)).thenAnswer(invocationOnMock -> dbInner);
				return dbInner;
			});
		}

		private static void assertCanMapMultisigWithInnerTransaction(
				final Function<TestContext, AbstractBlockTransfer> addInnerTransaction) {
			// Arrange:
			final TestContext context = new TestContext();
			final AbstractBlockTransfer dbInner = addInnerTransaction.apply(context);
			final MultisigTransaction model = context.createModel();

			// Act:
			final DbMultisigTransaction dbModel = context.mapping.map(model);

			// Assert:
			context.assertDbModelWithInner(dbModel, dbInner);
		}
	}

	// endregion

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final org.nem.core.model.Account sender = Utils.generateRandomAccount();
		private final Set<MultisigSignatureTransaction> signatures = new HashSet<>();
		private final Set<DbMultisigSignatureTransaction> expectedDbSignatures = new HashSet<>();
		private Transaction otherTransaction;
		private DbTransferTransaction expectedTransfer;

		private final MultisigTransactionModelToDbModelMapping mapping = new MultisigTransactionModelToDbModelMapping(this.mapper);

		public TestContext() {
			this.otherTransaction = new MockTransaction();
			Mockito.when(this.mapper.map(this.sender, DbAccount.class)).thenReturn(this.dbSender);
		}

		private void addSignature() {
			final DbMultisigSignatureTransaction dbSignature = new DbMultisigSignatureTransaction();
			final MultisigSignatureTransaction signature = new MultisigSignatureTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(),
					this.otherTransaction.getSigner(), HashUtils.calculateHash(this.otherTransaction));
			Mockito.when(this.mapper.map(Mockito.refEq(signature), Mockito.eq(DbMultisigSignatureTransaction.class)))
					.thenReturn(dbSignature);

			this.signatures.add(signature);
			this.expectedDbSignatures.add(dbSignature);
		}

		public void addTransfer() {
			this.otherTransaction = RandomTransactionFactory.createTransfer();
			this.expectedTransfer = new DbTransferTransaction();
			Mockito.when(this.mapper.map(this.otherTransaction, DbTransferTransaction.class)).thenReturn(this.expectedTransfer);
		}

		public MultisigTransaction createModel() {
			final MultisigTransaction model = new MultisigTransaction(TimeInstant.ZERO, this.sender, this.otherTransaction);
			this.signatures.forEach(model::addSignature);
			return model;
		}

		public void assertDbModel(final DbMultisigTransaction dbModel, final int numExpectedSignatures) {
			MatcherAssert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));

			MatcherAssert.assertThat(dbModel.getTransferTransaction(), IsEqual.equalTo(this.expectedTransfer));
			for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
				MatcherAssert.assertThat(entry.getFromMultisig.apply(dbModel),
						TransactionTypes.TRANSFER == entry.type ? IsEqual.equalTo(this.expectedTransfer) : IsNull.nullValue());
			}

			MatcherAssert.assertThat(dbModel.getMultisigSignatureTransactions().size(), IsEqual.equalTo(numExpectedSignatures));
			MatcherAssert.assertThat(dbModel.getMultisigSignatureTransactions(), IsEqual.equalTo(this.expectedDbSignatures));

			for (final DbMultisigSignatureTransaction signature : dbModel.getMultisigSignatureTransactions()) {
				MatcherAssert.assertThat(signature.getMultisigTransaction(), IsEqual.equalTo(dbModel));
			}
		}

		public void assertDbModelWithInner(final DbMultisigTransaction dbModel, final AbstractBlockTransfer dbInner) {
			for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
				MatcherAssert.assertThat(entry.getFromMultisig.apply(dbModel),
						dbInner.getClass().equals(entry.dbModelClass) ? IsEqual.equalTo(dbInner) : IsNull.nullValue());
			}

			MatcherAssert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));
			MatcherAssert.assertThat(dbModel.getMultisigSignatureTransactions().size(), IsEqual.equalTo(0));
		}
	}
}
