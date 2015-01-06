package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.dbmodel.DbMultisigTransaction;

public class MultisigSignatureDbModelToModelMappingTest extends AbstractTransferDbModelToModelMappingTest<DbMultisigSignatureTransaction, MultisigSignatureTransaction> {

	@Test
	public void signatureCanBeMappedToModelWhenLinkedMultisigTransactionHasInnerTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMultisigSignatureTransaction dbSignature = context.createDbSignature();

		// Act:
		final MultisigSignatureTransaction model = context.mapping.map(dbSignature);

		// Assert:
		context.assertModel(model);
	}

	@Test
	public void signatureCannotBeMappedToModelWhenLinkedMultisigTransactionDoesNotHaveInnerTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMultisigSignatureTransaction dbSignature = context.createDbSignature();
		dbSignature.getMultisigTransaction().setTransferTransaction(null);

		// Act:
		ExceptionAssert.assertThrows(
				v -> context.mapping.map(dbSignature),
				IllegalArgumentException.class);
	}

	@Override
	protected DbMultisigSignatureTransaction createDbModel() {
		final DbMultisigTransaction dbMultisigTransfer = new DbMultisigTransaction();
		dbMultisigTransfer.setTransferTransaction(new DbTransferTransaction());
		final DbMultisigSignatureTransaction dbSignature = new DbMultisigSignatureTransaction();
		dbSignature.setMultisigTransaction(dbMultisigTransfer);
		return dbSignature;
	}

	@Override
	protected IMapping<DbMultisigSignatureTransaction, MultisigSignatureTransaction> createMapping(final IMapper mapper) {
		return new MultisigSignatureDbModelToModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final org.nem.nis.dbmodel.Account dbSender = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final Account sender = Utils.generateRandomAccount();
		private final Hash otherTransactionHash = Utils.generateRandomHash();
		private final MultisigSignatureDbModelToModelMapping mapping = new MultisigSignatureDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbSender, Account.class)).thenReturn(this.sender);
		}

		public DbMultisigSignatureTransaction createDbSignature() {
			final DbMultisigSignatureTransaction dbSignature = new DbMultisigSignatureTransaction();
			dbSignature.setTimeStamp(4444);
			dbSignature.setSender(this.dbSender);
			dbSignature.setMultisigTransaction(new DbMultisigTransaction());

			final DbTransferTransaction dbTransferTransaction = new DbTransferTransaction();
			dbTransferTransaction.setTransferHash(this.otherTransactionHash);
			dbSignature.getMultisigTransaction().setTransferTransaction(dbTransferTransaction);

			dbSignature.setFee(0L);
			dbSignature.setDeadline(0);
			return dbSignature;
		}

		public void assertModel(final MultisigSignatureTransaction model) {
			Assert.assertThat(model.getTimeStamp(), IsEqual.equalTo(new TimeInstant(4444)));
			Assert.assertThat(model.getSigner(), IsEqual.equalTo(this.sender));
			Assert.assertThat(model.getOtherTransactionHash(), IsEqual.equalTo(this.otherTransactionHash));
		}
	}
}