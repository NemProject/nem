package org.nem.core.test;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Static helper class for writing parameterized tests.
 */
public class ParameterizedUtils {

	/**
	 * Wraps a collection of integers into a collection of object arrays that can be used in
	 * parameterized tests.
	 *
	 * @param values The collection of integers.
	 * @return The collection of object arrays.
	 */
	public static Collection<Object[]> wrap(final Collection<Integer> values) {
		return values.stream()
				.map(v -> new Object[] { v })
				.collect(Collectors.toList());
	}
}
