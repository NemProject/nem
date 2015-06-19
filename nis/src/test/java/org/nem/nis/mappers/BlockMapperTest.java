package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.test.*;

import java.util.*;

/**
 * This test has really evolved into a mapper integration test.
 * The specific mapping tests are the unit tests.
 */
public class BlockMapperTest {

	//region blocks with single transaction of given type
	@Test
	public void blockModelWithoutTransactionsCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final DbBlock dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel);
		Assert.assertThat(dbModel.getBlockTransferTransactions().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockImportanceTransferTransactions().size(), IsEqual.equalTo(0));
	}

	@Test
	public void blockModelWithTransferTransactionsCanBeMappedToDbModel() {
		// Arrange:
		final int NUM_TRANSACTIONS = 3;
		final TestContext context = new TestContext();
		context.addTransactions();

		// Act:
		final DbBlock dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel);
		Assert.assertThat(dbModel.getBlockTransferTransactions().size(), IsEqual.equalTo(NUM_TRANSACTIONS));
		Assert.assertThat(dbModel.getBlockImportanceTransferTransactions().size(), IsEqual.equalTo(0));
		for (int i = 0; i < NUM_TRANSACTIONS; ++i) {
			final DbTransferTransaction dbTransferTransaction = dbModel.getBlockTransferTransactions().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransferTransaction.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
	}

	@Test
	public void blockModelWithImportanceTransfersCanBeMappedToDbModel() {
		// Arrange:
		final int NUM_TRANSACTIONS = 2;
		final TestContext context = new TestContext();
		context.addImportanceTransferTransactions();

		// Act:
		final DbBlock dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel);
		Assert.assertThat(dbModel.getBlockImportanceTransferTransactions().size(), IsEqual.equalTo(NUM_TRANSACTIONS));
		Assert.assertThat(dbModel.getBlockTransferTransactions().size(), IsEqual.equalTo(0));
		for (int i = 0; i < NUM_TRANSACTIONS; ++i) {
			final DbImportanceTransferTransaction dbTransfer = dbModel.getBlockImportanceTransferTransactions().get(i);
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
		final DbBlock dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel);
		Assert.assertThat(dbModel.getBlockTransferTransactions().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockImportanceTransferTransactions().size(), IsEqual.equalTo(0));
	}

	@Test
	public void blockModelWithMultisigModificationTransactionCanBeMappedToDbModel() {
		// Arrange:
		final int NUM_TRANSACTIONS = 2;
		final TestContext context = new TestContext();
		context.addMultisigModificationTransactions();

		// Act:
		final DbBlock dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel);
		Assert.assertThat(dbModel.getBlockMultisigAggregateModificationTransactions().size(), IsEqual.equalTo(NUM_TRANSACTIONS));
		Assert.assertThat(dbModel.getBlockImportanceTransferTransactions().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockTransferTransactions().size(), IsEqual.equalTo(0));
		for (int i = 0; i < NUM_TRANSACTIONS; ++i) {
			final DbMultisigAggregateModificationTransaction dbTransfer = dbModel.getBlockMultisigAggregateModificationTransactions().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
	}

	// this test currently fails due to fee calculation
	@Test
	public void blockModelWithMultisigTransactionCanBeMappedToDbModel() {
		// Arrange:
		final int NUM_TRANSACTIONS = 2;
		final TestContext context = new TestContext();
		context.addMultisigTransactions();

		// Act:
		final DbBlock dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel);
		// note: we expect both multisig txes and block transfers
		Assert.assertThat(dbModel.getBlockMultisigTransactions().size(), IsEqual.equalTo(NUM_TRANSACTIONS));
		Assert.assertThat(dbModel.getBlockMultisigAggregateModificationTransactions().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockImportanceTransferTransactions().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockTransferTransactions().size(), IsEqual.equalTo(0));

		for (int i = 0; i < NUM_TRANSACTIONS; ++i) {
			final DbMultisigTransaction dbTransfer = dbModel.getBlockMultisigTransactions().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
	}

	// this test currently fails due to fee calculation
	@Test
	public void blockModelWithMultisigTransactionWithSignaturesCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMultisigTransactionsWithSigners();

		// Act:
		final DbBlock dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel);
		// note: we expect both multisig txes and block transfers
		Assert.assertThat(dbModel.getBlockMultisigTransactions().size(), IsEqual.equalTo(1));
		Assert.assertThat(dbModel.getBlockMultisigAggregateModificationTransactions().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockImportanceTransferTransactions().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockTransferTransactions().size(), IsEqual.equalTo(0));

		final DbMultisigTransaction dbMultisig = dbModel.getBlockMultisigTransactions().get(0);
		final Transaction transaction = context.getModel().getTransactions().get(0);
		Assert.assertThat(dbMultisig.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));

		Assert.assertThat(dbMultisig.getMultisigSignatureTransactions().size(), IsEqual.equalTo(2));
	}

	@Test
	public void blockModelWithProvisionNamespaceTransactionsCanBeMappedToDbModel() {
		// Arrange:
		final int NUM_TRANSACTIONS = 2;
		final TestContext context = new TestContext();
		context.addProvisionNamespaceTransactions();

		// Act:
		final DbBlock dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel);
		Assert.assertThat(dbModel.getBlockProvisionNamespaceTransactions().size(), IsEqual.equalTo(NUM_TRANSACTIONS));
		Assert.assertThat(dbModel.getBlockTransferTransactions().size(), IsEqual.equalTo(0));
		for (int i = 0; i < NUM_TRANSACTIONS; ++i) {
			final DbProvisionNamespaceTransaction dbTransfer = dbModel.getBlockProvisionNamespaceTransactions().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
	}

	//endregion

	// roundtrip test of block with transaction
	@Test
	public void blockModelWithoutTransactionsCanBeRoundTripped() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbBlock dbModel = context.toDbModel();

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
		final DbBlock dbModel = context.toDbModel();

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
		final DbBlock dbModel = context.toDbModel();

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
		final DbBlock dbModel = context.toDbModel();

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
	public void blockModelWithMultisigModificationTransactionCanBeRoundTripped() {
		// Arrange:
		final int NUM_TRANSACTIONS = 2;
		final TestContext context = new TestContext();
		context.addMultisigModificationTransactions();
		final DbBlock dbModel = context.toDbModel();

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
		final DbBlock dbModel = context.toDbModel();

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
		context.signModel();
		final DbBlock dbModel = context.toDbModel();

		// Act:
		final Block model = context.toModel(dbModel);

		// Assert:
		context.assertModel(model);
		Assert.assertThat(model.getTransactions().size(), IsEqual.equalTo(1));
		final MultisigTransaction transaction = (MultisigTransaction)model.getTransactions().get(0);

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
		// order matters, as harvesting will create block in that order
		context.addMultisigModificationTransactions();
		context.addImportanceTransferTransactions();
		context.addTransactions();

		// Act:
		final DbBlock dbModel = context.toDbModel();

		// Assert:
		context.assertDbModel(dbModel);
		Assert.assertThat(dbModel.getBlockMultisigAggregateModificationTransactions().size(), IsEqual.equalTo(NUM_TRANSACTIONS_A));
		Assert.assertThat(dbModel.getBlockImportanceTransferTransactions().size(), IsEqual.equalTo(NUM_TRANSACTIONS_B));
		Assert.assertThat(dbModel.getBlockTransferTransactions().size(), IsEqual.equalTo(NUM_TRANSACTIONS_C));
		for (int i = 0; i < NUM_TRANSACTIONS_A; ++i) {
			final DbMultisigAggregateModificationTransaction dbTransfer = dbModel.getBlockMultisigAggregateModificationTransactions().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
		for (int i = 2; i < 2 + NUM_TRANSACTIONS_B; ++i) {
			final DbImportanceTransferTransaction dbTransfer = dbModel.getBlockImportanceTransferTransactions().get(i - 2);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
		for (int i = 4; i < 4 + NUM_TRANSACTIONS_C; ++i) {
			final DbTransferTransaction dbTransferTransaction = dbModel.getBlockTransferTransactions().get(i - 4);
			final Transaction transaction = context.getModel().getTransactions().get(i);
			Assert.assertThat(dbTransferTransaction.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
	}

	@Test
	public void blockModelWithSortedTransactionsCanBeRoundTripped() {
		// Arrange:
		final int NUM_TRANSACTIONS_A = 2;
		final int NUM_TRANSACTIONS_B = 2;
		final int NUM_TRANSACTIONS_C = 3;
		final TestContext context = new TestContext();

		// order matters, let's assume fees were such that block have been created in such order
		final ImportanceTransferMode mode = ImportanceTransferMode.Activate;
		final MultisigModificationType modificationType = MultisigModificationType.AddCosignatory;

		// TO
		context.model.addTransaction(new TransferTransaction(new TimeInstant(100), context.account1, context.account2, new Amount(7), null));
		final List<MultisigCosignatoryModification> modifications1 = Collections.singletonList(
				new MultisigCosignatoryModification(modificationType, context.account2));
		context.model.addTransaction(new MultisigAggregateModificationTransaction(new TimeInstant(200), context.account1, modifications1));
		context.model.addTransaction(new ImportanceTransferTransaction(new TimeInstant(300), context.account1, mode, context.account2));
		context.model.addTransaction(new TransferTransaction(new TimeInstant(400), context.account2, context.account3, new Amount(11), null));
		final List<MultisigCosignatoryModification> modifications2 = Collections.singletonList(
				new MultisigCosignatoryModification(modificationType, context.account3));
		context.model.addTransaction(new MultisigAggregateModificationTransaction(new TimeInstant(500), context.account1, modifications2));
		context.model.addTransaction(new ImportanceTransferTransaction(new TimeInstant(600), context.account3, mode, context.account2));
		context.model.addTransaction(new TransferTransaction(new TimeInstant(700), context.account3, context.account1, new Amount(4), null));
		for (final Transaction transaction : context.model.getTransactions()) {
			transaction.setFee(Amount.fromNem(1000L));
			transaction.sign();
		}

		context.signModel();
		final DbBlock dbModel = context.toDbModel();

		// Act:
		final Block model = context.toModel(dbModel);

		// Assert:
		context.assertDbModel(dbModel);
		Assert.assertThat(dbModel.getBlockMultisigAggregateModificationTransactions().size(), IsEqual.equalTo(NUM_TRANSACTIONS_A));
		Assert.assertThat(dbModel.getBlockImportanceTransferTransactions().size(), IsEqual.equalTo(NUM_TRANSACTIONS_B));
		Assert.assertThat(dbModel.getBlockTransferTransactions().size(), IsEqual.equalTo(NUM_TRANSACTIONS_C));
		for (int i = 0; i < NUM_TRANSACTIONS_A; ++i) {
			final DbMultisigAggregateModificationTransaction dbTransfer = dbModel.getBlockMultisigAggregateModificationTransactions().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(3 * i + 1);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
		for (int i = 0; i < NUM_TRANSACTIONS_B; ++i) {
			final DbImportanceTransferTransaction dbTransfer = dbModel.getBlockImportanceTransferTransactions().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(3 * i + 2);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
		for (int i = 0; i < NUM_TRANSACTIONS_C; ++i) {
			final DbTransferTransaction dbTransferTransaction = dbModel.getBlockTransferTransactions().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(3 * i);
			Assert.assertThat(dbTransferTransaction.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}

		// assert model
		Assert.assertThat(model.getSignature(), IsEqual.equalTo(context.model.getSignature()));
		for (int i = 0; i < NUM_TRANSACTIONS_A + NUM_TRANSACTIONS_B + NUM_TRANSACTIONS_C; ++i) {
			final Transaction expected = context.getModel().getTransactions().get(i);
			final Transaction actual = model.getTransactions().get(i);
			Assert.assertThat(HashUtils.calculateHash(expected), IsEqual.equalTo(HashUtils.calculateHash(actual)));
		}
	}

	// this test is currently expected to fail
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
		context.model.addTransaction(new MultisigTransaction(new TimeInstant(400), context.account1, transaction2));

		context.signModel();

		final DbBlock dbModel = context.toDbModel();

		// Act:
		final Block model = context.toModel(dbModel);

		// Assert:
		context.assertDbModel(dbModel);

		Assert.assertThat(dbModel.getBlockMultisigTransactions().size(), IsEqual.equalTo(2));
		Assert.assertThat(dbModel.getBlockMultisigAggregateModificationTransactions().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockImportanceTransferTransactions().size(), IsEqual.equalTo(0));
		Assert.assertThat(dbModel.getBlockTransferTransactions().size(), IsEqual.equalTo(2));
		for (int i = 0; i < 2; ++i) {
			final DbTransferTransaction dbTransferTransaction = dbModel.getBlockTransferTransactions().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(2 * i);
			Assert.assertThat(dbTransferTransaction.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
		}
		for (int i = 0; i < 2; ++i) {
			final DbMultisigTransaction dbTransfer = dbModel.getBlockMultisigTransactions().get(i);
			final Transaction transaction = context.getModel().getTransactions().get(2 * i + 1);
			Assert.assertThat(dbTransfer.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(transaction)));
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
		final DbBlock dbModel = context.toDbModel();
		dbModel.setDifficulty(null);

		// Act:
		final Block model = context.toModel(dbModel);

		// Assert:
		Assert.assertThat(model.getDifficulty(), IsEqual.equalTo(new BlockDifficulty(0)));
	}

	private class TestContext {
		private final Block model;
		private final DbAccount dbHarvester;
		private final Account account1;
		private final Account account2;
		private final Account account3;
		private final Account account4;
		private final List<Account> accounts = new ArrayList<>();
		private final MockAccountDao accountDao;
		private final Hash blockGenerationHash;
		private Hash hash;
		private Account lessor;
		private DbAccount dbLessor;

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

			this.dbHarvester = this.createDbAccount(this.model.getSigner());

			this.accountDao = new MockAccountDao();
			this.accountDao.addMapping(this.model.getSigner(), this.dbHarvester);

			for (int i = 0; i < 4; ++i) {
				final Account account = Utils.generateRandomAccount();
				this.accounts.add(account);
				this.accountDao.addMapping(account, this.createDbAccount(account));
			}

			this.account1 = this.accounts.get(0);
			this.account2 = this.accounts.get(1);
			this.account3 = this.accounts.get(2);
			this.account4 = this.accounts.get(3);
		}

		public void addLessor() {
			this.lessor = Utils.generateRandomAccount();
			this.dbLessor = this.createDbAccount(this.lessor);
			this.accountDao.addMapping(this.lessor, this.dbLessor);
			this.model.setLessor(this.lessor);
		}

		private DbAccount createDbAccount(final Account account) {
			return new DbAccount(account.getAddress());
		}

		public Block getModel() {
			return this.model;
		}

		public DbBlock toDbModel() {
			return MapperUtils.createModelToDbModelMapper(this.accountDao).map(this.model, DbBlock.class);
		}

		public Block toModel(final DbBlock dbBlock) {
			final MockAccountLookup mockAccountLookup = new MockAccountLookup();
			mockAccountLookup.setMockAccount(this.model.getSigner());
			this.accounts.forEach(mockAccountLookup::setMockAccount);
			return MapperUtils.createDbModelToModelNisMapper(mockAccountLookup).map(dbBlock);
		}

		public void addTransactions() {
			this.model.addTransaction(new TransferTransaction(
					new TimeInstant(100), this.account1, this.account2, new Amount(7), null));
			this.model.addTransaction(new TransferTransaction(
					new TimeInstant(200), this.account2, this.account3, new Amount(11), null));
			this.model.addTransaction(new TransferTransaction(
					new TimeInstant(300), this.account3, this.account1, new Amount(4), null));

			this.signModel();
		}

		public void addImportanceTransferTransactions() {
			this.model.addTransaction(new ImportanceTransferTransaction(
					new TimeInstant(150), this.account1, ImportanceTransferMode.Activate, this.account2));
			this.model.addTransaction(new ImportanceTransferTransaction(
					new TimeInstant(250), this.account3, ImportanceTransferMode.Activate, this.account2));

			this.signModel();
		}

		public void addMultisigModificationTransactions() {
			final List<MultisigCosignatoryModification> modifications1 = Collections.singletonList(
					new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, this.account2));
			this.model.addTransaction(new MultisigAggregateModificationTransaction(new TimeInstant(150), this.account1, modifications1));
			final List<MultisigCosignatoryModification> modifications2 = Collections.singletonList(
					new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, this.account3));
			this.model.addTransaction(new MultisigAggregateModificationTransaction(new TimeInstant(250), this.account1, modifications2));

			this.signModel();
		}

		public void addMultisigTransactions() {
			final Transaction transfer1 = new TransferTransaction(new TimeInstant(100), this.account1, this.account2, new Amount(7), null);
			final Transaction transfer2 = new TransferTransaction(new TimeInstant(200), this.account1, this.account2, new Amount(11), null);

			this.model.addTransaction(new MultisigTransaction(new TimeInstant(100), this.account3, transfer1));
			this.model.addTransaction(new MultisigTransaction(new TimeInstant(200), this.account3, transfer2));

			this.signModel();
		}

		public void addMultisigTransactionsWithSigners() {
			final Transaction transfer1 = new TransferTransaction(new TimeInstant(100), this.account1, this.account2, new Amount(7), null);
			final Hash transferHash = HashUtils.calculateHash(transfer1);

			final MultisigTransaction multisigTransaction = new MultisigTransaction(new TimeInstant(100), this.account3, transfer1);
			this.model.addTransaction(multisigTransaction);

			final MultisigSignatureTransaction multisigSignature1 = new MultisigSignatureTransaction(
					new TimeInstant(123),
					this.account4,
					this.account1,
					transferHash);
			multisigSignature1.sign();
			multisigTransaction.addSignature(multisigSignature1);

			final MultisigSignatureTransaction multisigSignature2 = new MultisigSignatureTransaction(
					new TimeInstant(132),
					this.account2,
					this.account1,
					transferHash);
			multisigSignature2.sign();
			multisigTransaction.addSignature(multisigSignature2);

			this.signModel();
		}

		public void addProvisionNamespaceTransactions() {
			this.model.addTransaction(new ProvisionNamespaceTransaction(
					new TimeInstant(350), this.account1, this.account2, Amount.fromNem(25000), new NamespaceIdPart("bar"), new NamespaceId("foo")));
			this.model.addTransaction(new ProvisionNamespaceTransaction(
					new TimeInstant(450), this.account3, this.account4, Amount.fromNem(35000), new NamespaceIdPart("baz"), new NamespaceId("qux")));

			this.signModel();
		}

		private void signModel() {
			this.model.getTransactions().forEach(org.nem.core.model.Transaction::sign);
			this.model.sign();
			this.hash = HashUtils.calculateHash(this.model);
		}

		public void assertDbModel(final DbBlock dbModel) {
			Assert.assertThat(dbModel.getId(), IsNull.nullValue());
			Assert.assertThat(dbModel.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
			Assert.assertThat(dbModel.getPrevBlockHash(), IsEqual.equalTo(this.model.getPreviousBlockHash()));
			Assert.assertThat(dbModel.getBlockHash(), IsEqual.equalTo(this.hash));
			Assert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(721));
			Assert.assertThat(dbModel.getHarvester(), IsEqual.equalTo(this.dbHarvester));
			Assert.assertThat(dbModel.getHarvesterProof(), IsEqual.equalTo(this.model.getSignature().getBytes()));
			Assert.assertThat(dbModel.getHeight(), IsEqual.equalTo(17L));
			Assert.assertThat(dbModel.getTotalFee(), IsEqual.equalTo(this.model.getTotalFee().getNumMicroNem()));
			Assert.assertThat(dbModel.getDifficulty(), IsEqual.equalTo(79_876_543_211_237L));
			Assert.assertThat(dbModel.getGenerationHash(), IsEqual.equalTo(this.blockGenerationHash));
			Assert.assertThat(dbModel.getLessor(), IsEqual.equalTo(this.dbLessor));

			final PublicKey signerPublicKey = this.model.getSigner().getAddress().getPublicKey();
			Assert.assertThat(dbModel.getHarvester().getPublicKey(), IsEqual.equalTo(signerPublicKey));
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
