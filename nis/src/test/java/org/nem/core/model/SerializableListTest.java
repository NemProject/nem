package org.nem.core.model;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsSame;
import org.junit.*;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.MockSerializableEntity;

import java.util.ArrayList;
import java.util.List;

public class SerializableListTest {

	//region Constructors

	@Test
	public void ctorCapacityHasNoImpactOnSize() {
		// Act:
		final SerializableList<MockSerializableEntity> list = new SerializableList<>(100);

		// Assert:
		Assert.assertThat(list.size(), IsEqual.equalTo(0));
	}

	@Test
	public void ctorListInitializesSerializableListWithItems() {
		// Arrange:
		final List<MockSerializableEntity> rawList = new ArrayList<>();
		rawList.add(new MockSerializableEntity());
		rawList.add(new MockSerializableEntity());

		// Act:
		final SerializableList<MockSerializableEntity> list = new SerializableList<>(rawList);

		// Assert:
		Assert.assertThat(list.size(), IsEqual.equalTo(2));
		Assert.assertThat(list.get(0), IsSame.sameInstance(rawList.get(0)));
		Assert.assertThat(list.get(1), IsSame.sameInstance(rawList.get(1)));
	}

	@Test
	public void ctorListIsDetachedFromSerializableList() {
		// Arrange:
		final List<MockSerializableEntity> rawList = new ArrayList<>();
		rawList.add(new MockSerializableEntity());
		rawList.add(new MockSerializableEntity());

		// Act:
		final SerializableList<MockSerializableEntity> list = new SerializableList<>(rawList);
		rawList.add(new MockSerializableEntity());

		// Assert:
		Assert.assertThat(rawList.size(), IsEqual.equalTo(3));
		Assert.assertThat(list.size(), IsEqual.equalTo(2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorListCannotContainNullItems() {
		// Arrange:
		final List<MockSerializableEntity> rawList = new ArrayList<>();
		rawList.add(new MockSerializableEntity());
		rawList.add(null);
		rawList.add(new MockSerializableEntity());

		// Act:
		new SerializableList<>(rawList);
	}

	// endregion

	// region Add

	@Test
	public void addAddsItemsToList() {
		// Arrange:
		final MockSerializableEntity entity1 = new MockSerializableEntity();
		final MockSerializableEntity entity2 = new MockSerializableEntity();
		final MockSerializableEntity entity3 = new MockSerializableEntity();
		final SerializableList<MockSerializableEntity> list = new SerializableList<>(0);

		// Act:
		list.add(entity1);
		list.add(entity2);
		list.add(entity3);

		// Assert:
		Assert.assertThat(list.size(), IsEqual.equalTo(3));
		Assert.assertThat(list.get(0), IsEqual.equalTo(entity1));
		Assert.assertThat(list.get(1), IsEqual.equalTo(entity2));
		Assert.assertThat(list.get(2), IsEqual.equalTo(entity3));
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotAddNullItemToList() {
		// Arrange:
		final SerializableList<MockSerializableEntity> list = new SerializableList<>(0);

		// Act:
		list.add(null);
	}

	// endregion

	// region FindFirst simple

	@Test
	public void canCompareEmptyChains() {
		// Arrange:
		final SerializableList<MockSerializableEntity> list1 = new SerializableList<>(10);
		final SerializableList<MockSerializableEntity> list2 = new SerializableList<>(20);

		// Assert:
		assertListComparison(list1, list2, 0, true);
	}

	@Test
	public void canCompareWithSelf() {
		// Arrange:
		final SerializableList<MockSerializableEntity> list = new SerializableList<>(10);
		list.add(new MockSerializableEntity());
		list.add(new MockSerializableEntity());

		// Assert:
		assertListComparison(list, list, 2, true);
	}

	@Test
	public void canCompareWithSelfUsingAdd() {
		// Arrange:
		final SerializableList<MockSerializableEntity> list = new SerializableList<>(10);
		list.add(new MockSerializableEntity());
		list.add(new MockSerializableEntity());

		// Assert:
		assertListComparison(list, list, 2, true);
	}

	// endregion

	// region FindFirst two chains

	@Test
	public void canCompareTwoConsistentChains() {
		// Arrange:
		final SerializableList<MockSerializableEntity> list1 = new SerializableList<>(10);
		list1.add(new MockSerializableEntity(5, "foo", 6));
		list1.add(new MockSerializableEntity(8, "bar", 7));

		final SerializableList<MockSerializableEntity> list2 = new SerializableList<>(10);
		list2.add(new MockSerializableEntity(5, "foo", 6));
		list2.add(new MockSerializableEntity(8, "bar", 7));

		// Assert:
		assertListComparison(list1, list2, 2, true);
	}

	@Test
	public void canCompareConsistentChainsWithDifferentLengths() {
		// Arrange:
		final SerializableList<MockSerializableEntity> list1 = new SerializableList<>(10);
		list1.add(new MockSerializableEntity(5, "foo", 6));

		final SerializableList<MockSerializableEntity> list2 = new SerializableList<>(10);
		list2.add(new MockSerializableEntity(5, "foo", 6));
		list2.add(new MockSerializableEntity(8, "bar", 7));

		// Assert:
		assertListComparison(list1, list2, 1, false);
	}

	@Test
	public void canCompareInconsistentChains() {
		// Arrange:
		final SerializableList<MockSerializableEntity> list1 = new SerializableList<>(10);
		list1.add(new MockSerializableEntity(5, "a", 6));
		list1.add(new MockSerializableEntity(5, "b", 6));
		list1.add(new MockSerializableEntity(5, "c", 6));
		list1.add(new MockSerializableEntity(5, "d", 6));
		list1.add(new MockSerializableEntity(5, "e", 6));

		final SerializableList<MockSerializableEntity> list2 = new SerializableList<>(10);
		list2.add(new MockSerializableEntity(5, "a", 6));
		list2.add(new MockSerializableEntity(5, "b", 6));
		list2.add(new MockSerializableEntity(5, "d", 6));
		list2.add(new MockSerializableEntity(5, "e", 6));

		// Assert:
		assertListComparison(list1, list2, 2, false);
	}

	// endregion

	//region Serialization

	@Test
	public void canSerializeList() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final SerializableList<MockSerializableEntity> list1 = new SerializableList<>(10);
		list1.add(new MockSerializableEntity(5, "foo", 6));
		list1.add(new MockSerializableEntity(8, "bar", 7));

		// Act:
		list1.serialize(serializer);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		final JSONArray dataArray = (JSONArray)object.get("data");
		Assert.assertThat(dataArray.size(), IsEqual.equalTo(2));
		Assert.assertThat(deserializeFromObject(dataArray.get(0)), IsEqual.equalTo(list1.get(0)));
		Assert.assertThat(deserializeFromObject(dataArray.get(1)), IsEqual.equalTo(list1.get(1)));
	}

	//endregion

	private static void assertListComparison(
			final SerializableList<MockSerializableEntity> list1,
			final SerializableList<MockSerializableEntity> list2,
			final int expectedDifferenceIndex,
			final boolean expectedEquals) {
		// Assert:
		Assert.assertThat(list1.equals(list2), IsEqual.equalTo(expectedEquals));
		Assert.assertThat(list2.equals(list1), IsEqual.equalTo(expectedEquals));
		Assert.assertThat(list1.findFirstDifference(list2), IsEqual.equalTo(expectedDifferenceIndex));
		Assert.assertThat(list2.findFirstDifference(list1), IsEqual.equalTo(expectedDifferenceIndex));
	}

	private static MockSerializableEntity deserializeFromObject(final Object object) {
		return new MockSerializableEntity(new JsonDeserializer((JSONObject)object, null));
	}
}
