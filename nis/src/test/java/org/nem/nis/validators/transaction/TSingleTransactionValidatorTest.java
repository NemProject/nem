package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.nis.ForkConfiguration;

public class TSingleTransactionValidatorTest {

	@Test
	public void defaultGetNameReturnsTypeName() {
		// Arrange:
		final ForkConfiguration forkConfiguration = new ForkConfiguration.Builder().build();
		final TSingleTransactionValidator<?> validator = new TransferTransactionValidator(forkConfiguration.getRemoteAccountForkHeight(),
				forkConfiguration.getmultisigMOfNForkHeight());

		// Act:
		final String name = validator.getName();

		// Assert:
		MatcherAssert.assertThat(name, IsEqual.equalTo("TransferTransactionValidator"));
	}
}
