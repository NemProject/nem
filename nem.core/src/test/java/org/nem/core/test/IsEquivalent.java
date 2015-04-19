package org.nem.core.test;

import org.hamcrest.core.IsEqual;

import java.util.*;

/**
 * Matcher that checks the equivalency of a collection to another collection or array.
 *
 * @param <T> The collection element type.
 */
public class IsEquivalent<T> extends org.hamcrest.BaseMatcher<Collection<T>> {
	private final Collection<T> lhs;

	/**
	 * Creates a new IsEquivalent matcher.
	 *
	 * @param lhs The array to match.
	 */
	private IsEquivalent(final T[] lhs) {
		this.lhs = Arrays.asList(lhs);
	}

	/**
	 * Creates a new IsEquivalent matcher.
	 *
	 * @param lhs The collection to match.
	 */
	private IsEquivalent(final Collection<T> lhs) {
		this.lhs = lhs;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean matches(final java.lang.Object arg) {
		if (!(arg instanceof Collection<?>)) {
			return false;
		}

		final Collection<T> rhs = (Collection<T>)arg;
		if (this.lhs.size() != rhs.size()) {
			return false;
		}

		final HashSet<T> lhsSet = new HashSet<>(this.lhs);
		final HashSet<T> rhsSet = new HashSet<>(rhs);
		return lhsSet.equals(rhsSet);
	}

	@Override
	public void describeTo(final org.hamcrest.Description description) {
		// use the IsEqual matcher to generate descriptions
		IsEqual.equalTo(this.lhs).describeTo(description);
	}

	/**
	 * Creates an equivalency matcher that checks equivalence to a collection.
	 *
	 * @param collection The collection to compare against.
	 * @param <T> The element type.
	 * @return The matcher.
	 */
	@org.hamcrest.Factory
	public static <T> org.hamcrest.Matcher<Collection<T>> equivalentTo(final Collection<T> collection) {
		return new IsEquivalent<>(collection);
	}

	/**
	 * Creates an equivalency matcher that checks equivalence to an array.
	 *
	 * @param array The collection to compare against.
	 * @param <T> The element type.
	 * @return The matcher.
	 */
	@SafeVarargs
	@org.hamcrest.Factory
	public static <T> org.hamcrest.Matcher<Collection<T>> equivalentTo(final T... array) {
		return new IsEquivalent<>(array);
	}
}