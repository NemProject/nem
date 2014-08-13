package org.nem.core.serialization;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.*;

import java.util.*;
import java.util.function.Function;

public class SerializableListTest {

	//region Constructors

	@Test
	public void ctorCapacityHasNoImpactOnSize() {
		// Act:
		final SerializableList<MockSerializableEntity> list = new SerializableList<>(100);

		// Assert:
		Assert.assertThat(list.size(), IsEqual.equalTo(0));
		Assert.assertThat(list.getLabel(), IsEqual.equalTo("data"));
	}

	@Test
	public void ctorCapacityCanSpecifyCustomLabel() {
		// Act:
		final SerializableList<MockSerializableEntity> list = new SerializableList<>(100, "items");

		// Assert:
		Assert.assertThat(list.size(), IsEqual.equalTo(0));
		Assert.assertThat(list.getLabel(), IsEqual.equalTo("items"));
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
		Assert.assertThat(list.getLabel(), IsEqual.equalTo("data"));
	}

	@Test
	public void ctorListCanSpecifyCustomLabel() {
		// Arrange:
		final List<MockSerializableEntity> rawList = new ArrayList<>();
		rawList.add(new MockSerializableEntity());
		rawList.add(new MockSerializableEntity());

		// Act:
		final SerializableList<MockSerializableEntity> list = new SerializableList<>(rawList, "items");

		// Assert:
		Assert.assertThat(list.size(), IsEqual.equalTo(2));
		Assert.assertThat(list.get(0), IsSame.sameInstance(rawList.get(0)));
		Assert.assertThat(list.get(1), IsSame.sameInstance(rawList.get(1)));
		Assert.assertThat(list.getLabel(), IsEqual.equalTo("items"));
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

	//region asCollection

	@Test
	public void asCollectionReturnsRawCollection() {
		// Arrange:
		final List<MockSerializableEntity> rawList = Arrays.asList(
				new MockSerializableEntity(12, "a", 12),
				new MockSerializableEntity(4, "b", 4),
				new MockSerializableEntity(122, "c", 122));
		final SerializableList<MockSerializableEntity> list = new SerializableList<>(rawList);

		// Assert:
		Assert.assertThat(list.asCollection(), IsEquivalent.equivalentTo(rawList));
	}

	//endregion

	//region Serialization

	@Test
	public void canSerializeListWithDefaultLabel() {
		// Assert:
		assertSerializedData(list -> {
			final SerializableList<MockSerializableEntity> serializableList = new SerializableList<>(10);
			list.forEach(serializableList::add);
			return serializableList;
		}, "data");
		assertSerializedData(SerializableList::new, "data");
	}

	@Test
	public void canSerializeListWithCustomLabel() {
		// Assert:
		assertSerializedData(list -> {
			final SerializableList<MockSerializableEntity> serializableList = new SerializableList<>(10, "objects");
			list.forEach(serializableList::add);
			return serializableList;
		}, "objects");
		assertSerializedData(list -> new SerializableList<>(list, "objects"), "objects");
	}

	private static void assertSerializedData(
			final Function<List<MockSerializableEntity>, SerializableList<MockSerializableEntity>> factory,
			final String expectedArrayName) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final SerializableList<MockSerializableEntity> list1 = factory.apply(
				Arrays.asList(new MockSerializableEntity(5, "foo", 6), new MockSerializableEntity(8, "bar", 7)));

		// Act:
		list1.serialize(serializer);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		final JSONArray dataArray = (JSONArray)object.get(expectedArrayName);
		Assert.assertThat(dataArray.size(), IsEqual.equalTo(2));
		Assert.assertThat(deserializeFromObject(dataArray.get(0)), IsEqual.equalTo(list1.get(0)));
		Assert.assertThat(deserializeFromObject(dataArray.get(1)), IsEqual.equalTo(list1.get(1)));
	}

	@Test
	public void canRoundTripList() {
		// Arrange:
		final SerializableList<MockSerializableEntity> originalList = new SerializableList<>(10);
		originalList.add(new MockSerializableEntity(5, "foo", 6));
		originalList.add(new MockSerializableEntity(8, "bar", 7));

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalList, null);
		final SerializableList<MockSerializableEntity> list =
				new SerializableList<>(deserializer, MockSerializableEntity::new);

		// Assert:
		Assert.assertThat(list.asCollection(), IsEquivalent.equivalentTo(originalList.asCollection()));
	}

	@Test
	public void canRoundTripListWithCustomLabel() {
		// Arrange:
		final SerializableList<MockSerializableEntity> originalList = new SerializableList<>(10, "objects");
		originalList.add(new MockSerializableEntity(5, "foo", 6));
		originalList.add(new MockSerializableEntity(8, "bar", 7));

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalList, null);
		final SerializableList<MockSerializableEntity> list =
				new SerializableList<>(deserializer, MockSerializableEntity::new, "objects");

		// Assert:
		Assert.assertThat(list.asCollection(), IsEquivalent.equivalentTo(originalList.asCollection()));
	}

	//endregion

	//region hashCode / equals

	@Test
	public void hashCodeIsConsistentForUnchangedList() {
		// Arrange:
		final SerializableList<MockSerializableEntity> list = new SerializableList<>(10);
		final int hashCode = list.hashCode();

		// Assert:
		Assert.assertThat(list.hashCode(), IsEqual.equalTo(hashCode));
	}

	@Test
	public void hashCodeChangesWhenListChanges() {
		// Arrange:
		final SerializableList<MockSerializableEntity> list = new SerializableList<>(10);
		final int hashCode = list.hashCode();

		// Act:
		list.add(new MockSerializableEntity());

		// Assert:
		Assert.assertThat(list.hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final List<MockSerializableEntity> entities1 = Arrays.asList(
				new MockSerializableEntity(5, "foo", 6),
				new MockSerializableEntity(8, "bar", 7));
		final List<MockSerializableEntity> entities2 = Arrays.asList(
				new MockSerializableEntity(5, "foo", 6),
				new MockSerializableEntity(8, "bar", 8));

		final SerializableList<MockSerializableEntity> list1 = new SerializableList<>(entities1);
		final SerializableList<MockSerializableEntity> list2 = new SerializableList<>(entities1);
		final SerializableList<MockSerializableEntity> list3 = new SerializableList<>(entities2);

		// Assert:
		Assert.assertThat(list2, IsEqual.equalTo(list1));
		Assert.assertThat(list3, IsNot.not(IsEqual.equalTo(list1)));
		Assert.assertThat(entities1, IsNot.not((Object)IsEqual.equalTo(list1)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(list1)));
	}

	//endregion

	private static void assertListComparison(
			final SerializableList<MockSerializableEntity> list1,
			final SerializableList<MockSerializableEntity> list2,
			final int expectedDifferenceIndex,
			final boolean expectedEquals) {
		// Assert:
		if (expectedEquals) {
			Assert.assertThat(list1, IsEqual.equalTo(list2));
			Assert.assertThat(list2, IsEqual.equalTo(list1));
		} else {
			Assert.assertThat(list1, IsNot.not(IsEqual.equalTo(list2)));
			Assert.assertThat(list2, IsNot.not(IsEqual.equalTo(list1)));
		}

		Assert.assertThat(list1.findFirstDifference(list2), IsEqual.equalTo(expectedDifferenceIndex));
		Assert.assertThat(list2.findFirstDifference(list1), IsEqual.equalTo(expectedDifferenceIndex));
	}

	private static MockSerializableEntity deserializeFromObject(final Object object) {
		return new MockSerializableEntity(new JsonDeserializer((JSONObject)object, null));
	}
}
