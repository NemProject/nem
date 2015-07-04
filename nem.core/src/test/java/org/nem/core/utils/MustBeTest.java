package org.nem.core.utils;

import org.junit.Test;
import org.nem.core.model.primitive.*;
import org.nem.core.test.ExceptionAssert;

import java.util.*;
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

	//region positive

	@Test
	public void positiveThrowsIfAmountIsZero() {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> MustBe.positive(GenericAmount.ZERO, "zero"),
				IllegalArgumentException.class,
				ex -> ex.getMessage().contains("zero"));
	}

	@Test
	public void positiveDoesNotThrowIfAmountIsNonZero() {
		// Assert: no exception
		MustBe.positive(GenericAmount.fromValue(1), "zero");
		MustBe.positive(GenericAmount.fromValue(123), "zero");
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