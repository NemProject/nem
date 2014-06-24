package org.nem.nis.audit;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeProvider;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.stream.Collectors;

public class AuditCollectionTest {

	//region ids and times

	@Test
	public void collectionGeneratesUniqueAuditEntryIds() {
		// Arrange:
		final AuditCollection collection = createCollection(10);

		// Act:
		collection.add("a", "a");
		collection.add("b", "b");
		collection.add("c", "c");
		collection.remove("b", "b");
		collection.add("d", "d");
		collection.remove("d", "d");
		collection.add("e", "e");

		final List<Integer> ids = collection.getMostRecentEntries().stream()
				.map(AuditEntry::getId)
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(ids, IsEqual.equalTo(Arrays.asList(5, 4, 3, 2, 1)));
	}

	@Test
	public void collectionSetsStartTimeOnAdd() {
		// Arrange:
		final TimeProvider timeProvider = NisUtils.createMockTimeProvider(1, 4, 6, 7, 9, 11, 17);
		final AuditCollection collection = new AuditCollection(10, timeProvider);

		// Act:
		collection.add("a", "a");
		collection.add("b", "b");
		collection.add("c", "c");
		collection.remove("b", "b");
		collection.add("d", "d");
		collection.remove("d", "d");
		collection.add("e", "e");

		final List<Integer> startTimes = collection.getMostRecentEntries().stream()
				.map(e -> e.getStartTime().getRawTime())
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(startTimes, IsEqual.equalTo(Arrays.asList(17, 9, 6, 4, 1)));
	}

	//endregion

	//region pruning

	@Test
	public void mostRecentCollectionCanNotGrowBeyondMaxSize() {
		// Arrange:
		final AuditCollection collection = createCollection(3);

		// Act:
		collection.add("1", "1");
		collection.add("2", "2");
		collection.add("3", "3");
		collection.add("4", "4");

		// Assert:
		Assert.assertThat(collection.getOutstandingEntries(), IsEqual.equalTo(createEntries(1, 2, 3, 4)));
		Assert.assertThat(new ArrayList<>(collection.getMostRecentEntries()), IsEqual.equalTo(createEntries(4, 3, 2)));
	}

	//endregion

	//region add

	@Test
	public void entryIsAddedToOutstandingAndMostRecentCollections() {
		// Arrange:
		final AuditCollection collection = createCollection(10);

		// Act:
		collection.add("1", "1");

		// Assert:
		Assert.assertThat(collection.getOutstandingEntries(), IsEqual.equalTo(createEntries(1)));
		Assert.assertThat(new ArrayList<>(collection.getMostRecentEntries()), IsEqual.equalTo(createEntries(1)));
	}

	@Test
	public void multipleEntriesCanBeAdded() {
		// Arrange:
		final AuditCollection collection = createCollection(10);

		// Act:
		collection.add("1", "1");
		collection.add("2", "2");
		collection.add("1", "1");
		collection.add("3", "3");


		// Assert:
		Assert.assertThat(collection.getOutstandingEntries(), IsEqual.equalTo(createEntries(1, 2, 1, 3)));
		Assert.assertThat(new ArrayList<>(collection.getMostRecentEntries()), IsEqual.equalTo(createEntries(3, 1, 2, 1)));
	}

	//endregion

	//region remove

	@Test
	public void outOfCollectionRemovalAttemptIsIgnored() {
		// Arrange:
		final AuditCollection collection = createCollection(10);

		// Act:
		collection.add("1", "1");
		collection.add("2", "2");
		collection.add("3", "3");
		collection.remove("4", "4");

		// Assert:
		Assert.assertThat(collection.getOutstandingEntries(), IsEqual.equalTo(createEntries(1, 2, 3)));
		Assert.assertThat(new ArrayList<>(collection.getMostRecentEntries()), IsEqual.equalTo(createEntries(3, 2, 1)));
	}

	@Test
	public void entryIsRemovedFromOutstandingCollectionWhenCompleted() {
		// Arrange:
		final AuditCollection collection = createCollection(10);

		// Act:
		collection.add("1", "1");
		collection.add("2", "2");
		collection.add("3", "3");
		collection.remove("2", "2");

		// Assert:
		Assert.assertThat(collection.getOutstandingEntries(), IsEqual.equalTo(createEntries(1, 3)));
		Assert.assertThat(new ArrayList<>(collection.getMostRecentEntries()), IsEqual.equalTo(createEntries(3, 2, 1)));
	}

	@Test
	public void onlyFirstMatchingEntryIsRemoved() {
		// Arrange:
		final AuditCollection collection = createCollection(10);

		// Act:
		collection.add("1", "1");
		collection.add("2", "2");
		collection.add("1", "1");
		collection.add("3", "3");
		collection.remove("1", "1");

		// Assert:
		Assert.assertThat(collection.getOutstandingEntries(), IsEqual.equalTo(createEntries(2, 1, 3)));
		Assert.assertThat(new ArrayList<>(collection.getMostRecentEntries()), IsEqual.equalTo(createEntries(3, 1, 2, 1)));
	}

	//endregion

	//region serialization

	@Test
	public void collectionCanBeSerialized() {
		// Arrange:
		final AuditCollection collection = createCollection(10);
		collection.add("1", "1");
		collection.add("2", "2");
		collection.add("1", "1");
		collection.add("3", "3");
		collection.remove("1", "1");
		collection.remove("7", "7");

		// Act:
		final JSONObject jsonObject = JsonSerializer.serializeToJson(collection);
		final JsonDeserializer deserializer = new JsonDeserializer(jsonObject, null);

		// Assert:
		Assert.assertThat(2, IsEqual.equalTo(jsonObject.size()));
		Assert.assertThat(
				deserializer.readObjectArray("outstanding", d -> Integer.parseInt(d.readString("host"))),
				IsEqual.equalTo(Arrays.asList(2, 1, 3)));
		Assert.assertThat(
				deserializer.readObjectArray("most-recent", d -> Integer.parseInt(d.readString("host"))),
				IsEqual.equalTo(Arrays.asList(3, 1, 2, 1)));
	}

	//endregion

	private static AuditCollection createCollection(final int maxEntries) {
		final TimeProvider timeProvider = NisUtils.createMockTimeProvider(1, 3);
		return new AuditCollection(maxEntries, timeProvider);
	}

	private static AuditEntry createEntry(final int id) {
		final TimeProvider timeProvider = NisUtils.createMockTimeProvider(1, 3);
		final String idAsString = String.format("%d", id);
		return new AuditEntry(id, idAsString, idAsString, timeProvider);
	}

	private static List<AuditEntry> createEntries(final int... ids) {
		final List<AuditEntry> entries = new ArrayList<>();
		for (final int id : ids)
			entries.add(createEntry(id));

		return entries;
	}
}