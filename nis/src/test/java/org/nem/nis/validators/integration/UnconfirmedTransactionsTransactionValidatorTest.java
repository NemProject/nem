package org.nem.nis.validators.integration;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.test.NisUtils;

import java.util.*;

public class UnconfirmedTransactionsTransactionValidatorTest extends AbstractTransactionValidationTest {

	@Override
	protected void assertTransactions(
			final ReadOnlyNisCache nisCache,
			final List<Transaction> all,
			List<Transaction> expectedFiltered,
			final ValidationResult expectedResult) {
		// Arrange:
		final UnconfirmedTransactions transactions = new UnconfirmedTransactions(
				NisUtils.createTransactionValidatorFactory(),
				nisCache,
				Utils.createMockTimeProvider(CURRENT_TIME.getRawTime()),
				() -> BlockHeight.MAX.prev());

		expectedFiltered = new ArrayList<>(expectedFiltered);
		for (final Transaction t : all) {
			// Act:
			final ValidationResult result = transactions.addNew(t);

			// Assert:
			if (expectedFiltered.contains(t)) {
				Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
				expectedFiltered.remove(t);
			} else {
				Assert.assertThat(result, IsEqual.equalTo(expectedResult));
			}
		}
	}

	@Override
	protected boolean isSingleBlockUsed() {
		return false;
	}

	@Override
	protected ValidationResult getHashConflictResult() {
		return ValidationResult.NEUTRAL;
	}

	@Override
	protected boolean allowsConflicting() {
		return true;
	}

	@Override
	protected boolean allowsIncomplete() {
		return true;
	}
}
