package org.nem.core.utils;

import org.junit.Test;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.ExceptionAssert;

import java.util.Collections;
import java.util.regex.Pattern;

public class MustBeTest {

	//region notNull

	@Test
	public void notNullThrowsIfObjectIsNull() {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> MustBe.notNull(null, "test"),
				IllegalArgumentException.class,
				ex -> ex.getMessage().contains("test"));
	}

	@Test
	public void notNullDoesNotThrowIfObjectIsNotNull() {
		// Assert: no exception
		MustBe.notNull(new Object(), "test");
	}

	//endregion

	//region match

	@Test
	public void matchThrowsIfStringIsNull() {
		// Assert:
		assertMatchThrows(null);
	}

	@Test
	public void matchThrowsIfStringIsEmpty() {
		// Assert:
		assertMatchThrows("");
	}

	@Test
	public void matchThrowsIfStringDoesNotMatchPattern() {
		// Assert:
		assertMatchThrows("13G74");
	}

	@Test
	public void matchThrowsIfStringIsTooLong() {
		// Assert:
		assertMatchThrows("01234567890");
	}

	@Test
	public void matchDoesNotThrowIfAllConditionsAreSatisfied() {
		// Assert: no exception
		MustBe.match("13674", "input", Pattern.compile("[0-9]*"), 10);
		MustBe.match("0123456789", "input", Pattern.compile("[0-9]*"), 10);
	}

	private static void assertMatchThrows(final String input) {
		ExceptionAssert.assertThrows(
				v -> MustBe.match(input, "input", Pattern.compile("[0-9]*"), 10),
				IllegalArgumentException.class,
				ex -> ex.getMessage().contains("input"));
	}

	//endregion

	//region inRange (integer)

	@Test
	public void inRangeThrowsIfValueIsLessThanMinValue() {
		// Assert:
		assertInRangeThrows(-3);
		assertInRangeThrows(-1000);
	}

	@Test
	public void inRangeThrowsIfValueIsGreaterThanMaxValue() {
		// Assert:
		assertInRangeThrows(6);
		assertInRangeThrows(1000);
	}

	@Test
	public void inRangeDoesNotThrowIfValueIsInRange() {
		// Assert:
		MustBe.inRange(-2, "val", -2, 5);
		MustBe.inRange(1, "val", -2, 5);
		MustBe.inRange(5, "val", -2, 5);
	}

	private static void assertInRangeThrows(final int input) {
		ExceptionAssert.assertThrows(
				v -> MustBe.inRange(input, "val", -2, 5),
				IllegalArgumentException.class,
				ex -> ex.getMessage().contains("val"));
	}

	//region inRange (long)

	@Test
	public void inRangeThrowsIfLongValueIsLessThanMinValue() {
		// Assert:
		assertLongInRangeThrows(-3L);
		assertLongInRangeThrows(-1000L);
	}

	@Test
	public void inRangeThrowsIfLongValueIsGreaterThanMaxValue() {
		// Assert:
		assertLongInRangeThrows(6L);
		assertLongInRangeThrows(1000L);
	}

	@Test
	public void inRangeDoesNotThrowIfLongValueIsInRange() {
		// Assert:
		MustBe.inRange(-2L, "val", -2L, 5L);
		MustBe.inRange(1L, "val", -2L, 5L);
		MustBe.inRange(5L, "val", -2L, 5L);
	}

	private static void assertLongInRangeThrows(final long input) {
		ExceptionAssert.assertThrows(
				v -> MustBe.inRange(input, "val", -2L, 5L),
				IllegalArgumentException.class,
				ex -> ex.getMessage().contains("val"));
	}

	//endregion

	//region positive

	@Test
	public void positiveThrowsIfAmountIsZero() {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> MustBe.positive(Quantity.ZERO, "zero"),
				IllegalArgumentException.class,
				ex -> ex.getMessage().contains("zero"));
	}

	@Test
	public void positiveDoesNotThrowIfAmountIsNonZero() {
		// Assert: no exception
		MustBe.positive(Quantity.fromValue(1), "zero");
		MustBe.positive(Quantity.fromValue(123), "zero");
	}

	//endregion

	//region positive

	@Test
	public void emptyThrowsIfCollectionIsNotEmpty() {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> MustBe.empty(Collections.singletonList(123), "list"),
				IllegalArgumentException.class,
				ex -> ex.getMessage().contains("list"));
	}

	@Test
	public void emptyDoesNotThrowIfCollectionIsEmpty() {
		// Assert: no exception
		MustBe.empty(Collections.emptyList(), "list");
	}

	//endregion
}