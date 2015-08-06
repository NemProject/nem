package org.nem.core.utils;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class SetOnceTest {

	@Test
	public void defaultValueIsConstructorParameter() {
		// Arrange:
		final SetOnce<Integer> wrapper = new SetOnce<>(17);

		// Act:
		final Integer value = wrapper.get();

		// Assert:
		Assert.assertThat(value, IsEqual.equalTo(17));
	}

	@Test
	public void defaultValueCanBeChangedToCustomValue() {
		// Arrange:
		final SetOnce<Integer> wrapper = new SetOnce<>(17);

		// Act:
		wrapper.set(54);
		final Integer value = wrapper.get();

		// Assert:
		Assert.assertThat(value, IsEqual.equalTo(54));
	}

	@Test
	public void defaultValueCanBeReset() {
		// Arrange:
		final SetOnce<Integer> wrapper = new SetOnce<>(17);

		// Act:
		wrapper.set(54);
		wrapper.set(null);
		final Integer value = wrapper.get();

		// Assert:
		Assert.assertThat(value, IsEqual.equalTo(17));
	}

	@Test
	public void defaultValueCannotBeChangedAfterBeingSet() {
		// Arrange:
		final SetOnce<Integer> wrapper = new SetOnce<>(17);
		wrapper.set(54);

		// Act:
		ExceptionAssert.assertThrows(
				v -> wrapper.set(77),
				IllegalStateException.class);
	}
}