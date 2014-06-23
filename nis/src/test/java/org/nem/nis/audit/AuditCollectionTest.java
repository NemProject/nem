package org.nem.nis.audit;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeProvider;
import org.nem.nis.test.NisUtils;

import java.util.*;

public class AuditCollectionTest {

	//region pruning

	@Test
	public void mostRecentCollectionCanNotGrowBeyondMaxSize() {
		// Arrange:
		final AuditCollection collection = new AuditCollection(3);

		// Act:
		collection.add(createEntry(1));
		collection.add(createEntry(2));
		collection.add(createEntry(3));
		collection.add(createEntry(4));

		// Assert:
		Assert.assertThat(collection.getOutstandingEntries(), IsEqual.equalTo(createEntries(1, 2, 3, 4)));
		Assert.assertThat(new ArrayList<>(collection.getMostRecentEntries()), IsEqual.equalTo(createEntries(2, 3, 4)));
	}

	//endregion

	//region add

	@Test
	public void entryIsAddedToOutstandingAndMostRecentCollections() {
		// Arrange:
		final AuditCollection collection = new AuditCollection(10);

		// Act:
		collection.add(createEntry(1));

		// Assert:
		final List<AuditEntry> expectedEntries = createEntries(1);
		Assert.assertThat(collection.getOutstandingEntries(), IsEqual.equalTo(expectedEntries));
		Assert.assertThat(new ArrayList<>(collection.getMostRecentEntries()), IsEqual.equalTo(expectedEntries));
	}

	@Test
	public void multipleEntriesCanBeAdded() {
		// Arrange:
		final AuditCollection collection = new AuditCollection(10);

		// Act:
		collection.add(createEntry(1));
		collection.add(createEntry(2));
		collection.add(createEntry(1));
		collection.add(createEntry(3));


		// Assert:
		final List<AuditEntry> expectedEntries = createEntries(1, 2, 1, 3);
		Assert.assertThat(collection.getOutstandingEntries(), IsEqual.equalTo(expectedEntries));
		Assert.assertThat(new ArrayList<>(collection.getMostRecentEntries()), IsEqual.equalTo(expectedEntries));
	}

	//endregion

	//region remove

	@Test
	public void outOfCollectionRemovalAttemptIsIgnored() {
		// Arrange:
		final AuditCollection collection = new AuditCollection(10);

		// Act:
		collection.add(createEntry(1));
		collection.add(createEntry(2));
		collection.add(createEntry(3));
		collection.remove(createEntry(4));

		// Assert:
		final List<AuditEntry> expectedEntries = createEntries(1, 2, 3);
		Assert.assertThat(collection.getOutstandingEntries(), IsEqual.equalTo(expectedEntries));
		Assert.assertThat(new ArrayList<>(collection.getMostRecentEntries()), IsEqual.equalTo(expectedEntries));
	}

	@Test
	public void entryIsRemovedFromOutstandingCollectionWhenCompleted() {
		// Arrange:
		final AuditCollection collection = new AuditCollection(10);

		// Act:
		collection.add(createEntry(1));
		collection.add(createEntry(2));
		collection.add(createEntry(3));
		collection.remove(createEntry(2));

		// Assert:
		Assert.assertThat(collection.getOutstandingEntries(), IsEqual.equalTo(createEntries(1, 3)));
		Assert.assertThat(new ArrayList<>(collection.getMostRecentEntries()), IsEqual.equalTo(createEntries(1, 2, 3)));
	}

	@Test
	public void onlyFirstMatchingEntryIsRemoved() {
		// Arrange:
		final AuditCollection collection = new AuditCollection(10);

		// Act:
		collection.add(createEntry(1));
		collection.add(createEntry(2));
		collection.add(createEntry(1));
		collection.add(createEntry(3));
		collection.remove(createEntry(1));

		// Assert:
		Assert.assertThat(collection.getOutstandingEntries(), IsEqual.equalTo(createEntries(2, 1, 3)));
		Assert.assertThat(new ArrayList<>(collection.getMostRecentEntries()), IsEqual.equalTo(createEntries(1, 2, 1, 3)));
	}

	//endregion

	//region serialization

	@Test
	public void collectionCanBeSerialized() {
		// Arrange:
		final AuditCollection collection = new AuditCollection(10);
		collection.add(createEntry(1));
		collection.add(createEntry(2));
		collection.add(createEntry(1));
		collection.add(createEntry(3));
		collection.remove(createEntry(1));
		collection.remove(createEntry(7));

		// Act:
		final JSONObject jsonObject = JsonSerializer.serializeToJson(collection);
		final JsonDeserializer deserializer = new JsonDeserializer(jsonObject, null);

		// Assert:
		Assert.assertThat(2, IsEqual.equalTo(jsonObject.size()));
		Assert.assertThat(
				deserializer.readObjectArray("outstanding", d -> d.readInt("id")),
				IsEqual.equalTo(Arrays.asList(2, 1, 3)));
		Assert.assertThat(
				deserializer.readObjectArray("most-recent", d -> d.readInt("id")),
				IsEqual.equalTo(Arrays.asList(1, 2, 1, 3)));
	}

	//endregion

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