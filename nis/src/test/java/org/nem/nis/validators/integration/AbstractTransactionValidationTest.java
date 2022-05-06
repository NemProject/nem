package org.nem.nis.validators.integration;

import net.minidev.json.JSONObject;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.NisCacheFactory;

import java.util.*;
import java.util.function.Function;

@SuppressWarnings("serial")
public abstract class AbstractTransactionValidationTest {
	protected static final TimeInstant CURRENT_TIME = new SystemTimeProvider().getCurrentTime();

	@Before
	public void setup() {
		final MosaicFeeInformation feeInfo = new MosaicFeeInformation(Supply.fromValue(100_000_000), 3);
		final Set<MosaicId> knownMosaicIds = new HashSet<MosaicId>() {
			{
				this.add(Utils.createMosaicId(1));
				this.add(Utils.createMosaicId(2));
				this.add(Utils.createMosaicId("foo", "mosaic"));
			}
		};
		NemGlobals.setTransactionFeeCalculator(new TransactionFeeCalculatorBeforeFork(id -> knownMosaicIds.contains(id) ? feeInfo : null));
	}

	@After
	public void destroy() {
		Utils.resetGlobals();
	}

	// region transfers

	@Test
	public void getBlockTransactionsAllowsEarlierBlockTransfersToBeSpentLater() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(20));
		final Account recipient = context.addAccount(Amount.fromNem(10));
		final TimeInstant currentTime = CURRENT_TIME;

		// - T(O) - S: 20 | R: 10
		// - T(1) - S -15-> R | S: 02 | R: 25
		// - T(2) - R -12-> S | S: 14 | R: 11 # this transfer is allowed even though it wouldn't be allowed in reverse order
		final Transaction t1 = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(15));
		t1.setFee(Amount.fromNem(3));
		t1.sign();
		final Transaction t2 = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(12));
		t2.setFee(Amount.fromNem(2));
		t2.sign();

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2), Arrays.asList(t1, t2), ValidationResult.SUCCESS);
	}

	@Test
	public void getBlockTransactionsExcludesTransactionFromNextBlockIfConfirmedBalanceIsInsufficient() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(20));
		final Account recipient = context.addAccount(Amount.fromNem(10));
		final TimeInstant currentTime = CURRENT_TIME;

		// - T(O) - S: 20 | R: 10
		// - T(1) - R -12-> S | XXX
		// - T(2) - S -15-> R | S: 03 | R: 25
		final Transaction t1 = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(12));
		t1.setFee(Amount.fromNem(3));
		t1.sign();
		final Transaction t2 = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(15));
		t2.setFee(Amount.fromNem(2));
		t2.sign();

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2), Collections.singletonList(t2),
				ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	// endregion

	// region importance transfer

	@Test
	public void getBlockTransactionsDoesNotAllowConflictingImportanceTransfersToBeInSingleBlock() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(50000));
		final Account remote = context.addAccount(Amount.ZERO);
		final Account remote2 = context.addAccount(Amount.ZERO);

		final Transaction t1 = createActivateImportanceTransfer(sender, remote);
		final Transaction t2 = createActivateImportanceTransfer(sender, remote2);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2), Collections.singletonList(t1),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	// endregion

	// region multisig modification

	@Test
	public void getBlockTransactionsDoesNotAllowMultipleMultisigModificationsForSameAccountToBeInSingleBlock() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account cosigner3 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig, cosigner1);

		final Transaction t1 = createMultisigModification(multisig, cosigner1, cosigner2);
		final MultisigTransaction t2 = createMultisigModification(multisig, cosigner1, cosigner3);
		t2.addSignature(createSignature(cosigner2, multisig, t2.getOtherTransaction()));

		// Act / Assert:
		final ValidationResult expectedResult = this.isSingleBlockUsed()
				? ValidationResult.FAILURE_CONFLICTING_MULTISIG_MODIFICATION
				: ValidationResult.SUCCESS;
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2),
				this.isSingleBlockUsed() ? Collections.singletonList(t1) : Arrays.asList(t1, t2), expectedResult);
	}

	// endregion

	// region multisig - default min signatures (all)

	@Test
	public void getBlockTransactionsDoesNotReturnMultisigTransactionIfMultisigSignaturesAreNotPresent() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account recipient = context.addAccount(Amount.ZERO);

		context.setCosigner(multisig, cosigner1);
		context.setCosigner(multisig, cosigner2);

		final Transaction t1 = createTransferTransaction(CURRENT_TIME, multisig, recipient, Amount.fromNem(7));
		t1.setSignature(null);
		final MultisigTransaction mt1 = createMultisig(cosigner1, t1);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(mt1),
				this.allowsIncomplete() ? Collections.singletonList(mt1) : Collections.emptyList(),
				ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS);
	}

	@Test
	public void getBlockTransactionsReturnsMultisigTransactionIfMultisigSignaturesArePresent() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account recipient = context.addAccount(Amount.ZERO);

		context.setCosigner(multisig, cosigner1);
		context.setCosigner(multisig, cosigner2);

		final Transaction t1 = createTransferTransaction(CURRENT_TIME, multisig, recipient, Amount.fromNem(7));
		t1.setSignature(null);
		final MultisigTransaction mt1 = createMultisig(cosigner1, t1);
		mt1.addSignature(createSignature(cosigner2, multisig, t1));

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(mt1), Collections.singletonList(mt1), ValidationResult.SUCCESS);
	}

	// endregion

	// region multisig - custom min signatures

	@Test
	public void getBlockTransactionsDoesNotReturnMultisigTransactionIfLessThanMinCosignatoriesMultisigSignaturesArePresent() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account cosigner3 = context.addAccount(Amount.ZERO);
		final Account recipient = context.addAccount(Amount.ZERO);

		context.setCosigner(multisig, cosigner1);
		context.setCosigner(multisig, cosigner2);
		context.setCosigner(multisig, cosigner3);
		context.setMinCosignatories(multisig, 2);

		final Transaction t1 = createTransferTransaction(CURRENT_TIME, multisig, recipient, Amount.fromNem(7));
		t1.setSignature(null);
		final MultisigTransaction mt1 = createMultisig(cosigner1, t1);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(mt1),
				this.allowsIncomplete() ? Collections.singletonList(mt1) : Collections.emptyList(),
				ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS);
	}

	@Test
	public void getBlockTransactionsReturnsMultisigTransactionIfMinCosignatoriesMultisigSignaturesArePresent() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account cosigner3 = context.addAccount(Amount.ZERO);
		final Account recipient = context.addAccount(Amount.ZERO);

		context.setCosigner(multisig, cosigner1);
		context.setCosigner(multisig, cosigner2);
		context.setCosigner(multisig, cosigner3);
		context.setMinCosignatories(multisig, 2);

		final Transaction t1 = createTransferTransaction(CURRENT_TIME, multisig, recipient, Amount.fromNem(7));
		t1.setSignature(null);
		final MultisigTransaction mt1 = createMultisig(cosigner1, t1);
		mt1.addSignature(createSignature(cosigner2, multisig, t1));

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(mt1), Collections.singletonList(mt1), ValidationResult.SUCCESS);
	}

	@Test
	public void getBlockTransactionsDoesReturnMultisigTransactionWithInnerDelModificationIfMinCosignatoriesMultisigSignaturesArePresent() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account cosigner3 = context.addAccount(Amount.ZERO);
		final Account cosigner4 = context.addAccount(Amount.ZERO);

		// - set four possible cosigners and two min cosigners
		context.setCosigner(multisig, cosigner1);
		context.setCosigner(multisig, cosigner2);
		context.setCosigner(multisig, cosigner3);
		context.setCosigner(multisig, cosigner4);
		context.setMinCosignatories(multisig, 2);

		// create a transaction with two signatures (cosigner1 and cosigner2)
		final MultisigTransaction mt1 = createMultisigModification(multisig, cosigner1,
				Collections.singletonList(new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, cosigner4)));
		mt1.getOtherTransaction().setSignature(null);
		mt1.addSignature(createSignature(cosigner2, multisig, mt1.getOtherTransaction()));

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(mt1), Collections.singletonList(mt1), ValidationResult.SUCCESS);
	}

	// endregion

	// region general

	@Test
	public void allTransactionsInChainMustNotBePastDeadline() {
		// Assert:
		this.assertSingleBadTransactionIsFilteredOut(context -> {
			final TimeInstant currentTime = CURRENT_TIME.addHours(2);
			final Transaction t = context.prepareMockTransaction(new MockTransaction(0, currentTime));
			context.prepareAccount(t.getSigner(), Amount.fromNem(100));
			t.setDeadline(currentTime.addHours(-1));
			return t;
		}, ValidationResult.FAILURE_PAST_DEADLINE);
	}

	@Test
	public void allTransactionsInChainMustHaveValidTimestamp() {
		// Assert:
		this.assertSingleBadTransactionIsFilteredOut(context -> {
			final TimeInstant futureTime = CURRENT_TIME.addHours(2);
			final Transaction t = context.prepareMockTransaction(new MockTransaction(0, futureTime));
			context.prepareAccount(t.getSigner(), Amount.fromNem(100));
			t.setDeadline(futureTime.addSeconds(10));
			return t;
		}, ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE);
	}

	@Test
	public void chainWithTransactionWithInsufficientFeeIsInvalid() {
		// Assert:
		this.assertSingleBadTransactionIsFilteredOut(context -> {
			final Transaction t = context.createValidSignedTransaction();
			t.setFee(Amount.fromNem(1));
			return t;
		}, ValidationResult.FAILURE_INSUFFICIENT_FEE);
	}

	private void assertSingleBadTransactionIsFilteredOut(final Function<TestContext, Transaction> getBadTransaction,
			final ValidationResult expectedResult) {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction t1 = context.createValidSignedTransaction();
		final Transaction t2 = getBadTransaction.apply(context);
		t2.sign();
		final Transaction t3 = context.createValidSignedTransaction();

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2, t3), Arrays.asList(t1, t3), expectedResult);
	}

	@Test
	public void chainIsInvalidIfTransactionHashAlreadyExistInHashCache() {
		// Assert:
		final TestContext context = new TestContext();
		final Transaction t1 = context.createValidSignedTransaction();
		final Transaction t2 = context.createValidSignedTransaction();
		final Transaction t3 = context.createValidSignedTransaction();

		final NisCache copyCache = context.nisCache.copy();
		copyCache.getTransactionHashCache()
				.put(new HashMetaDataPair(HashUtils.calculateHash(t2), new HashMetaData(new BlockHeight(10), CURRENT_TIME)));
		copyCache.commit();

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2, t3), Arrays.asList(t1, t3), ValidationResult.NEUTRAL);
	}

	@Test
	public void chainIsInvalidIfDuplicateTransactionExistsInChain() {
		// Assert:
		final TestContext context = new TestContext();
		final Transaction t1 = context.createValidSignedTransaction();
		final Transaction t2 = context.createValidSignedTransaction();

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2, t1), Arrays.asList(t1, t2), this.getHashConflictResult());
	}

	@Test
	public void chainWithAllValidTransactionsIsValid() {
		// Assert:
		final TestContext context = new TestContext();
		final Transaction t1 = context.createValidSignedTransaction();
		final Transaction t2 = context.createValidSignedTransaction();
		final Transaction t3 = context.createValidSignedTransaction();

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2, t3), Arrays.asList(t1, t2, t3), ValidationResult.SUCCESS);
	}

	// endregion

	// region importance transfer

	@Test
	public void chainWithImportanceTransferToNonZeroBalanceAccountIsInvalid() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100000));
		final Account account2 = context.addAccount(Amount.fromNem(100000));

		final Transaction t1 = createActivateImportanceTransfer(account1, account2);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(t1), Collections.emptyList(),
				ValidationResult.FAILURE_DESTINATION_ACCOUNT_IN_USE);
	}

	@Test
	public void chainWithTransferToRemoteFollowedByImportanceTransferIsInvalid() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100000));
		final Account account2 = context.addAccount(Amount.ZERO);

		final Transaction t1 = createTransferTransaction(account1, account2, new Amount(7));
		final Transaction t2 = createActivateImportanceTransfer(account1, account2);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2), Collections.singletonList(t1),
				ValidationResult.FAILURE_DESTINATION_ACCOUNT_IN_USE);
	}

	@Test
	public void chainWithConflictingImportanceTransfersIsInvalid() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(20000));
		final Account account2 = context.addAccount(Amount.fromNem(20000));
		final Account accountX = Utils.generateRandomAccount();

		final Transaction t1 = createActivateImportanceTransfer(account1, accountX);
		final Transaction t2 = createActivateImportanceTransfer(account2, accountX);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2), Collections.singletonList(t1),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void cannotSendTransferToRemoteAccount() {
		// Assert:
		this.assertTransferIsInvalidForRemoteAccount(TransferTransaction::getRecipient);
	}

	@Test
	public void cannotSendTransferFromRemoteAccount() {
		// Assert:
		this.assertTransferIsInvalidForRemoteAccount(VerifiableEntity::getSigner);
	}

	private void assertTransferIsInvalidForRemoteAccount(final Function<TransferTransaction, Account> getRemoteAccount) {
		// Assert:
		this.assertSingleBadTransactionIsFilteredOut(context -> {
			// Arrange:
			final Account signer = context.addAccount(Amount.fromNem(100));
			final TransferTransaction t1 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(10));

			// - make the transaction signer a remote account
			context.makeRemote(getRemoteAccount.apply(t1));
			return t1;
		}, ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_REMOTE);
	}

	// endregion

	// region transfer

	@Test
	public void chainIsValidIfAccountSpendsAmountReceivedEarlier() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(1000));
		final Account account2 = context.addAccount(Amount.fromNem(1000));

		final Transaction t1 = createTransferTransaction(account1, account2, Amount.fromNem(500));
		final Transaction t2 = createTransferTransaction(account2, account1, Amount.fromNem(1250));
		final Transaction t3 = createTransferTransaction(account1, account2, Amount.fromNem(1700));

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2, t3), Arrays.asList(t1, t2, t3), ValidationResult.SUCCESS);
	}

	@Test
	public void chainIsInvalidIfItContainsTransferTransactionHavingSignerWithInsufficientBalance() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account signer = context.addAccount(Amount.fromNem(17));
		final Transaction t1 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(20));

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(t1), Collections.emptyList(),
				ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	@Test
	public void chainIsValidIfItContainsTransferTransactionHavingSignerWithExactBalance() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account signer = context.addAccount(Amount.fromNem(22));
		final Transaction t1 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(20));

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(t1), Collections.singletonList(t1), ValidationResult.SUCCESS);
	}

	@Test
	public void chainIsInvalidIfItContainsMultipleTransferTransactionsFromSameSignerHavingSignerWithInsufficientBalanceForAll() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account signer = context.addAccount(Amount.fromNem(36));
		final Transaction t1 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(9));
		final Transaction t2 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(8));
		final Transaction t3 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(14));

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2, t3), Arrays.asList(t1, t2),
				ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	// endregion

	// region without additional mosaic transfer fee

	@Test
	public void chainIsInvalidIfItContainsTransferTransactionHavingSignerWithInsufficientMosaics() {
		// Assert:
		// no additional mosaic transfer fee is deducted from the signers account
		this.assertInsufficientMosaicBalanceForSingleTransaction(null, 123, 124000);
	}

	@Test
	public void chainIsValidIfItContainsTransferTransactionHavingSignerWithExactlyEnoughMosaics() {
		// Assert:
		// no additional mosaic transfer fee is deducted from the signers account
		this.assertSufficientMosaicBalanceForSingleTransaction(null, 123, 123000);
	}

	@Test
	public void chainIsInvalidIfItContainsMultipleTransferTransactionsFromSameSignerHavingSignerWithInsufficientMosaicsForAll() {
		// Arrange:
		// no additional mosaic transfer fee is deducted from the signers account
		this.assertInsufficientMosaicBalanceForMultipleTransactions(null, 123, 100000, 20000, 4000);
	}

	@Test
	public void chainIsValidIfItContainsMultipleTransferTransactionsFromSameSignerHavingSignerWithExactlyEnoughMosaicsForAll() {
		// Arrange:
		// no additional mosaic transfer fee is deducted from the signers account
		this.assertSufficientMosaicBalanceForMultipleTransactions(null, 123, 100000, 20000, 3000);
	}

	// endregion

	// region with additional mosaic transfer fee

	@Test
	public void chainIsInvalidIfItContainsTransferTransactionHavingSignerWithInsufficientMosaicsForMosaicTransferFee() {
		// Assert:
		// an additional mosaic transfer fee with quantity 123 is deducted from the signers account
		this.assertInsufficientMosaicBalanceForSingleTransaction(createMosaicLevy(), 123, 123000);
	}

	@Test
	public void chainIsValidIfItContainsTransferTransactionHavingSignerWithExactlyEnoughMosaicsIncludingMosaicTransferFee() {
		// Assert:
		// an additional mosaic transfer fee with quantity 123 is deducted from the signers account
		this.assertSufficientMosaicBalanceForSingleTransaction(createMosaicLevy(), 123, 123000 - 123);
	}

	@Test
	public void chainIsInvalidIfItContainsMultipleTransferTransactionsFromSameSignerHavingSignerWithInsufficientMosaicsForMosaicTransferFee() {
		// an additional mosaic transfer fee with quantity 123 is deducted from the signers account
		this.assertInsufficientMosaicBalanceForMultipleTransactions(createMosaicLevy(), 123, 100000 - 123, 20000 - 123, 3000);
	}

	@Test
	public void chainIsValidIfItContainsMultipleTransferTransactionsFromSameSignerHavingSignerWithExactlyEnoughMosaicsForAllIncludingMosaicTransferFee() {
		// Arrange:
		// an additional mosaic transfer fee with quantity 123 is deducted from the signers account
		this.assertSufficientMosaicBalanceForMultipleTransactions(createMosaicLevy(), 123, 100000 - 123, 20000 - 123, 3000 - 123);
	}

	// endregion

	private static MosaicLevy createMosaicLevy() {
		return new MosaicLevy(MosaicTransferFeeType.Absolute, Utils.generateRandomAccount(), Utils.createMosaicId(1),
				Quantity.fromValue(123));
	}

	private void assertSufficientMosaicBalanceForSingleTransaction(final MosaicLevy levy, final long supply, final long quantity) {
		// Assert:
		this.assertMosaicBalanceValidationForSingleTransaction(levy, supply, quantity, ValidationResult.SUCCESS);
	}

	private void assertInsufficientMosaicBalanceForSingleTransaction(final MosaicLevy levy, final long supply, final long quantity) {
		// Assert:
		this.assertMosaicBalanceValidationForSingleTransaction(levy, supply, quantity, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	private void assertMosaicBalanceValidationForSingleTransaction(final MosaicLevy levy, final long supply, final long quantity,
			final ValidationResult expectedResult) {
		// Arrange:
		// - due to the way the globals are setup, mosaic 1 has a transfer fee but mosaic 2 does not
		final int id = null == levy ? 2 : 1;
		final TestContext context = new TestContext();
		final Account signer = context.addAccount(Amount.fromNem(500));
		context.prepareMosaic(signer, Utils.createMosaicId(id), Supply.fromValue(supply), levy);
		final Transaction t1 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(1),
				Utils.createMosaic(id, quantity));
		prepareWithMinimumFee(t1, context.nisCache);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(t1),
				expectedResult.isSuccess() ? Collections.singletonList(t1) : Collections.emptyList(), expectedResult);
	}

	private void assertSufficientMosaicBalanceForMultipleTransactions(final MosaicLevy levy, final long supply, final long quantity1,
			final long quantity2, final long quantity3) {
		this.assertMosaicBalanceValidationForMultipleTransactions(levy, supply, quantity1, quantity2, quantity3, ValidationResult.SUCCESS);
	}

	private void assertInsufficientMosaicBalanceForMultipleTransactions(final MosaicLevy levy, final long supply, final long quantity1,
			final long quantity2, final long quantity3) {
		this.assertMosaicBalanceValidationForMultipleTransactions(levy, supply, quantity1, quantity2, quantity3,
				ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	private void assertMosaicBalanceValidationForMultipleTransactions(final MosaicLevy levy, final long supply, final long quantity1,
			final long quantity2, final long quantity3, final ValidationResult expectedResult) {
		// Arrange:
		// - due to the way the globals are setup, mosaic 1 has a transfer fee but mosaic 2 does not
		final int id = null == levy ? 2 : 1;
		final TestContext context = new TestContext();
		final Account signer = context.addAccount(Amount.fromNem(800));
		context.prepareMosaic(signer, Utils.createMosaicId(id), Supply.fromValue(supply), levy);
		final Transaction t1 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(1),
				Utils.createMosaic(id, quantity1));
		prepareWithMinimumFee(t1, context.nisCache);

		final Transaction t2 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(1),
				Utils.createMosaic(id, quantity2));
		prepareWithMinimumFee(t2, context.nisCache);

		final Transaction t3 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(1),
				Utils.createMosaic(id, quantity3));
		prepareWithMinimumFee(t3, context.nisCache);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2, t3),
				expectedResult.isSuccess() ? Arrays.asList(t1, t2, t3) : Arrays.asList(t1, t2), expectedResult);
	}

	private static void prepareWithMinimumFee(final Transaction transaction, final ReadOnlyNisCache nisCache) {
		final NamespaceCacheLookupAdapters adapters = new NamespaceCacheLookupAdapters(nisCache.getNamespaceCache());
		final TransactionFeeCalculator calculator = new TransactionFeeCalculatorBeforeFork(adapters.asMosaicFeeInformationLookup());
		transaction.setFee(calculator.calculateMinimumFee(transaction));
		transaction.sign();
	}

	// region bag validation

	@Test
	public void cannotValidateFractionalMosaicTransfers() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account signer = context.addAccount(Amount.fromNem(500));
		context.prepareMosaic(signer, Utils.createMosaicId(1), Supply.fromValue(1000), null);
		final Transaction t1 = createTransferTransaction(signer, Utils.generateRandomAccount(),
				Amount.fromNem(2).add(Amount.fromMicroNem(1)), Utils.createMosaic(1, 1000));
		prepareWithMinimumFee(t1, context.nisCache);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(t1), Collections.emptyList(),
				ValidationResult.FAILURE_MOSAIC_DIVISIBILITY_VIOLATED);
	}

	@Test
	public void cannotValidateTransfersOfNonTransferableMosaics() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account creator = context.addAccount(Amount.fromNem(500));
		final Account intermediary = context.addAccount(Amount.fromNem(500));
		context.prepareMosaic(creator, Utils.createMosaicId(1), Supply.fromValue(100), Utils.createMosaicProperties(0L, 3, true, false),
				null);

		final Transaction t1 = createTransferTransaction(creator, intermediary, Amount.fromNem(2), Utils.createMosaic(1, 1000));
		prepareWithMinimumFee(t1, context.nisCache);

		final Transaction t2 = createTransferTransaction(intermediary, Utils.generateRandomAccount(), Amount.fromNem(2),
				Utils.createMosaic(1, 500));
		prepareWithMinimumFee(t2, context.nisCache);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2), Collections.singletonList(t1),
				ValidationResult.FAILURE_MOSAIC_NOT_TRANSFERABLE);
	}

	// endregion

	// region multisig modification tests

	@Test
	public void blockCanContainMultipleMultisigModificationsForDifferentAccounts() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig1 = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig1, cosigner1);

		final Account multisig2 = context.addAccount(Amount.fromNem(2000));
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig2, cosigner2);

		final Transaction t1 = createMultisigModification(multisig1, cosigner1);
		final Transaction t2 = createMultisigModification(multisig2, cosigner2);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2), Arrays.asList(t1, t2), ValidationResult.SUCCESS);
	}

	@Test
	public void blockCannotContainMultipleMultisigModificationsForSameAccountWhenInitiatedBySameCosigner() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig1 = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account cosigner3 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig1, cosigner1);
		context.setCosigner(multisig1, cosigner2);

		final MultisigTransaction t1 = createMultisigModification(multisig1, cosigner1, cosigner3);
		t1.addSignature(createSignature(cosigner2, multisig1, t1.getOtherTransaction()));

		final MultisigTransaction t2 = createMultisigModification(multisig1, cosigner1);
		t2.addSignature(createSignature(cosigner2, multisig1, t2.getOtherTransaction()));
		t2.addSignature(createSignature(cosigner3, multisig1, t2.getOtherTransaction()));

		// Act / Assert:
		final ValidationResult expectedResult = this.isSingleBlockUsed()
				? ValidationResult.FAILURE_CONFLICTING_MULTISIG_MODIFICATION
				: ValidationResult.SUCCESS;
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2),
				this.isSingleBlockUsed() ? Collections.singletonList(t1) : Arrays.asList(t1, t2), expectedResult);
	}

	@Test
	public void blockCannotContainMultipleMultisigModificationsForSameAccountWhenInitiatedByDifferentCosigners() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig1 = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account cosigner3 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig1, cosigner1);
		context.setCosigner(multisig1, cosigner2);

		final MultisigTransaction t1 = createMultisigModification(multisig1, cosigner1, cosigner3);
		t1.addSignature(createSignature(cosigner2, multisig1, t1.getOtherTransaction()));

		final MultisigTransaction t2 = createMultisigModification(multisig1, cosigner2);
		t2.addSignature(createSignature(cosigner1, multisig1, t2.getOtherTransaction()));
		t2.addSignature(createSignature(cosigner3, multisig1, t2.getOtherTransaction()));

		// Act / Assert:
		final ValidationResult expectedResult = this.isSingleBlockUsed()
				? ValidationResult.FAILURE_CONFLICTING_MULTISIG_MODIFICATION
				: ValidationResult.SUCCESS;
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2),
				this.isSingleBlockUsed() ? Collections.singletonList(t1) : Arrays.asList(t1, t2), expectedResult);
	}

	@Test
	public void blockCannotContainModificationWithMultipleDeletes() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account cosigner3 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig, cosigner1);
		context.setCosigner(multisig, cosigner2);
		context.setCosigner(multisig, cosigner3);

		final List<MultisigCosignatoryModification> modifications = Arrays.asList(
				new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, cosigner2),
				new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, cosigner3));

		final Transaction t1 = createMultisigModification(multisig, cosigner1, modifications);

		// Act / Assert:
		// - unconfirmed transactions does not validate multiple delete modifications
		this.assertTransactions(context.nisCache, Collections.singletonList(t1), Collections.emptyList(),
				ValidationResult.FAILURE_MULTISIG_MODIFICATION_MULTIPLE_DELETES);
	}

	@Test
	public void blockCanContainModificationWithSingleDelete() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig, cosigner1);
		context.setCosigner(multisig, cosigner2);

		final List<MultisigCosignatoryModification> modifications = Collections
				.singletonList(new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, cosigner2));

		final Transaction t1 = createMultisigModification(multisig, cosigner1, modifications);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(t1), Collections.singletonList(t1), ValidationResult.SUCCESS);
	}

	@Test
	public void blockCanContainModificationWithSingleDeleteAndMultipleAdds() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig, cosigner1);
		context.setCosigner(multisig, cosigner2);

		final List<MultisigCosignatoryModification> modifications = Arrays.asList(
				new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, cosigner2),
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, Utils.generateRandomAccount()),
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, Utils.generateRandomAccount()));

		final Transaction t1 = createMultisigModification(multisig, cosigner1, modifications);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(t1), Collections.singletonList(t1), ValidationResult.SUCCESS);
	}

	// endregion

	// region multisig tests

	@Test
	public void canValidateMultisigTransferFromCosignerAccountWithZeroBalance() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(1000));
		final Account cosigner = context.addAccount(Amount.ZERO);
		final Account recipient = context.addAccount(Amount.ZERO);
		final Transaction t1 = createMultisigWithSignatures(context, multisig, cosigner, recipient);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(t1), Collections.singletonList(t1), ValidationResult.SUCCESS);
	}

	@Test
	public void canValidateMultisigTransferWithMultipleSignaturesFromCosignerAccountWithZeroBalance() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(1000));
		final Account cosigner = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account cosigner3 = context.addAccount(Amount.ZERO);
		final Account recipient = context.addAccount(Amount.ZERO);
		final Transaction t1 = createMultisigWithSignatures(context, multisig, cosigner, Arrays.asList(cosigner2, cosigner3), recipient);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(t1), Collections.singletonList(t1), ValidationResult.SUCCESS);
	}

	protected static MultisigTransaction createMultisigWithSignatures(final TestContext context, final Account multisig,
			final Account cosigner, final Account recipient) {
		return createMultisigWithSignatures(context, multisig, cosigner, Collections.emptyList(), recipient);
	}

	protected static MultisigTransaction createMultisigWithSignatures(final TestContext context, final Account multisig,
			final Account cosigner, final List<Account> otherCosigners, final Account recipient) {
		// Arrange:
		context.setCosigner(multisig, cosigner);
		for (final Account otherCosigner : otherCosigners) {
			context.setCosigner(multisig, otherCosigner);
		}

		final TimeInstant currentTime = CURRENT_TIME;
		final Transaction transfer = createTransferTransaction(multisig, recipient, Amount.fromNem(100));
		transfer.setFee(Amount.fromNem(10));
		transfer.setSignature(null);

		final MultisigTransaction msTransaction = new MultisigTransaction(currentTime, cosigner, transfer);
		msTransaction.setFee(Amount.fromNem(200));

		for (final Account otherCosigner : otherCosigners) {
			final MultisigSignatureTransaction signatureTransaction = new MultisigSignatureTransaction(currentTime, otherCosigner, multisig,
					transfer);
			signatureTransaction.setFee(Amount.fromNem(6));
			msTransaction.addSignature(prepareTransaction(signatureTransaction));
		}

		return prepareTransaction(msTransaction);
	}

	@Test
	public void afterConvertingAccountToMultisigOtherTransactionsCannotBeMadeFromThatAccount() {
		// Arrange:
		final TestContext context = new TestContext();

		final TimeInstant currentTime = CURRENT_TIME;
		final Account multisig = context.addAccount(Amount.fromNem(10000));
		final Account cosigner = context.addAccount(Amount.fromNem(10000));

		// - make the account multisig
		Transaction t1 = new MultisigAggregateModificationTransaction(currentTime, multisig,
				Collections.singletonList(new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, cosigner)));
		t1 = prepareTransaction(t1);

		// - create a transfer transaction from the multisig account
		final Transaction t2 = createTransferTransaction(multisig, Utils.generateRandomAccount(), Amount.fromNem(100));

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2), Collections.singletonList(t1),
				ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG);
	}

	@Test
	public void chainIsValidIfItContainsMultisigTransferTransactionHavingSignerWithExactBalance() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction mt1 = createMultisigTransactionForFeeTests(context, 0);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(mt1), Collections.singletonList(mt1), ValidationResult.SUCCESS);
	}

	@Test
	public void chainIsInvalidIfItContainsMultisigTransferTransactionHavingSignerWithLessThanRequiredBalance() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction mt1 = createMultisigTransactionForFeeTests(context, -1);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(mt1), Collections.emptyList(),
				ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	private static MultisigTransaction createMultisigTransactionForFeeTests(final TestContext context, final int balanceDelta) {
		final Account multisig = context.addAccount(Amount.fromNem(100 + 10 + 11 + 6 + balanceDelta));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig, cosigner1);
		context.setCosigner(multisig, cosigner2);

		// - a transfer transaction from the multisig account
		final Transaction t1 = createTransferTransaction(multisig, Utils.generateRandomAccount(), Amount.fromNem(100));
		t1.setSignature(null);
		t1.setFee(Amount.fromNem(10));

		// - create a multisig transaction initiated by cosigner 1 with a signature from cosigner 2
		final MultisigTransaction mt1 = new MultisigTransaction(CURRENT_TIME, cosigner1, t1);
		mt1.setFee(Amount.fromNem(11));
		mt1.addSignature(createSignature(cosigner2, multisig, t1));

		prepareTransaction(mt1);
		return mt1;
	}

	// endregion

	// region provision namespace transaction

	@Test
	public void canValidateValidProvisionNamespaceTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100_000));
		final ProvisionNamespaceTransaction transaction = createProvisionNamespaceTransaction(sender, null, "foo", 108);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(transaction), Collections.singletonList(transaction),
				ValidationResult.SUCCESS);
	}

	@Test
	public void canValidateValidChainOfProvisionNamespaceTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100_000));
		final ProvisionNamespaceTransaction t1 = createProvisionNamespaceTransaction(sender, null, "foo", 108);
		final ProvisionNamespaceTransaction t2 = createProvisionNamespaceTransaction(sender, "foo", "bar", 108);
		final ProvisionNamespaceTransaction t3 = createProvisionNamespaceTransaction(sender, "foo.bar", "baz", 108);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t1, t2, t3), Arrays.asList(t1, t2, t3), ValidationResult.SUCCESS);
	}

	@Test
	public void cannotValidateInvalidChainOfProvisionNamespaceTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100_000));
		final ProvisionNamespaceTransaction t1 = createProvisionNamespaceTransaction(sender, null, "foo", 108);
		final ProvisionNamespaceTransaction t2 = createProvisionNamespaceTransaction(sender, "foo", "bar", 108);
		final ProvisionNamespaceTransaction t3 = createProvisionNamespaceTransaction(sender, "foo.bar", "baz", 108);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(t3, t2, t1), Collections.singletonList(t1),
				ValidationResult.FAILURE_NAMESPACE_UNKNOWN);
	}

	private static ProvisionNamespaceTransaction createProvisionNamespaceTransaction(final Account sender, final String parent,
			final String newPart, final long fee) {
		final ProvisionNamespaceTransaction transaction = new ProvisionNamespaceTransaction(CURRENT_TIME, sender,
				new NamespaceIdPart(newPart), null == parent ? null : new NamespaceId(parent));
		transaction.setFee(Amount.fromNem(fee));
		return prepareTransaction(transaction);
	}

	// endregion

	// region MosaicDefinitionCreationTransaction

	@Test
	public void canValidateValidMosaicDefinitionCreationTransactionThatCreatesNewDefinition() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100_000));
		context.prepareNamespace(sender, new NamespaceId("foo"));
		final MosaicDefinitionCreationTransaction transaction = createMosaicDefinitionCreationTransaction(sender, "foo");

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(transaction), Collections.singletonList(transaction),
				ValidationResult.SUCCESS);
	}

	@Test
	public void canValidateValidMosaicDefinitionCreationTransactionThatModifiesExistingDefinition() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100_000));
		final MosaicDefinitionCreationTransaction transaction = createMosaicDefinitionCreationTransaction(sender, "foo");
		context.prepareMosaic(sender, transaction.getMosaicDefinition().getId(), Supply.fromValue(1234),
				Utils.createMosaicLevy(MosaicConstants.MOSAIC_ID_XEM));

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(transaction), Collections.singletonList(transaction),
				ValidationResult.SUCCESS);
	}

	@Test
	public void cannotValidateMosaicDefinitionCreationTransactionWithConflictingOwner() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100_000));
		context.prepareNamespace(Utils.generateRandomAccount(), new NamespaceId("foo"));
		final MosaicDefinitionCreationTransaction transaction = createMosaicDefinitionCreationTransaction(sender, "foo");

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(transaction), Collections.emptyList(),
				ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT);
	}

	// TODO 20151018 J-G: is this a bug?
	@Test
	public void multipleMosaicDefinitionCreationTransactionsForSameMosaicDefinitionAreAllowed() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(200_000));
		context.prepareNamespace(sender, new NamespaceId("foo"));
		final Transaction transaction1 = createMosaicDefinitionCreationTransaction(sender, "foo", "d1");
		final Transaction transaction2 = createMosaicDefinitionCreationTransaction(sender, "foo", "d2");

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(transaction1, transaction2), Arrays.asList(transaction1, transaction2),
				ValidationResult.SUCCESS);
	}

	@Test
	public void mosaicDefinitionCreationTransactionAndSupplyChangeForSameMosaicDefinitionAreNotAllowed() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(200_000));
		context.prepareNamespace(sender, new NamespaceId("foo"));
		final Transaction transaction1 = createMosaicDefinitionCreationTransaction(sender, "foo", "d1");
		final Transaction transaction2 = createMosaicSupplyChangeTransaction(sender, Utils.createMosaicId("foo", "mosaic"));

		// Act / Assert:
		this.assertConflictingMosaicCreation(context.nisCache, transaction1, transaction2);
	}

	@Test
	public void mosaicDefinitionCreationTransactionAndTransferForSameMosaicDefinitionAreNotAllowed() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(200_000));
		context.prepareNamespace(sender, new NamespaceId("foo"));
		final MosaicId mosaicId = Utils.createMosaicId("foo", "mosaic");
		final Transaction transaction1 = createMosaicDefinitionCreationTransaction(sender, "foo", "d1");
		final Transaction transaction2 = createTransferTransaction(sender, Utils.generateRandomAccount(), Amount.fromNem(10),
				new Mosaic(mosaicId, new Quantity(9)));

		// Act / Assert:
		this.assertConflictingMosaicCreation(context.nisCache, transaction1, transaction2);
	}

	@Test
	public void mosaicDefinitionCreationTransactionReferencingNamespaceProvisioningInSameBlockIsNotAllowed() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(200_000));
		final Transaction transaction1 = createProvisionNamespaceTransaction(sender, new NamespaceIdPart("foo"), null);
		final Transaction transaction2 = createMosaicDefinitionCreationTransaction(sender, "foo", "d1");

		// Act / Assert:
		this.assertConflictingMosaicCreation(context.nisCache, transaction1, transaction2);
	}

	private void assertConflictingMosaicCreation(final ReadOnlyNisCache cache, final Transaction transaction1,
			final Transaction transaction2) {
		this.assertTransactions(cache, Arrays.asList(transaction1, transaction2),
				this.isSingleBlockUsed() ? Collections.singletonList(transaction1) : Arrays.asList(transaction1, transaction2),
				this.isSingleBlockUsed() ? ValidationResult.FAILURE_CONFLICTING_MOSAIC_CREATION : ValidationResult.SUCCESS);
	}

	private static MosaicDefinitionCreationTransaction createMosaicDefinitionCreationTransaction(final Account sender,
			final String namespaceId) {
		return createMosaicDefinitionCreationTransaction(sender, namespaceId, "desc");
	}

	private static MosaicDefinitionCreationTransaction createMosaicDefinitionCreationTransaction(final Account sender,
			final String namespaceId, final String description) {
		final MosaicId mosaicId = Utils.createMosaicId(namespaceId, "mosaic");
		final MosaicDefinition mosaicDefinition = new MosaicDefinition(sender, mosaicId, new MosaicDescriptor(description),
				Utils.createMosaicProperties(1_000_000L, 3, true, true), Utils.createMosaicLevy(MosaicConstants.MOSAIC_ID_XEM));
		final MosaicDefinitionCreationTransaction transaction = new MosaicDefinitionCreationTransaction(CURRENT_TIME, sender,
				mosaicDefinition);
		transaction.setFee(Amount.fromNem(108));
		return prepareTransaction(transaction);
	}

	private static ProvisionNamespaceTransaction createProvisionNamespaceTransaction(final Account sender, final NamespaceIdPart part,
			NamespaceId parent) {
		final ProvisionNamespaceTransaction transaction = new ProvisionNamespaceTransaction(CURRENT_TIME, sender, part, parent);
		transaction.setFee(Amount.fromNem(108));
		return prepareTransaction(transaction);
	}

	// endregion

	// region MosaicSupplyChangeTransaction

	@Test
	public void canValidateValidMosaicSupplyChangeTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100_000));
		final MosaicId mosaicId = Utils.createMosaicId(5);
		context.prepareMosaic(sender, mosaicId, Supply.fromValue(100), Utils.createMosaicLevy(MosaicConstants.MOSAIC_ID_XEM));
		final MosaicSupplyChangeTransaction transaction = createMosaicSupplyChangeTransaction(sender, mosaicId);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(transaction), Collections.singletonList(transaction),
				ValidationResult.SUCCESS);
	}

	@Test
	public void cannotValidateMosaicSupplyChangeTransactionWithConflictingOwner() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100_000));
		final MosaicId mosaicId = Utils.createMosaicId(5);
		context.prepareMosaic(Utils.generateRandomAccount(), mosaicId, Supply.fromValue(100),
				Utils.createMosaicLevy(MosaicConstants.MOSAIC_ID_XEM));
		final MosaicSupplyChangeTransaction transaction = createMosaicSupplyChangeTransaction(sender, mosaicId);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(transaction), Collections.emptyList(),
				ValidationResult.FAILURE_MOSAIC_CREATOR_CONFLICT);
	}

	@Test
	public void cannotValidateMosaicSupplyChangeTransactionForMosaicWithImmutableSupply() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100_000));
		final MosaicId mosaicId = Utils.createMosaicId(5);
		context.prepareMosaic(sender, mosaicId, Supply.fromValue(100), Utils.createMosaicProperties(0L, 3, false, true), null);
		final MosaicSupplyChangeTransaction transaction = createMosaicSupplyChangeTransaction(sender, mosaicId);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(transaction), Collections.emptyList(),
				ValidationResult.FAILURE_MOSAIC_SUPPLY_IMMUTABLE);
	}

	private static MosaicSupplyChangeTransaction createMosaicSupplyChangeTransaction(final Account sender, final MosaicId mosaicId) {
		final MosaicSupplyChangeTransaction transaction = new MosaicSupplyChangeTransaction(CURRENT_TIME, sender, mosaicId,
				MosaicSupplyType.Create, Supply.fromValue(345));
		transaction.setFee(Amount.fromNem(108));
		return prepareTransaction(transaction);
	}

	// endregion

	// region basic multisig

	@Test
	public void chainWithMultisigTransactionsIssuedByNonCosignatoryIsInvalid() {
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(168));
		final Account cosigner1 = context.addAccount(Amount.ZERO);

		final Transaction transfer = createTransferTransaction(multisig, Utils.generateRandomAccount(), Amount.fromNem(10));
		final MultisigTransaction mt1 = createMultisig(cosigner1, transfer);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(mt1), Collections.emptyList(),
				ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER);
	}

	@Test
	public void chainWithMultisigTransactionIssuedByCosignatoryIsValid() {
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(168));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig, cosigner1);

		final Transaction t1 = createTransferTransaction(multisig, Utils.generateRandomAccount(), Amount.fromNem(10));
		t1.setSignature(null);
		final MultisigTransaction mt1 = createMultisig(cosigner1, t1);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(mt1), Collections.singletonList(mt1), ValidationResult.SUCCESS);
	}

	@Test
	public void chainContainingTransferTransactionIssuedFromMultisigIsInvalid() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(34));
		final Account cosigner1 = context.addAccount(Amount.fromNem(200));
		context.setCosigner(multisig, cosigner1);

		final Transaction t1 = createTransferTransaction(multisig, Utils.generateRandomAccount(), Amount.fromNem(10));

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(t1), Collections.emptyList(),
				ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG);
	}

	@Test
	public void chainWithMultipleMultisigTransactionIsValid() {
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(500));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig, cosigner1);

		final Account recipient = Utils.generateRandomAccount();
		final Transaction t1 = createTransferTransaction(multisig, recipient, Amount.fromNem(10));
		t1.setSignature(null);
		final MultisigTransaction mt1 = createMultisig(cosigner1, t1);

		final Transaction t2 = createTransferTransaction(multisig, recipient, Amount.fromNem(100));
		t2.setSignature(null);
		final MultisigTransaction mt2 = createMultisig(cosigner1, t2);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(mt1, mt2), Arrays.asList(mt1, mt2), ValidationResult.SUCCESS);
	}

	// endregion

	// region inner transaction hash check

	/**
	 * A inner transaction should not be "reusable" in different multisig transactions. This is important because not checking this would
	 * leave us prone to bit more sophisticated version of "replay" attack: > I'm one of cosigners I generate MT with inner Transfer to my
	 * account > after everyone signed it, I reuse inner transfer but I change outer MT (ofc I'd probably have limited amount of time
	 * (MAX_ALLOWED_SECONDS_AHEAD_OF_TIME) but such thing shouldn't be feasible in first place)
	 */

	@Test
	public void chainIsInvalidIfContainsMultipleMultisigTransactionsWithSameInnerTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(234));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig, cosigner1);

		final Account recipient = Utils.generateRandomAccount();
		final Transaction t1 = createTransferTransaction(multisig, recipient, Amount.fromNem(10));
		t1.setSignature(null);
		final MultisigTransaction mt1 = createMultisig(cosigner1, t1);
		final MultisigTransaction mt2 = createMultisig(cosigner1, t1);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(mt1, mt2), Collections.singletonList(mt1), this.getHashConflictResult());
	}

	@Test
	public void chainIsInvalidIfContainsSameTransferInAndOutOfMultisigTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(234));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig, cosigner1);

		final Account recipient = Utils.generateRandomAccount();
		final Transaction t1 = createTransferTransaction(multisig, recipient, Amount.fromNem(10));
		t1.setSignature(null);
		final MultisigTransaction mt1 = createMultisig(cosigner1, t1);
		final Transaction t2 = createTransferTransaction(multisig, recipient, Amount.fromNem(10));

		// Act / Assert:
		this.assertTransactions(context.nisCache, Arrays.asList(mt1, t2), Collections.singletonList(mt1), this.getHashConflictResult());
	}

	// endregion

	// region multisig conversion

	@Test
	public void canConvertAccountToMultisig() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig1 = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);

		final Transaction t1 = createMultisigConversion(multisig1, cosigner1);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(t1), Collections.singletonList(t1), ValidationResult.SUCCESS);
	}

	@Test
	public void canConvertAccountToNonMultisig() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig1 = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig1, cosigner1);

		final Transaction t1 = createMultisigModification(multisig1, cosigner1,
				Collections.singletonList(new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, cosigner1)));

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(t1), Collections.singletonList(t1), ValidationResult.SUCCESS);
	}

	// endregion

	// region multisig cosigner is not allowed

	@Test
	public void multisigAccountCannotBeAddedAsCosigner() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig1 = context.addAccount(Amount.fromNem(2000));
		final Account multisig2 = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig1, cosigner1);
		context.setCosigner(multisig2, cosigner2);

		final Transaction t1 = createMultisigModification(multisig1, cosigner1, multisig2);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(t1), Collections.emptyList(),
				ValidationResult.FAILURE_MULTISIG_ACCOUNT_CANNOT_BE_COSIGNER);
	}

	@Test
	public void cosigningAccountCannotBeConvertedToMultisig() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig1 = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.fromNem(2000));
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig1, cosigner1);

		// - make cosigner1 multisig by adding cosigner2 as a cosigner
		final Transaction t1 = createMultisigConversion(cosigner1, cosigner2);

		// Act / Assert:
		this.assertTransactions(context.nisCache, Collections.singletonList(t1), Collections.emptyList(),
				ValidationResult.FAILURE_MULTISIG_ACCOUNT_CANNOT_BE_COSIGNER);
	}

	// endregion

	// region version check

	@Test
	public void cannotAllowV2MultisigModificationBeforeForkHeight() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig1 = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig1, cosigner1);

		final Transaction t1 = createMultisigModification(multisig1, cosigner1);

		// Act / Assert:
		// (use FORK - 2 so that the next block is FORK - 1)
		this.assertTransactions(new BlockHeight(BlockMarkerConstants.MULTISIG_M_OF_N_FORK(t1.getVersion()) - 2), context.nisCache,
				Collections.singletonList(t1), Collections.emptyList(),
				ValidationResult.FAILURE_MULTISIG_V2_AGGREGATE_MODIFICATION_BEFORE_FORK);
	}

	@Test
	public void canAllowV2MultisigModificationAtForkHeight() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig1 = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig1, cosigner1);

		final Transaction t1 = createMultisigModification(multisig1, cosigner1);

		// Act / Assert:
		// (use FORK - 1 so that the next block is FORK)
		this.assertTransactions(new BlockHeight(BlockMarkerConstants.MULTISIG_M_OF_N_FORK(t1.getVersion()) - 1), context.nisCache,
				Collections.singletonList(t1), Collections.singletonList(t1), ValidationResult.SUCCESS);
	}

	@Test
	public void cannotAllowTransactionsWithVersionsTooHigh() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction t1 = changeTransactionVersion(RandomTransactionFactory.createImportanceTransfer(), 2);

		// Act / Assert:
		this.assertTransactions(new BlockHeight(10000), context.nisCache, Collections.singletonList(t1), Collections.emptyList(),
				ValidationResult.FAILURE_ENTITY_INVALID_VERSION);
	}

	private static Transaction changeTransactionVersion(final Transaction templateTransaction, final int version) {
		final JSONObject jsonObject = JsonSerializer.serializeToJson(templateTransaction.asNonVerifiable());
		jsonObject.put("version", version | NetworkInfos.getDefault().getVersion() << 24);
		final Transaction transaction = TransactionFactory.NON_VERIFIABLE.deserialize(Utils.createDeserializer(jsonObject));
		transaction.setDeadline(transaction.getTimeStamp().addMinutes(1));
		transaction.signBy(templateTransaction.getSigner());
		return transaction;
	}

	// endregion

	// region assertTransactions and protected functions

	private void assertTransactions(final ReadOnlyNisCache nisCache, final List<Transaction> all, final List<Transaction> expectedFiltered,
			final ValidationResult expectedResult) {
		this.assertTransactions(new BlockHeight(511000), nisCache, all, expectedFiltered, expectedResult);
	}

	protected abstract void assertTransactions(final BlockHeight chainHeight, final ReadOnlyNisCache nisCache, final List<Transaction> all,
			final List<Transaction> expectedFiltered, final ValidationResult expectedResult);

	protected boolean isSingleBlockUsed() {
		return true;
	}

	protected boolean allowsIncomplete() {
		return false;
	}

	protected ValidationResult getHashConflictResult() {
		return ValidationResult.FAILURE_TRANSACTION_DUPLICATE_IN_CHAIN;
	}

	// endregion

	// region transaction factory functions

	private static TransferTransaction createTransferTransaction(final TimeInstant timeStamp, final Account sender, final Account recipient,
			final Amount amount, final TransferTransactionAttachment attachment) {
		final TransferTransaction transaction = new TransferTransaction(timeStamp, sender, recipient, amount, attachment);
		return prepareTransaction(transaction);
	}

	private static TransferTransaction createTransferTransaction(final TimeInstant timeStamp, final Account sender, final Account recipient,
			final Amount amount) {
		return createTransferTransaction(timeStamp, sender, recipient, amount, null);
	}

	private static TransferTransaction createTransferTransaction(final Account sender, final Account recipient, final Amount amount) {
		return createTransferTransaction(CURRENT_TIME, sender, recipient, amount, null);
	}

	private static TransferTransaction createTransferTransaction(final Account sender, final Account recipient, final Amount amount,
			final Mosaic mosaic) {
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
		attachment.addMosaic(mosaic);
		return createTransferTransaction(CURRENT_TIME, sender, recipient, amount, attachment);
	}

	private static Transaction createActivateImportanceTransfer(final Account sender, final Account remote) {
		final Transaction transaction = new ImportanceTransferTransaction(CURRENT_TIME, sender, ImportanceTransferMode.Activate, remote);
		return prepareTransaction(transaction);
	}

	private static Transaction createMultisigConversion(final Account multisig, final Account cosigner) {
		final Transaction transaction = new MultisigAggregateModificationTransaction(CURRENT_TIME, multisig,
				Collections.singletonList(new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, cosigner)));
		return prepareTransaction(transaction);
	}

	private static MultisigTransaction createMultisigModification(final Account multisig, final Account cosigner,
			final List<MultisigCosignatoryModification> modifications) {
		final Transaction transaction = new MultisigAggregateModificationTransaction(CURRENT_TIME, multisig, modifications);
		transaction.setDeadline(CURRENT_TIME.addMinutes(1));
		transaction.setSignature(null);
		return createMultisig(cosigner, transaction);
	}

	private static MultisigTransaction createMultisigModification(final Account multisig, final Account cosigner,
			final Account newCosigner) {
		return createMultisigModification(multisig, cosigner,
				Collections.singletonList(new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, newCosigner)));
	}

	private static MultisigTransaction createMultisigModification(final Account multisig, final Account cosigner) {
		return createMultisigModification(multisig, cosigner, Utils.generateRandomAccount());
	}

	private static MultisigTransaction createMultisig(final Account cosigner, final Transaction innerTransaction) {
		final MultisigTransaction transaction = new MultisigTransaction(CURRENT_TIME, cosigner, innerTransaction);
		return prepareTransaction(transaction);
	}

	private static MultisigSignatureTransaction createSignature(final Account cosigner, final Account multisig,
			final Transaction innerTransaction) {
		final MultisigSignatureTransaction transaction = new MultisigSignatureTransaction(CURRENT_TIME, cosigner, multisig,
				innerTransaction);
		return prepareTransaction(transaction);
	}

	// endregion

	private static <T extends Transaction> T prepareTransaction(final T transaction) {
		// set the deadline appropriately and sign
		transaction.setDeadline(transaction.getTimeStamp().addMinutes(1));
		transaction.sign();
		return transaction;
	}

	public static class TestContext {
		public final ReadOnlyNisCache nisCache = NisCacheFactory.createReal();

		public TestContext() {
			// add one large account that is harvesting-eligible
			this.addAccount(Amount.fromNem(100000));

			// add one mosaic with a levy
			final NisCache copyCache = this.nisCache.copy();
			final MosaicId mosaicId = Utils.createMosaicId(1);
			final Account namespaceOwner = Utils.generateRandomAccount();
			copyCache.getNamespaceCache().add(new Namespace(mosaicId.getNamespaceId(), namespaceOwner, BlockHeight.ONE));

			final MosaicLevy mosaicLevy = Utils.createMosaicLevy();
			final MosaicDefinition mosaicDefinition = new MosaicDefinition(namespaceOwner, mosaicId, new MosaicDescriptor("awesome mosaic"),
					Utils.createMosaicProperties(), mosaicLevy);
			copyCache.getNamespaceCache().get(mosaicId.getNamespaceId()).getMosaics().add(mosaicDefinition);
			copyCache.commit();
		}

		public Account addAccount(final Amount amount) {
			return this.prepareAccount(Utils.generateRandomAccount(), amount);
		}

		public Account prepareAccount(final Account account, final Amount amount) {
			final NisCache copyCache = this.nisCache.copy();
			final AccountState accountState = copyCache.getAccountStateCache().findStateByAddress(account.getAddress());
			accountState.getAccountInfo().incrementBalance(amount);
			accountState.setHeight(BlockHeight.ONE);
			accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, amount);
			copyCache.commit();
			return account;
		}

		public void prepareMosaic(final Account account, final MosaicId mosaicId, final Supply supply, final MosaicLevy levy) {
			final MosaicProperties properties = Utils.createMosaicProperties(0L, 3, true, true);
			this.prepareMosaic(account, mosaicId, supply, properties, levy);
		}

		public void prepareMosaic(final Account account, final MosaicId mosaicId, final Supply supply, final MosaicProperties properties,
				final MosaicLevy levy) {
			final NisCache copyCache = this.nisCache.copy();
			final Namespace namespace = new Namespace(mosaicId.getNamespaceId(), account, new BlockHeight(511000));
			final MosaicDefinition mosaicDefinition = new MosaicDefinition(account, mosaicId, new MosaicDescriptor("description"),
					properties, levy);
			final NamespaceCache namespaceCache = copyCache.getNamespaceCache();
			namespaceCache.add(namespace);
			final NamespaceEntry namespaceEntry = namespaceCache.get(namespace.getId());
			final MosaicEntry mosaicEntry = namespaceEntry.getMosaics().add(mosaicDefinition);
			mosaicEntry.increaseSupply(supply);
			copyCache.commit();
		}

		public void prepareNamespace(final Account account, final NamespaceId namespaceId) {
			final NisCache copy = this.nisCache.copy();
			final Namespace namespace = new Namespace(namespaceId, account, new BlockHeight(511000));
			copy.getNamespaceCache().add(namespace);
			copy.commit();
		}

		public void makeRemote(final Account account) {
			final NisCache copyCache = this.nisCache.copy();
			final ImportanceTransferMode mode = ImportanceTransferMode.Activate;
			copyCache.getAccountStateCache().findStateByAddress(account.getAddress()).getRemoteLinks()
					.addLink(new RemoteLink(Utils.generateRandomAddress(), BlockHeight.ONE, mode, RemoteLink.Owner.RemoteHarvester));
			copyCache.commit();
		}

		private void setCosigner(final Account multisig, final Account cosigner) {
			final NisCache copyCache = this.nisCache.copy();
			final AccountStateCache accountStateCache = copyCache.getAccountStateCache();
			accountStateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(cosigner.getAddress());
			accountStateCache.findStateByAddress(cosigner.getAddress()).getMultisigLinks().addCosignatoryOf(multisig.getAddress());
			copyCache.commit();
		}

		private void setMinCosignatories(final Account multisig, final int minCosignatories) {
			final NisCache copyCache = this.nisCache.copy();
			final AccountStateCache accountStateCache = copyCache.getAccountStateCache();
			final MultisigLinks multisigLinks = accountStateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks();
			multisigLinks.incrementMinCosignatoriesBy(minCosignatories - multisigLinks.minCosignatories());
			copyCache.commit();
		}

		public MockTransaction createValidSignedTransaction() {
			return this.createValidSignedTransaction(Utils.generateRandomAccount());
		}

		public MockTransaction createValidSignedTransaction(final Account signer) {
			final TimeInstant timeStamp = CURRENT_TIME;
			final MockTransaction transaction = new MockTransaction(signer, 12, timeStamp);
			return this.prepareMockTransaction(transaction);
		}

		private MockTransaction prepareMockTransaction(final MockTransaction transaction) {
			prepareTransaction(transaction);
			this.prepareAccount(transaction.getSigner(), Amount.fromNem(100));
			return transaction;
		}
	}
}
