package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.MosaicCreationTransaction;
import org.nem.core.model.ValidationResult;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.mosaic.MosaicProperties;
import org.nem.core.model.mosaic.MosaicPropertiesImpl;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.model.primitive.GenericAmount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.test.DebitPredicates;
import org.nem.nis.validators.ValidationContext;

import java.util.Properties;

public class MosaicCreationTransactionValidatorTest {
	private static final Account SIGNER = Utils.generateRandomAccount();
	private static final BlockHeight VALIDATION_HEIGHT = new BlockHeight(21);

	//region valid

	@Test
	public void validTransactionValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.activateNamespaceAtHeight(SIGNER, VALIDATION_HEIGHT);
		final MosaicCreationTransaction transaction = createTransaction();

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
		final MosaicCreationTransaction transaction = createTransaction();

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
		final MosaicCreationTransaction transaction = createTransaction();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT));
	}

	//endregion

	private static MosaicCreationTransaction createTransaction() {
		final Mosaic mosaic = new Mosaic(
				SIGNER,
				createProperties(),
				GenericAmount.fromValue(123));
		return new MosaicCreationTransaction(TimeInstant.ZERO, SIGNER, mosaic);
	}

	private static MosaicProperties createProperties() {
		final Properties properties = new Properties();
		properties.put("name", "Alice's gift vouchers");
		properties.put("namespace", "alice.vouchers");
		return new MosaicPropertiesImpl(properties);
	}

	private static class TestContext {
		final NamespaceCache namespaceCache = new DefaultNamespaceCache();
		final MosaicCache mosaicCache = new DefaultMosaicCache();
		final MosaicCreationTransactionValidator validator = new MosaicCreationTransactionValidator(this.namespaceCache, this.mosaicCache);

		public void activateNamespaceAtHeight(final Account signer, final BlockHeight height) {
			this.namespaceCache.add(new Namespace(new NamespaceId("alice"), signer, height));
			this.namespaceCache.add(new Namespace(new NamespaceId("alice.vouchers"), signer, height));
		}

		public ValidationResult validate(final MosaicCreationTransaction transaction) {
			return this.validator.validate(transaction, new ValidationContext(VALIDATION_HEIGHT, DebitPredicates.Throw));
		}
	}
}