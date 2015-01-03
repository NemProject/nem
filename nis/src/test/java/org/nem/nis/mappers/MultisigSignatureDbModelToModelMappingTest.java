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

public class MultisigSignatureDbModelToModelMappingTest {

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

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final org.nem.nis.dbmodel.Account dbSender = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final Account sender = Utils.generateRandomAccount();
		private final Signature signature = Utils.generateRandomSignature();
		private final Hash otherTransactionHash = Utils.generateRandomHash();
		private final MultisigSignatureDbModelToModelMapping mapping = new MultisigSignatureDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbSender, Account.class)).thenReturn(this.sender);
		}

		public MultisigSignature createDbSignature() {
			final MultisigSignature dbSignature = new MultisigSignature();
			dbSignature.setSender(this.dbSender);

			dbSignature.setFee(98765432L);
			dbSignature.setTimeStamp(4444);
			dbSignature.setDeadline(123);
			dbSignature.setSenderProof(this.signature.getBytes());

			dbSignature.setMultisigTransaction(new MultisigTransaction());
			dbSignature.getMultisigTransaction().setTransferHash(this.otherTransactionHash);
			return dbSignature;
		}

		public void assertModel(final MultisigSignatureTransaction model) {
			Assert.assertThat(model.getTimeStamp(), IsEqual.equalTo(new TimeInstant(4444)));
			Assert.assertThat(model.getSigner(), IsEqual.equalTo(this.sender));
			Assert.assertThat(model.getOtherTransactionHash(), IsEqual.equalTo(this.otherTransactionHash));

			Assert.assertThat(model.getFee(), IsEqual.equalTo(Amount.fromMicroNem(98765432)));
			Assert.assertThat(model.getDeadline(), IsEqual.equalTo(new TimeInstant(123)));
			Assert.assertThat(model.getSignature(), IsEqual.equalTo(this.signature));
		}
	}
}