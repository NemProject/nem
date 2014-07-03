package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.ValidationResult;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class NisRequestResultTest {

	@Test
	public void canCreateResultAroundExplicitFields() {
		// Arrange + Act:
		final NisRequestResult result = new NisRequestResult(42, 1337, "Neutral result");

		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(42));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(1337));
		Assert.assertThat(result.getMessage(), IsEqual.equalTo("Neutral result"));
	}

	@Test
	public void canCreateResultAroundValidationResult() {
		// Arrange + Act:
		final NisRequestResult result = new NisRequestResult(ValidationResult.FAILURE_CHAIN_INVALID);

		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(NisRequestResult.TYPE_VALIDATION_RESULT));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(ValidationResult.FAILURE_CHAIN_INVALID.getValue()));
		Assert.assertThat(result.getMessage(), IsEqual.equalTo("FAILURE_CHAIN_INVALID"));
	}

	@Test
	public void isErrorReturnsFalseForNeutralAndSuccessCode() {
		// Arrange + Act:
		final NisRequestResult result = new NisRequestResult(
				NisRequestResult.TYPE_VALIDATION_RESULT,
				NisRequestResult.CODE_NEUTRAL,
				"Neutral result");
		final NisRequestResult result2 = new NisRequestResult(
				NisRequestResult.TYPE_VALIDATION_RESULT,
				NisRequestResult.CODE_SUCCESS,
				"Neutral result");

		// Assert:
		Assert.assertThat(result.isError(), IsEqual.equalTo(false));
		Assert.assertThat(result2.isError(), IsEqual.equalTo(false));
	}

	@Test
	public void isErrorReturnsTrueForCodeOtherThanNeutralOrSuccess() {
		// Arrange + Act:
		final NisRequestResult result = new NisRequestResult(
				NisRequestResult.TYPE_VALIDATION_RESULT,
				2,
				"Neutral result");

		// Assert:
		Assert.assertThat(result.isError(), IsEqual.equalTo(true));
	}

	@Test
	public void canRoundTripNisRequestResult() {
		// Arrange + Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				new NisRequestResult(42, 1337, "Neutral result"),
				null);
		final NisRequestResult result = new NisRequestResult(deserializer);

		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(42));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(1337));
		Assert.assertThat(result.getMessage(), IsEqual.equalTo("Neutral result"));
	}
}
