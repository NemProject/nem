package org.nem.nis.audit;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.IsEquivalent;

import java.util.*;

public class AuditCollectionTest {

	//region pruning

	@Test
	public void mostRecentCollectionCanNotGrowBeyondMaxSize() {
		// Arrange:
		final AuditCollection collection = new AuditCollection(3);

		// Act:
		collection.add(new AuditEntry("1", "1"));
		collection.add(new AuditEntry("2", "2"));
		collection.add(new AuditEntry("3", "3"));
		collection.add(new AuditEntry("4", "4"));

		// Assert:
		final List<AuditEntry> expectedOutstandingEntries = Arrays.asList(
				new AuditEntry("1", "1"),
				new AuditEntry("2", "2"),
				new AuditEntry("3", "3"),
				new AuditEntry("4", "4"));
		final List<AuditEntry> expectedMostRecentEntries = Arrays.asList(
				new AuditEntry("2", "2"),
				new AuditEntry("3", "3"),
				new AuditEntry("4", "4"));
		Assert.assertThat(collection.getOutstandingEntries(), IsEquivalent.equivalentTo(expectedOutstandingEntries));
		Assert.assertThat(collection.getMostRecentEntries(), IsEquivalent.equivalentTo(expectedMostRecentEntries));
	}

	//endregion

	//region add

	@Test
	public void entryIsAddedToOutstandingAndMostRecentCollections() {
		// Arrange:
		final AuditCollection collection = new AuditCollection(10);

		// Act:
		collection.add(new AuditEntry("1", "1"));

		// Assert:
		final List<AuditEntry> expectedEntries = Arrays.asList(new AuditEntry("1", "1"));
		Assert.assertThat(collection.getOutstandingEntries(), IsEquivalent.equivalentTo(expectedEntries));
		Assert.assertThat(collection.getMostRecentEntries(), IsEquivalent.equivalentTo(expectedEntries));
	}

	@Test
	public void multipleEntriesCanBeAdded() {
		// Arrange:
		final AuditCollection collection = new AuditCollection(10);

		// Act:
		collection.add(new AuditEntry("1", "1"));
		collection.add(new AuditEntry("2", "2"));
		collection.add(new AuditEntry("1", "1"));
		collection.add(new AuditEntry("3", "3"));


		// Assert:
		final List<AuditEntry> expectedEntries = Arrays.asList(
				new AuditEntry("1", "1"),
				new AuditEntry("2", "2"),
				new AuditEntry("1", "1"),
				new AuditEntry("3", "3"));
		Assert.assertThat(collection.getOutstandingEntries(), IsEquivalent.equivalentTo(expectedEntries));
		Assert.assertThat(collection.getMostRecentEntries(), IsEquivalent.equivalentTo(expectedEntries));
	}

	//endregion

	//region remove

	@Test
	public void entryIsRemovedFromOutstandingCollectionWhenCompleted() {
		// Arrange:
		final AuditCollection collection = new AuditCollection(10);

		// Act:
		collection.add(new AuditEntry("1", "1"));
		collection.add(new AuditEntry("2", "2"));
		collection.add(new AuditEntry("3", "3"));
		collection.remove(new AuditEntry("2", "2"));

		// Assert:
		final List<AuditEntry> expectedOutstandingEntries = Arrays.asList(
				new AuditEntry("1", "1"),
				new AuditEntry("3", "3"));
		final List<AuditEntry> expectedMostRecentEntries = Arrays.asList(
				new AuditEntry("1", "1"),
				new AuditEntry("2", "2"),
				new AuditEntry("3", "3"));
		Assert.assertThat(collection.getOutstandingEntries(), IsEquivalent.equivalentTo(expectedOutstandingEntries));
		Assert.assertThat(collection.getMostRecentEntries(), IsEquivalent.equivalentTo(expectedMostRecentEntries));
	}

	@Test
	public void onlyFirstMatchingEntryIsRemoved() {
		// Arrange:
		final AuditCollection collection = new AuditCollection(10);

		// Act:
		collection.add(new AuditEntry("1", "1"));
		collection.add(new AuditEntry("2", "2"));
		collection.add(new AuditEntry("1", "1"));
		collection.add(new AuditEntry("3", "3"));
		collection.remove(new AuditEntry("1", "1"));

		// Assert:
		final List<AuditEntry> expectedOutstandingEntries = Arrays.asList(
				new AuditEntry("2", "2"),
				new AuditEntry("1", "1"),
				new AuditEntry("3", "3"));
		final List<AuditEntry> expectedMostRecentEntries = Arrays.asList(
				new AuditEntry("1", "1"),
				new AuditEntry("2", "2"),
				new AuditEntry("1", "1"),
				new AuditEntry("3", "3"));
		Assert.assertThat(collection.getOutstandingEntries(), IsEquivalent.equivalentTo(expectedOutstandingEntries));
		Assert.assertThat(collection.getMostRecentEntries(), IsEquivalent.equivalentTo(expectedMostRecentEntries));
	}

	//endregion

	//region serialization

	@Test
	public void collectionCanBeSerialized() {
		// Arrange:
		final AuditCollection collection = new AuditCollection(10);
		collection.add(new AuditEntry("1", "1"));
		collection.add(new AuditEntry("2", "2"));
		collection.add(new AuditEntry("1", "1"));
		collection.add(new AuditEntry("3", "3"));
		collection.remove(new AuditEntry("1", "1"));

		// Act:
		final JSONObject jsonObject = JsonSerializer.serializeToJson(collection);
		final JsonDeserializer deserializer = new JsonDeserializer(jsonObject, null);

		// Assert:
		final List<AuditEntry> expectedOutstandingEntries = Arrays.asList(
				new AuditEntry("2", "2"),
				new AuditEntry("1", "1"),
				new AuditEntry("3", "3"));
		final List<AuditEntry> expectedMostRecentEntries = Arrays.asList(
				new AuditEntry("1", "1"),
				new AuditEntry("2", "2"),
				new AuditEntry("1", "1"),
				new AuditEntry("3", "3"));
		Assert.assertThat(2, IsEqual.equalTo(jsonObject.size()));
		Assert.assertThat(
				deserializer.readObjectArray("outstanding", AuditEntry::new),
				IsEquivalent.equivalentTo(expectedOutstandingEntries));
		Assert.assertThat(
				deserializer.readObjectArray("most-recent", AuditEntry::new),
				IsEquivalent.equivalentTo(expectedMostRecentEntries));
	}

	//endregion
}