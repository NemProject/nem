package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.nis.test.*;
import org.nem.nis.validators.ValidationContext;

public class TransactionNetworkValidatorTest {

	@After
	public void resetDefaultNetwork() {
		NetworkInfos.setDefault(null);
	}

	@Test
	public void transactionMatchingNetworkPassesValidation() {
		// Arrange: switch to the main network and create a transaction
		NetworkInfos.setDefault(NetworkInfos.getMainNetworkInfo());
		final Transaction transaction = RandomTransactionFactory.createTransfer();

		// Act:
		final ValidationResult result = validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionNotMatchingNetworkDoesNotPassValidation() {
		// Arrange: switch to the main network and create a transaction
		NetworkInfos.setDefault(NetworkInfos.getMainNetworkInfo());
		final Transaction transaction = RandomTransactionFactory.createTransfer();

		// Arrange: switch back to the (test) network
		NetworkInfos.setDefault(null);
		NetworkInfos.setDefault(NetworkInfos.getTestNetworkInfo());

		// Act:
		final ValidationResult result = validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_WRONG_NETWORK));
	}

	private static ValidationResult validate(final Transaction transaction) {
		return new TransactionNetworkValidator().validate(transaction, new ValidationContext(DebitPredicates.Throw));
	}
}