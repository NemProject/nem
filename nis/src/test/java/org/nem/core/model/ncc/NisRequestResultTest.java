package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class NisRequestResultTest {
	@Test
	public void nisRequestResultCtorSetsProperFields() {
		// Arrange + Act:
		final NisRequestResult result = new NisRequestResult(
				NisRequestResult.TYPE_VALIDATION_RESULT,
				NisRequestResult.CODE_NEUTRAL,
				"Neutral result");

		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(NisRequestResult.TYPE_VALIDATION_RESULT));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(NisRequestResult.CODE_NEUTRAL));
		Assert.assertThat(result.getMessage(), IsEqual.equalTo("Neutral result"));
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
		// Arrange:
		final NisRequestResult entity = new NisRequestResult(
				NisRequestResult.TYPE_VALIDATION_RESULT,
				NisRequestResult.CODE_NEUTRAL,
				"Neutral result");

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(entity, null);
		final NisRequestResult result = new NisRequestResult(deserializer);

		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(NisRequestResult.TYPE_VALIDATION_RESULT));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(NisRequestResult.CODE_NEUTRAL));
		Assert.assertThat(result.getMessage(), IsEqual.equalTo("Neutral result"));
	}

}
