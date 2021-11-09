package org.nem.peer.trust;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class TrustParametersTest {

	@Test
	public void stringValueCanBeSetAndRetrieved() {
		// Arrange:
		final TrustParameters params = new TrustParameters();

		// Act:
		params.set("s", "foobar");

		// Assert:
		MatcherAssert.assertThat(params.get("s"), IsEqual.equalTo("foobar"));
		MatcherAssert.assertThat(params.get("s", "default"), IsEqual.equalTo("foobar"));
	}

	@Test
	public void intValueCanBeSetAndRetrieved() {
		// Arrange:
		final TrustParameters params = new TrustParameters();

		// Act:
		params.set("i", "123");

		// Assert:
		MatcherAssert.assertThat(params.getAsInteger("i"), IsEqual.equalTo(123));
		MatcherAssert.assertThat(params.getAsInteger("i", 7), IsEqual.equalTo(123));
	}

	@Test
	public void doubleValueCanBeSetAndRetrieved() {
		// Arrange:
		final TrustParameters params = new TrustParameters();

		// Act:
		params.set("d", "123.45");

		// Assert:
		MatcherAssert.assertThat(params.getAsDouble("d"), IsEqual.equalTo(123.45));
		MatcherAssert.assertThat(params.getAsDouble("d", 3.14), IsEqual.equalTo(123.45));
	}

	@Test
	public void defaultStringValueCanBeSetAndRetrieved() {
		// Arrange:
		final TrustParameters params = new TrustParameters();

		// Assert:
		MatcherAssert.assertThat(params.get("s", "default"), IsEqual.equalTo("default"));
	}

	@Test
	public void defaultIntValueCanBeSetAndRetrieved() {
		// Arrange:
		final TrustParameters params = new TrustParameters();

		// Assert:
		MatcherAssert.assertThat(params.getAsInteger("i", 7), IsEqual.equalTo(7));
	}

	@Test
	public void defaultDoubleValueCanBeSetAndRetrieved() {
		// Arrange:
		final TrustParameters params = new TrustParameters();

		// Assert:
		MatcherAssert.assertThat(params.getAsDouble("d", 3.14), IsEqual.equalTo(3.14));
	}

	@Test(expected = IllegalArgumentException.class)
	public void unsetStringValueCannotBeRetrieved() {
		// Arrange:
		final TrustParameters params = new TrustParameters();

		// Act:
		params.get("s");
	}

	@Test(expected = IllegalArgumentException.class)
	public void unsetIntegerValueCannotBeRetrieved() {
		// Arrange:
		final TrustParameters params = new TrustParameters();

		// Act:
		params.getAsInteger("i");
	}

	@Test(expected = IllegalArgumentException.class)
	public void unsetDoubleValueCannotBeRetrieved() {
		// Arrange:
		final TrustParameters params = new TrustParameters();

		// Act:
		params.getAsDouble("d");
	}
}
