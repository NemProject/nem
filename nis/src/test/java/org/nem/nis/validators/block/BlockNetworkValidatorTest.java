package org.nem.nis.validators.block;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.nis.test.NisUtils;

public class BlockNetworkValidatorTest {

	@After
	public void resetDefaultNetwork() {
		NetworkInfos.setDefault(null);
	}

	@Test
	public void blockMatchingNetworkPassesValidation() {
		// Arrange: switch to the main network and create a block
		setDefaultNetwork(NetworkInfos.getMainNetworkInfo());
		final Block block = NisUtils.createRandomBlock();

		// Act:
		final ValidationResult result = validate(block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionNotMatchingNetworkDoesNotPassValidation() {
		// Arrange: switch to the main network and create a block
		setDefaultNetwork(NetworkInfos.getMainNetworkInfo());
		final Block block = NisUtils.createRandomBlock();

		// Arrange: switch back to the (test) network
		setDefaultNetwork(NetworkInfos.getTestNetworkInfo());

		// Act:
		final ValidationResult result = validate(block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_WRONG_NETWORK));
	}

	private static void setDefaultNetwork(final NetworkInfo networkInfo) {
		NetworkInfos.setDefault(null);
		NetworkInfos.setDefault(networkInfo);
	}

	private static ValidationResult validate(final Block block) {
		return new BlockNetworkValidator().validate(block);
	}
}
