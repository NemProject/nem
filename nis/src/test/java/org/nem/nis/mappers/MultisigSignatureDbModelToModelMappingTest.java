package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.nis.dbmodel.*;

public class MultisigSignatureDbModelToModelMappingTest
		extends
			AbstractTransferDbModelToModelMappingTest<DbMultisigSignatureTransaction, MultisigSignatureTransaction> {

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
		ExceptionAssert.assertThrows(v -> context.mapping.map(dbSignature), IllegalArgumentException.class);
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
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final Account sender = Utils.generateRandomAccount();
		private final DbAccount dbMultisigSender = Mockito.mock(DbAccount.class);
		private final Account multisigSender = Utils.generateRandomAccount();
		private final DbAccount dbOtherSender = Mockito.mock(DbAccount.class);
		private final Account otherSender = Utils.generateRandomAccount();
		private final Hash otherTransactionHash = Utils.generateRandomHash();
		private final MultisigSignatureDbModelToModelMapping mapping = new MultisigSignatureDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbSender, Account.class)).thenReturn(this.sender);
			Mockito.when(this.mapper.map(this.dbMultisigSender, Account.class)).thenReturn(this.multisigSender);
			Mockito.when(this.mapper.map(this.dbOtherSender, Account.class)).thenReturn(this.otherSender);
		}

		public DbMultisigSignatureTransaction createDbSignature() {
			final DbTransferTransaction dbTransfer = new DbTransferTransaction();
			dbTransfer.setSender(this.dbOtherSender);
			dbTransfer.setTransferHash(this.otherTransactionHash);

			final DbMultisigTransaction dbMultisig = new DbMultisigTransaction();
			dbMultisig.setSender(this.dbMultisigSender);
			dbMultisig.setTransferTransaction(dbTransfer);

			final DbMultisigSignatureTransaction dbSignature = new DbMultisigSignatureTransaction();
			dbSignature.setTimeStamp(4444);
			dbSignature.setSender(this.dbSender);
			dbSignature.setMultisigTransaction(dbMultisig);
			dbSignature.setFee(0L);
			dbSignature.setDeadline(0);
			return dbSignature;
		}

		public void assertModel(final MultisigSignatureTransaction model) {
			MatcherAssert.assertThat(model.getDebtor(), IsEqual.equalTo(this.otherSender));
			MatcherAssert.assertThat(model.getOtherTransactionHash(), IsEqual.equalTo(this.otherTransactionHash));
		}
	}
}
