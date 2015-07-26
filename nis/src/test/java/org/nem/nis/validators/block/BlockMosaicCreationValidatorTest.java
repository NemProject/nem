package org.nem.nis.validators.block;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.*;

public class BlockMosaicCreationValidatorTest {

	//region valid

	@Test
	public void blockWithNoTransactionsValidates() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		context.assertValidation(ValidationResult.SUCCESS);
	}

	@Test
	public void blockWithMultipleMosaicCreationsValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicCreation(Utils.createMosaicId(100));
		context.addMosaicCreation(Utils.createMosaicId(101));

		// Assert:
		context.assertValidation(ValidationResult.SUCCESS);
	}

	@Test
	public void blockWithMosaicCreationAndNonConflictingSupplyChangeValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicCreation(Utils.createMosaicId(100));
		context.addMosaicSupplyChange(Utils.createMosaicId(101));

		// Assert:
		context.assertValidation(ValidationResult.SUCCESS);
	}

	@Test
	public void blockWithMosaicCreationAndNonConflictingTransferValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicCreation(Utils.createMosaicId(100));
		context.addTransfer(Utils.createMosaicId(101));

		// Assert:
		context.assertValidation(ValidationResult.SUCCESS);
	}

	//endregion

	//region invalid

	@Test
	public void blockWithMosaicCreationAndConflictingSupplyChangeDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicCreation(Utils.createMosaicId(100));
		context.addMosaicSupplyChange(Utils.createMosaicId(100));

		// Assert:
		context.assertValidation(ValidationResult.FAILURE_CONFLICTING_MOSAIC_CREATION);
	}

	@Test
	public void blockWithMosaicCreationAndConflictingSupplyChangeReverseOrderDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicSupplyChange(Utils.createMosaicId(100));
		context.addMosaicCreation(Utils.createMosaicId(100));

		// Assert:
		context.assertValidation(ValidationResult.FAILURE_CONFLICTING_MOSAIC_CREATION);
	}

	@Test
	public void blockWithMosaicCreationAndConflictingTransferDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicCreation(Utils.createMosaicId(100));
		context.addTransfer(Utils.createMosaicId(100));

		// Assert:
		context.assertValidation(ValidationResult.FAILURE_CONFLICTING_MOSAIC_CREATION);
	}

	@Test
	public void blockWithMosaicCreationAndConflictingTransferReverseOrderDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addTransfer(Utils.createMosaicId(100));
		context.addMosaicCreation(Utils.createMosaicId(100));

		// Assert:
		context.assertValidation(ValidationResult.FAILURE_CONFLICTING_MOSAIC_CREATION);
	}

	//endregion

	private static class TestContext {
		private final Account signer = Utils.generateRandomAccount();
		private final Block block = NisUtils.createRandomBlock();
		private final BlockMosaicCreationValidator validator = new BlockMosaicCreationValidator();

		public void addMosaicCreation(final MosaicId mosaicId) {
			final Mosaic mosaic = Utils.createMosaic(this.signer, mosaicId, Utils.createMosaicProperties());
			this.block.addTransaction(new MosaicCreationTransaction(TimeInstant.ZERO, mosaic.getCreator(), mosaic));
		}

		public void addMosaicSupplyChange(final MosaicId mosaicId) {
			final Transaction transaction = new SmartTileSupplyChangeTransaction(
					TimeInstant.ZERO,
					this.signer,
					mosaicId,
					SmartTileSupplyType.CreateSmartTiles,
					new Quantity(1000));
			this.block.addTransaction(transaction);
		}

		public void addTransfer(final MosaicId mosaicId) {
			final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
			attachment.addMosaicTransfer(Utils.createMosaicId(1), new Quantity(1));
			attachment.addMosaicTransfer(mosaicId, new Quantity(43));
			attachment.addMosaicTransfer(Utils.createMosaicId(2), new Quantity(3));
			final Transaction transaction = new TransferTransaction(
					TimeInstant.ZERO,
					this.signer,
					Utils.generateRandomAccount(),
					Amount.fromNem(1234),
					attachment);
			this.block.addTransaction(transaction);
		}

		private void assertValidation(final ValidationResult expectedResult) {
			// Act:
			final ValidationResult result = this.validator.validate(this.block);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(expectedResult));
		}
	}
}