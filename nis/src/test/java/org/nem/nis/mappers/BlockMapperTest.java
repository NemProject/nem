package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.crypto.PublicKey;
import org.nem.nis.dbmodel.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockDifficulty;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.MockAccountDao;

public class BlockMapperTest {

	@Test
	public void blockModelWithoutTransactionsCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel, 0);
		Assert.assertThat(dbModel.getBlockTransfers().size(), IsEqual.equalTo(0));
	}

	@Test
	public void blockModelWithTransactionsCanBeMappedToDbModel() {
		// Arrange:
		final int NUM_TRANSACTIONS = 3;
		final TestContext context = new TestContext();
		context.addTransactions();

		// Act:
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel, NUM_TRANSACTIONS);
		Assert.assertThat(dbModel.getBlockTransfers().size(), IsEqual.equalTo(NUM_TRANSACTIONS));
		for (int i = 0; i < NUM_TRANSACTIONS; ++i) {
			final Transfer dbTransfer = dbModel.getBlockTransfers().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
	}

	@Test
	public void blockModelWithoutTransactionsCanBeRoundTripped() {
		// Arrange:
		final TestContext context = new TestContext();
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Act:
		final Block model = context.toModel(dbModel);

		// Assert:
		context.assertModel(model);
	}

	@Test
	public void blockModelWithTransactionsCanBeRoundTripped() {
		// Arrange:
		final int NUM_TRANSACTIONS = 3;
		final TestContext context = new TestContext();
		context.addTransactions();
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Act:
		final Block model = context.toModel(dbModel);

		// Assert:
		context.assertModel(model);
		Assert.assertThat(model.getTransactions().size(), IsEqual.equalTo(NUM_TRANSACTIONS));
		for (int i = 0; i < NUM_TRANSACTIONS; ++i) {
			final Transaction originalTransaction = context.getModel().getTransactions().get(i);
			final Hash originalTransactionHash = HashUtils.calculateHash(originalTransaction);

			final Transaction transaction = model.getTransactions().get(i);
			final Hash transactionHash = HashUtils.calculateHash(transaction);

			Assert.assertThat(transactionHash, IsEqual.equalTo(originalTransactionHash));
		}
	}

	@Test
	public void dbModelWithoutDifficultyCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();
		dbModel.setDifficulty(null);

		// Act:
		final Block model = context.toModel(dbModel);

		// Assert:
		Assert.assertThat(model.getDifficulty(), IsEqual.equalTo(new BlockDifficulty(0)));
	}

	private class TestContext {

		private final Block model;
		private final org.nem.nis.dbmodel.Account dbForager;
		private final Account account1;
		private final org.nem.nis.dbmodel.Account dbAccount1;
		private final Account account2;
		private final org.nem.nis.dbmodel.Account dbAccount2;
		private final Account account3;
		private final org.nem.nis.dbmodel.Account dbAccount3;
		private final MockAccountDao accountDao;
		private final Hash blockGenerationHash;
		private Hash hash;

		public TestContext() {
			this.blockGenerationHash = Utils.generateRandomHash();
			this.model = new Block(
					Utils.generateRandomAccount(),
					Utils.generateRandomHash(),
					this.blockGenerationHash,
					new TimeInstant(721),
					new BlockHeight(17));

			this.model.setDifficulty(new BlockDifficulty(79_876_543_211_237L));
			this.signModel();

			this.dbForager = new org.nem.nis.dbmodel.Account();
			this.dbForager.setPrintableKey(this.model.getSigner().getAddress().getEncoded());
			this.dbForager.setPublicKey(this.model.getSigner().getKeyPair().getPublicKey());

			this.account1 = Utils.generateRandomAccount();
			this.dbAccount1 = createDbAccount(this.account1);

			this.account2 = Utils.generateRandomAccount();
			this.dbAccount2 = createDbAccount(this.account2);

			this.account3 = Utils.generateRandomAccount();
			this.dbAccount3 = createDbAccount(this.account3);

			this.accountDao = new MockAccountDao();
			this.accountDao.addMapping(this.model.getSigner(), this.dbForager);
			this.accountDao.addMapping(this.account1, this.dbAccount1);
			this.accountDao.addMapping(this.account2, this.dbAccount2);
			this.accountDao.addMapping(this.account3, this.dbAccount3);
		}

		private org.nem.nis.dbmodel.Account createDbAccount(final Account account) {
			final org.nem.nis.dbmodel.Account dbAccount = new org.nem.nis.dbmodel.Account();
			dbAccount.setPublicKey(account.getKeyPair().getPublicKey());
			dbAccount.setPrintableKey(account.getAddress().getEncoded());
			return dbAccount;
		}

		public Block getModel() {
			return this.model;
		}

		public org.nem.nis.dbmodel.Block toDbModel() {
			return BlockMapper.toDbModel(this.model, new AccountDaoLookupAdapter(this.accountDao));
		}

		public Block toModel(final org.nem.nis.dbmodel.Block dbBlock) {
			final MockAccountLookup mockAccountLookup = new MockAccountLookup();
			mockAccountLookup.setMockAccount(this.model.getSigner());
			mockAccountLookup.setMockAccount(this.account1);
			mockAccountLookup.setMockAccount(this.account2);
			mockAccountLookup.setMockAccount(this.account3);
			return BlockMapper.toModel(dbBlock, mockAccountLookup);
		}

		public void addTransactions() {
			this.model.addTransaction(new TransferTransaction(
					new TimeInstant(100), this.account1, this.account2, new Amount(7), null));
			this.model.addTransaction(new TransferTransaction(
					new TimeInstant(200), this.account2, this.account3, new Amount(11), null));
			this.model.addTransaction(new TransferTransaction(
					new TimeInstant(300), this.account3, this.account1, new Amount(4), null));

			for (final Transaction transaction : this.model.getTransactions())
				transaction.sign();

			this.signModel();
		}

		private void signModel() {
			this.model.sign();
			this.hash = HashUtils.calculateHash(this.model);
		}

		public void assertDbModel(final org.nem.nis.dbmodel.Block dbModel, final long expectedFee) {
			Assert.assertThat(dbModel.getId(), IsNull.nullValue());
			Assert.assertThat(dbModel.getShortId(), IsEqual.equalTo(this.hash.getShortId()));
			Assert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));
			Assert.assertThat(dbModel.getPrevBlockHash(), IsEqual.equalTo(this.model.getPreviousBlockHash()));
			Assert.assertThat(dbModel.getBlockHash(), IsEqual.equalTo(this.hash));
			Assert.assertThat(dbModel.getTimestamp(), IsEqual.equalTo(721));
			Assert.assertThat(dbModel.getForger(), IsEqual.equalTo(this.dbForager));
			Assert.assertThat(dbModel.getForgerProof(), IsEqual.equalTo(this.model.getSignature().getBytes()));
			Assert.assertThat(dbModel.getHeight(), IsEqual.equalTo(17L));
			Assert.assertThat(dbModel.getTotalAmount(), IsEqual.equalTo(0L));
			Assert.assertThat(dbModel.getTotalFee(), IsEqual.equalTo(Amount.fromNem(expectedFee).getNumMicroNem()));
			Assert.assertThat(dbModel.getNextBlockId(), IsNull.nullValue());
			Assert.assertThat(dbModel.getDifficulty(), IsEqual.equalTo(79_876_543_211_237L));
			Assert.assertThat(dbModel.getGenerationHash(), IsEqual.equalTo(this.blockGenerationHash));

			final PublicKey signerPublicKey = this.model.getSigner().getKeyPair().getPublicKey();
			Assert.assertThat(dbModel.getForger().getPublicKey(), IsEqual.equalTo(signerPublicKey));
		}

		public void assertModel(final Block rhs) {
			Assert.assertThat(HashUtils.calculateHash(rhs), IsEqual.equalTo(hash));
			Assert.assertThat(rhs.getType(), IsEqual.equalTo(model.getType()));
			Assert.assertThat(rhs.getVersion(), IsEqual.equalTo(model.getVersion()));
			Assert.assertThat(rhs.getPreviousBlockHash(), IsEqual.equalTo(model.getPreviousBlockHash()));
			Assert.assertThat(rhs.getTimeStamp(), IsEqual.equalTo(model.getTimeStamp()));
			Assert.assertThat(rhs.getSigner(), IsEqual.equalTo(model.getSigner()));
			Assert.assertThat(rhs.getSignature(), IsEqual.equalTo(model.getSignature()));
			Assert.assertThat(rhs.getHeight(), IsEqual.equalTo(model.getHeight()));
			Assert.assertThat(rhs.getTotalFee(), IsEqual.equalTo(model.getTotalFee()));
			Assert.assertThat(rhs.getDifficulty(), IsEqual.equalTo(model.getDifficulty()));
			Assert.assertThat(rhs.getGenerationHash(), IsEqual.equalTo(model.getGenerationHash()));
		}
	}
}
