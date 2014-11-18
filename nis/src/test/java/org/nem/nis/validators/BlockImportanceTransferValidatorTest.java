package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.test.NisUtils;

public class BlockImportanceTransferValidatorTest {
	private static final long TEST_HEIGHT = BlockMarkerConstants.BETA_IT_VALIDATION_FORK;

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
		context.addTransaction();
		context.addTransaction();

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void blockWithImportanceTransferValidates() {
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
	public void blockWithTransfersAndImportanceTransferValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addImportanceTransaction(Utils.generateRandomAccount());
		context.addTransaction();
		context.addImportanceTransaction(Utils.generateRandomAccount());
		context.addTransaction();

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	// such block actually won't be validated, but for a different reason:
	// sender is required to have minHarvesterBalance (commonAccount)
	// but at the same time recipient is required to have zero balance (again commonAccount)
	@Test
	public void blockWithConflictingImportanceTransferSenderAndRecipientValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addImportanceTransaction(context.commonAccount, Utils.generateRandomAccount());
		context.addImportanceTransaction(Utils.generateRandomAccount(), context.commonAccount);

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region invalid blocks

	@Test
	public void blockWithConflictingImportanceTransferSenderDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addImportanceTransaction(context.commonAccount, Utils.generateRandomAccount());
		context.addImportanceTransaction(context.commonAccount, Utils.generateRandomAccount());

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CONFLICTING_IMPORTANCE_TRANSFER));
	}

	@Test
	public void blockWithConflictingImportanceTransferRecipientDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addImportanceTransaction(Utils.generateRandomAccount(), context.commonAccount);
		context.addImportanceTransaction(Utils.generateRandomAccount(), context.commonAccount);

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CONFLICTING_IMPORTANCE_TRANSFER));
	}

	@Test
	public void blockWithTransfersAndConflictingImportanceTransferRecipientDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addImportanceTransaction(Utils.generateRandomAccount(), context.commonAccount);
		context.addTransaction();
		context.addImportanceTransaction(Utils.generateRandomAccount(), context.commonAccount);
		context.addTransaction();

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CONFLICTING_IMPORTANCE_TRANSFER));
	}

	//endregion

	private static class TestContext {
		private final Account harvester = Utils.generateRandomAccount();
		private final Block block = NisUtils.createRandomBlockWithHeight(this.harvester, TEST_HEIGHT);
		private final BlockValidator validator = new BlockImportanceTransferValidator();
		private final Account commonAccount = Utils.generateRandomAccount();

		private void addTransaction() {
			this.addTransaction(Utils.generateRandomAccount());
		}

		private void addTransaction(final Account account) {
			this.block.addTransaction(new MockTransaction(account));
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