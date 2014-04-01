package org.nem.core.test;

import org.hamcrest.core.*;
import org.junit.*;

/**
 * Static class containing custom asserts.
 */
public class CustomAsserts {

	/**
	 * Asserts that a MockSerializableEntity has the expected property values.
	 *
	 * @param object              The entity.
	 * @param expectedIntValue    The expected int value.
	 * @param expectedStringValue The expected string value.
	 * @param expectedLongValue   The expected long value.
	 */
	public static void assertMockSerializableEntity(
			MockSerializableEntity object,
			int expectedIntValue,
			String expectedStringValue,
			long expectedLongValue) {
		// Assert:
		Assert.assertThat(object.getIntValue(), IsEqual.equalTo(expectedIntValue));
		Assert.assertThat(object.getStringValue(), IsEqual.equalTo(expectedStringValue));
		Assert.assertThat(object.getLongValue(), IsEqual.equalTo(expectedLongValue));
	}
}
