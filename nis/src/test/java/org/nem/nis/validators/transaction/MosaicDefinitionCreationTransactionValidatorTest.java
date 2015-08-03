package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.state.MosaicEntry;
import org.nem.nis.test.*;
import org.nem.nis.validators.ValidationContext;

import java.util.*;

public class MosaicDefinitionCreationTransactionValidatorTest {
	private static final Account SIGNER = Utils.generateRandomAccount();
	private static final BlockHeight VALIDATION_HEIGHT = new BlockHeight(21);
	private static final Account ADMITTER = MosaicConstants.MOSAIC_ADMITTER;
	private static final Amount CREATION_FEE = Amount.fromNem(50_000);

	//region valid

	@Test
	public void transactionIsValidIfMosaicDefinitionIsNew() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransaction();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionPropertyChangeIsValidIfMosaicDefinitionExistsAndCreatorOwnsEntireSupply() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		final MosaicDefinition mosaicDefinition = createAlteredMosaicDefinition(transaction.getMosaicDefinition(), createCustomMosaicProperties());
		context.addMosaicDefinition(mosaicDefinition);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionDescriptionChangeIsValidIfMosaicDefinitionExistsAndCreatorOwnsEntireSupply() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		final MosaicDefinition mosaicDefinition = createAlteredMosaicDefinition(transaction.getMosaicDefinition(), "some desc");
		context.addMosaicDefinition(mosaicDefinition);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionDescriptionChangeIsValidIfMosaicDefinitionExistsAndCreatorOwnsPartialSupply() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		final MosaicDefinition mosaicDefinition = createAlteredMosaicDefinition(transaction.getMosaicDefinition(), "some desc");
		context.addMosaicDefinition(mosaicDefinition);
		context.makeOwnerHavePartialSupply(mosaicDefinition);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionPropertyAndDescriptionChangeIsValidIfMosaicDefinitionExistsAndCreatorOwnsFullSupply() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		final MosaicDefinition mosaicDefinition = createAlteredMosaicDefinition(transaction.getMosaicDefinition(), "some desc", createCustomMosaicProperties());
		context.addMosaicDefinition(mosaicDefinition);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionIsInvalidIfCreationFeeIsMinimum() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransaction(CREATION_FEE);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionIsInvalidIfCreationFeeIsGreaterThanMinimum() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransaction(CREATION_FEE.add(Amount.fromNem(100)));

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region invalid

	@Test
	public void transactionIsInvalidIfNamespaceIsInactive() {
		// Arrange:
		final TestContext context = new TestContext();
		context.activateNamespaceAtHeight(SIGNER, VALIDATION_HEIGHT.next());
		final MosaicDefinitionCreationTransaction transaction = createTransaction();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_EXPIRED));
	}

	@Test
	public void transactionIsInvalidIfNamespaceIsNotOwned() {
		// Arrange:
		final TestContext context = new TestContext();
		context.activateNamespaceAtHeight(Utils.generateRandomAccount(), VALIDATION_HEIGHT);
		final MosaicDefinitionCreationTransaction transaction = createTransaction();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT));
	}

	@Test
	public void transactionPropertyChangeIsInvalidIfMosaicDefinitionExistsAndCreatorOwnsPartialSupply() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		final MosaicDefinition mosaicDefinition = createAlteredMosaicDefinition(transaction.getMosaicDefinition(), createCustomMosaicProperties());
		context.addMosaicDefinition(mosaicDefinition);
		context.makeOwnerHavePartialSupply(mosaicDefinition);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_MODIFICATION_NOT_ALLOWED));
	}

	@Test
	public void transactionPropertyAndDescriptionChangeIsInalidIfMosaicDefinitionExistsAndCreatorOwnsPartialSupply() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		final MosaicDefinition mosaicDefinition = createAlteredMosaicDefinition(transaction.getMosaicDefinition(), "some desc", createCustomMosaicProperties());
		context.addMosaicDefinition(mosaicDefinition);
		context.makeOwnerHavePartialSupply(mosaicDefinition);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_MODIFICATION_NOT_ALLOWED));
	}

	@Test
	public void transactionChangeIsInvalidIfMosaicDefinitionExistsAndTransactionResultsInNoChange() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		context.addMosaicDefinition(transaction.getMosaicDefinition());

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_MODIFICATION_NOT_ALLOWED));
	}

	@Test
	public void transactionIsInvalidIfAdmitterIsInvalid() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransaction(Utils.generateRandomAccount());

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_INVALID_ADMITTER));
	}

	@Test
	public void transactionIsInvalidIfCreationFeeIsLessThanMinimum() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransaction(CREATION_FEE.subtract(Amount.fromNem(1)));

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_INVALID_CREATION_FEE));
	}

	//endregion

	private static MosaicDefinitionCreationTransaction createTransaction() {
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(SIGNER);
		return new MosaicDefinitionCreationTransaction(TimeInstant.ZERO, SIGNER, mosaicDefinition, ADMITTER, CREATION_FEE);
	}

	private static MosaicDefinitionCreationTransaction createTransaction(final Account admitter) {
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(SIGNER);
		return new MosaicDefinitionCreationTransaction(TimeInstant.ZERO, SIGNER, mosaicDefinition, admitter, CREATION_FEE);
	}

	private static MosaicDefinitionCreationTransaction createTransaction(final Amount creationFee) {
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(SIGNER);
		return new MosaicDefinitionCreationTransaction(TimeInstant.ZERO, SIGNER, mosaicDefinition, ADMITTER, creationFee);
	}

	//region createAlteredMosaicDefinition

	public static MosaicDefinition createAlteredMosaicDefinition(final MosaicDefinition mosaicDefinition, final String description) {
		return createAlteredMosaicDefinition(mosaicDefinition, description, mosaicDefinition.getProperties());
	}

	public static MosaicDefinition createAlteredMosaicDefinition(final MosaicDefinition mosaicDefinition, final MosaicProperties properties) {
		return createAlteredMosaicDefinition(mosaicDefinition, mosaicDefinition.getProperties().toString(), properties);
	}

	public static MosaicDefinition createAlteredMosaicDefinition(final MosaicDefinition mosaicDefinition, final String description, final MosaicProperties properties) {
		return new MosaicDefinition(
				mosaicDefinition.getCreator(),
				mosaicDefinition.getId(),
				new MosaicDescriptor(description),
				properties);
	}

	//endregion

	public static MosaicProperties createCustomMosaicProperties() {
		final Properties properties = new Properties();
		properties.put("divisibility", "5");
		properties.put("quantity", "567");
		return new DefaultMosaicProperties(properties);
	}

	private static TestContext createContextWithValidNamespace() {
		final TestContext context = new TestContext();
		context.activateNamespaceAtHeight(SIGNER, VALIDATION_HEIGHT);
		return context;
	}

	private static class TestContext {
		final NamespaceCache namespaceCache = new DefaultNamespaceCache();
		final MosaicDefinitionCreationTransactionValidator validator = new MosaicDefinitionCreationTransactionValidator(this.namespaceCache);

		public void activateNamespaceAtHeight(final Account signer, final BlockHeight height) {
			for (final String namespace : Arrays.asList("alice", "alice.vouchers")) {
				this.namespaceCache.add(new Namespace(new NamespaceId(namespace), signer, height));
			}
		}

		public void addMosaicDefinition(final MosaicDefinition mosaicDefinition) {
			this.namespaceCache.get(mosaicDefinition.getId().getNamespaceId()).getMosaics().add(mosaicDefinition);
		}

		public void makeOwnerHavePartialSupply(final MosaicDefinition mosaicDefinition) {
			final MosaicEntry mosaicEntry = this.getMosaicEntry(mosaicDefinition.getId());
			mosaicEntry.increaseSupply(Supply.fromValue(234));
			mosaicEntry.getBalances().decrementBalance(mosaicDefinition.getCreator().getAddress(), Quantity.fromValue(123_000));
		}

		public ValidationResult validate(final MosaicDefinitionCreationTransaction transaction) {
			return this.validator.validate(transaction, new ValidationContext(VALIDATION_HEIGHT, ValidationStates.Throw));
		}

		private MosaicEntry getMosaicEntry(final MosaicId mosaicId) {
			return  this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId);
		}
	}
}