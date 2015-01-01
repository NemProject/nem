package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.Transaction;
import org.nem.core.model.ValidationResult;
import org.nem.core.model.primitive.Amount;
import org.nem.nis.cache.AccountStateCache;

public class MultisigSignerModificationTransactionValidatorTest {
	@Test
	public void canValidateOtherTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = Mockito.mock(Transaction.class);

		// Act
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	private static class TestContext {
		private final AccountStateCache accountCache = Mockito.mock(AccountStateCache.class);
		private final MultisigSignerModificationTransactionValidator validator;

		private TestContext() {
			this.validator = new MultisigSignerModificationTransactionValidator(this.accountCache);
		}

		private ValidationResult validate(final Transaction transaction) {
			return validator.validate(transaction, new ValidationContext((final Account account, final Amount amount) -> true));
		}
	}
}
