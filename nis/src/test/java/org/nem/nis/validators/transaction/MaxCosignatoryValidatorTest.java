package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.DebitPredicates;
import org.nem.nis.validators.ValidationContext;

import java.util.*;

public class MaxCosignatoryValidatorTest {
	private static final int MAX_COSIGNERS = BlockChainConstants.MAX_ALLOWED_COSIGNATORIES_PER_ACCOUNT;

	//region new multisig account

	@Test
	public void modificationThatIncreasesNewMultisigAccountCosignersToLessThanMaxValidates() {
		// Assert:
		assertNewMultisigAccountModificationValidation(15, ValidationResult.SUCCESS);
	}

	@Test
	public void modificationThatIncreasesNewMultisigAccountCosignersToMaxValidates() {
		// Assert:
		assertNewMultisigAccountModificationValidation(MAX_COSIGNERS, ValidationResult.SUCCESS);
	}

	@Test
	public void modificationThatIncreasesNewMultisigAccountCosignersToGreaterThanMaxDoesNotValidate() {
		// Assert:
		assertNewMultisigAccountModificationValidation(MAX_COSIGNERS + 1, ValidationResult.FAILURE_TOO_MANY_MULTISIG_COSIGNERS);
	}

	private static void assertNewMultisigAccountModificationValidation(final int numCosigners, final ValidationResult expectedResult) {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigAggregateModificationTransaction transaction = context.createModificationTransaction(numCosigners, 0);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	//endregion

	//region existing multisig account

	@Test
	public void modificationThatIncreasesExistingMultisigAccountCosignersToLessThanMaxValidates() {
		// Assert:
		assertExistingMultisigAccountModificationValidation(15, ValidationResult.SUCCESS);
	}

	@Test
	public void modificationThatIncreasesExistingMultisigAccountCosignersToMaxValidates() {
		// Assert:
		assertExistingMultisigAccountModificationValidation(MAX_COSIGNERS, ValidationResult.SUCCESS);
	}

	@Test
	public void modificationThatIncreasesExistingMultisigAccountCosignersToGreaterThanMaxDoesNotValidate() {
		// Assert:
		assertExistingMultisigAccountModificationValidation(MAX_COSIGNERS + 1, ValidationResult.FAILURE_TOO_MANY_MULTISIG_COSIGNERS);
	}

	private static void assertExistingMultisigAccountModificationValidation(final int numCosigners, final ValidationResult expectedResult) {
		// Arrange:
		final TestContext context = new TestContext();
		context.addNumCosigners(10);
		final MultisigAggregateModificationTransaction transaction = context.createModificationTransaction(numCosigners - 10, 0);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	//endregion

	//region existing (adds and deletes)

	@Test
	public void modificationThatReplacesExistingMultisigAccountCosignersAtMaxValidates() {
		// Assert:
		assertExistingMultisigAccountModificationAddDeleteValidation(5, 5, ValidationResult.SUCCESS);
	}

	@Test
	public void modificationThatDeletesExistingMultisigAccountCosignersAtMaxValidates() {
		// Assert:
		assertExistingMultisigAccountModificationAddDeleteValidation(0, 5, ValidationResult.SUCCESS);
	}

	@Test
	public void modificationThatNetAddsExistingMultisigAccountCosignersAtMaxDoesNot() {
		// Assert:
		assertExistingMultisigAccountModificationAddDeleteValidation(6, 5, ValidationResult.FAILURE_TOO_MANY_MULTISIG_COSIGNERS);
	}

	@Test
	public void modificationThatNetDeletesExistingMultisigAccountCosignersAtMaxDoesNot() {
		// Assert:
		assertExistingMultisigAccountModificationAddDeleteValidation(5, 6, ValidationResult.SUCCESS);
	}

	private static void assertExistingMultisigAccountModificationAddDeleteValidation(final int numAdds, final int numDeletes, final ValidationResult expectedResult) {
		// Arrange:
		final TestContext context = new TestContext();
		context.addNumCosigners(MAX_COSIGNERS);
		final MultisigAggregateModificationTransaction transaction = context.createModificationTransaction(numAdds, numDeletes);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	//endregion

	private static class TestContext {
		private final ReadOnlyAccountStateCache accountStateCache = Mockito.mock(ReadOnlyAccountStateCache.class);
		private final TSingleTransactionValidator<MultisigAggregateModificationTransaction> validator = new MaxCosignatoryValidator(this.accountStateCache);
		private final Account multisig = Utils.generateRandomAccount();
		private final AccountState multisigAccountState = new AccountState(this.multisig.getAddress());

		public TestContext() {
			Mockito.when(this.accountStateCache.findStateByAddress(this.multisig.getAddress())).thenReturn(multisigAccountState);
		}

		public ValidationResult validate(final MultisigAggregateModificationTransaction transaction) {
			return this.validator.validate(transaction, new ValidationContext(DebitPredicates.Throw));
		}

		public void addNumCosigners(final int numCosigners) {
			for (int i = 0; i < numCosigners; ++i) {
				this.multisigAccountState.getMultisigLinks().addCosignatory(Utils.generateRandomAddress());
			}
		}

		public MultisigAggregateModificationTransaction createModificationTransaction(final int numAdds, final int numDeletes) {
			final List<MultisigCosignatoryModification> modifications = new ArrayList<>();
			for (int i = 0; i < numAdds; ++i) {
				modifications.add(new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, Utils.generateRandomAccount()));
			}

			for (int i = 0; i < numDeletes; ++i) {
				modifications.add(new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, Utils.generateRandomAccount()));
			}

			return new MultisigAggregateModificationTransaction(TimeInstant.ZERO, this.multisig, modifications);
		}
	}
}