package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.MultisigModification;
import org.nem.core.model.MultisigTransaction;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.test.RandomTransactionFactory;

import java.util.*;
import java.util.function.*;

public class MultisigTransactionDbModelToModelMappingTest extends AbstractTransferDbModelToModelMappingTest<org.nem.nis.dbmodel.MultisigTransaction, MultisigTransaction> {

	//region supported multisig transfer types

	@Test
	public void canMapMultisigTransferToModel() {
		// Assert:
		assertCanMapMultisigWithInnerTransaction(TestContext::addTransfer);
	}

	@Test
	public void canMapMultisigImportanceTransferToModel() {
		// Assert:
		assertCanMapMultisigWithInnerTransaction(TestContext::addImportanceTransfer);
	}

	@Test
	public void canMapMultisigSignerModificationTransferToModel() {
		// Assert:
		assertCanMapMultisigWithInnerTransaction(TestContext::addSignerModification);
	}

	private static void assertCanMapMultisigWithInnerTransaction(final Consumer<TestContext> addInnerTransaction) {
		// Arrange:
		final TestContext context = new TestContext();
		addInnerTransaction.accept(context);
		final org.nem.nis.dbmodel.MultisigTransaction dbTransfer = context.createDbModel();

		// Act:
		final MultisigTransaction model = context.mapping.map(dbTransfer);

		// Assert:
		context.assertModel(model, 0);
	}

	@Test
	public void cannotMapOtherMultisigTransferToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final org.nem.nis.dbmodel.MultisigTransaction dbTransfer = context.createDbModel();

		// Act:
		ExceptionAssert.assertThrows(
				v -> context.mapping.map(dbTransfer),
				IllegalArgumentException.class);
	}

	//endregion

	@Test
	public void canMapMultisigWithSingleSignatureToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addTransfer();
		context.addSignature();
		final org.nem.nis.dbmodel.MultisigTransaction dbTransfer = context.createDbModel();

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
		final org.nem.nis.dbmodel.MultisigTransaction dbTransfer = context.createDbModel();

		// Act:
		final MultisigTransaction model = context.mapping.map(dbTransfer);

		// Assert:
		context.assertModel(model, 3);
	}

	@Override
	protected org.nem.nis.dbmodel.MultisigTransaction createDbModel() {
		final ImportanceTransfer dbTransfer = new ImportanceTransfer();
		dbTransfer.setMode(1);

		final org.nem.nis.dbmodel.MultisigTransaction dbMultisigTransfer = new org.nem.nis.dbmodel.MultisigTransaction();
		dbMultisigTransfer.setImportanceTransfer(dbTransfer);
		dbMultisigTransfer.setMultisigSignatures(new HashSet<>());
		return dbMultisigTransfer;
	}

	@Override
	protected IMapping<org.nem.nis.dbmodel.MultisigTransaction, MultisigTransaction> createMapping(final IMapper mapper) {
		// ugly, but the passed in IMapper is a mock object, and we need to set it up to return a non-null transaction
		// for the inner transaction
		final ImportanceTransferTransaction transfer = RandomTransactionFactory.createImportanceTransfer();
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(Transaction.class))).thenReturn(transfer);

		return new MultisigTransactionDbModelToModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final org.nem.nis.dbmodel.Account dbSender = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final org.nem.core.model.Account sender = Utils.generateRandomAccount();
		private final Set<MultisigSignature> dbSignatures = new HashSet<>();
		private final Set<MultisigSignatureTransaction> expectedSignatures = new HashSet<>();
		private final org.nem.nis.dbmodel.MultisigTransaction dbTransfer = new org.nem.nis.dbmodel.MultisigTransaction();
		private Transaction expectedOtherTransaction;

		private final MultisigTransactionDbModelToModelMapping mapping = new MultisigTransactionDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbSender, org.nem.core.model.Account.class)).thenReturn(this.sender);
		}

		private void addSignature() {
			final MultisigSignature dbSignature = Mockito.mock(MultisigSignature.class);
			final MultisigSignatureTransaction signature = new MultisigSignatureTransaction(
					TimeInstant.ZERO,
					Utils.generateRandomAccount(),
					HashUtils.calculateHash(this.expectedOtherTransaction));
			Mockito.when(this.mapper.map(dbSignature, MultisigSignatureTransaction.class)).thenReturn(signature);

			this.dbSignatures.add(dbSignature);
			this.expectedSignatures.add(signature);
		}

		public void addTransfer() {
			this.addTransfer(
					new Transfer(),
					RandomTransactionFactory.createTransfer(),
					org.nem.nis.dbmodel.MultisigTransaction::setTransfer);
		}

		public void addImportanceTransfer() {
			this.addTransfer(
					new ImportanceTransfer(),
					RandomTransactionFactory.createImportanceTransfer(),
					org.nem.nis.dbmodel.MultisigTransaction::setImportanceTransfer);
		}

		public void addSignerModification() {
			this.addTransfer(
					new MultisigSignerModification(),
					RandomTransactionFactory.createSignerModification(),
					org.nem.nis.dbmodel.MultisigTransaction::setMultisigSignerModification);
		}

		private <TDbTransfer extends AbstractBlockTransfer, TModelTransfer extends Transaction> void addTransfer(
				final TDbTransfer dbTransfer,
				final TModelTransfer transfer,
				final BiConsumer<org.nem.nis.dbmodel.MultisigTransaction, TDbTransfer> setTransferInMultisig) {
			Mockito.when(this.mapper.map(dbTransfer, Transaction.class)).thenReturn(transfer);

			setTransferInMultisig.accept(this.dbTransfer, dbTransfer);
			this.expectedOtherTransaction = transfer;
		}

		public org.nem.nis.dbmodel.MultisigTransaction createDbModel() {
			this.dbTransfer.setTimeStamp(4444);
			this.dbTransfer.setSender(this.dbSender);
			this.dbTransfer.setMultisigSignatures(this.dbSignatures);

			// zero out required fields
			dbTransfer.setFee(0L);
			dbTransfer.setDeadline(0);
			return this.dbTransfer;
		}

		public void assertModel(final MultisigTransaction model, final int numExpectedSignatures) {
			Assert.assertThat(model.getTimeStamp(), IsEqual.equalTo(new TimeInstant(4444)));
			Assert.assertThat(model.getSigner(), IsEqual.equalTo(this.sender));

			Assert.assertThat(model.getOtherTransaction(), IsEqual.equalTo(this.expectedOtherTransaction));
			Assert.assertThat(model.getCosignerSignatures().size(), IsEqual.equalTo(numExpectedSignatures));

			Assert.assertThat(model.getCosignerSignatures(), IsEqual.equalTo(this.expectedSignatures));
		}
	}
}