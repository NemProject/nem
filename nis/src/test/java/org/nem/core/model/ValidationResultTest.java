package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class ValidationResultTest {

	//region getValue / fromValue

	@Test
	public void getValueReturnsUnderlyingValue() {
		// Act:
		final ValidationResult result = ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE;

		// Assert:
		Assert.assertThat(result.getValue(), IsEqual.equalTo(8));
	}

	@Test
	public void canCreateResultAroundKnownValue() {
		// Act:
		final ValidationResult result = ValidationResult.fromValue(8);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE));
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotCreateResultAroundUnknownValue() {
		// Assert:
		ValidationResult.fromValue(1337);
	}

	//endregion

	//region predicates

	@Test
	public void isSuccessOnlyReturnsTrueForSuccessValues() {
		// Assert:
		Assert.assertThat(ValidationResult.SUCCESS.isSuccess(), IsEqual.equalTo(true));
		Assert.assertThat(ValidationResult.NEUTRAL.isSuccess(), IsEqual.equalTo(false));
		Assert.assertThat(ValidationResult.FAILURE_UNKNOWN.isSuccess(), IsEqual.equalTo(false));
	}

	@Test
	public void isFailureOnlyReturnsTrueForFailureValues() {
		// Assert:
		Assert.assertThat(ValidationResult.SUCCESS.isFailure(), IsEqual.equalTo(false));
		Assert.assertThat(ValidationResult.NEUTRAL.isFailure(), IsEqual.equalTo(false));
		Assert.assertThat(ValidationResult.FAILURE_UNKNOWN.isFailure(), IsEqual.equalTo(true));
	}

	//endregion
}