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

public class MultisigSignatureModelToDbModelMappingTest {

	@Test
	public void signatureCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigSignatureTransaction signature = context.createModel();

		// Act:
		final MultisigSignature dbModel = context.mapping.map(signature);

		// Assert:
		context.assertDbModel(dbModel, HashUtils.calculateHash(signature));
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final org.nem.nis.dbmodel.Account dbSender = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final Account sender = Utils.generateRandomAccount();
		private final Signature signature = Utils.generateRandomSignature();
		private final Hash otherTransactionHash = Utils.generateRandomHash();
		private final MultisigSignatureModelToDbModelMapping mapping = new MultisigSignatureModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.sender, org.nem.nis.dbmodel.Account.class)).thenReturn(this.dbSender);
		}

		public MultisigSignatureTransaction createModel() {
			final MultisigSignatureTransaction transfer = new MultisigSignatureTransaction(
					new TimeInstant(4444),
					this.sender,
					this.otherTransactionHash);

			transfer.setFee(Amount.fromMicroNem(98765432L));
			transfer.setDeadline(new TimeInstant(123));
			transfer.setSignature(this.signature);
			return transfer;
		}

		public void assertDbModel(final MultisigSignature dbModel, final Hash expectedHash) {
			Assert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));

			Assert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(4444));
			Assert.assertThat(dbModel.getSender(), IsEqual.equalTo(this.dbSender));
			Assert.assertThat(dbModel.getMultisigTransaction(), IsEqual.equalTo(null)); // TODO clearly wrong

			Assert.assertThat(dbModel.getFee(), IsEqual.equalTo(98765432L));
			Assert.assertThat(dbModel.getDeadline(), IsEqual.equalTo(123));
			Assert.assertThat(dbModel.getSenderProof(), IsEqual.equalTo(this.signature.getBytes()));

			Assert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(expectedHash));
		}
	}
}