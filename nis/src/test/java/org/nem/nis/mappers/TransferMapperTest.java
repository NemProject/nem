package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.messages.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Transfer;
import org.nem.nis.test.MockAccountDao;

public class TransferMapperTest {

	@Test
	public void transferModelWithoutMessageCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext(null);

		// Act:
		final Transfer dbModel = context.toDbModel(7);

		// Assert:
		context.assertDbModel(dbModel, 7);
	}

	@Test
	public void transferModelWithPlainMessageCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext(new PlainMessage(new byte[] { 12, 45, 16 }));

		// Act:
		final Transfer dbModel = context.toDbModel(7);

		// Assert:
		context.assertDbModel(dbModel, 7);
	}

	@Test
	public void transferModelWithSecureMessageCanBeMappedToDbModel() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final TestContext context = new TestContext(
				SecureMessage.fromDecodedPayload(sender, recipient, new byte[] { 12, 45, 16 }),
				sender,
				recipient);

		// Act:
		final Transfer dbModel = context.toDbModel(7);

		// Assert:
		context.assertDbModel(dbModel, 7);
	}

	@Test
	public void transferModelWithoutMessageCanBeRoundTripped() {
		// Arrange:
		final TestContext context = new TestContext(null);
		final Transfer dbModel = context.toDbModel(7);

		// Act:
		final TransferTransaction model = context.toModel(dbModel);

		// Assert:
		context.assertModel(model);
	}

	@Test
	public void transferModelWithPlainMessageCanBeRoundTripped() {
		// Arrange:
		final TestContext context = new TestContext(new PlainMessage(new byte[] { 12, 45, 16 }));
		final Transfer dbModel = context.toDbModel(7);

		// Act:
		final TransferTransaction model = context.toModel(dbModel);

		// Assert:
		context.assertModel(model);
	}

	@Test
	public void transferModelWithSecureMessageCanBeRoundTripped() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final TestContext context = new TestContext(
				SecureMessage.fromDecodedPayload(sender, recipient, new byte[] { 12, 45, 16 }),
				sender,
				recipient);
		final Transfer dbModel = context.toDbModel(7);

		// Act:
		final TransferTransaction model = context.toModel(dbModel);

		// Assert:
		context.assertModel(model);
	}

	@Test(expected = IllegalArgumentException.class)
	public void transferDbModelWithUnknownMessageTypeCannotBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext(new PlainMessage(new byte[] { 12, 45, 16 }));
		final Transfer dbModel = context.toDbModel(7);
		dbModel.setMessageType(-1);

		// Act:
		context.toModel(dbModel);
	}

	private class TestContext {

		private final TransferTransaction model;
		private final org.nem.nis.dbmodel.Account dbSender;
		private final org.nem.nis.dbmodel.Account dbRecipient;
		private final MockAccountDao accountDao;
		private final Hash hash;

		public TestContext(final Message message, final Account sender, final Account recipient) {
			this.model = new TransferTransaction(
					new TimeInstant(721),
					sender,
					recipient,
					new Amount(144),
					message);

			this.model.setFee(Amount.fromNem(11));
			this.model.setDeadline(new TimeInstant(800));
			this.model.sign();

			this.dbSender = new org.nem.nis.dbmodel.Account();
			this.dbSender.setPrintableKey(this.model.getSigner().getAddress().getEncoded());
			this.dbSender.setPublicKey(this.model.getSigner().getAddress().getPublicKey());

			this.dbRecipient = new org.nem.nis.dbmodel.Account();
			this.dbRecipient.setPrintableKey(this.model.getRecipient().getAddress().getEncoded());

			this.accountDao = new MockAccountDao();
			this.accountDao.addMapping(this.model.getSigner(), this.dbSender);
			this.accountDao.addMapping(this.model.getRecipient(), this.dbRecipient);

			this.hash = HashUtils.calculateHash(this.model);
		}

		public TestContext(final Message message) {
			this(message, Utils.generateRandomAccount(), Utils.generateRandomAccount());
		}

		public TransferTransaction getModel() {
			return this.model;
		}

		public Transfer toDbModel(final int blockIndex) {
			// TODO 20141010 J-G guess we should validate orderIndex like blockIndex
			return TransferMapper.toDbModel(this.model, blockIndex, 0, new AccountDaoLookupAdapter(this.accountDao));
		}

		public TransferTransaction toModel(final Transfer dbTransfer) {
			final MockAccountLookup mockAccountLookup = new MockAccountLookup();
			mockAccountLookup.setMockAccount(this.model.getSigner());
			mockAccountLookup.setMockAccount(this.model.getRecipient());
			return TransferMapper.toModel(dbTransfer, mockAccountLookup);
		}

		public void assertDbModel(final Transfer dbModel, final int blockId) {
			Assert.assertThat(dbModel.getId(), IsNull.nullValue());
			Assert.assertThat(dbModel.getShortId(), IsEqual.equalTo(this.hash.getShortId()));
			Assert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(this.hash));
			Assert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));
			Assert.assertThat(dbModel.getType(), IsEqual.equalTo(TransactionTypes.TRANSFER));
			Assert.assertThat(dbModel.getFee(), IsEqual.equalTo(Amount.fromNem(11L).getNumMicroNem()));
			Assert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(721));
			Assert.assertThat(dbModel.getDeadline(), IsEqual.equalTo(800));
			Assert.assertThat(dbModel.getSender(), IsEqual.equalTo(this.dbSender));
			Assert.assertThat(dbModel.getSenderProof(), IsEqual.equalTo(this.model.getSignature().getBytes()));
			Assert.assertThat(dbModel.getRecipient(), IsEqual.equalTo(this.dbRecipient));
			Assert.assertThat(dbModel.getAmount(), IsEqual.equalTo(144L));
			Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));
			Assert.assertThat(dbModel.getBlock(), IsNull.nullValue());

			// leaving for now, as I'm not sure if I can remove it without consequences
			// TODO 20141010 J-G why would you want to remove it?
			Assert.assertThat(dbModel.getBlkIndex(), IsEqual.equalTo(blockId));

			final PublicKey signerPublicKey = this.model.getSigner().getAddress().getPublicKey();
			Assert.assertThat(dbModel.getSender().getPublicKey(), IsEqual.equalTo(signerPublicKey));
			final PublicKey recipientPublicKey = this.model.getRecipient().getAddress().getPublicKey();
			Assert.assertThat(dbModel.getRecipient().getPublicKey(), IsEqual.equalTo(recipientPublicKey));
		}

		public void assertModel(final TransferTransaction rhs) {
			Assert.assertThat(HashUtils.calculateHash(rhs), IsEqual.equalTo(this.hash));
			Assert.assertThat(rhs.getVersion(), IsEqual.equalTo(this.model.getVersion()));
			Assert.assertThat(rhs.getType(), IsEqual.equalTo(this.model.getType()));
			Assert.assertThat(rhs.getFee(), IsEqual.equalTo(this.model.getFee()));
			Assert.assertThat(rhs.getTimeStamp(), IsEqual.equalTo(this.model.getTimeStamp()));
			Assert.assertThat(rhs.getDeadline(), IsEqual.equalTo(this.model.getDeadline()));
			Assert.assertThat(rhs.getSigner(), IsEqual.equalTo(this.model.getSigner()));
			Assert.assertThat(rhs.getSignature(), IsEqual.equalTo(this.model.getSignature()));
			Assert.assertThat(rhs.getRecipient(), IsEqual.equalTo(this.model.getRecipient()));
			Assert.assertThat(rhs.getRecipient().getAddress().getPublicKey(), IsEqual.equalTo(this.model.getRecipient().getAddress().getPublicKey()));
			Assert.assertThat(rhs.getAmount(), IsEqual.equalTo(this.model.getAmount()));
			this.assertAreEqual(this.model.getMessage(), rhs.getMessage());
		}

		private void assertAreEqual(final Message lhs, final Message rhs) {
			if (null == lhs || null == rhs) {
				Assert.assertThat(rhs, IsEqual.equalTo(lhs));
				return;
			}

			Assert.assertThat(rhs.getType(), IsEqual.equalTo(lhs.getType()));
			Assert.assertThat(rhs.getEncodedPayload(), IsEqual.equalTo(lhs.getEncodedPayload()));
		}
	}
}