package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.ValidationResult;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class NemRequestResultTest {

	@Test
	public void canCreateResultAroundExplicitFields() {
		// Arrange + Act:
		final NemRequestResult result = new NemRequestResult(42, 1337, "Neutral result");

		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(42));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(1337));
		Assert.assertThat(result.getMessage(), IsEqual.equalTo("Neutral result"));
	}

	@Test
	public void canCreateResultAroundValidationResult() {
		// Arrange + Act:
		final NemRequestResult result = new NemRequestResult(ValidationResult.FAILURE_CHAIN_INVALID);

		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(NemRequestResult.TYPE_VALIDATION_RESULT));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(ValidationResult.FAILURE_CHAIN_INVALID.getValue()));
		Assert.assertThat(result.getMessage(), IsEqual.equalTo("FAILURE_CHAIN_INVALID"));
	}

	@Test
	public void isErrorReturnsFalseForNeutralAndSuccessCode() {
		// Arrange + Act:
		final NemRequestResult result = new NemRequestResult(
				NemRequestResult.TYPE_VALIDATION_RESULT,
				NemRequestResult.CODE_NEUTRAL,
				"Neutral result");
		final NemRequestResult result2 = new NemRequestResult(
				NemRequestResult.TYPE_VALIDATION_RESULT,
				NemRequestResult.CODE_SUCCESS,
				"Neutral result");

		// Assert:
		Assert.assertThat(result.isError(), IsEqual.equalTo(false));
		Assert.assertThat(result2.isError(), IsEqual.equalTo(false));
	}

	@Test
	public void isErrorReturnsTrueForCodeOtherThanNeutralOrSuccess() {
		// Arrange + Act:
		final NemRequestResult result = new NemRequestResult(
				NemRequestResult.TYPE_VALIDATION_RESULT,
				2,
				"Neutral result");

		// Assert:
		Assert.assertThat(result.isError(), IsEqual.equalTo(true));
	}

	@Test
	public void canRoundTripNisRequestResult() {
		// Arrange + Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				new NemRequestResult(42, 1337, "Neutral result"),
				null);
		final NemRequestResult result = new NemRequestResult(deserializer);

		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(42));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(1337));
		Assert.assertThat(result.getMessage(), IsEqual.equalTo("Neutral result"));
	}
}
