package org.nem.core.utils;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsSame;
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
	public void canCopyLargerToSmaller() {
		// Arrange:
		final CircularStack<Integer> source = createStack(10);
		final CircularStack<Integer> destination = createStack(3);

		// Act:
		for (int i = 0; i < 10; ++i) {
			source.add(i);
		}
		source.shallowCopyTo(destination);

		// Assert:
		Assert.assertThat(source.size(), IsEqual.equalTo(10));
		Assert.assertThat(destination.size(), IsEqual.equalTo(3));
		int i = 7;
		for (final Integer element : destination) {
			Assert.assertThat(element, IsEqual.equalTo(i++));
		}
	}

	@Test
	public void canCopySmallerToLarger() {
		// Arrange:
		final CircularStack<Integer> source = createStack(3);
		final CircularStack<Integer> destination = createStack(10);

		// Act:
		for (int i = 0; i < 10; ++i) {
			source.add(i);
		}
		source.shallowCopyTo(destination);

		// Assert:
		Assert.assertThat(source.size(), IsEqual.equalTo(3));
		Assert.assertThat(destination.size(), IsEqual.equalTo(3));
		int i = 7;
		for (final Integer element : destination) {
			Assert.assertThat(element, IsEqual.equalTo(i++));
		}
		i = 7;
		for (final Integer element : source) {
			Assert.assertThat(element, IsEqual.equalTo(i++));
		}
	}

	@Test
	public void copyIsAShallowCopy() {
		// Arrange:
		final CircularStack<Integer> stack1 = createStack(3);
		final CircularStack<Integer> stack2 = createStack(3);

		// Act:
		for (int i = 0; i < 10; ++i) {
			stack1.add(i);
		}
		stack1.shallowCopyTo(stack2);

		// Assert:
		Assert.assertThat(stack1.size(), IsEqual.equalTo(3));
		Assert.assertThat(stack2.size(), IsEqual.equalTo(3));
		for (int i = 0; i < 3; ++i) {
			Assert.assertThat(stack1.get(), IsSame.sameInstance(stack2.get()));
			stack1.remove();
			stack2.remove();
		}
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
