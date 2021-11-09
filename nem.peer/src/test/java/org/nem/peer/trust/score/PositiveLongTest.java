package org.nem.peer.trust.score;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class PositiveLongTest {

	@Test
	public void canBeCreatedWithInitialValue() {
		// Arrange:
		final PositiveLong value = new PositiveLong(7);

		// Assert:
		MatcherAssert.assertThat(value.get(), IsEqual.equalTo(7L));
	}

	@Test
	public void initialValueIsConstrained() {
		// Arrange:
		final PositiveLong value = new PositiveLong(-7);

		// Assert:
		MatcherAssert.assertThat(value.get(), IsEqual.equalTo(0L));
	}

	@Test
	public void valueCannotBeSetToNegativeValue() {
		// Arrange:
		final PositiveLong value = new PositiveLong(7);

		// Act:
		value.set(-3);

		// Assert:
		MatcherAssert.assertThat(value.get(), IsEqual.equalTo(0L));
	}

	@Test
	public void valueCanBeSetToZeroValue() {
		// Arrange:
		final PositiveLong value = new PositiveLong(0);

		// Act:
		value.set(0);

		// Assert:
		MatcherAssert.assertThat(value.get(), IsEqual.equalTo(0L));
	}

	@Test
	public void valueCanBeSetToPositiveValue() {
		// Arrange:
		final PositiveLong value = new PositiveLong(7);

		// Act:
		value.set(3);

		// Assert:
		MatcherAssert.assertThat(value.get(), IsEqual.equalTo(3L));
	}

	@Test
	public void valueCanBeIncremented() {
		// Arrange:
		final PositiveLong value = new PositiveLong(7);

		// Act:
		value.increment();

		// Assert:
		MatcherAssert.assertThat(value.get(), IsEqual.equalTo(8L));
	}
}
