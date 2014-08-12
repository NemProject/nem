package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;

import java.util.*;

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
		// Arrange:
		final Set<ValidationResult> successValues = new HashSet<ValidationResult>() {
			{ add(ValidationResult.SUCCESS); }
		};

		// Assert:
		for (final ValidationResult result : ValidationResult.values()) {
			Assert.assertThat(result.isSuccess(), IsEqual.equalTo(successValues.contains(result)));
		}
	}

	@Test
	public void isFailureOnlyReturnsTrueForFailureValues() {
		// Arrange:
		final Set<ValidationResult> nonFailureValues = new HashSet<ValidationResult>() {
			{ add(ValidationResult.SUCCESS); }

			{ add(ValidationResult.NEUTRAL); }
		};

		// Assert:
		for (final ValidationResult result : ValidationResult.values()) {
			Assert.assertThat(result.isFailure(), IsEqual.equalTo(!nonFailureValues.contains(result)));
		}
	}

	//endregion
}