package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.MultisigSignature;
import org.nem.nis.test.MockAccountDao;

public class MultisigSignatureTransactionMapperTest {
	@Test
	public void multisigSignatureTransactionCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final MultisigSignature dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel);
	}

	@Test
	public void multisigSignatureTransactionCanBeRoundTripped() {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigSignature dbModel = context.toDbModel();

		// Act:
		final MultisigSignatureTransaction model = context.toModel(dbModel);

		// Assert:
		context.assertModel(model);
	}

	private static class TestContext {
		private final MultisigTransaction multisigTransaction;
		private final org.nem.nis.dbmodel.MultisigTransaction multisigDbTransaction;
		private final MultisigSignatureTransaction model;
		private final org.nem.nis.dbmodel.Account dbSender;
		private final MockAccountDao accountDao;
		private final Hash hash;
		private final Hash otherHash;
		final MockAccountLookup mockAccountLookup = new MockAccountLookup();

		private TestContext() {
			this(Utils.generateRandomAccount(), Utils.generateRandomHash());
		}

		private TestContext(final Account sender, final Hash hash) {
			this.multisigTransaction = Mockito.mock(MultisigTransaction.class);
			this.multisigDbTransaction = Mockito.mock(org.nem.nis.dbmodel.MultisigTransaction.class);
			this.otherHash = hash;
			Mockito.when(this.multisigTransaction.getOtherTransactionHash()).thenReturn(hash);

			this.model = new MultisigSignatureTransaction(
					new TimeInstant(721),
					sender,
					hash
			);
			this.model.setFee(Amount.fromNem(11));
			this.model.setDeadline(new TimeInstant(800));
			this.model.sign();

			this.hash = HashUtils.calculateHash(this.model);
			this.mockAccountLookup.setMockAccount(this.model.getSigner());

			this.dbSender = new org.nem.nis.dbmodel.Account();
			this.dbSender.setPrintableKey(this.model.getSigner().getAddress().getEncoded());
			this.dbSender.setPublicKey(this.model.getSigner().getAddress().getPublicKey());

			this.accountDao = new MockAccountDao();
			this.accountDao.addMapping(this.model.getSigner(), this.dbSender);
		}

		public MultisigSignature toDbModel() {
			return MultisigSignatureTransactionMapper.toDbModel(this.multisigDbTransaction, new AccountDaoLookupAdapter(this.accountDao), this.model);
		}

		public MultisigSignatureTransaction toModel(final MultisigSignature dbModel) {
			MultisigSignatureTransaction f = MultisigSignatureTransactionMapper.toModel(dbModel, this.mockAccountLookup, this.multisigTransaction);
			return f;
		}

		public void assertDbModel(final MultisigSignature dbModel) {
			Assert.assertThat(dbModel.getId(), IsNull.nullValue());
			Assert.assertThat(dbModel.getShortId(), IsEqual.equalTo(this.hash.getShortId()));
			Assert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(this.hash));
			Assert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));
			Assert.assertThat(dbModel.getFee(), IsEqual.equalTo(Amount.fromNem(11L).getNumMicroNem()));
			Assert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(721));
			Assert.assertThat(dbModel.getDeadline(), IsEqual.equalTo(800));
			Assert.assertThat(dbModel.getSender(), IsEqual.equalTo(this.dbSender));
			Assert.assertThat(dbModel.getSenderProof(), IsEqual.equalTo(this.model.getSignature().getBytes()));

			final PublicKey signerPublicKey = this.model.getSigner().getAddress().getPublicKey();
			Assert.assertThat(dbModel.getSender().getPublicKey(), IsEqual.equalTo(signerPublicKey));

			Assert.assertThat(dbModel.getMultisigTransaction(), IsEqual.equalTo(this.multisigDbTransaction));
		}

		public void assertModel(final MultisigSignatureTransaction rhs) {
			Assert.assertThat(HashUtils.calculateHash(rhs), IsEqual.equalTo(this.hash));
			Assert.assertThat(rhs.getVersion(), IsEqual.equalTo(this.model.getVersion()));
			Assert.assertThat(rhs.getType(), IsEqual.equalTo(this.model.getType()));
			Assert.assertThat(rhs.getFee(), IsEqual.equalTo(this.model.getFee()));
			Assert.assertThat(rhs.getTimeStamp(), IsEqual.equalTo(this.model.getTimeStamp()));
			Assert.assertThat(rhs.getDeadline(), IsEqual.equalTo(this.model.getDeadline()));
			Assert.assertThat(rhs.getSigner(), IsEqual.equalTo(this.model.getSigner()));
			Assert.assertThat(rhs.getSignature(), IsEqual.equalTo(this.model.getSignature()));

			Assert.assertThat(rhs.getOtherTransactionHash(), IsEqual.equalTo(this.otherHash));
		}
	}
}
