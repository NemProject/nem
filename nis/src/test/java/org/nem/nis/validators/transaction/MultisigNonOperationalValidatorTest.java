package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.MultisigTestContext;
import org.nem.nis.ForkConfiguration;

import java.util.*;

public class MultisigNonOperationalValidatorTest {

	// region non-multisig account

	@Test
	public void nonMultisigAccountCanIssueAnyTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account account = Utils.generateRandomAccount();
		context.addState(account);
		final Transaction transaction = createMockTransaction(account);

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void nonMultisigAccountCanIssueUnsignedChildTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account account = Utils.generateRandomAccount();
		context.addState(account);
		final Transaction transaction = createMockTransaction(account);
		transaction.setSignature(null); // note, we're not signing transaction which means it's a child transaction

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	// endregion

	// region multisig account

	@Test
	public void multisigAccountCannotIssueAnyTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = createMultisigAccount(context);
		final Transaction transaction = createMockTransaction(multisig);

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG));
	}

	@Test
	public void multisigAccountCannotIssueMultisigModification() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = createMultisigAccount(context);

		final Transaction transaction = new MultisigAggregateModificationTransaction(TimeInstant.ZERO, multisig, Collections.singletonList(
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, Utils.generateRandomAccount())));
		transaction.sign();

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG));
	}

	@Test
	public void multisigAccountCanIssueUnsignedChildTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = createMultisigAccount(context);
		final Transaction transaction = createMockTransaction(multisig);
		transaction.setSignature(null); // note, we're not signing transaction which means it's a child transaction

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	// endregion

	// region multisig account - treasury reissuance

	@Test
	public void multisigAccountCanIssueAllowedTransactionAtForkHeight() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = createMultisigAccount(context);
		final Transaction transaction = createMockTransaction(multisig);

		final ArrayList<Hash> transactionHashes = new ArrayList<Hash>();
		transactionHashes.add(HashUtils.calculateHash(transaction));
		final ForkConfiguration forkConfiguration = new ForkConfiguration(new BlockHeight(12345), transactionHashes, new ArrayList<Hash>());

		// Act:
		final ValidationResult result = context.validateNonOperational(new BlockHeight(12345), forkConfiguration, transaction);

		// Assert
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigAccountCannotIssueAllowedTransactionAtOtherHeight() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = createMultisigAccount(context);
		final Transaction transaction = createMockTransaction(multisig);

		final ArrayList<Hash> transactionHashes = new ArrayList<Hash>();
		transactionHashes.add(HashUtils.calculateHash(transaction));
		final ForkConfiguration forkConfiguration = new ForkConfiguration(new BlockHeight(12345), transactionHashes, new ArrayList<Hash>());

		// Act:
		final ValidationResult result = context.validateNonOperational(new BlockHeight(12346), forkConfiguration, transaction);

		// Assert
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG));
	}

	@Test
	public void multisigAccountCannotIssueAllowedFallbackTransactionAtForkHeight() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = createMultisigAccount(context);
		final Transaction transaction = createMockTransaction(multisig);

		final ArrayList<Hash> transactionHashes = new ArrayList<Hash>();
		transactionHashes.add(HashUtils.calculateHash(transaction));
		final ForkConfiguration forkConfiguration = new ForkConfiguration(new BlockHeight(12345), new ArrayList<Hash>(), transactionHashes);

		// Act:
		final ValidationResult result = context.validateNonOperational(new BlockHeight(12345), forkConfiguration, transaction);

		// Assert
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG));
	}

	// endregion

	private static Transaction createMockTransaction(final Account account) {
		final Transaction transaction = new MockTransaction(account);
		transaction.sign();
		return transaction;
	}

	private static Account createMultisigAccount(final MultisigTestContext context) {
		final Account multisig = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig);
		return multisig;
	}
}
