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
	public void canAggregateZeroResults() {
		// Act:
		final Collection<ValidationResult> results = Collections.emptyList();

		// Assert:
		assertAggregationResult(results, ValidationResult.SUCCESS, false);
	}

	@Test
	public void canAggregateOneResult() {
		// Act:
		final Collection<ValidationResult> results = Collections.singletonList(ValidationResult.FAILURE_CHAIN_INVALID);

		// Assert:
		assertAggregationResult(results, ValidationResult.FAILURE_CHAIN_INVALID, false);
	}

	@Test
	public void canAggregateMultipleResults() {
		// Act:
		final Collection<ValidationResult> results = Arrays.asList(
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS);

		// Assert:
		assertAggregationResult(results, ValidationResult.SUCCESS, false);
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

	//region aggregate (no short circuit)

	@Test
	public void canNoShortCircuitAggregateZeroResults() {
		// Act:
		final Collection<ValidationResult> results = Collections.emptyList();

		// Assert:
		assertNoShortCircuitAggregationResult(results, ValidationResult.SUCCESS);
	}

	@Test
	public void canNoShortCircuitAggregateOneResult() {
		// Act:
		final Collection<ValidationResult> results = Collections.singletonList(ValidationResult.FAILURE_CHAIN_INVALID);

		// Assert:
		assertNoShortCircuitAggregationResult(results, ValidationResult.FAILURE_CHAIN_INVALID);
	}

	@Test
	public void canNoShortCircuitAggregateMultipleResults() {
		// Act:
		final Collection<ValidationResult> results = Arrays.asList(
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS);

		// Assert:
		assertNoShortCircuitAggregationResult(results, ValidationResult.SUCCESS);
	}

	@Test
	public void aggregateNoShortCircuitReturnsFailureWhenPassedIteratorWithAtLeastOneFailureResult() {
		// Act:
		final Collection<ValidationResult> results = Arrays.asList(
				ValidationResult.SUCCESS,
				ValidationResult.FAILURE_CHAIN_INVALID,
				ValidationResult.SUCCESS);

		// Assert:
		assertNoShortCircuitAggregationResult(results, ValidationResult.FAILURE_CHAIN_INVALID);
	}

	@Test
	public void aggregateNoShortCircuitReturnsNeutralWhenPassedIteratorWithAtLeastOneNeutralResultAndNoFailureResults() {
		// Act:
		final Collection<ValidationResult> results = Arrays.asList(
				ValidationResult.SUCCESS,
				ValidationResult.NEUTRAL,
				ValidationResult.SUCCESS);

		// Assert:
		assertNoShortCircuitAggregationResult(results, ValidationResult.NEUTRAL);
	}

	@Test
	public void aggregateNoShortCircuitGivesHigherPrecedenceToFailureResultThanNeutralResult() {
		// Arrange:
		final Collection<ValidationResult> results = Arrays.asList(
				ValidationResult.SUCCESS,
				ValidationResult.NEUTRAL,
				ValidationResult.FAILURE_CHAIN_INVALID);

		// Assert:
		assertNoShortCircuitAggregationResult(results, ValidationResult.FAILURE_CHAIN_INVALID);
	}

	private static void assertNoShortCircuitAggregationResult(
			final Collection<ValidationResult> results,
			final ValidationResult expectedResult) {
		// Act:
		final Iterator<ValidationResult> resultIterator = results.iterator();
		final ValidationResult result = ValidationResult.aggregateNoShortCircuit(resultIterator);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
		Assert.assertThat(resultIterator.hasNext(), IsEqual.equalTo(false));
	}

	//endregion
}