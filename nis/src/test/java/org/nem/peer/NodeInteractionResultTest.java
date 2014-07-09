package org.nem.peer;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.ValidationResult;
import org.nem.nis.sync.ComparisonResult;

import java.util.*;

public class NodeInteractionResultTest {

	@Test
	public void canCreateResultFromValidationResult() {
		// Arrange:
		final Map<ValidationResult, NodeInteractionResult> expectedMappings = new HashMap<ValidationResult, NodeInteractionResult>() {
			{
				put(ValidationResult.SUCCESS, NodeInteractionResult.SUCCESS);
				put(ValidationResult.NEUTRAL, NodeInteractionResult.NEUTRAL);
				put(ValidationResult.FAILURE_ENTITY_UNUSABLE, NodeInteractionResult.NEUTRAL);
			}
		};

		for (final ValidationResult validationResult : ValidationResult.values()) {
			// Act:
			final NodeInteractionResult result = NodeInteractionResult.fromValidationResult(validationResult);

			// Assert:
			final NodeInteractionResult expectedResult = expectedMappings.getOrDefault(
					validationResult,
					NodeInteractionResult.FAILURE);
			Assert.assertThat(result, IsEqual.equalTo(expectedResult));
		}
	}

	@Test
	public void canCreateResultFromComparisonResultCode() {
		// Arrange:
		final Set<ComparisonResult.Code> neutralCodes = new HashSet<>(Arrays.asList(
				ComparisonResult.Code.REMOTE_IS_SYNCED,
				ComparisonResult.Code.REMOTE_IS_TOO_FAR_BEHIND,
				ComparisonResult.Code.REMOTE_REPORTED_EQUAL_CHAIN_SCORE,
				ComparisonResult.Code.REMOTE_REPORTED_LOWER_CHAIN_SCORE
		));

		for (final ComparisonResult.Code code : ComparisonResult.Code.values()) {
			// Act:
			final NodeInteractionResult result = NodeInteractionResult.fromComparisonResultCode(code);

			// Assert:
			final NodeInteractionResult expectedResult = neutralCodes.contains(code)
					? NodeInteractionResult.NEUTRAL
					: NodeInteractionResult.FAILURE;
			Assert.assertThat(result, IsEqual.equalTo(expectedResult));
		}
	}
}