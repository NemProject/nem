package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.MultisigTransaction;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.test.RandomTransactionFactory;

import java.util.*;
import java.util.function.*;

public class MultisigTransactionModelToDbModelMappingTest extends AbstractTransferModelToDbModelMappingTest<MultisigTransaction, DbMultisigTransaction> {

	//region supported multisig transfer types

	@Test
	public void oneCanMapMultisigTransferToDbModelTestExistsForEachRegisteredMultisigEmbeddableTransactionType() {
		// Assert:
		Assert.assertThat(
				3, // the number of canMapMultisig*ToDbModel tests
				IsEqual.equalTo(TransactionRegistry.multisigEmbeddableSize()));
	}

	@Test
	public void canMapMultisigTransferToDbModel() {
		// Assert:
		assertCanMapMultisigWithInnerTransaction(TestContext::addTransfer);
	}

	@Test
	public void canMapMultisigImportanceTransferToDbModel() {
		// Assert:
		assertCanMapMultisigWithInnerTransaction(TestContext::addImportanceTransfer);
	}

	@Test
	public void canMapMultisigSignerModificationTransferToDbModel() {
		// Assert:
		assertCanMapMultisigWithInnerTransaction(TestContext::addSignerModification);
	}

	private static void assertCanMapMultisigWithInnerTransaction(final Consumer<TestContext> addInnerTransaction) {
		// Arrange:
		final TestContext context = new TestContext();
		addInnerTransaction.accept(context);
		final MultisigTransaction model = context.createModel();

		// Act:
		final DbMultisigTransaction dbModel = context.mapping.map(model);

		// Assert:
		context.assertDbModel(dbModel, 0);
	}

	@Test
	public void cannotMapOtherMultisigTransferToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigTransaction model = context.createModel();

		// Act:
		ExceptionAssert.assertThrows(
				v -> context.mapping.map(model),
				IllegalArgumentException.class);
	}

	//endregion

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

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final org.nem.nis.dbmodel.Account dbSender = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final org.nem.core.model.Account sender = Utils.generateRandomAccount();
		private final Set<MultisigSignatureTransaction> signatures = new HashSet<>();
		private final Set<DbMultisigSignatureTransaction> expectedDbSignatures = new HashSet<>();
		private Transaction otherTransaction;
		private DbTransferTransaction expectedDbTransferTransaction;
		private ImportanceTransfer expectedImportanceTransfer;
		private DbMultisigAggregateModificationTransaction expectedSignerModification;

		private final MultisigTransactionModelToDbModelMapping mapping = new MultisigTransactionModelToDbModelMapping(this.mapper);

		public TestContext() {
			this.otherTransaction = new MockTransaction();
			Mockito.when(this.mapper.map(this.sender, org.nem.nis.dbmodel.Account.class)).thenReturn(this.dbSender);
		}

		private void addSignature() {
			final DbMultisigSignatureTransaction dbSignature = new DbMultisigSignatureTransaction();
			final MultisigSignatureTransaction signature = new MultisigSignatureTransaction(
					TimeInstant.ZERO,
					Utils.generateRandomAccount(),
					HashUtils.calculateHash(this.otherTransaction));
			Mockito.when(this.mapper.map(Mockito.refEq(signature), Mockito.eq(DbMultisigSignatureTransaction.class))).thenReturn(dbSignature);

			this.signatures.add(signature);
			this.expectedDbSignatures.add(dbSignature);
		}

		public void addTransfer() {
			this.otherTransaction = RandomTransactionFactory.createTransfer();
			this.expectedDbTransferTransaction = new DbTransferTransaction();
			Mockito.when(this.mapper.map(this.otherTransaction, DbTransferTransaction.class)).thenReturn(this.expectedDbTransferTransaction);
		}

		public void addImportanceTransfer() {
			this.otherTransaction = RandomTransactionFactory.createImportanceTransfer();
			this.expectedImportanceTransfer = new ImportanceTransfer();
			Mockito.when(this.mapper.map(this.otherTransaction, ImportanceTransfer.class)).thenReturn(this.expectedImportanceTransfer);
		}

		public void addSignerModification() {
			this.otherTransaction = RandomTransactionFactory.createSignerModification();
			this.expectedSignerModification = new DbMultisigAggregateModificationTransaction();
			Mockito.when(this.mapper.map(this.otherTransaction, DbMultisigAggregateModificationTransaction.class)).thenReturn(this.expectedSignerModification);
		}

		public MultisigTransaction createModel() {
			final MultisigTransaction model = new MultisigTransaction(
					TimeInstant.ZERO,
					this.sender,
					this.otherTransaction);
			this.signatures.forEach(model::addSignature);
			return model;
		}

		public void assertDbModel(final DbMultisigTransaction dbModel, final int numExpectedSignatures) {
			Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));

			Assert.assertThat(dbModel.getTransferTransaction(), IsEqual.equalTo(this.expectedDbTransferTransaction));
			Assert.assertThat(dbModel.getImportanceTransfer(), IsEqual.equalTo(this.expectedImportanceTransfer));
			Assert.assertThat(dbModel.getMultisigAggregateModificationTransaction(), IsEqual.equalTo(this.expectedSignerModification));

			Assert.assertThat(dbModel.getMultisigSignatureTransactions().size(), IsEqual.equalTo(numExpectedSignatures));
			Assert.assertThat(dbModel.getMultisigSignatureTransactions(), IsEqual.equalTo(this.expectedDbSignatures));

			for (final DbMultisigSignatureTransaction signature : dbModel.getMultisigSignatureTransactions()) {
				Assert.assertThat(signature.getMultisigTransaction(), IsEqual.equalTo(dbModel));
			}
		}
	}
}