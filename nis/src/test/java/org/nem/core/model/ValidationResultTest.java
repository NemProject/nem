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
			{ this.add(ValidationResult.SUCCESS); }
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
			{ this.add(ValidationResult.SUCCESS); }

			{ this.add(ValidationResult.NEUTRAL); }
		};

		// Assert:
		for (final ValidationResult result : ValidationResult.values()) {
			Assert.assertThat(result.isFailure(), IsEqual.equalTo(!nonFailureValues.contains(result)));
		}
	}

	//endregion

	//region aggregate

	@Test
	public void aggregateReturnsSuccessWhenPassedEmptyIterator() {
		// Act:
		final Collection<ValidationResult> results = Arrays.asList();

		// Assert:
		assertAggregationResult(results, ValidationResult.SUCCESS, false);
	}

	@Test
	public void aggregateReturnsSingleResultWhenPassedIteratorWithSingleResult() {
		// Act:
		final Collection<ValidationResult> results = Arrays.asList(ValidationResult.FAILURE_CHAIN_INVALID);

		// Assert:
		assertAggregationResult(results, ValidationResult.FAILURE_CHAIN_INVALID, false);
	}

	@Test
	public void aggregateReturnsFailureWhenPassedIteratorWithAtLeastOneFailureResult() {
		// Act:
		final Collection<ValidationResult> results = Arrays.asList(
				ValidationResult.SUCCESS,
				ValidationResult.FAILURE_CHAIN_INVALID,
				ValidationResult.SUCCESS);

		// Assert:
		assertAggregationResult(results, ValidationResult.FAILURE_CHAIN_INVALID, true);
	}

	@Test
	public void aggregateReturnsNeutralWhenPassedIteratorWithAtLeastOneNeutralResultAndNoFailureResults() {
		// Act:
		final Collection<ValidationResult> results = Arrays.asList(
				ValidationResult.SUCCESS,
				ValidationResult.NEUTRAL,
				ValidationResult.SUCCESS);

		// Assert:
		assertAggregationResult(results, ValidationResult.NEUTRAL, false);
	}

	@Test
	public void aggregateGivesHigherPrecedenceToFailureResultThanNeutralResult() {
		// Arrange:
		final Collection<ValidationResult> results = Arrays.asList(
				ValidationResult.SUCCESS,
				ValidationResult.NEUTRAL,
				ValidationResult.FAILURE_CHAIN_INVALID);

		// Assert:
		assertAggregationResult(results, ValidationResult.FAILURE_CHAIN_INVALID, false);
	}

	private static void assertAggregationResult(
			final Collection<ValidationResult> results,
			final ValidationResult expectedResult,
			final boolean isShortCircuited) {
		// Act:
		final Iterator<ValidationResult> resultIterator = results.iterator();
		final ValidationResult result = ValidationResult.aggregate(resultIterator);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
		Assert.assertThat(resultIterator.hasNext(), IsEqual.equalTo(isShortCircuited));
	}

	//endregion
}