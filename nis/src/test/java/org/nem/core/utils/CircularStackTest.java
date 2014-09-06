package org.nem.core.utils;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class CircularStackTest {
	@Test(expected = IndexOutOfBoundsException.class)
	public void getOnEmptyStackThrowsException() {
		// Arrange:
		final CircularStack<Integer> intStack = createStack(3);

		// Act:
		intStack.get();
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void removingFromEmptyStackThrowsException() {
		// Arrange:
		final CircularStack<Integer> intStack = createStack(3);

		// Act:
		intStack.remove();
	}

	@Test
	public void canAddSingleElementToCircularStack() {
		// Arrange:
		final CircularStack<Integer> intStack = createStack(3);

		// Act:
		intStack.add(666);

		// Assert:
		Assert.assertThat(intStack.get(), IsEqual.equalTo(666));
		Assert.assertThat(intStack.size(), IsEqual.equalTo(1));
	}

	@Test
	public void canAddLimitElementsToCircularStack() {
		// Arrange:
		final CircularStack<Integer> intStack = createStack(3);

		// Act:
		intStack.add(666);
		intStack.add(777);
		intStack.add(888);

		// Assert:
		Assert.assertThat(intStack.get(), IsEqual.equalTo(888));
		Assert.assertThat(intStack.size(), IsEqual.equalTo(3));
	}

	@Test
	public void canAddMultipleElementsToCircularStack() {
		// Arrange:
		final CircularStack<Integer> intStack = createStack(3);

		// Act:
		for (int i = 0; i < 112; ++i) {
			intStack.add(666 + i);
		}

		// Assert:
		Assert.assertThat(intStack.get(), IsEqual.equalTo(666 + 111));
		Assert.assertThat(intStack.size(), IsEqual.equalTo(3));
	}

	@Test
	public void removingFromStackChangesSize() {
		// Arrange:
		final CircularStack<Integer> intStack = createStack(3);

		// Act:
		for (int i = 0; i < 112; ++i) {
			intStack.add(666 + i);
		}
		intStack.remove();

		// Assert:
		Assert.assertThat(intStack.get(), IsEqual.equalTo(666 + 111 - 1));
		Assert.assertThat(intStack.size(), IsEqual.equalTo(2));
	}

	@Test
	public void canIterateOverStack() {
		// Arrange:
		final CircularStack<Integer> intStack = createStack(3);

		// Act:
		for (int i = 0; i < 112; ++i) {
			intStack.add(666 + i);
		}

		// Assert:
		int i = 666 + 111 - 2;
		for (final Integer elem : intStack) {
			Assert.assertThat(elem, IsEqual.equalTo(i));
			++i;
		}
	}

	private CircularStack<Integer> createStack(final int i) {
		return new CircularStack<>(i);
	}

}
