package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.RandomTransactionFactory;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.ValidationContext;

public class TransactionNetworkValidatorTest {

	@After
	public void resetDefaultNetwork() {
		NetworkInfos.setDefault(null);
	}

	@Test
	public void transactionMatchingNetworkPassesValidation() {
		// Arrange: switch to the main network and create a transaction
		setDefaultNetwork(NetworkInfos.getMainNetworkInfo());
		final Transaction transaction = RandomTransactionFactory.createTransfer();

		// Act:
		final ValidationResult result = validate(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionNotMatchingNetworkDoesNotPassValidation() {
		// Arrange: switch to the main network and create a transaction
		setDefaultNetwork(NetworkInfos.getMainNetworkInfo());
		final Transaction transaction = RandomTransactionFactory.createTransfer();

		// Arrange: switch back to the (test) network
		setDefaultNetwork(NetworkInfos.getTestNetworkInfo());

		// Act:
		final ValidationResult result = validate(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_WRONG_NETWORK));
	}

	private static void setDefaultNetwork(final NetworkInfo networkInfo) {
		NetworkInfos.setDefault(null);
		NetworkInfos.setDefault(networkInfo);
	}

	private static ValidationResult validate(final Transaction transaction) {
		return new TransactionNetworkValidator().validate(transaction, new ValidationContext(ValidationStates.Throw));
	}
}
