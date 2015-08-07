package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.ValidationContext;

import java.util.Arrays;

public class ProvisionNamespaceTransactionValidatorTest {
	private static final int BLOCKS_PER_YEAR = BlockChainConstants.ESTIMATED_BLOCKS_PER_YEAR;
	private static final int BLOCKS_PER_MONTH = BlockChainConstants.ESTIMATED_BLOCKS_PER_MONTH;
	private static final PublicKey RENTAL_FEE_SINK_PUBLIC_KEY = PublicKey.fromHexString("3e82e1c1e4a75adaa3cba8c101c3cd31d9817a2eb966eb3b511fb2ed45b8e262");
	private static final Account RENTAL_FEE_SINK = new Account(Address.fromPublicKey(RENTAL_FEE_SINK_PUBLIC_KEY));
	private static final Amount ROOT_RENTAL_FEE = Amount.fromNem(50000);
	private static final Amount SUBLEVEL_RENTAL_FEE = Amount.fromNem(5000);

	//region valid (basic)

	@Test
	public void validTransactionWithNonRootNamespacePassesValidator() {
		// Assert:
		assertValid("foo", "bar");
	}

	@Test
	public void validTransactionWithRootNamespacePassesValidator() {
		// Assert:
		assertValid(null, "bar");
	}

	//endregion

	//region invalid non-root

	@Test
	public void transactionWithNonRootNamespaceDoesNotPassValidatorIfParentNamespaceIsUnknown() {
		// Arrange: remove the parent from the cache so that it becomes unknown
		final TestContext context = new TestContext("foo", "bar");
		final ProvisionNamespaceTransaction transaction = createTransaction(context);
		context.namespaceCache.remove(context.parent);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_UNKNOWN));
	}

	@Test
	public void transactionWithNonRootNamespaceDoesNotPassValidatorIfRootNamespaceIsNotActive() {
		// Arrange:
		final TestContext context = new TestContext("foo", "bar");
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act: root namespace foo is active from height 50 on, so it is inactive at 40
		final ValidationResult result = context.validate(transaction, 40);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_EXPIRED));
	}

	@Test
	public void transactionWithNonRootNamespaceDoesNotPassValidatorIfTransactionSignerIsNotParentNamespaceOwner() {
		// Arrange:
		final TestContext context = new TestContext("foo", "bar");
		context.setSigner(Utils.generateRandomAccount());
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT));
	}

	//endregion

	//region name check

	@Test
	public void transactionWithNonRootNamespaceDoesNotPassValidatorIfNewPartLengthIsLargerThanMaxSublevelLength() {
		// Assert:
		assertInvalidName("foo", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0");
	}

	@Test
	public void transactionWithNonRootNamespacePassesValidatorIfNewPartLengthIsEqualToMaxSublevelLength() {
		// Assert:
		assertValid("foo", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
	}

	@Test
	public void transactionWithRootNamespaceDoesNotPassValidatorIfNewPartLengthIsLargerThanMaxRootLength() {
		// Assert:
		assertInvalidName(null, "0123456789abcdefg");
	}

	@Test
	public void transactionWithRootNamespacePassesValidatorIfNewPartLengthIsEqualToMaxRootLength() {
		// Assert:
		assertValid(null, "0123456789abcdef");
	}

	//endregion

	//region reserved root check

	@Test
	public void transactionWithNonClaimableSubNamespaceDoesNotPassValidator() {
		// Assert:
		ReservedNamespaceFilter.getAll().stream().forEach(r -> assertNotClaimable("xyz", r.toString()));
	}

	@Test
	public void transactionWithNonClaimableRootNamespaceDoesNotPassValidator() {
		// Assert:
		ReservedNamespaceFilter.getAll().stream()
				.forEach(r -> assertNotClaimable(null, r.toString()));
	}

	@Test
	public void transactionWithNonClaimableRootNamespaceWithClaimableSubNamespaceDoesNotPassValidator() {
		// Assert:
		ReservedNamespaceFilter.getAll().stream()
				.forEach(r -> assertNotClaimable(r.toString(), "xyz"));
	}

	//endregion

	//region rental fee sink check

	@Test
	public void transactionWithNonRootNamespaceDoesNotPassValidatorIfRentalFeeSinkIsInvalid() {
		// Assert:
		assertInvalidRentalFeeSink("foo", "bar");
	}

	@Test
	public void transactionWithRootNamespaceDoesNotPassValidatorIfRentalFeeSinkIsInvalid() {
		// Assert:
		assertInvalidRentalFeeSink(null, "bar");
	}

	//endregion

	//region rental fee check

	@Test
	public void transactionWithNonRootNamespaceDoesNotPassValidatorIfRentalFeeIsLessThanMinimum() {
		// Assert:
		assertRentalFee("foo", "bar", SUBLEVEL_RENTAL_FEE.subtract(Amount.fromNem(1)), ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
	}

	@Test
	public void transactionWithNonRootNamespacePassesValidatorIfRentalFeeIsMinimum() {
		// Assert:
		assertRentalFee("foo", "bar", SUBLEVEL_RENTAL_FEE, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithNonRootNamespacePassesValidatorIfRentalFeeIsGreaterThanMinimum() {
		// Assert:
		assertRentalFee("foo", "bar", SUBLEVEL_RENTAL_FEE.add(Amount.fromNem(100)), ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithRootNamespaceDoesNotPassValidatorIfRentalFeeIsLessThanMinimum() {
		// Assert:
		assertRentalFee(null, "bar", ROOT_RENTAL_FEE.subtract(Amount.fromNem(1)), ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
	}

	@Test
	public void transactionWithRootNamespacePassesValidatorIfRentalFeeIsMinimum() {
		// Assert:
		assertRentalFee(null, "bar", ROOT_RENTAL_FEE, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithRootNamespacePassesValidatorIfRentalFeeIsGreaterThanMinimum() {
		// Assert:
		assertRentalFee(null, "bar", ROOT_RENTAL_FEE.add(Amount.fromNem(100)), ValidationResult.SUCCESS);
	}

	//endregion

	//region renewal

	//region non-root

	@Test
	public void transactionWithNonRootNamespaceCannotBeRenewedDirectly() {
		// Arrange:
		// - set foo and foo.bar to have an effective block height of 100000
		final TestContext context = new TestContext("foo", "bar");
		final ProvisionNamespaceTransaction transaction = createTransaction(context);
		for (final String name : Arrays.asList("foo", "foo.bar")) {
			final Namespace namespace = new Namespace(
					new NamespaceId(name),
					TestContext.OWNER,
					new BlockHeight(100000));
			context.namespaceCache.add(namespace);
		}

		// Act: at the validation height, both should be active and the root should be eligible for renewal
		final ValidationResult result = context.validate(transaction, 100000 + BLOCKS_PER_YEAR - 1);

		// Assert: the non-root part is not renewable
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_ALREADY_EXISTS));
	}

	//endregion

	//region root

	@Test
	public void transactionWithRootNamespaceDoesNotPassValidatorIfResultingNamespaceAlreadyExistsAndDoesNotExpireWithinAMonth() {
		// Assert:
		assertRenewalOfRootWithExpiration(
				null,
				BLOCKS_PER_MONTH + 1,
				ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY);
	}

	@Test
	public void transactionWithRootNamespacePassesValidatorIfResultingNamespaceAlreadyExistsAndNamespaceExpiresWithinAMonth() {
		// Assert:
		assertRenewalOfRootWithExpiration(null, BLOCKS_PER_MONTH - 1, ValidationResult.SUCCESS);
		assertRenewalOfRootWithExpiration(null, BLOCKS_PER_MONTH, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithRootNamespacePassesValidatorIfResultingNamespaceAlreadyExistsAndNamespaceExpired() {
		// Assert:
		assertRenewalOfRootWithExpiration(null, -1, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithNewOwnerDoesNotPassValidatorIfResultingNamespaceAlreadyExistsAndDoesNotExpireWithinAMonth() {
		// Assert:
		assertRenewalOfRootWithExpiration(
				Utils.generateRandomAccount(),
				BLOCKS_PER_MONTH + 1,
				ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY);
	}

	@Test
	public void transactionWithNewOwnerDoesNotPassValidatorIfResultingNamespaceAlreadyExistsAndExpiresWithinAMonth() {
		// Assert:
		assertRenewalOfRootWithExpiration(
				Utils.generateRandomAccount(),
				BLOCKS_PER_MONTH - 1,
				ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY);
		assertRenewalOfRootWithExpiration(
				Utils.generateRandomAccount(),
				BLOCKS_PER_MONTH,
				ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY);
	}

	@Test
	public void transactionWithRootNamespaceWithNewOwnerDoesNotPassValidatorIfResultingNamespaceAlreadyExistsAndNamespaceExpiredLessThanAMonthAgo() {
		// Assert:
		assertRenewalOfRootWithExpiration(
				Utils.generateRandomAccount(),
				-BLOCKS_PER_MONTH + 1,
				ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY);
	}

	@Test
	public void transactionWithRootNamespaceWithNewOwnerPassesValidatorIfResultingNamespaceAlreadyExistsAndExpiredAtLeastAMonthAgo() {
		// Assert:
		assertRenewalOfRootWithExpiration(Utils.generateRandomAccount(), -BLOCKS_PER_MONTH, ValidationResult.SUCCESS);
		assertRenewalOfRootWithExpiration(Utils.generateRandomAccount(), -BLOCKS_PER_MONTH - 1, ValidationResult.SUCCESS);
	}

	//endregion

	//endregion

	//region helper asserts

	private static void assertValid(final String parent, final String part) {
		// Assert:
		assertValidationResult(parent, part, ValidationResult.SUCCESS);
	}

	private static void assertInvalidName(final String parent, final String part) {
		// Assert:
		assertValidationResult(parent, part, ValidationResult.FAILURE_NAMESPACE_INVALID_NAME);
	}

	private static void assertNotClaimable(final String parent, final String part) {
		// Assert:
		assertValidationResult(parent, part, ValidationResult.FAILURE_NAMESPACE_NOT_CLAIMABLE);
	}

	private static void assertValidationResult(final String parent, final String part, final ValidationResult expectedResult) {
		// Arrange:
		final TestContext context = new TestContext(parent, part);
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	private static void assertInvalidRentalFeeSink(final String parent, final String part) {
		// Arrange:
		final TestContext context = new TestContext(parent, part);
		context.setRentalFeeSink(Utils.generateRandomAccount());
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE_SINK));
	}

	private static void assertRentalFee(final String parent, final String part, final Amount rentalFee, final ValidationResult expectedResult) {
		// Arrange:
		final TestContext context = new TestContext(parent, part);
		context.setRentalFee(rentalFee);
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	private static void assertRenewalOfRootWithExpiration(
			final Account signer,
			final int expirationBlocks,
			final ValidationResult expectedResult) {
		// Arrange:
		final TestContext context = new TestContext(null, "foo");
		if (null != signer) {
			context.setSigner(signer);
		}

		final ProvisionNamespaceTransaction transaction = createTransaction(context);
		final Namespace namespace = new Namespace(
				new NamespaceId(context.part.toString()),
				TestContext.OWNER,
				new BlockHeight(100000 + expirationBlocks));
		context.namespaceCache.add(namespace);

		// Act:
		final ValidationResult result = context.validate(transaction, 100000 + BLOCKS_PER_YEAR);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	//endregion

	private static class TestContext {
		private static final Account OWNER = Utils.generateRandomAccount();
		private Account signer;
		private Account rentalFeeSink;
		private Amount rentalFee;
		private final NamespaceId parent;
		private final Namespace parentNamespace;
		private final NamespaceIdPart part;
		private final NamespaceCache namespaceCache = new DefaultNamespaceCache();
		private final TSingleTransactionValidator<ProvisionNamespaceTransaction> validator = new ProvisionNamespaceTransactionValidator(this.namespaceCache);

		private TestContext(final String parent, final String part) {
			this.rentalFeeSink = RENTAL_FEE_SINK;
			this.rentalFee = null == parent ? ROOT_RENTAL_FEE : SUBLEVEL_RENTAL_FEE;
			this.signer = OWNER;
			this.parent = null == parent ? null : new NamespaceId(parent);
			this.parentNamespace = new Namespace(this.parent, OWNER, new BlockHeight(50));
			this.part = new NamespaceIdPart(part);
			if (null != this.parent) {
				this.namespaceCache.add(this.parentNamespace);
			}
		}

		public void setSigner(final Account account) {
			this.signer = account;
		}

		public void setRentalFeeSink(final Account account) {
			this.rentalFeeSink = account;
		}

		public void setRentalFee(final Amount amount) {
			this.rentalFee = amount;
		}

		public ValidationResult validate(final ProvisionNamespaceTransaction transaction, final long height) {
			return this.validator.validate(transaction, new ValidationContext(new BlockHeight(height), ValidationStates.Throw));
		}
	}

	private static ProvisionNamespaceTransaction createTransaction(final TestContext context) {
		return new ProvisionNamespaceTransaction(
				TimeInstant.ZERO,
				context.signer,
				context.rentalFeeSink,
				context.rentalFee,
				context.part,
				context.parent);
	}
}
