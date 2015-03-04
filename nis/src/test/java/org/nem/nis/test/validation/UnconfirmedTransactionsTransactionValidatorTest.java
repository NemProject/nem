package org.nem.nis.test.validation;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.test.NisUtils;

import java.util.List;

public class UnconfirmedTransactionsTransactionValidatorTest extends AbstractTransactionValidationTest {

	@Override
	protected void assertTransactions(
			final ReadOnlyNisCache nisCache,
			final List<Transaction> all,
			final List<Transaction> expectedFiltered,
			final ValidationResult expectedResult) {
		// Arrange:
		final UnconfirmedTransactions transactions = new UnconfirmedTransactions(
				NisUtils.createTransactionValidatorFactory(),
				nisCache,
				Utils.createMockTimeProvider(CURRENT_TIME.getRawTime()));

		for (final Transaction t : all) {
			// Act:
			final ValidationResult result = transactions.addNew(t);

			// Assert:
			if (expectedFiltered.contains(t)) {
				Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			} else {
				Assert.assertThat(result, IsEqual.equalTo(expectedResult));
			}
		}
	}

	@Override
	protected boolean isSingleBlockUsed() {
		return false;
	}
}
