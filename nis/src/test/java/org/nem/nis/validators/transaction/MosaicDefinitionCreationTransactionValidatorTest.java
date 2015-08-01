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
import org.nem.nis.test.DebitPredicates;
import org.nem.nis.validators.ValidationContext;

import java.util.*;

public class MosaicDefinitionCreationTransactionValidatorTest {
	private static final Account SIGNER = Utils.generateRandomAccount();
	private static final BlockHeight VALIDATION_HEIGHT = new BlockHeight(21);

	//region valid

	@Test
	public void validTransactionValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.activateNamespaceAtHeight(SIGNER, VALIDATION_HEIGHT);
		final MosaicDefinitionCreationTransaction transaction = createTransaction();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionIsValidIfMosaicAlreadyExistsInCacheAndMosaicPropertiesAreDifferentAndCreatorOwnsEntireSupply() {
		// Arrange:
		final TestContext context = new TestContext();
		context.activateNamespaceAtHeight(SIGNER, VALIDATION_HEIGHT);
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(
				SIGNER,
				new MosaicId(new NamespaceId("alice.vouchers"), "Alice's gift vouchers"),
				createCustomMosaicProperties());
		context.addMosaicDefinition(mosaicDefinition);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionIsValidIfMosaicAlreadyExistsInCacheAndMosaicPropertiesAreNotDifferentAndCreatorDoesNotOwnEntireSupply() {
		// Arrange:
		final TestContext context = new TestContext();
		context.activateNamespaceAtHeight(SIGNER, VALIDATION_HEIGHT);
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		final MosaicDefinition mosaicDefinition = transaction.getMosaicDefinition();
		context.addMosaicDefinition(createMosaicDefinitionWithDifferentDescriptor(transaction.getMosaicDefinition()));
		context.incrementSupply(mosaicDefinition.getId(), Supply.fromValue(234));
		context.decrementCreatorBalance(mosaicDefinition.getId(), mosaicDefinition.getCreator().getAddress(), Quantity.fromValue(123_000));

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
	public void transactionIsInvalidIfMosaicAlreadyExistsInCacheAndMosaicPropertiesAreDifferentAndCreatorDoesNotOwnEntireSupply() {
		// Arrange:
		final TestContext context = new TestContext();
		context.activateNamespaceAtHeight(SIGNER, VALIDATION_HEIGHT);
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(
				SIGNER,
				new MosaicId(new NamespaceId("alice.vouchers"), "Alice's gift vouchers"),
				createCustomMosaicProperties());
		context.addMosaicDefinition(mosaicDefinition);
		context.decrementCreatorBalance(mosaicDefinition.getId(), mosaicDefinition.getCreator().getAddress(), Quantity.fromValue(123_000));

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_MODIFICATION_NOT_ALLOWED));
	}

	//endregion

	private static MosaicDefinitionCreationTransaction createTransaction() {
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(SIGNER);
		return new MosaicDefinitionCreationTransaction(TimeInstant.ZERO, SIGNER, mosaicDefinition);
	}

	public static MosaicDefinition createMosaicDefinitionWithDifferentDescriptor(final MosaicDefinition mosaicDefinition) {
		return new MosaicDefinition(
				mosaicDefinition.getCreator(),
				mosaicDefinition.getId(),
				new MosaicDescriptor("some description"),
				mosaicDefinition.getProperties());
	}

	public static MosaicProperties createCustomMosaicProperties() {
		final Properties properties = new Properties();
		properties.put("divisibility", "5");
		properties.put("quantity", "567");
		return new DefaultMosaicProperties(properties);
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

		public void incrementSupply(final MosaicId mosaicId, final Supply supply) {
			final MosaicEntry mosaicEntry = this.getMosaicEntry(mosaicId);
			mosaicEntry.increaseSupply(supply);
		}

		public void decrementCreatorBalance(final MosaicId mosaicId, final Address creatorAddress, final Quantity quantity) {
			final MosaicEntry mosaicEntry = this.getMosaicEntry(mosaicId);
			mosaicEntry.getBalances().decrementBalance(creatorAddress, quantity);
		}

		public ValidationResult validate(final MosaicDefinitionCreationTransaction transaction) {
			return this.validator.validate(transaction, new ValidationContext(VALIDATION_HEIGHT, DebitPredicates.Throw));
		}

		private MosaicEntry getMosaicEntry(final MosaicId mosaicId) {
			return  this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId);
		}
	}
}