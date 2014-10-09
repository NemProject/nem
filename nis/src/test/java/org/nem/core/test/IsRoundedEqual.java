package org.nem.core.test;

import org.hamcrest.core.IsEqual;

/**
 * Matcher that checks the that two double values are equal within an epsilon value.
 */
public class IsRoundedEqual extends org.hamcrest.BaseMatcher<Double> {
	private final Double lhs;
	private final int numPlaces;

	/**
	 * Creates a new IsRoundedEqual matcher.
	 *
	 * @param lhs The value to match.
	 * @param numPlaces The number of places that each number should be rounded.
	 */
	private IsRoundedEqual(final Double lhs, final int numPlaces) {
		this.lhs = lhs;
		this.numPlaces = numPlaces;
	}

	@Override
	public boolean matches(final Object arg) {
		if (!(arg instanceof Double)) {
			return false;
		}

		final Double rhs = (Double)arg;
		return roundTo(this.lhs, this.numPlaces) == roundTo(rhs, this.numPlaces);
	}

	@Override
	public void describeTo(final org.hamcrest.Description description) {
		// use the IsEqual matcher to generate descriptions
		IsEqual.equalTo(this.lhs).describeTo(description);
	}

	private static double roundTo(final double value, final int numPlaces) {
		final double multipler = Math.pow(10, numPlaces);
		return Math.round(value * multipler) / multipler;
	}

	/**
	 * Creates a rounded equal matcher that checks double (near) equality.
	 *
	 * @param value The value to match.
	 * @return The matcher.
	 */
	@org.hamcrest.Factory
	public static org.hamcrest.Matcher<Double> equalTo(final Double value) {
		return equalTo(value, 10);
	}

	/**
	 * Creates a rounded equal matcher that checks double (near) equality.
	 *
	 * @param value The value to match.
	 * @param numPlaces The number of places that each number should be rounded.
	 * @return The matcher.
	 */
	@org.hamcrest.Factory
	public static <T> org.hamcrest.Matcher<Double> equalTo(final Double value, final int numPlaces) {
		return new IsRoundedEqual(value, numPlaces);
	}
}