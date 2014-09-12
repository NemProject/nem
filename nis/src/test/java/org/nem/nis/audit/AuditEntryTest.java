package org.nem.nis.audit;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;
import org.nem.core.time.*;

public class AuditEntryTest {

	@Test
	public void entryExposesConstructorParameters() {
		// Act:
		final TimeProvider timeProvider = Utils.createMockTimeProvider(12, 53, 60);
		final AuditEntry entry = new AuditEntry(7, "localhost", "/chain/last-block", timeProvider);

		// Assert:
		Assert.assertThat(entry.getId(), IsEqual.equalTo(7));
		Assert.assertThat(entry.getHost(), IsEqual.equalTo("localhost"));
		Assert.assertThat(entry.getPath(), IsEqual.equalTo("/chain/last-block"));
		Assert.assertThat(entry.getStartTime(), IsEqual.equalTo(new TimeInstant(12)));
	}

	@Test
	public void getElapsedTimeReturnsCurrentElapsedTime() {
		// Act:
		final TimeProvider timeProvider = Utils.createMockTimeProvider(12, 53, 60);
		final AuditEntry entry = new AuditEntry(7, "host", "path", timeProvider);

		// Assert:
		Assert.assertThat(entry.getElapsedTime(), IsEqual.equalTo(41));
		Assert.assertThat(entry.getElapsedTime(), IsEqual.equalTo(48));
	}

	@Test
	public void entryCanBeSerialized() {
		// Arrange:
		final TimeProvider timeProvider = Utils.createMockTimeProvider(12, 53, 60);
		final AuditEntry entry = new AuditEntry(7, "localhost", "/chain/last-block", timeProvider);

		// Act:
		final JSONObject jsonObject = JsonSerializer.serializeToJson(entry);
		final JsonDeserializer deserializer = new JsonDeserializer(jsonObject, null);

		// Assert:
		Assert.assertThat(5, IsEqual.equalTo(jsonObject.size()));
		Assert.assertThat(deserializer.readInt("id"), IsEqual.equalTo(7));
		Assert.assertThat(deserializer.readString("host"), IsEqual.equalTo("localhost"));
		Assert.assertThat(deserializer.readString("path"), IsEqual.equalTo("/chain/last-block"));
		Assert.assertThat(deserializer.readInt("start-time"), IsEqual.equalTo(12));
		Assert.assertThat(deserializer.readInt("elapsed-time"), IsEqual.equalTo(41));
	}

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		final AuditEntry entry = new AuditEntry(7, "host", "path", timeProvider);

		// Assert:
		Assert.assertThat(new AuditEntry(7, "host", "path", timeProvider), IsEqual.equalTo(entry));
		Assert.assertThat(new AuditEntry(8, "host", "path", timeProvider), IsEqual.equalTo(entry));
		Assert.assertThat(new AuditEntry(7, "host2", "path", timeProvider), IsNot.not(IsEqual.equalTo(entry)));
		Assert.assertThat(new AuditEntry(7, "host", "path2", timeProvider), IsNot.not(IsEqual.equalTo(entry)));
		Assert.assertThat(new AuditEntry(7, "host", "path", Mockito.mock(TimeProvider.class)), IsEqual.equalTo(entry));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(entry)));
		Assert.assertThat("host", IsNot.not(IsEqual.equalTo((Object)entry)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		final AuditEntry entry = new AuditEntry(7, "host", "path", timeProvider);
		final int hashCode = entry.hashCode();

		// Assert:
		Assert.assertThat(new AuditEntry(7, "host", "path", timeProvider).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new AuditEntry(8, "host", "path", timeProvider).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new AuditEntry(7, "host2", "path", timeProvider).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new AuditEntry(7, "host", "path2", timeProvider).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new AuditEntry(7, "host", "path", Mockito.mock(TimeProvider.class)).hashCode(), IsEqual.equalTo(hashCode));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsAppropriateRepresentation() {
		// Arrange:
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		final AuditEntry entry = new AuditEntry(7, "localhost", "/chain/last-block", timeProvider);

		// Assert:
		Assert.assertThat(entry.toString(), IsEqual.equalTo("#7 (localhost -> /chain/last-block)"));
	}

	//endregion
}