package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.ImportanceTransfer;
import org.nem.nis.dbmodel.Transfer;
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
		Assert.assertThat(dbModel.getBlockImportanceTransfers().size(), IsEqual.equalTo(0));
	}

	@Test
	public void blockModelWithTransferTransactionsCanBeMappedToDbModel() {
		// Arrange:
		final int NUM_TRANSACTIONS = 3;
		final TestContext context = new TestContext();
		context.addTransactions();

		// Act:
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel, NUM_TRANSACTIONS);
		Assert.assertThat(dbModel.getBlockTransfers().size(), IsEqual.equalTo(NUM_TRANSACTIONS));
		Assert.assertThat(dbModel.getBlockImportanceTransfers().size(), IsEqual.equalTo(0));
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
	public void blockModelWithTransferTransactionsCanBeRoundTripped() {
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
	public void blockModelWithImportanceTransfersCanBeMappedToDbModel() {
		// Arrange:
		final int NUM_TRANSACTIONS = 2;
		final TestContext context = new TestContext();
		context.addImportanceTransferTransactions();

		// Act:
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel, NUM_TRANSACTIONS);
		Assert.assertThat(dbModel.getBlockImportanceTransfers().size(), IsEqual.equalTo(NUM_TRANSACTIONS));
		Assert.assertThat(dbModel.getBlockTransfers().size(), IsEqual.equalTo(0));
		for (int i = 0; i < NUM_TRANSACTIONS; ++i) {
			final ImportanceTransfer dbTransfer = dbModel.getBlockImportanceTransfers().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
	}

	@Test
	public void blockModelWithImportanceTransfersCanBeRoundTripped() {
		// Arrange:
		final int NUM_TRANSACTIONS = 2;
		final TestContext context = new TestContext();
		context.addImportanceTransferTransactions();
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
	public void blockModelWithTransactionsCanBeMappedToDbModel() {
		// Arrange:
		final int NUM_TRANSACTIONS_A = 2;
		final int NUM_TRANSACTIONS_B = 3;
		final TestContext context = new TestContext();
		// order matters, as foraging will create block in that order
		context.addImportanceTransferTransactions();
		context.addTransactions();

		// Act:
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel, NUM_TRANSACTIONS_A + NUM_TRANSACTIONS_B);
		Assert.assertThat(dbModel.getBlockImportanceTransfers().size(), IsEqual.equalTo(NUM_TRANSACTIONS_A));
		Assert.assertThat(dbModel.getBlockTransfers().size(), IsEqual.equalTo(NUM_TRANSACTIONS_B));
		for (int i = 0; i < NUM_TRANSACTIONS_A; ++i) {
			final ImportanceTransfer dbTransfer = dbModel.getBlockImportanceTransfers().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
		for (int i = NUM_TRANSACTIONS_A; i < NUM_TRANSACTIONS_A + NUM_TRANSACTIONS_B; ++i) {
			final Transfer dbTransfer = dbModel.getBlockTransfers().get(i - NUM_TRANSACTIONS_A);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
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

	@Test
	public void dbModelWithNemesisTypeCanBeMappedToNemesisModel() {
		// Arrange:
		final DeserializationContext deserializationContext = new DeserializationContext(new MockAccountLookup());
		final TestContext context = new TestContext();
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();
		dbModel.setHeight(1L);

		// Act:
		final Block model = context.toModel(dbModel);

		// Assert:
		Assert.assertThat(model, IsInstanceOf.instanceOf(NemesisBlock.class));
		Assert.assertThat(
				HashUtils.calculateHash(model),
				IsEqual.equalTo(HashUtils.calculateHash(NemesisBlock.fromResource(deserializationContext))));
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
			this.dbAccount1 = this.createDbAccount(this.account1);

			this.account2 = Utils.generateRandomAccount();
			this.dbAccount2 = this.createDbAccount(this.account2);

			this.account3 = Utils.generateRandomAccount();
			this.dbAccount3 = this.createDbAccount(this.account3);

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

			for (final Transaction transaction : this.model.getTransactions()) {
				transaction.sign();
			}

			this.signModel();
		}

		public void addImportanceTransferTransactions() {
			this.model.addTransaction(new ImportanceTransferTransaction(
					new TimeInstant(150), this.account1, ImportanceTransferTransactionDirection.Transfer, this.account2.getAddress()));
			this.model.addTransaction(new ImportanceTransferTransaction(
					new TimeInstant(250), this.account3, ImportanceTransferTransactionDirection.Transfer, this.account2.getAddress()));

			for (final Transaction transaction : this.model.getTransactions()) {
				transaction.sign();
			}

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
			Assert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(721));
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
			Assert.assertThat(HashUtils.calculateHash(rhs), IsEqual.equalTo(this.hash));
			Assert.assertThat(rhs.getType(), IsEqual.equalTo(this.model.getType()));
			Assert.assertThat(rhs.getVersion(), IsEqual.equalTo(this.model.getVersion()));
			Assert.assertThat(rhs.getPreviousBlockHash(), IsEqual.equalTo(this.model.getPreviousBlockHash()));
			Assert.assertThat(rhs.getTimeStamp(), IsEqual.equalTo(this.model.getTimeStamp()));
			Assert.assertThat(rhs.getSigner(), IsEqual.equalTo(this.model.getSigner()));
			Assert.assertThat(rhs.getSignature(), IsEqual.equalTo(this.model.getSignature()));
			Assert.assertThat(rhs.getHeight(), IsEqual.equalTo(this.model.getHeight()));
			Assert.assertThat(rhs.getTotalFee(), IsEqual.equalTo(this.model.getTotalFee()));
			Assert.assertThat(rhs.getDifficulty(), IsEqual.equalTo(this.model.getDifficulty()));
			Assert.assertThat(rhs.getGenerationHash(), IsEqual.equalTo(this.model.getGenerationHash()));
		}
	}
}
