package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.*;
import org.nem.nis.test.*;
import org.nem.nis.validators.ValidationContext;

import java.util.*;

public class ProvisionNamespaceTransactionValidatorTest {
	private static final int BLOCKS_PER_YEAR = NisTestConstants.ESTIMATED_BLOCKS_PER_YEAR;
	private static final int BLOCKS_PER_MONTH = NisTestConstants.ESTIMATED_BLOCKS_PER_MONTH;
	private static final PublicKey RENTAL_FEE_SINK_PUBLIC_KEY = PublicKey
			.fromHexString("3e82e1c1e4a75adaa3cba8c101c3cd31d9817a2eb966eb3b511fb2ed45b8e262");
	private static final Account RENTAL_FEE_SINK = new Account(Address.fromPublicKey(RENTAL_FEE_SINK_PUBLIC_KEY));
	private static final long ROOT_RENTAL_FEE_BEFORE_FORK = 50000;
	private static final long SUBLEVEL_RENTAL_FEE_BEFORE_FORK = 5000;
	private static final long ROOT_RENTAL_FEE_AFTER_FIRST_FORK = 1500;
	private static final long SUBLEVEL_RENTAL_FEE_AFTER_FIRST_FORK = 200;
	private static final long ROOT_RENTAL_FEE_AFTER_SECOND_FORK = 100;
	private static final long SUBLEVEL_RENTAL_FEE_AFTER_SECOND_FORK = 10;
	private static final long FIRST_FEE_FORK_HEIGHT = BlockMarkerConstants.FEE_FORK(0x98 << 24);
	private static final long SECOND_FEE_FORK_HEIGHT = BlockMarkerConstants.SECOND_FEE_FORK(0x98 << 24);

	// region valid (basic)

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

	// endregion

	// region invalid non-root

	@Test
	public void transactionWithNonRootNamespaceDoesNotPassValidatorIfParentNamespaceIsUnknown() {
		// Arrange: remove the parent from the cache so that it becomes unknown
		final TestContext context = new TestContext("foo", "bar");
		final ProvisionNamespaceTransaction transaction = createTransaction(context);
		context.namespaceCache.remove(context.parent);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_UNKNOWN));
	}

	@Test
	public void transactionWithNonRootNamespaceDoesNotPassValidatorIfRootNamespaceIsNotActive() {
		// Arrange:
		final TestContext context = new TestContext("foo", "bar");
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act: root namespace foo is active from height 50 on, so it is inactive at 40
		final ValidationResult result = context.validate(transaction, 40);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_EXPIRED));
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
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT));
	}

	// endregion

	// region name check

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

	// endregion

	// region reserved root check

	@Test
	public void transactionWithNonClaimableSubNamespaceDoesNotPassValidator() {
		// Assert:
		ReservedNamespaceFilter.getAll().stream().forEach(r -> assertNotClaimable("xyz", r.toString()));
	}

	@Test
	public void transactionWithNonClaimableRootNamespaceDoesNotPassValidator() {
		// Assert:
		ReservedNamespaceFilter.getAll().stream().forEach(r -> assertNotClaimable(null, r.toString()));
	}

	@Test
	public void transactionWithNonClaimableRootNamespaceWithClaimableSubNamespaceDoesNotPassValidator() {
		// Assert:
		ReservedNamespaceFilter.getAll().stream().forEach(r -> assertNotClaimable(r.toString(), "xyz"));
	}

	// endregion

	// region rental fee sink check

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

	// endregion

	// region rental fee check

	@Test
	public void transactionWithNonRootNamespaceDoesNotPassValidatorIfRentalFeeIsLessThanMinimum() {
		// Assert:
		assertRentalFee("foo", "bar", Amount.fromNem(SUBLEVEL_RENTAL_FEE_BEFORE_FORK - 1),
				ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
	}

	@Test
	public void transactionWithNonRootNamespacePassesValidatorIfRentalFeeIsMinimum() {
		// Assert:
		assertRentalFee("foo", "bar", Amount.fromNem(SUBLEVEL_RENTAL_FEE_BEFORE_FORK), ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithNonRootNamespacePassesValidatorIfRentalFeeIsGreaterThanMinimum() {
		// Assert:
		assertRentalFee("foo", "bar", Amount.fromNem(SUBLEVEL_RENTAL_FEE_BEFORE_FORK + 100), ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithRootNamespaceDoesNotPassValidatorIfRentalFeeIsLessThanMinimum() {
		// Assert:
		assertRentalFee(null, "bar", Amount.fromNem(ROOT_RENTAL_FEE_BEFORE_FORK - 1),
				ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
	}

	@Test
	public void transactionWithRootNamespacePassesValidatorIfRentalFeeIsMinimum() {
		// Assert:
		assertRentalFee(null, "bar", Amount.fromNem(ROOT_RENTAL_FEE_BEFORE_FORK), ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithRootNamespacePassesValidatorIfRentalFeeIsGreaterThanMinimum() {
		// Assert:
		assertRentalFee(null, "bar", Amount.fromNem(ROOT_RENTAL_FEE_BEFORE_FORK + 100), ValidationResult.SUCCESS);
	}

	// endregion

	// region renewal -non-root

	@Test
	public void transactionWithNonRootNamespaceCannotBeRenewedDirectly() {
		// Arrange:
		// - set foo and foo.bar to have an effective block height of 100000
		final TestContext context = new TestContext("foo", "bar");
		final ProvisionNamespaceTransaction transaction = createTransaction(context);
		for (final String name : Arrays.asList("foo", "foo.bar")) {
			final Namespace namespace = new Namespace(new NamespaceId(name), TestContext.OWNER, new BlockHeight(100000));
			context.namespaceCache.add(namespace);
		}

		// Act: at the validation height, both should be active and the root should be eligible for renewal
		final ValidationResult result = context.validate(transaction, 100000 + BLOCKS_PER_YEAR - 1);

		// Assert: the non-root part is not renewable
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_ALREADY_EXISTS));
	}

	// endregion

	// region root

	@Test
	public void transactionWithRootNamespaceDoesNotPassValidatorIfResultingNamespaceAlreadyExistsAndDoesNotExpireWithinAMonth() {
		// Assert:
		assertRenewalOfRootWithExpiration(null, BLOCKS_PER_MONTH + 1, ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY);
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
		assertRenewalOfRootWithExpiration(Utils.generateRandomAccount(), BLOCKS_PER_MONTH + 1,
				ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY);
	}

	@Test
	public void transactionWithNewOwnerDoesNotPassValidatorIfResultingNamespaceAlreadyExistsAndExpiresWithinAMonth() {
		// Assert:
		assertRenewalOfRootWithExpiration(Utils.generateRandomAccount(), BLOCKS_PER_MONTH - 1,
				ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY);
		assertRenewalOfRootWithExpiration(Utils.generateRandomAccount(), BLOCKS_PER_MONTH,
				ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY);
	}

	@Test
	public void transactionWithRootNamespaceWithNewOwnerDoesNotPassValidatorIfResultingNamespaceAlreadyExistsAndNamespaceExpiredLessThanAMonthAgo() {
		// Assert:
		assertRenewalOfRootWithExpiration(Utils.generateRandomAccount(), -BLOCKS_PER_MONTH + 1,
				ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY);
	}

	@Test
	public void transactionWithRootNamespaceWithNewOwnerPassesValidatorIfResultingNamespaceAlreadyExistsAndExpiredAtLeastAMonthAgo() {
		// Assert:
		assertRenewalOfRootWithExpiration(Utils.generateRandomAccount(), -BLOCKS_PER_MONTH, ValidationResult.SUCCESS);
		assertRenewalOfRootWithExpiration(Utils.generateRandomAccount(), -BLOCKS_PER_MONTH - 1, ValidationResult.SUCCESS);
	}

	// endregion

	// region helper asserts

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
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	private static void assertInvalidRentalFeeSink(final String parent, final String part) {
		// Arrange:
		final TestContext context = new TestContext(parent, part);
		context.setRentalFeeSink(Utils.generateRandomAccount());
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE_SINK));
	}

	private static void assertRentalFee(final String parent, final String part, final Amount rentalFee,
			final ValidationResult expectedResult) {
		// Arrange:
		final TestContext context = new TestContext(parent, part);
		context.setRentalFee(rentalFee);
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act:
		final ValidationResult result = context.validate(transaction, 100);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	private static void assertRenewalOfRootWithExpiration(final Account signer, final int expirationBlocks,
			final ValidationResult expectedResult) {
		// Arrange:
		final TestContext context = new TestContext(null, "foo");
		if (null != signer) {
			context.setSigner(signer);
		}

		final ProvisionNamespaceTransaction transaction = createTransaction(context);
		final Namespace namespace = new Namespace(new NamespaceId(context.part.toString()), TestContext.OWNER,
				new BlockHeight(100000 + expirationBlocks));
		context.namespaceCache.add(namespace);

		// Act:
		final ValidationResult result = context.validate(transaction, 100000 + BLOCKS_PER_YEAR);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	// endregion

	// region fee forks - first fee fork

	@Test
	public void transactionBeforeFirstForkWithLessThan50kXemFeeForRootNamespaceIsInvalid() {
		// Arrange:
		final Collection<Long> heights = Arrays.asList(1L, 10L, 1000L, 10000L, 100000L, FIRST_FEE_FORK_HEIGHT - 1);
		final Collection<Long> fees = Arrays.asList(0L, 1L, 10L, 100L, 1000L, 10000L, ROOT_RENTAL_FEE_BEFORE_FORK - 1);
		heights.forEach(height -> {
			fees.forEach(fee -> {
				// Assert:
				assertValidationResult(true, Amount.fromNem(fee), new BlockHeight(height),
						ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
			});
		});
	}

	@Test
	public void transactionBeforeFirstForkWithLessThan5kXemFeeForSubNamespaceIsInvalid() {
		// Arrange:
		final Collection<Long> heights = Arrays.asList(1L, 10L, 1000L, 10000L, 100000L, FIRST_FEE_FORK_HEIGHT - 1);
		final Collection<Long> fees = Arrays.asList(0L, 1L, 10L, 100L, 1000L, SUBLEVEL_RENTAL_FEE_BEFORE_FORK - 1);
		heights.forEach(height -> {
			fees.forEach(fee -> {
				// Assert:
				assertValidationResult(false, Amount.fromNem(fee), new BlockHeight(height),
						ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
			});
		});
	}

	@Test
	public void transactionAtFirstForkHeightWithLessThan1500XemFeeForRootNamespaceIsInvalid() {
		// Arrange:
		final Collection<Long> fees = Arrays.asList(0L, 1L, 10L, 100L, 1000L, ROOT_RENTAL_FEE_AFTER_FIRST_FORK - 1);
		fees.forEach(fee -> {
			// Assert:
			assertValidationResult(true, Amount.fromNem(fee), new BlockHeight(FIRST_FEE_FORK_HEIGHT),
					ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
		});
	}

	@Test
	public void transactionAtFirstForkHeightWithLessThan200XemFeeForSubNamespaceIsInvalid() {
		// Arrange:
		final Collection<Long> fees = Arrays.asList(0L, 1L, 10L, 100L, SUBLEVEL_RENTAL_FEE_AFTER_FIRST_FORK - 1);
		fees.forEach(fee -> {
			// Assert:
			assertValidationResult(false, Amount.fromNem(fee), new BlockHeight(FIRST_FEE_FORK_HEIGHT),
					ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
		});
	}

	@Test
	public void transactionAfterFirstForkHeightWithLessThan1500XemFeeForRootNamespaceIsInvalid() {
		final Collection<Long> heights = Arrays.asList(FIRST_FEE_FORK_HEIGHT + 1, FIRST_FEE_FORK_HEIGHT + 10, FIRST_FEE_FORK_HEIGHT + 1000);
		final Collection<Long> fees = Arrays.asList(0L, 1L, 10L, 100L, 1000L, ROOT_RENTAL_FEE_AFTER_FIRST_FORK - 1);
		heights.forEach(height -> {
			fees.forEach(fee -> {
				// Assert:
				assertValidationResult(true, Amount.fromNem(fee), new BlockHeight(height),
						ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
			});
		});
	}

	@Test
	public void transactionAfterFirstForkHeightWithLessThan200XemFeeForSubNamespaceIsInvalid() {
		final Collection<Long> heights = Arrays.asList(FIRST_FEE_FORK_HEIGHT + 1, FIRST_FEE_FORK_HEIGHT + 10, FIRST_FEE_FORK_HEIGHT + 1000);
		final Collection<Long> fees = Arrays.asList(0L, 1L, 10L, 100L, SUBLEVEL_RENTAL_FEE_AFTER_FIRST_FORK - 1);
		heights.forEach(height -> {
			fees.forEach(fee -> {
				// Assert:
				assertValidationResult(false, Amount.fromNem(fee), new BlockHeight(height),
						ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
			});
		});
	}

	@Test
	public void transactionAtFirstForkHeightWithAtLeast1500XemFeeForRootNamespaceIsValid() {
		// Arrange:
		final Collection<Long> fees = Arrays.asList(ROOT_RENTAL_FEE_AFTER_FIRST_FORK, ROOT_RENTAL_FEE_AFTER_FIRST_FORK + 1,
				ROOT_RENTAL_FEE_AFTER_FIRST_FORK + 100, ROOT_RENTAL_FEE_AFTER_FIRST_FORK + 1000, ROOT_RENTAL_FEE_AFTER_FIRST_FORK + 10000);
		fees.forEach(fee -> {
			// Assert:
			assertValidationResult(true, Amount.fromNem(fee), new BlockHeight(FIRST_FEE_FORK_HEIGHT), ValidationResult.SUCCESS);
		});
	}

	@Test
	public void transactionAtFirstForkHeightWithAtLeast200XemFeeForSubNamespaceIsValid() {
		// Arrange:
		final Collection<Long> fees = Arrays.asList(SUBLEVEL_RENTAL_FEE_AFTER_FIRST_FORK, SUBLEVEL_RENTAL_FEE_AFTER_FIRST_FORK + 1,
				SUBLEVEL_RENTAL_FEE_AFTER_FIRST_FORK + 100, SUBLEVEL_RENTAL_FEE_AFTER_FIRST_FORK + 1000,
				SUBLEVEL_RENTAL_FEE_AFTER_FIRST_FORK + 10000);
		fees.forEach(fee -> {
			// Assert:
			assertValidationResult(false, Amount.fromNem(fee), new BlockHeight(FIRST_FEE_FORK_HEIGHT), ValidationResult.SUCCESS);
		});
	}

	@Test
	public void transactionAfterFirstForkHeightWithAtLeast1500XemFeeForRootNamespaceIsValid() {
		final Collection<Long> heights = Arrays.asList(FIRST_FEE_FORK_HEIGHT + 1, FIRST_FEE_FORK_HEIGHT + 10, FIRST_FEE_FORK_HEIGHT + 1000);
		final Collection<Long> fees = Arrays.asList(ROOT_RENTAL_FEE_AFTER_FIRST_FORK, ROOT_RENTAL_FEE_AFTER_FIRST_FORK + 1,
				ROOT_RENTAL_FEE_AFTER_FIRST_FORK + 100, ROOT_RENTAL_FEE_AFTER_FIRST_FORK + 1000, ROOT_RENTAL_FEE_AFTER_FIRST_FORK + 10000);
		heights.forEach(height -> {
			fees.forEach(fee -> {
				// Assert:
				assertValidationResult(true, Amount.fromNem(fee), new BlockHeight(height), ValidationResult.SUCCESS);
			});
		});
	}

	@Test
	public void transactionAfterFirstForkHeightWithAtLeast200XemFeeForSubNamespaceIsValid() {
		final Collection<Long> heights = Arrays.asList(FIRST_FEE_FORK_HEIGHT + 1, FIRST_FEE_FORK_HEIGHT + 10, FIRST_FEE_FORK_HEIGHT + 1000);
		final Collection<Long> fees = Arrays.asList(SUBLEVEL_RENTAL_FEE_AFTER_FIRST_FORK, SUBLEVEL_RENTAL_FEE_AFTER_FIRST_FORK + 1,
				SUBLEVEL_RENTAL_FEE_AFTER_FIRST_FORK + 100, SUBLEVEL_RENTAL_FEE_AFTER_FIRST_FORK + 1000,
				SUBLEVEL_RENTAL_FEE_AFTER_FIRST_FORK + 10000);
		heights.forEach(height -> {
			fees.forEach(fee -> {
				// Assert:
				assertValidationResult(false, Amount.fromNem(fee), new BlockHeight(height), ValidationResult.SUCCESS);
			});
		});
	}

	// endregion

	// region fee forks - second fee fork

	@Test
	public void transactionBeforeSecondForkWithLessThan1500XemFeeForRootNamespaceIsInvalid() {
		// Arrange:
		final Collection<Long> heights = Arrays.asList(1L, 10L, 1000L, 10000L, 900000L, SECOND_FEE_FORK_HEIGHT - 1);
		final Collection<Long> fees = Arrays.asList(0L, 1L, 10L, 100L, 1000L, ROOT_RENTAL_FEE_AFTER_FIRST_FORK - 1);
		heights.forEach(height -> {
			fees.forEach(fee -> {
				// Assert:
				assertValidationResult(true, Amount.fromNem(fee), new BlockHeight(height),
						ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
			});
		});
	}

	@Test
	public void transactionBeforeSecondForkWithLessThan200XemFeeForSubNamespaceIsInvalid() {
		// Arrange:
		final Collection<Long> heights = Arrays.asList(1L, 10L, 1000L, 10000L, 900000L, SECOND_FEE_FORK_HEIGHT - 1);
		final Collection<Long> fees = Arrays.asList(0L, 1L, 10L, 100L, SUBLEVEL_RENTAL_FEE_AFTER_FIRST_FORK - 1);
		heights.forEach(height -> {
			fees.forEach(fee -> {
				// Assert:
				assertValidationResult(false, Amount.fromNem(fee), new BlockHeight(height),
						ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
			});
		});
	}

	@Test
	public void transactionAtSecondForkHeightWithLessThan100XemFeeForRootNamespaceIsInvalid() {
		// Arrange:
		final Collection<Long> fees = Arrays.asList(0L, 1L, 10L, 50L, ROOT_RENTAL_FEE_AFTER_SECOND_FORK - 1);
		fees.forEach(fee -> {
			// Assert:
			assertValidationResult(true, Amount.fromNem(fee), new BlockHeight(SECOND_FEE_FORK_HEIGHT),
					ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
		});
	}

	@Test
	public void transactionAtSecondForkHeightWithLessThan10XemFeeForSubNamespaceIsInvalid() {
		// Arrange:
		final Collection<Long> fees = Arrays.asList(0L, 1L, 5L, SUBLEVEL_RENTAL_FEE_AFTER_SECOND_FORK - 1);
		fees.forEach(fee -> {
			// Assert:
			assertValidationResult(false, Amount.fromNem(fee), new BlockHeight(SECOND_FEE_FORK_HEIGHT),
					ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
		});
	}

	@Test
	public void transactionAfterSecondForkHeightWithLessThan100XemFeeForRootNamespaceIsInvalid() {
		final Collection<Long> heights = Arrays.asList(SECOND_FEE_FORK_HEIGHT + 1, SECOND_FEE_FORK_HEIGHT + 10,
				SECOND_FEE_FORK_HEIGHT + 1000);
		final Collection<Long> fees = Arrays.asList(0L, 1L, 10L, 50L, ROOT_RENTAL_FEE_AFTER_SECOND_FORK - 1);
		heights.forEach(height -> {
			fees.forEach(fee -> {
				// Assert:
				assertValidationResult(true, Amount.fromNem(fee), new BlockHeight(height),
						ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
			});
		});
	}

	@Test
	public void transactionAfterSecondForkHeightWithLessThan10XemFeeForSubNamespaceIsInvalid() {
		final Collection<Long> heights = Arrays.asList(SECOND_FEE_FORK_HEIGHT + 1, SECOND_FEE_FORK_HEIGHT + 10,
				SECOND_FEE_FORK_HEIGHT + 1000);
		final Collection<Long> fees = Arrays.asList(0L, 1L, 5L, SUBLEVEL_RENTAL_FEE_AFTER_SECOND_FORK - 1);
		heights.forEach(height -> {
			fees.forEach(fee -> {
				// Assert:
				assertValidationResult(false, Amount.fromNem(fee), new BlockHeight(height),
						ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE);
			});
		});
	}

	@Test
	public void transactionAtSecondForkHeightWithAtLeast100XemFeeForRootNamespaceIsValid() {
		// Arrange:
		final Collection<Long> fees = Arrays.asList(ROOT_RENTAL_FEE_AFTER_SECOND_FORK, ROOT_RENTAL_FEE_AFTER_SECOND_FORK + 1,
				ROOT_RENTAL_FEE_AFTER_SECOND_FORK + 100, ROOT_RENTAL_FEE_AFTER_SECOND_FORK + 1000,
				ROOT_RENTAL_FEE_AFTER_SECOND_FORK + 10000);
		fees.forEach(fee -> {
			// Assert:
			assertValidationResult(true, Amount.fromNem(fee), new BlockHeight(SECOND_FEE_FORK_HEIGHT), ValidationResult.SUCCESS);
		});
	}

	@Test
	public void transactionAtSecondForkHeightWithAtLeast10XemFeeForSubNamespaceIsValid() {
		// Arrange:
		final Collection<Long> fees = Arrays.asList(SUBLEVEL_RENTAL_FEE_AFTER_SECOND_FORK, SUBLEVEL_RENTAL_FEE_AFTER_SECOND_FORK + 1,
				SUBLEVEL_RENTAL_FEE_AFTER_SECOND_FORK + 100, SUBLEVEL_RENTAL_FEE_AFTER_SECOND_FORK + 1000,
				SUBLEVEL_RENTAL_FEE_AFTER_SECOND_FORK + 10000);
		fees.forEach(fee -> {
			// Assert:
			assertValidationResult(false, Amount.fromNem(fee), new BlockHeight(SECOND_FEE_FORK_HEIGHT), ValidationResult.SUCCESS);
		});
	}

	@Test
	public void transactionAfterSecondForkHeightWithAtLeast100XemFeeForRootNamespaceIsValid() {
		final Collection<Long> heights = Arrays.asList(SECOND_FEE_FORK_HEIGHT + 1, SECOND_FEE_FORK_HEIGHT + 10,
				SECOND_FEE_FORK_HEIGHT + 1000);
		final Collection<Long> fees = Arrays.asList(ROOT_RENTAL_FEE_AFTER_SECOND_FORK, ROOT_RENTAL_FEE_AFTER_SECOND_FORK + 1,
				ROOT_RENTAL_FEE_AFTER_SECOND_FORK + 100, ROOT_RENTAL_FEE_AFTER_SECOND_FORK + 1000,
				ROOT_RENTAL_FEE_AFTER_SECOND_FORK + 10000);
		heights.forEach(height -> {
			fees.forEach(fee -> {
				// Assert:
				assertValidationResult(true, Amount.fromNem(fee), new BlockHeight(height), ValidationResult.SUCCESS);
			});
		});
	}

	@Test
	public void transactionAfterSecondForkHeightWithAtLeast10XemFeeForSubNamespaceIsValid() {
		final Collection<Long> heights = Arrays.asList(SECOND_FEE_FORK_HEIGHT + 1, SECOND_FEE_FORK_HEIGHT + 10,
				SECOND_FEE_FORK_HEIGHT + 1000);
		final Collection<Long> fees = Arrays.asList(SUBLEVEL_RENTAL_FEE_AFTER_SECOND_FORK, SUBLEVEL_RENTAL_FEE_AFTER_SECOND_FORK + 1,
				SUBLEVEL_RENTAL_FEE_AFTER_SECOND_FORK + 100, SUBLEVEL_RENTAL_FEE_AFTER_SECOND_FORK + 1000,
				SUBLEVEL_RENTAL_FEE_AFTER_SECOND_FORK + 10000);
		heights.forEach(height -> {
			fees.forEach(fee -> {
				// Assert:
				assertValidationResult(false, Amount.fromNem(fee), new BlockHeight(height), ValidationResult.SUCCESS);
			});
		});
	}

	// endregion

	private static void assertValidationResult(final boolean isRoot, final Amount fee, final BlockHeight height,
			final ValidationResult expectedResult) {
		// Arrange:
		final TestContext context = new TestContext(isRoot ? null : "foo", "bar", height, fee);
		final ProvisionNamespaceTransaction transaction = createTransaction(context);

		// Act:
		final ValidationResult result = context.validate(transaction, height.getRaw());

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	private static class TestContext {
		private static final Account OWNER = Utils.generateRandomAccount();
		private Account signer;
		private Account rentalFeeSink;
		private Amount rentalFee;
		private final NamespaceId parent;
		private final Namespace parentNamespace;
		private final NamespaceIdPart part;
		private final NamespaceCache namespaceCache = new DefaultNamespaceCache().copy();
		private final TSingleTransactionValidator<ProvisionNamespaceTransaction> validator = new ProvisionNamespaceTransactionValidator(
				this.namespaceCache);

		private TestContext(final String parent, final String part) {
			this(parent, part, new BlockHeight(50),
					null == parent ? Amount.fromNem(ROOT_RENTAL_FEE_BEFORE_FORK) : Amount.fromNem(SUBLEVEL_RENTAL_FEE_BEFORE_FORK));
		}

		private TestContext(final String parent, final String part, final BlockHeight height, final Amount rentalFee) {
			this.rentalFeeSink = RENTAL_FEE_SINK;
			this.rentalFee = rentalFee;
			this.signer = OWNER;
			this.parent = null == parent ? null : new NamespaceId(parent);
			this.parentNamespace = new Namespace(this.parent, OWNER, height);
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
		return new ProvisionNamespaceTransaction(TimeInstant.ZERO, context.signer, context.rentalFeeSink, context.rentalFee, context.part,
				context.parent);
	}
}
