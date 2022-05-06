package org.nem.peer;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.ValidationResult;

import java.util.*;

public class NodeInteractionResultTest {

	@Test
	@SuppressWarnings("serial")
	public void canCreateResultFromValidationResult() {
		// Arrange:
		final Map<ValidationResult, NodeInteractionResult> expectedMappings = new HashMap<ValidationResult, NodeInteractionResult>() {
			{
				this.put(ValidationResult.SUCCESS, NodeInteractionResult.SUCCESS);
				this.put(ValidationResult.NEUTRAL, NodeInteractionResult.NEUTRAL);
				this.put(ValidationResult.FAILURE_ENTITY_UNUSABLE_OUT_OF_SYNC, NodeInteractionResult.NEUTRAL);
				this.put(ValidationResult.FAILURE_TRANSACTION_CACHE_TOO_FULL, NodeInteractionResult.NEUTRAL);
			}
		};

		for (final ValidationResult validationResult : ValidationResult.values()) {
			// Act:
			final NodeInteractionResult result = NodeInteractionResult.fromValidationResult(validationResult);

			// Assert:
			final NodeInteractionResult expectedResult = expectedMappings.getOrDefault(validationResult, NodeInteractionResult.FAILURE);
			MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
		}
	}
}
