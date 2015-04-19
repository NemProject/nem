package org.nem.core.utils;

import org.hamcrest.core.*;
import org.junit.*;

import java.util.Map;

public class AbstractTwoLevelMapTest {

	@Test
	public void previouslyUnknownValueCanBeRetrieved() {
		// Arrange:
		final AbstractTwoLevelMap<String, MockValue> map = new MockTwoLevelMap();

		// Act:
		final MockValue value = map.getItem("foo", "bar");

		// Assert:
		Assert.assertThat(value, IsNull.notNullValue());
	}

	@Test
	public void sameValueIsReturnedForSameKeys() {
		// Arrange:
		final AbstractTwoLevelMap<String, MockValue> map = new MockTwoLevelMap();

		// Act:
		final MockValue value1 = map.getItem("foo", "bar");
		final MockValue value2 = map.getItem("foo", "bar");

		// Assert:
		Assert.assertThat(value2, IsSame.sameInstance(value1));
	}

	@Test
	public void valueIsDirectional() {
		// Arrange:
		final AbstractTwoLevelMap<String, MockValue> map = new MockTwoLevelMap();

		// Act:
		final MockValue value1 = map.getItem("foo", "bar");
		final MockValue value2 = map.getItem("bar", "foo");

		// Assert:
		Assert.assertThat(value2, IsNot.not(IsSame.sameInstance(value1)));
	}

	@Test
	public void previouslyUnknownValueMapCanBeRetrieved() {
		// Arrange:
		final AbstractTwoLevelMap<String, MockValue> map = new MockTwoLevelMap();

		// Act:
		final Map<String, MockValue> values = map.getItems("foo");

		// Assert:
		Assert.assertThat(values, IsNull.notNullValue());
	}

	@Test
	public void getItemAndGetItemsReturnSameValueForSameKeys() {
		// Arrange:
		final AbstractTwoLevelMap<String, MockValue> map = new MockTwoLevelMap();

		// Act:
		final MockValue value1 = map.getItem("foo", "bar");
		final MockValue value2 = map.getItems("foo").get("bar");

		// Assert:
		Assert.assertThat(value2, IsSame.sameInstance(value1));
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
