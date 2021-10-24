package org.nem.core.utils;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.IsEquivalent;

import java.util.*;

public class AbstractTwoLevelMapTest {

	@Test
	public void previouslyUnknownValueCanBeRetrieved() {
		// Arrange:
		final AbstractTwoLevelMap<String, MockValue> map = new MockTwoLevelMap();

		// Act:
		final MockValue value = map.getItem("foo", "bar");

		// Assert:
		MatcherAssert.assertThat(value, IsNull.notNullValue());
	}

	@Test
	public void sameValueIsReturnedForSameKeys() {
		// Arrange:
		final AbstractTwoLevelMap<String, MockValue> map = new MockTwoLevelMap();

		// Act:
		final MockValue value1 = map.getItem("foo", "bar");
		final MockValue value2 = map.getItem("foo", "bar");

		// Assert:
		MatcherAssert.assertThat(value2, IsSame.sameInstance(value1));
	}

	@Test
	public void valueIsDirectional() {
		// Arrange:
		final AbstractTwoLevelMap<String, MockValue> map = new MockTwoLevelMap();

		// Act:
		final MockValue value1 = map.getItem("foo", "bar");
		final MockValue value2 = map.getItem("bar", "foo");

		// Assert:
		MatcherAssert.assertThat(value2, IsNot.not(IsSame.sameInstance(value1)));
	}

	@Test
	public void previouslyUnknownValueMapCanBeRetrieved() {
		// Arrange:
		final AbstractTwoLevelMap<String, MockValue> map = new MockTwoLevelMap();

		// Act:
		final Map<String, MockValue> values = map.getItems("foo");

		// Assert:
		MatcherAssert.assertThat(values, IsNull.notNullValue());
	}

	@Test
	public void getItemAndGetItemsReturnSameValueForSameKeys() {
		// Arrange:
		final AbstractTwoLevelMap<String, MockValue> map = new MockTwoLevelMap();

		// Act:
		final MockValue value1 = map.getItem("foo", "bar");
		final MockValue value2 = map.getItems("foo").get("bar");

		// Assert:
		MatcherAssert.assertThat(value2, IsSame.sameInstance(value1));
	}

	@Test
	public void removeRemovesKeyFromMap() {
		// Arrange:
		final AbstractTwoLevelMap<String, MockValue> map = new MockTwoLevelMap();
		map.getItems("foo");
		map.getItems("bar");
		map.getItems("baz");

		// Sanity:
		MatcherAssert.assertThat(map.keySet(), IsEquivalent.equivalentTo(Arrays.asList("foo", "bar", "baz")));

		// Act:
		map.remove("bar");

		// Assert:
		MatcherAssert.assertThat(map.keySet(), IsEquivalent.equivalentTo(Arrays.asList("foo", "baz")));
	}

	@Test
	public void removeDoesNothingIfKeyIsNotPresentInMap() {
		// Arrange:
		final AbstractTwoLevelMap<String, MockValue> map = new MockTwoLevelMap();
		map.getItems("foo");
		map.getItems("bar");
		map.getItems("baz");

		// Sanity:
		MatcherAssert.assertThat(map.keySet(), IsEquivalent.equivalentTo(Arrays.asList("foo", "bar", "baz")));

		// Act:
		map.remove("qux");
		map.remove("alice");

		// Assert:
		MatcherAssert.assertThat(map.keySet(), IsEquivalent.equivalentTo(Arrays.asList("foo", "bar", "baz")));
	}

	@Test
	public void keySetReturnsCollectionOfAllKeys() {
		// Arrange:
		final AbstractTwoLevelMap<String, MockValue> map = new MockTwoLevelMap();
		map.getItems("foo");
		map.getItems("bar");
		map.getItems("baz");

		// Assert:
		MatcherAssert.assertThat(map.keySet(), IsEquivalent.equivalentTo(Arrays.asList("foo", "bar", "baz")));
	}

	@Test
	public void keySetReturnsEmptyCollectionIfMapIsEmpty() {
		// Arrange:
		final AbstractTwoLevelMap<String, MockValue> map = new MockTwoLevelMap();

		// Assert:
		MatcherAssert.assertThat(map.keySet(), IsEquivalent.equivalentTo(Collections.emptyList()));
	}

	private static class MockValue {
	}

	private static class MockTwoLevelMap extends AbstractTwoLevelMap<String, MockValue> {

		@Override
		protected MockValue createValue() {
			return new MockValue();
		}
	}
}
