package org.nem.nis.validators.block;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.NisUtils;

public class BlockMosaicDefinitionCreationValidatorTest {

	// region valid

	@Test
	public void blockWithNoTransactionsValidates() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		context.assertValidation(ValidationResult.SUCCESS);
	}

	@Test
	public void blockWithMultipleMosaicDefinitionCreationsValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinitionCreation(Utils.createMosaicId(100));
		context.addMosaicDefinitionCreation(Utils.createMosaicId(101));

		// Assert:
		context.assertValidation(ValidationResult.SUCCESS);
	}

	@Test
	public void blockWithMosaicDefinitionCreationAndNonConflictingSupplyChangeValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinitionCreation(Utils.createMosaicId(100));
		context.addMosaicSupplyChange(Utils.createMosaicId(101));

		// Assert:
		context.assertValidation(ValidationResult.SUCCESS);
	}

	@Test
	public void blockWithMosaicDefinitionCreationAndNonConflictingTransferValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinitionCreation(Utils.createMosaicId(100));
		context.addTransfer(Utils.createMosaicId(101));

		// Assert:
		context.assertValidation(ValidationResult.SUCCESS);
	}

	@Test
	public void blockWithMosaicDefinitionCreationAndNonConflictingProvisionRootOrSubNamespaceTransactionValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinitionCreation(Utils.createMosaicId(100));
		context.addNamespaceProvisioning(new NamespaceIdPart("foo"), null);
		context.addNamespaceProvisioning(new NamespaceIdPart("foo"), new NamespaceId("bar"));

		// Assert:
		context.assertValidation(ValidationResult.SUCCESS);
	}

	@Test
	public void blockWithMosaicDefinitionCreationAndMultipleNonConflictingProvisionRootOrSubNamespaceTransactionsValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinitionCreation(Utils.createMosaicId(100));
		context.addNamespaceProvisioning(new NamespaceIdPart("foo"), null);
		context.addNamespaceProvisioning(new NamespaceIdPart("bar"), new NamespaceId("foo"));
		context.addNamespaceProvisioning(new NamespaceIdPart("bazz"), null);
		context.addNamespaceProvisioning(new NamespaceIdPart("bob"), new NamespaceId("alice"));

		// Assert:
		context.assertValidation(ValidationResult.SUCCESS);
	}

	// endregion

	// region invalid

	@Test
	public void blockWithMosaicDefinitionCreationAndConflictingSupplyChangeDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinitionCreation(Utils.createMosaicId(100));
		context.addMosaicSupplyChange(Utils.createMosaicId(100));

		// Assert:
		context.assertValidation(ValidationResult.FAILURE_CONFLICTING_MOSAIC_CREATION);
	}

	@Test
	public void blockWithMosaicDefinitionCreationAndConflictingSupplyChangeReverseOrderDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicSupplyChange(Utils.createMosaicId(100));
		context.addMosaicDefinitionCreation(Utils.createMosaicId(100));

		// Assert:
		context.assertValidation(ValidationResult.FAILURE_CONFLICTING_MOSAIC_CREATION);
	}

	@Test
	public void blockWithMosaicDefinitionCreationAndConflictingTransferDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinitionCreation(Utils.createMosaicId(100));
		context.addTransfer(Utils.createMosaicId(100));

		// Assert:
		context.assertValidation(ValidationResult.FAILURE_CONFLICTING_MOSAIC_CREATION);
	}

	@Test
	public void blockWithMosaicDefinitionCreationAndConflictingTransferReverseOrderDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addTransfer(Utils.createMosaicId(100));
		context.addMosaicDefinitionCreation(Utils.createMosaicId(100));

		// Assert:
		context.assertValidation(ValidationResult.FAILURE_CONFLICTING_MOSAIC_CREATION);
	}

	@Test
	public void blockWithMosaicDefinitionCreationAndConflictingProvisionRootNamespaceTransactionDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinitionCreation(Utils.createMosaicId(100));
		context.addNamespaceProvisioning(new NamespaceIdPart("id100"), null);

		// Assert:
		context.assertValidation(ValidationResult.FAILURE_CONFLICTING_MOSAIC_CREATION);
	}

	@Test
	public void blockWithMosaicDefinitionCreationAndConflictingProvisionSubNamespaceTransactionDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinitionCreation(Utils.createMosaicId(new NamespaceId("foo.bar"), 100));
		context.addNamespaceProvisioning(new NamespaceIdPart("bar"), new NamespaceId("foo"));

		// Assert:
		context.assertValidation(ValidationResult.FAILURE_CONFLICTING_MOSAIC_CREATION);
	}

	@Test
	public void blockWithMosaicDefinitionCreationAndConflictingProvisionRootNamespaceTransactionReverseOrderDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addNamespaceProvisioning(new NamespaceIdPart("id100"), null);
		context.addMosaicDefinitionCreation(Utils.createMosaicId(100));

		// Assert:
		context.assertValidation(ValidationResult.FAILURE_CONFLICTING_MOSAIC_CREATION);
	}

	@Test
	public void blockWithMosaicDefinitionCreationAndConflictingProvisionSubNamespaceTransactionReverseOrderDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addNamespaceProvisioning(new NamespaceIdPart("bar"), new NamespaceId("foo"));
		context.addMosaicDefinitionCreation(Utils.createMosaicId(new NamespaceId("foo.bar"), 100));

		// Assert:
		context.assertValidation(ValidationResult.FAILURE_CONFLICTING_MOSAIC_CREATION);
	}

	// endregion

	private static class TestContext {
		private final Account signer = Utils.generateRandomAccount();
		private final Block block = NisUtils.createRandomBlock();
		private final BlockMosaicDefinitionCreationValidator validator = new BlockMosaicDefinitionCreationValidator();

		public void addMosaicDefinitionCreation(final MosaicId mosaicId) {
			final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(this.signer, mosaicId, Utils.createMosaicProperties());
			final Transaction transaction = new MosaicDefinitionCreationTransaction(TimeInstant.ZERO, mosaicDefinition.getCreator(),
					mosaicDefinition, Utils.generateRandomAccount(), Amount.fromNem(25));
			this.block.addTransaction(transaction);
		}

		public void addMosaicSupplyChange(final MosaicId mosaicId) {
			final Transaction transaction = new MosaicSupplyChangeTransaction(TimeInstant.ZERO, this.signer, mosaicId,
					MosaicSupplyType.Create, new Supply(1000));
			this.block.addTransaction(transaction);
		}

		public void addTransfer(final MosaicId mosaicId) {
			final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
			attachment.addMosaic(Utils.createMosaicId(1), new Quantity(1));
			attachment.addMosaic(mosaicId, new Quantity(43));
			attachment.addMosaic(Utils.createMosaicId(2), new Quantity(3));
			final Transaction transaction = new TransferTransaction(TimeInstant.ZERO, this.signer, Utils.generateRandomAccount(),
					Amount.fromNem(1234), attachment);
			this.block.addTransaction(transaction);
		}

		public void addNamespaceProvisioning(final NamespaceIdPart part, NamespaceId parent) {
			final Transaction transaction = new ProvisionNamespaceTransaction(TimeInstant.ZERO, this.signer, part, parent);
			this.block.addTransaction(transaction);
		}

		private void assertValidation(final ValidationResult expectedResult) {
			// Act:
			final ValidationResult result = this.validator.validate(this.block);

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
		}
	}
}
