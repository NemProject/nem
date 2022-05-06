package org.nem.nis.validators.integration;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.harvesting.*;
import org.nem.nis.test.*;
import org.nem.nis.ForkConfiguration;

import java.util.*;

public class UnconfirmedTransactionsTransactionValidatorTest extends AbstractTransactionValidationTest {

	@Override
	protected void assertTransactions(final BlockHeight chainHeight, final ReadOnlyNisCache nisCache, final List<Transaction> all,
			List<Transaction> expectedFiltered, final ValidationResult expectedResult) {
		// Arrange:
		final UnconfirmedStateFactory unconfirmedStateFactory = new UnconfirmedStateFactory(NisUtils.createTransactionValidatorFactory(),
				NisUtils.createBlockTransactionObserverFactory()::createExecuteCommitObserver,
				Utils.createMockTimeProvider(CURRENT_TIME.getRawTime()), () -> chainHeight, NisTestConstants.MAX_TRANSACTIONS_PER_BLOCK,
				new ForkConfiguration());
		final UnconfirmedTransactions transactions = new DefaultUnconfirmedTransactions(unconfirmedStateFactory, nisCache);

		expectedFiltered = new ArrayList<>(expectedFiltered);
		for (final Transaction t : all) {
			// Act:
			final ValidationResult result = transactions.addNew(t);

			// Assert:
			if (expectedFiltered.contains(t)) {
				MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
				expectedFiltered.remove(t);
			} else {
				MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
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
	protected boolean allowsIncomplete() {
		return true;
	}
}
