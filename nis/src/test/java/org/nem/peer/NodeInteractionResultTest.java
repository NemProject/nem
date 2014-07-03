package org.nem.peer;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.ValidationResult;

public class NodeInteractionResultTest {

	@Test
	public void canCreateResultFromValidationResult() {
		// Assert:
		Assert.assertThat(
				NodeInteractionResult.fromValidationResult(ValidationResult.SUCCESS),
				IsEqual.equalTo(NodeInteractionResult.SUCCESS));
		Assert.assertThat(
				NodeInteractionResult.fromValidationResult(ValidationResult.NEUTRAL),
				IsEqual.equalTo(NodeInteractionResult.NEUTRAL));
		Assert.assertThat(
				NodeInteractionResult.fromValidationResult(ValidationResult.FAILURE_UNKNOWN),
				IsEqual.equalTo(NodeInteractionResult.FAILURE));
	}
}