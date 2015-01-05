package org.nem.nis.mappers;

import junit.framework.TestCase;
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

public class MultisigTransactionModelToDbModelMappingTest extends AbstractTransferModelToDbModelMappingTest<MultisigTransaction, org.nem.nis.dbmodel.MultisigTransaction> {

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
		final org.nem.nis.dbmodel.MultisigTransaction dbModel = context.mapping.map(model);

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
		final org.nem.nis.dbmodel.MultisigTransaction dbModel = context.mapping.map(model);

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
		final org.nem.nis.dbmodel.MultisigTransaction dbModel = context.mapping.map(model);

		// Assert:
		context.assertDbModel(dbModel, 3);
	}

	@Override
	protected MultisigTransaction createModel(final TimeInstant timeStamp, final Account sender) {
		return new MultisigTransaction(timeStamp, sender, RandomTransactionFactory.createImportanceTransfer());
	}

	@Override
	protected IMapping<MultisigTransaction, org.nem.nis.dbmodel.MultisigTransaction> createMapping(final IMapper mapper) {
		return new MultisigTransactionModelToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final org.nem.nis.dbmodel.Account dbSender = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final org.nem.core.model.Account sender = Utils.generateRandomAccount();
		private final Set<MultisigSignatureTransaction> signatures = new HashSet<>();
		private final Set<MultisigSignature> expectedDbSignatures = new HashSet<>();
		private Transaction otherTransaction;
		private Transfer expectedTransfer;
		private ImportanceTransfer expectedImportanceTransfer;
		private MultisigSignerModification expectedSignerModification;

		private final MultisigTransactionModelToDbModelMapping mapping = new MultisigTransactionModelToDbModelMapping(this.mapper);

		public TestContext() {
			this.otherTransaction = new MockTransaction();
			Mockito.when(this.mapper.map(this.sender, org.nem.nis.dbmodel.Account.class)).thenReturn(this.dbSender);
		}

		private void addSignature() {
			final MultisigSignature dbSignature = new MultisigSignature();
			final MultisigSignatureTransaction signature = new MultisigSignatureTransaction(
					TimeInstant.ZERO,
					Utils.generateRandomAccount(),
					HashUtils.calculateHash(this.otherTransaction));
			Mockito.when(this.mapper.map(signature, MultisigSignature.class)).thenReturn(dbSignature);

			this.signatures.add(signature);
			this.expectedDbSignatures.add(dbSignature);
		}

		public void addTransfer() {
			this.otherTransaction = RandomTransactionFactory.createTransfer();
			this.expectedTransfer = new Transfer();
			Mockito.when(this.mapper.map(this.otherTransaction, Transfer.class)).thenReturn(this.expectedTransfer);
		}

		public void addImportanceTransfer() {
			this.otherTransaction = RandomTransactionFactory.createImportanceTransfer();
			this.expectedImportanceTransfer = new ImportanceTransfer();
			Mockito.when(this.mapper.map(this.otherTransaction, ImportanceTransfer.class)).thenReturn(this.expectedImportanceTransfer);
		}

		public void addSignerModification() {
			this.otherTransaction = RandomTransactionFactory.createSignerModification();
			this.expectedSignerModification = new MultisigSignerModification();
			Mockito.when(this.mapper.map(this.otherTransaction, MultisigSignerModification.class)).thenReturn(this.expectedSignerModification);
		}

		public MultisigTransaction createModel() {
			final MultisigTransaction model = new MultisigTransaction(
					TimeInstant.ZERO,
					this.sender,
					this.otherTransaction);
			this.signatures.forEach(model::addSignature);
			return model;
		}

		public void assertDbModel(final org.nem.nis.dbmodel.MultisigTransaction dbModel, final int numExpectedSignatures) {
			Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));

			Assert.assertThat(dbModel.getTransfer(), IsEqual.equalTo(this.expectedTransfer));
			Assert.assertThat(dbModel.getImportanceTransfer(), IsEqual.equalTo(this.expectedImportanceTransfer));
			Assert.assertThat(dbModel.getMultisigSignerModification(), IsEqual.equalTo(this.expectedSignerModification));

			Assert.assertThat(dbModel.getMultisigSignatures().size(), IsEqual.equalTo(numExpectedSignatures));
			Assert.assertThat(dbModel.getMultisigSignatures(), IsEqual.equalTo(this.expectedDbSignatures));

			for (final MultisigSignature signature : dbModel.getMultisigSignatures()) {
				Assert.assertThat(signature.getMultisigTransaction(), IsEqual.equalTo(dbModel));
			}
		}
	}
}