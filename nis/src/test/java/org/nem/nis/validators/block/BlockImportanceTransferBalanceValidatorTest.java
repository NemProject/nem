package org.nem.nis.validators.block;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.BlockValidator;

public class BlockImportanceTransferBalanceValidatorTest {
	private static final long TEST_HEIGHT = 123;

	//region valid blocks

	@Test
	public void blockWithoutTransactionsValidates() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void blockWithoutImportanceTransfersValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addTransferTransaction();
		context.addTransferTransaction();

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void blockWithImportanceTransfersValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addImportanceTransaction(Utils.generateRandomAccount());
		context.addImportanceTransaction(Utils.generateRandomAccount());

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void blockWithTransfersAndImportanceTransfersValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addImportanceTransaction(Utils.generateRandomAccount());
		context.addTransferTransaction();
		context.addImportanceTransaction(Utils.generateRandomAccount());
		context.addTransferTransaction();

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region invalid blocks

	@Test
	public void blockWithTransferAndConflictingImportanceTransferDoesNotValidate() {
		// Assert:
		assertBlockWithBalanceTransferAndConflictingImportanceTransferDoesNotValidate(10);
	}

	@Test
	public void blockWithZeroBalanceTransferAndConflictingImportanceTransferDoesNotValidate() {
		// Assert:
		assertBlockWithBalanceTransferAndConflictingImportanceTransferDoesNotValidate(0);
	}

	private static void assertBlockWithBalanceTransferAndConflictingImportanceTransferDoesNotValidate(final int amount) {
		// Arrange:
		final TestContext context = new TestContext();
		context.addImportanceTransaction(Utils.generateRandomAccount(), context.commonAccount);
		context.addTransferTransaction(context.commonAccount, Amount.fromNem(amount));

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_DESTINATION_ACCOUNT_HAS_PREEXISTING_BALANCE_TRANSFER));
	}

	//endregion

	private static class TestContext {
		private final Account harvester = Utils.generateRandomAccount();
		private final Block block = NisUtils.createRandomBlockWithHeight(this.harvester, TEST_HEIGHT);
		private final BlockValidator validator = new BlockImportanceTransferBalanceValidator();
		private final Account commonAccount = Utils.generateRandomAccount();

		private void addTransferTransaction() {
			this.addTransferTransaction(Utils.generateRandomAccount(), Amount.fromNem(10));
		}

		private void addTransferTransaction(final Account recipient, final Amount amount) {
			final Transaction t = new TransferTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(), recipient, amount, null);
			t.setFee(t.getFee());
			this.block.addTransaction(t);
		}

		private void addImportanceTransaction(final Account recipient) {
			this.addImportanceTransaction(Utils.generateRandomAccount(), recipient);
		}

		private void addImportanceTransaction(final Account sender, final Account recipient) {
			this.block.addTransaction(
					new ImportanceTransferTransaction(
							TimeInstant.ZERO,
							sender,
							ImportanceTransferTransaction.Mode.Activate,
							recipient));
		}
	}
}