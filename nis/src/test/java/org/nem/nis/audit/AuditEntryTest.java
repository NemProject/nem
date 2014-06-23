package org.nem.nis.audit;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class AuditEntryTest {

	@Test
	public void entryExposesConstructorParameters() {
		// Act:
		final AuditEntry entry = new AuditEntry("host", "path");

		// Assert:
		Assert.assertThat(entry.getHost(), IsEqual.equalTo("host"));
		Assert.assertThat(entry.getPath(), IsEqual.equalTo("path"));
	}

	@Test
	public void entryCanBeRoundTripped() {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(new AuditEntry("host", "path"), null);
		final AuditEntry entry = new AuditEntry(deserializer);

		// Assert:
		Assert.assertThat(entry.getHost(), IsEqual.equalTo("host"));
		Assert.assertThat(entry.getPath(), IsEqual.equalTo("path"));
	}

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final AuditEntry entry = new AuditEntry("host", "path");

		// Assert:
		Assert.assertThat(new AuditEntry("host", "path"), IsEqual.equalTo(entry));
		Assert.assertThat(new AuditEntry("host2", "path"), IsNot.not(IsEqual.equalTo(entry)));
		Assert.assertThat(new AuditEntry("host", "path2"), IsNot.not(IsEqual.equalTo(entry)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(entry)));
		Assert.assertThat("host", IsNot.not(IsEqual.equalTo((Object)entry)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final AuditEntry entry = new AuditEntry("host", "path");
		final int hashCode = entry.hashCode();

		// Assert:
		Assert.assertThat(new AuditEntry("host", "path").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new AuditEntry("host2", "path").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new AuditEntry("host", "path2").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsAppropriateRepresentation() {
		// Arrange:
		final AuditEntry entry = new AuditEntry("host", "path");

		// Assert:
		Assert.assertThat(entry.toString(), IsEqual.equalTo("host -> path"));
	}

	//endregion
}