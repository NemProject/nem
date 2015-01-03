package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.dbmodel.MultisigTransaction;

public class MultisigSignatureDbModelToModelMappingTest extends AbstractTransferDbModelToModelMappingTest<MultisigSignature, MultisigSignatureTransaction> {

	@Test
	public void signatureCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigSignature dbSignature = context.createDbSignature();

		// Act:
		final MultisigSignatureTransaction model = context.mapping.map(dbSignature);

		// Assert:
		context.assertModel(model);
	}

	@Override
	protected MultisigSignature createDbModel() {
		final MultisigSignature dbSignature = new MultisigSignature();
		dbSignature.setMultisigTransaction(new MultisigTransaction());
		return dbSignature;
	}

	@Override
	protected IMapping<MultisigSignature, MultisigSignatureTransaction> createMapping(final IMapper mapper) {
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

		public MultisigSignature createDbSignature() {
			final MultisigSignature dbSignature = new MultisigSignature();
			dbSignature.setTimeStamp(4444);
			dbSignature.setSender(this.dbSender);
			dbSignature.setMultisigTransaction(new MultisigTransaction());
			dbSignature.getMultisigTransaction().setTransferHash(this.otherTransactionHash);

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