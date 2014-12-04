package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.*;
import org.nem.core.model.MultisigTransaction;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.test.MockAccountDao;

public class BlockMapperTest {

	//region blocks with single transaction of given type
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
	public void blockModelWithLessorCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addLessor();

		// Act:
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel, 0);
		Assert.assertThat(dbModel.getBlockTransfers().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockImportanceTransfers().size(), IsEqual.equalTo(0));
	}

	@Test
	public void blockModelWithMultisigSignerModificationTransactionCanBeMappedToDbModel() {
		// Arrange:
		final int NUM_TRANSACTIONS = 2;
		final TestContext context = new TestContext();
		context.addMultisigSignerModificationTransactions();

		// Act:
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel, NUM_TRANSACTIONS*1000L);
		Assert.assertThat(dbModel.getBlockMultisigSignerModifications().size(), IsEqual.equalTo(NUM_TRANSACTIONS));
		Assert.assertThat(dbModel.getBlockImportanceTransfers().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockTransfers().size(), IsEqual.equalTo(0));
		for (int i = 0; i < NUM_TRANSACTIONS; ++i) {
			final MultisigSignerModification dbTransfer = dbModel.getBlockMultisigSignerModifications().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
	}

	@Test
	public void blockModelWithMultisigTransactionCanBeMappedToDbModel() {
		// Arrange:
		final int NUM_TRANSACTIONS = 2;
		final TestContext context = new TestContext();
		context.addMultisigTransactions();

		// Act:
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel, NUM_TRANSACTIONS * (100L + 1));
		// note: we expect both multisig txes and block transfers
		Assert.assertThat(dbModel.getBlockMultisigTransactions().size(), IsEqual.equalTo(NUM_TRANSACTIONS));
		Assert.assertThat(dbModel.getBlockMultisigSignerModifications().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockImportanceTransfers().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockTransfers().size(), IsEqual.equalTo(NUM_TRANSACTIONS));

		for (int i = 0; i < NUM_TRANSACTIONS; ++i) {
			final org.nem.nis.dbmodel.MultisigTransaction dbTransfer = dbModel.getBlockMultisigTransactions().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
	}

	@Test
	public void blockModelWithMultisigTransactionWithSignaturesCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMultisigTransactionsWithSigners();

		// Act:
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel, 1 * (100L + 1));
		// note: we expect both multisig txes and block transfers
		Assert.assertThat(dbModel.getBlockMultisigTransactions().size(), IsEqual.equalTo(1));
		Assert.assertThat(dbModel.getBlockMultisigSignerModifications().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockImportanceTransfers().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockTransfers().size(), IsEqual.equalTo(1));

		final org.nem.nis.dbmodel.MultisigTransaction dbMultisig = dbModel.getBlockMultisigTransactions().get(0);
		final Transaction transaction = context.getModel().getTransactions().get(0);
		Assert.assertThat(dbMultisig.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));

		Assert.assertThat(dbMultisig.getMultisigSignatures().size(), IsEqual.equalTo(2));
	}
	//endregion

	// roundtrip test of block with transaction
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
	public void blockModelWithLessorCanBeRoundTripped() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addLessor();
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Act:
		final Block model = context.toModel(dbModel);

		// Assert:
		context.assertModel(model);
	}
	//endregion

	// roundtrip test of block with multiple transactions
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
	public void blockModelWithMultisigSignerModificationTransactionCanBeRoundTripped() {
		// Arrange:
		final int NUM_TRANSACTIONS = 2;
		final TestContext context = new TestContext();
		context.addMultisigSignerModificationTransactions();
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
	public void blockModelWithMultisigTransactionCanBeRoundTripped() {
		// Arrange:
		final int NUM_TRANSACTIONS = 2;
		final TestContext context = new TestContext();
		context.addMultisigTransactions();
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
	public void blockModelWithMultisigTransactionWithSignaturesCanBeRoundTripped() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMultisigTransactionsWithSigners();
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Act:
		final Block model = context.toModel(dbModel);

		// Assert:
		context.assertModel(model);
		Assert.assertThat(model.getTransactions().size(), IsEqual.equalTo(1));
		final MultisigTransaction transaction = (MultisigTransaction) model.getTransactions().get(0);

		Assert.assertThat(transaction.getCosignerSignatures().size(), IsEqual.equalTo(2));
	}
	//endregion

	//region complex tests
	@Test
	public void blockModelWithTransactionsCanBeMappedToDbModel() {
		// Arrange:
		final int NUM_TRANSACTIONS_A = 2;
		final int NUM_TRANSACTIONS_B = 2;
		final int NUM_TRANSACTIONS_C = 3;
		final TestContext context = new TestContext();
		// order matters, as foraging will create block in that order
		context.addMultisigSignerModificationTransactions();
		context.addImportanceTransferTransactions();
		context.addTransactions();

		// Act:
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel, NUM_TRANSACTIONS_A * 1000L + NUM_TRANSACTIONS_B + NUM_TRANSACTIONS_C);
		Assert.assertThat(dbModel.getBlockMultisigSignerModifications().size(), IsEqual.equalTo(NUM_TRANSACTIONS_A));
		Assert.assertThat(dbModel.getBlockImportanceTransfers().size(), IsEqual.equalTo(NUM_TRANSACTIONS_B));
		Assert.assertThat(dbModel.getBlockTransfers().size(), IsEqual.equalTo(NUM_TRANSACTIONS_C));
		for (int i = 0; i < NUM_TRANSACTIONS_A; ++i) {
			final MultisigSignerModification dbTransfer = dbModel.getBlockMultisigSignerModifications().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
		for (int i = 2; i < 2 + NUM_TRANSACTIONS_B; ++i) {
			final ImportanceTransfer dbTransfer = dbModel.getBlockImportanceTransfers().get(i - 2);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
		for (int i = 4; i < 4 + NUM_TRANSACTIONS_C; ++i) {
			final Transfer dbTransfer = dbModel.getBlockTransfers().get(i - 4);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
	}

	@Test
	public void blockModelWithSortedTransactionsCanBeRoundTripped() {
		// TODO 20141010 J-G: do we also need a test that the transactions are saved sorted or do the dao tests cover that?
		// > actually, it looks like the previous test (blockModelWithTransactionsCanBeMappedToDbModel) is validating that, right?
		// TODO 20141119 G-J: not exactly sure what you mean, but I believe the answer is yes ;)
		// Arrange:
		final int NUM_TRANSACTIONS_A = 2;
		final int NUM_TRANSACTIONS_B = 2;
		final int NUM_TRANSACTIONS_C = 3;
		final TestContext context = new TestContext();

		// order matters, let's assume fees were such that block have been created in such order
		final ImportanceTransferTransaction.Mode mode = ImportanceTransferTransaction.Mode.Activate;
		final MultisigModificationType modificationType = MultisigModificationType.Add;

		context.model.addTransaction(new TransferTransaction(new TimeInstant(100), context.account1, context.account2, new Amount(7), null));
		context.model.addTransaction(new MultisigSignerModificationTransaction(new TimeInstant(200), context.account1, modificationType, context.account2));
		context.model.addTransaction(new ImportanceTransferTransaction(new TimeInstant(300), context.account1, mode, context.account2));
		context.model.addTransaction(new TransferTransaction(new TimeInstant(400), context.account2, context.account3, new Amount(11), null));
		context.model.addTransaction(new MultisigSignerModificationTransaction(new TimeInstant(500), context.account1, modificationType, context.account3));
		context.model.addTransaction(new ImportanceTransferTransaction(new TimeInstant(600), context.account3, mode, context.account2));
		context.model.addTransaction(new TransferTransaction(new TimeInstant(700), context.account3, context.account1, new Amount(4), null));
		for (final Transaction transaction : context.model.getTransactions()) {
			transaction.setFee(Amount.fromNem(1000L));
			transaction.sign();
		}

		context.signModel();
		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Act:
		final Block model = context.toModel(dbModel);

		// Assert:
		context.assertDbModel(dbModel, 1000L * (NUM_TRANSACTIONS_A + NUM_TRANSACTIONS_B + NUM_TRANSACTIONS_C));
		Assert.assertThat(dbModel.getBlockMultisigSignerModifications().size(), IsEqual.equalTo(NUM_TRANSACTIONS_A));
		Assert.assertThat(dbModel.getBlockImportanceTransfers().size(), IsEqual.equalTo(NUM_TRANSACTIONS_B));
		Assert.assertThat(dbModel.getBlockTransfers().size(), IsEqual.equalTo(NUM_TRANSACTIONS_C));
		for (int i = 0; i < NUM_TRANSACTIONS_A; ++i) {
			final MultisigSignerModification dbTransfer = dbModel.getBlockMultisigSignerModifications().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(3 * i + 1);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
		for (int i = 0; i < NUM_TRANSACTIONS_B; ++i) {
			final ImportanceTransfer dbTransfer = dbModel.getBlockImportanceTransfers().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(3 * i + 2);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
		for (int i = 0; i < NUM_TRANSACTIONS_C; ++i) {
			final Transfer dbTransfer = dbModel.getBlockTransfers().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(3 * i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}

		// assert model
		Assert.assertThat(model.getSignature(), IsEqual.equalTo(context.model.getSignature()));
		for (int i = 0; i < NUM_TRANSACTIONS_A + NUM_TRANSACTIONS_B + NUM_TRANSACTIONS_C; ++i) {
			final Transaction expected = context.getModel().getTransactions().get(i);
			final Transaction actual = model.getTransactions().get(i);
			Assert.assertThat(HashUtils.calculateHash(expected), IsEqual.equalTo(HashUtils.calculateHash(actual)));
		}
	}

	@Test
	public void blockModelWithTransactionsMixedWithMultisigTransactionsCanBeRoundTripped() {
		// Arrange:
		final TestContext context = new TestContext();

		// account3 is multisig account1 is cosignatory
		context.model.addTransaction(new TransferTransaction(new TimeInstant(100), context.account1, context.account2, new Amount(7), null));
		final Transaction transaction1 = new TransferTransaction(new TimeInstant(200), context.account3, context.account2, new Amount(11), null);
		context.model.addTransaction(new MultisigTransaction(new TimeInstant(200), context.account1, transaction1));
		context.model.addTransaction(new TransferTransaction(new TimeInstant(300), context.account1, context.account2, new Amount(13), null));
		final Transaction transaction2 = new TransferTransaction(new TimeInstant(400), context.account3, context.account2, new Amount(15), null);
		context.model.addTransaction(new MultisigTransaction(new TimeInstant(400), context.account1, transaction1));

		for (final Transaction transaction : context.model.getTransactions()) {
			transaction.sign();
		}
		context.signModel();

		final org.nem.nis.dbmodel.Block dbModel = context.toDbModel();

		// Act:
		final Block model = context.toModel(dbModel);

		// Assert:
		context.assertDbModel(dbModel, (100L + 1) * 2 + 2);

		Assert.assertThat(dbModel.getBlockMultisigTransactions().size(), IsEqual.equalTo(2));
		Assert.assertThat(dbModel.getBlockMultisigSignerModifications().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockImportanceTransfers().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockTransfers().size(), IsEqual.equalTo(4));
		for (int i = 0; i < 2; ++i) {
			// note: we're skipping in DB too, as those are the ones that "belong" to multisig TXes
			final Transfer dbTransfer = dbModel.getBlockTransfers().get(2*i);
			final Transaction transaction = context.getModel().getTransactions().get(2 * i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
		for (int i = 0; i < 2; ++i) {
			final org.nem.nis.dbmodel.MultisigTransaction dbTransfer = dbModel.getBlockMultisigTransactions().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(2 * i + 1);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}

		// WARNING: if test fails here it means you've changed order of TXes inside the dbModel
		for (int i = 0; i < 2; ++i) {
			final org.nem.nis.dbmodel.MultisigTransaction dbMultisig = dbModel.getBlockMultisigTransactions().get(i);
			// transfer that "belongs" to multisig
			final Transfer dbTransfer = dbModel.getBlockTransfers().get(2*i + 1);

			// YES, they should be equal
			Assert.assertThat(dbMultisig.getBlkIndex(), IsEqual.equalTo(2*i + 1));
			Assert.assertThat(dbTransfer.getBlkIndex(), IsEqual.equalTo(2*i + 1));
		}

		for (int i = 0; i < 4; ++i) {
			final Transaction expected = context.getModel().getTransactions().get(i);
			final Transaction actual = model.getTransactions().get(i);
			Assert.assertThat(HashUtils.calculateHash(expected), IsEqual.equalTo(HashUtils.calculateHash(actual)));
		}
	}
	//endregion

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
		private Account lessor;
		private org.nem.nis.dbmodel.Account dbLessor;

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

		public void addLessor() {
			this.lessor = Utils.generateRandomAccount();
			this.dbLessor = this.createDbAccount(this.lessor);
			this.accountDao.addMapping(this.lessor, this.dbLessor);
			this.model.setLessor(this.lessor);
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
					new TimeInstant(150), this.account1, ImportanceTransferTransaction.Mode.Activate, this.account2));
			this.model.addTransaction(new ImportanceTransferTransaction(
					new TimeInstant(250), this.account3, ImportanceTransferTransaction.Mode.Activate, this.account2));

			for (final Transaction transaction : this.model.getTransactions()) {
				transaction.sign();
			}

			this.signModel();
		}

		public void addMultisigSignerModificationTransactions() {
			this.model.addTransaction(new MultisigSignerModificationTransaction(
					new TimeInstant(150), this.account1, MultisigModificationType.Add, this.account2));
			this.model.addTransaction(new MultisigSignerModificationTransaction(
					new TimeInstant(250), this.account1, MultisigModificationType.Add, this.account3));

			for (final Transaction transaction : this.model.getTransactions()) {
				transaction.sign();
			}

			this.signModel();
		}

		public void addMultisigTransactions() {
			final Transaction transfer1 = new TransferTransaction(new TimeInstant(100), this.account1, this.account2, new Amount(7), null);
			final Transaction transfer2 = new TransferTransaction(new TimeInstant(200), this.account1, this.account2, new Amount(11), null);

			this.model.addTransaction(new MultisigTransaction(new TimeInstant(100), this.account3, transfer1));
			this.model.addTransaction(new MultisigTransaction(new TimeInstant(200), this.account3, transfer2));

			for (final Transaction transaction : this.model.getTransactions()) {
				transaction.sign();
			}

			this.signModel();
		}

		public void addMultisigTransactionsWithSigners() {
			final Transaction transfer1 = new TransferTransaction(new TimeInstant(100), this.account1, this.account2, new Amount(7), null);
			final Hash transferHash = HashUtils.calculateHash(transfer1);

			final MultisigTransaction multisigTransaction = new MultisigTransaction(new TimeInstant(100), this.account3, transfer1);
			this.model.addTransaction(multisigTransaction);

			final Signature signature1 = new Signature(Utils.generateRandomBytes(64));
			final MultisigSignatureTransaction multisigSignature1 = new MultisigSignatureTransaction(new TimeInstant(123), this.account1, transferHash, signature1);
			multisigSignature1.sign();
			multisigTransaction.addSignature(multisigSignature1);

			final Signature signature2 = new Signature(Utils.generateRandomBytes(64));
			final MultisigSignatureTransaction multisigSignature2 = new MultisigSignatureTransaction(new TimeInstant(132), this.account2, transferHash, signature2);
			multisigSignature2.sign();
			multisigTransaction.addSignature(multisigSignature2);

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
			Assert.assertThat(dbModel.getDifficulty(), IsEqual.equalTo(79_876_543_211_237L));
			Assert.assertThat(dbModel.getGenerationHash(), IsEqual.equalTo(this.blockGenerationHash));
			Assert.assertThat(dbModel.getLessor(), IsEqual.equalTo(this.dbLessor));

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
			Assert.assertThat(rhs.getLessor(), IsEqual.equalTo(this.model.getLessor()));
		}
	}
}
