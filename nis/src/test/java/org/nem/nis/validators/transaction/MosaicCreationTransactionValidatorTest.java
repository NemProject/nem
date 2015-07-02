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
import org.nem.nis.cache.ReadOnlyMosaicCache;
import org.nem.nis.cache.ReadOnlyNamespaceCache;
import org.nem.nis.test.DebitPredicates;
import org.nem.nis.validators.ValidationContext;

import java.util.Properties;

public class MosaicCreationTransactionValidatorTest {
	private static final Account SIGNER = Utils.generateRandomAccount();
	private static final TimeInstant TIME_INSTANT = new TimeInstant(123);

	//region valid

	@Test
	public void validTransactionValidates()
	{
		// Arrange:
		final TestContext context = new TestContext();
		context.mockNamespaceActive("alice.vouchers");
		context.mockNamespaceOwned("alice.vouchers");
		final MosaicCreationTransaction mosaicCreationTransaction = TestContext.createTransaction();

		// Act:
		final ValidationResult result = context.validate(mosaicCreationTransaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region invalid

	@Test
	public void transactionIsInvalidIfNamespaceIsInactive()
	{
		// Arrange:
		final TestContext context = new TestContext();
		context.mockNamespaceInactive("alice.vouchers");
		final MosaicCreationTransaction mosaicCreationTransaction = TestContext.createTransaction();

		// Act:
		final ValidationResult result = context.validate(mosaicCreationTransaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_EXPIRED));
	}

	@Test
	public void transactionIsInvalidIfNamespaceIsNotOwned()
	{
		// Arrange:
		final TestContext context = new TestContext();
		context.mockNamespaceActive("alice.vouchers");
		context.mockNamespaceNotOwned("alice.vouchers");
		final MosaicCreationTransaction mosaicCreationTransaction = TestContext.createTransaction();

		// Act:
		final ValidationResult result = context.validate(mosaicCreationTransaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT));
	}
	//endregion

	static class TestContext {
		final ReadOnlyNamespaceCache namespaceCache = Mockito.mock(ReadOnlyNamespaceCache.class);
		final ReadOnlyMosaicCache mosaicCache = Mockito.mock(ReadOnlyMosaicCache.class);
		final MosaicCreationTransactionValidator validator = new MosaicCreationTransactionValidator(namespaceCache, mosaicCache);
		final Namespace validNamespace = new Namespace(new NamespaceId("alice.vouchers"), SIGNER, new BlockHeight(21));
		final Namespace notOwnedNamespace = new Namespace(new NamespaceId("alice.vouchers"), Utils.generateRandomAccount(), new BlockHeight(21));

		private static MosaicProperties createProperties() {
			final Properties properties = new Properties();
			properties.put("name", "Alice's gift vouchers");
			properties.put("namespace", "alice.vouchers");
			return new MosaicPropertiesImpl(properties);
		}

		private static Mosaic createMosaic(
				final Account creator,
				final MosaicProperties properties,
				final GenericAmount amount) {
			return new Mosaic(creator, properties, amount);
		}

		private static MosaicCreationTransaction createTransaction() {
			return new MosaicCreationTransaction(TIME_INSTANT, SIGNER, createMosaic(
					SIGNER,
					createProperties(),
					GenericAmount.fromValue(123)));
		}

		public ValidationResult validate(final MosaicCreationTransaction transaction) {
			return this.validator.validate(transaction, new ValidationContext(new BlockHeight(1), DebitPredicates.Throw));
		}

		public void mockNamespaceActive(final String namespaceString) {
			final NamespaceId namespaceId = new NamespaceId(namespaceString);
			Mockito.when(namespaceCache.isActive(Mockito.eq(namespaceId), Mockito.any())).thenReturn(true);
		}

		public void mockNamespaceInactive(final String namespaceString) {
			final NamespaceId namespaceId = new NamespaceId(namespaceString);
			Mockito.when(namespaceCache.isActive(Mockito.eq(namespaceId), Mockito.any())).thenReturn(false);
		}

		public void mockNamespaceOwned(final String namespaceString) {
			final NamespaceId namespaceId = new NamespaceId(namespaceString);
			Mockito.when(namespaceCache.get(Mockito.eq(namespaceId))).thenReturn(validNamespace);
		}

		public void mockNamespaceNotOwned(final String namespaceString) {
			final NamespaceId namespaceId = new NamespaceId(namespaceString);
			Mockito.when(namespaceCache.get(Mockito.eq(namespaceId))).thenReturn(notOwnedNamespace);
		}
	}
}