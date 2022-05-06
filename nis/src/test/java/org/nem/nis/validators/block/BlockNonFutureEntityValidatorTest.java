package org.nem.nis.validators.block;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.BlockValidator;

public class BlockNonFutureEntityValidatorTest {

	@Test
	public void blockWithTimeStampLessThanFutureThresholdIsValid() {
		// Assert:
		this.assertBlockValidationResult(11, 20, ValidationResult.SUCCESS);
	}

	@Test
	public void blockWithTimeStampEqualToFutureThresholdIsValid() {
		// Assert:
		this.assertBlockValidationResult(11, 21, ValidationResult.SUCCESS);
	}

	@Test
	public void blockWithTimeStampGreaterThanFutureThresholdIsNotValid() {
		// Assert:
		this.assertBlockValidationResult(11, 22, ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE);
	}

	private void assertBlockValidationResult(final int currentTime, final int entityTime, final ValidationResult expectedResult) {
		// Arrange:
		final Block block = NisUtils.createRandomBlockWithTimeStamp(entityTime);
		final BlockValidator validator = createValidator(currentTime);

		// Act:
		final ValidationResult result = validator.validate(block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	private static BlockValidator createValidator(final int currentTime) {
		return new BlockNonFutureEntityValidator(Utils.createMockTimeProvider(currentTime));
	}
}
