package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.ReadOnlyNamespaceCache;
import org.nem.nis.test.DebitPredicates;
import org.nem.nis.validators.ValidationContext;

public class ProvisionNamespaceTransactionValidatorTest {
	private static final Account LESSOR = ProvisionNamespaceTransactionValidator.LESSOR;
	private static final Amount ROOT_RENTAL_FEE = ProvisionNamespaceTransactionValidator.ROOT_RENTAL_FEE;
	private static final Amount SUBLEVEL_RENTAL_FEE = ProvisionNamespaceTransactionValidator.SUBLEVEL_RENTAL_FEE;

	@Test
	public void validTransactionWithNonNullParentPassesValidator() {
		// Arrange:
		final TestContext context = new TestContext();
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void validTransactionWithNullParentPassesValidator() {
		// Arrange:
		final TestContext context = new TestContext(null, "bar");
		final ProvisionNamespaceTransaction transaction = createTransaction(context);
		Mockito.when(context.namespaceCache.contains(new NamespaceId("bar"))).thenReturn(false);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionDoesNotPassValidatorIfParentNamespaceHasExpired() {
		// Arrange:
		final TestContext context = new TestContext();
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act:
		final ValidationResult result = context.validate(transaction, 123);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_EXPIRED));
	}

	@Test
	public void transactionDoesNotPassValidatorIfParentNamespaceIsUnknown() {
		// Arrange:
		final TestContext context = new TestContext();
		final ProvisionNamespaceTransaction transaction = createTransaction(context);
		Mockito.when(context.namespaceCache.get(context.parent)).thenReturn(null);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_UNKNOWN));
	}

	@Test
	public void transactionDoesNotPassValidatorIfResultingNamespaceAlreadyExistsAndIsARootNamespaceAndNamespaceDoesNotExpireWithinAMonth() {
		// Arrange:
		final TestContext context = new TestContext(null, "foo");
		final ProvisionNamespaceTransaction transaction = createTransaction(context);
		final Namespace namespace = new Namespace(
				context.part.toNamespaceId(),
				context.signer,
				new BlockHeight(100 + 30 * BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY + 1));
		Mockito.when(context.namespaceCache.get(namespace.getId())).thenReturn(namespace);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY));
	}

	@Test
	public void transactionPassesValidatorIfResultingNamespaceAlreadyExistsAndIsRootANamespaceAndNamespaceExpiresWithinAMonth() {
		// Arrange:
		final TestContext context = new TestContext(null, "foo");
		final ProvisionNamespaceTransaction transaction = createTransaction(context);
		final Namespace namespace = new Namespace(
				context.part.toNamespaceId(),
				context.signer,
				new BlockHeight(100 + 30 * BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY));
		Mockito.when(context.namespaceCache.get(namespace.getId())).thenReturn(namespace);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionDoesNotPassValidatorIfResultingNamespaceAlreadyExistsAndIsNotARootNamespace() {
		// Arrange:
		final TestContext context = new TestContext();
		final ProvisionNamespaceTransaction transaction = createTransaction(context);
		final Namespace namespace = new Namespace(context.parent.concat(context.part),	context.signer,	new BlockHeight(100));
		Mockito.when(context.namespaceCache.get(namespace.getId())).thenReturn(namespace);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_ALREADY_EXISTS));
	}

	@Test
	public void transactionDoesNotPassValidatorIfTransactionSignerIsNotParentNamespaceOwner() {
		// Arrange:
		final TestContext context = new TestContext(Utils.generateRandomAccount());
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT));
	}

	@Test
	public void transactionDoesNotPassValidatorIfParentIsNullAndNewPartLengthIsLargerThanMaxRootLength() {
		// Arrange:
		final TestContext context = new TestContext(null, "0123456789abcdefg");
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_INVALID_NAME));
	}

	@Test
	public void transactionDoesNotPassValidatorIfParentIsNonNullAndNewPartLengthIsLargerThanMaxSublevelLength() {
		// Arrange:
		final TestContext context = new TestContext("foo", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0");
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_INVALID_NAME));
	}

	@Test
	public void transactionDoesNotPassValidatorIfLessorIsInvalid() {
		// Arrange:
		final TestContext context = new TestContext(Utils.generateRandomAccount(), SUBLEVEL_RENTAL_FEE);
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_INVALID_LESSOR));
	}

	@Test
	public void transactionDoesNotPassValidatorIfRentalFeeIsInvalidRootFee() {
		// Arrange:
		final TestContext context = new TestContext(LESSOR, ROOT_RENTAL_FEE.subtract(Amount.fromNem(1)), null);
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE));
	}

	@Test
	public void transactionDoesNotPassValidatorIfRentalFeeIsInvalidSublevelFee() {
		// Arrange:
		final TestContext context = new TestContext(LESSOR, SUBLEVEL_RENTAL_FEE.subtract(Amount.fromNem(1)));
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE));
	}

	private static class TestContext {
		private static final Account owner = Utils.generateRandomAccount();
		private final Account signer;
		private final Account lessor;
		private final Amount rentalFee;
		private final NamespaceId parent;
		private final Namespace parentNamespace;
		private final NamespaceIdPart part;
		private final ReadOnlyNamespaceCache namespaceCache = Mockito.mock(ReadOnlyNamespaceCache.class);
		private final TSingleTransactionValidator<ProvisionNamespaceTransaction> validator = new ProvisionNamespaceTransactionValidator(this.namespaceCache);

		private TestContext() {
			this(LESSOR, SUBLEVEL_RENTAL_FEE, "foo", "bar", owner);
		}

		private TestContext(final String parent, final String part) {
			this(LESSOR, null == parent ? ROOT_RENTAL_FEE : SUBLEVEL_RENTAL_FEE, parent, part, owner);
		}

		private TestContext(final Account signer) {
			this(LESSOR, SUBLEVEL_RENTAL_FEE, "foo", "bar", signer);
		}

		private TestContext(final Account lessor, final Amount rentalFee) {
			this(lessor, rentalFee, "foo", "bar", owner);
		}

		private TestContext(final Account lessor, final Amount rentalFee, final String parent) {
			this(lessor, rentalFee, parent, "bar", owner);
		}

		private TestContext(
				final Account lessor,
				final Amount rentalFee,
				final String parent,
				final String part,
				final Account signer) {
			this.lessor = lessor;
			this.rentalFee = rentalFee;
			this.signer = signer;
			this.parent = null == parent ? null : new NamespaceId(parent);
			this.parentNamespace = new Namespace(this.parent, owner, new BlockHeight(123));
			this.part = new NamespaceIdPart(part);
			if (null != this.parent) {
				Mockito.when(this.namespaceCache.get(this.parent)).thenReturn(this.parentNamespace);
				if (part.length() <= NamespaceId.MAX_SUBLEVEL_LENGTH) {
					Mockito.when(this.namespaceCache.contains(this.parent.concat(this.part))).thenReturn(false);
				}
			}
		}

		public ValidationResult validate(final ProvisionNamespaceTransaction transaction, final long height) {
			return this.validator.validate(transaction, new ValidationContext(new BlockHeight(height), DebitPredicates.Throw));
		}

	}

	private static ProvisionNamespaceTransaction createTransaction(final TestContext context) {
		return new ProvisionNamespaceTransaction(
				TimeInstant.ZERO,
				context.signer,
				context.lessor,
				context.rentalFee,
				context.part,
				context.parent);
	}
}
