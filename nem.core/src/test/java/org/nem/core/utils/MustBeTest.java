package org.nem.core.utils;

import org.junit.Test;
import org.nem.core.model.primitive.*;
import org.nem.core.test.ExceptionAssert;

import java.util.*;

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