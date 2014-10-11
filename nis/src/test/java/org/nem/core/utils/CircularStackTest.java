package org.nem.core.utils;

import org.hamcrest.core.*;
import org.junit.*;

public class CircularStackTest {
	@Test(expected = IndexOutOfBoundsException.class)
	public void peekOnEmptyStackThrowsException() {
		// Arrange:
		final CircularStack<Integer> intStack = this.createStack(3);

		// Act:
		intStack.peek();
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void popFromEmptyStackThrowsException() {
		// Arrange:
		final CircularStack<Integer> intStack = this.createStack(3);

		// Act:
		intStack.pop();
	}

	@Test
	public void canAddSingleElementToCircularStack() {
		// Arrange:
		final CircularStack<Integer> intStack = this.createStack(3);

		// Act:
		intStack.push(123);

		// Assert:
		Assert.assertThat(intStack.peek(), IsEqual.equalTo(123));
		Assert.assertThat(intStack.size(), IsEqual.equalTo(1));
	}

	@Test
	public void canAddLimitElementsToCircularStack() {
		// Arrange:
		final CircularStack<Integer> intStack = this.createStack(3);

		// Act:
		intStack.push(555);
		intStack.push(777);
		intStack.push(888);

		// Assert:
		Assert.assertThat(intStack.peek(), IsEqual.equalTo(888));
		Assert.assertThat(intStack.size(), IsEqual.equalTo(3));
	}

	@Test
	public void addingMoreThanLimitElementsToCircularStackAgesOutOlderElements() {
		// Arrange:
		final CircularStack<Integer> intStack = this.createStack(3);

		// Act:
		for (int i = 0; i < 100; ++i) {
			intStack.push(123 + i);
		}

		// Assert:
		Assert.assertThat(intStack.peek(), IsEqual.equalTo(123 + 99));
		Assert.assertThat(intStack.size(), IsEqual.equalTo(3));
	}

	@Test
	public void poppingFromStackDecreasesSizeByOne() {
		// Arrange:
		final CircularStack<Integer> intStack = this.createStack(3);

		// Act:
		for (int i = 0; i < 100; ++i) {
			intStack.push(123 + i);
		}

		intStack.pop();

		// Assert:
		Assert.assertThat(intStack.peek(), IsEqual.equalTo(123 + 99 - 1));
		Assert.assertThat(intStack.size(), IsEqual.equalTo(2));
	}

	//region shallowCopyTo

	@Test
	public void canCopyLargerToSmaller() {
		// Arrange:
		final CircularStack<Integer> source = this.createStack(10);
		final CircularStack<Integer> destination = this.createStack(3);

		// Act:
		for (int i = 0; i < 10; ++i) {
			source.push(i);
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
		final CircularStack<Integer> source = this.createStack(3);
		final CircularStack<Integer> destination = this.createStack(10);

		// Act:
		for (int i = 0; i < 3; ++i) {
			source.push(i);
		}

		source.shallowCopyTo(destination);

		// Assert:
		Assert.assertThat(source.size(), IsEqual.equalTo(3));
		Assert.assertThat(destination.size(), IsEqual.equalTo(3));
		int i = 0;
		for (final Integer element : destination) {
			Assert.assertThat(element, IsEqual.equalTo(i++));
		}
	}

	@Test
	public void copyIsAShallowCopy() {
		// Arrange:
		final CircularStack<Integer> stack1 = this.createStack(3);
		final CircularStack<Integer> stack2 = this.createStack(3);

		// Act:
		for (int i = 0; i < 10; ++i) {
			stack1.push(i);
		}
		stack1.shallowCopyTo(stack2);

		// Assert:
		Assert.assertThat(stack1.size(), IsEqual.equalTo(3));
		Assert.assertThat(stack2.size(), IsEqual.equalTo(3));
		for (int i = 0; i < 3; ++i) {
			Assert.assertThat(stack1.peek(), IsSame.sameInstance(stack2.peek()));
			stack1.pop();
			stack2.pop();
		}
	}

	//endregion

	//region iteration

	@Test
	public void canIterateOverStackFromOldestToNewestElement() {
		// Arrange:
		final CircularStack<Integer> intStack = this.createStack(3);

		// Act:
		for (int i = 0; i < 3; ++i) {
			intStack.push(123 + i);
		}

		// Assert:
		int i = 123;
		for (final Integer elem : intStack) {
			Assert.assertThat(elem, IsEqual.equalTo(i));
			++i;
		}
	}

	@Test
	public void canIterateOverStackFromNewestToOldestElementByPoppingAllElements() {
		// Arrange:
		final CircularStack<Integer> intStack = this.createStack(3);

		// Act:
		for (int i = 0; i < 3; ++i) {
			intStack.push(123 + i);
		}

		// Assert:
		for (int i = 125; i >= 123; --i) {
			Assert.assertThat(intStack.peek(), IsEqual.equalTo(i));
			intStack.pop();
		}

		Assert.assertThat(intStack.size(), IsEqual.equalTo(0));
	}

	//endregion

	private CircularStack<Integer> createStack(final int i) {
		return new CircularStack<>(i);
	}
}
