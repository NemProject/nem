package org.nem.core.node;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

public class NodeVersionTest {

	// region constants

	@Test
	public void zeroConstantIsCorrect() {
		// Assert:
		MatcherAssert.assertThat(NodeVersion.ZERO, IsEqual.equalTo(new NodeVersion(0, 0, 0)));
	}

	// endregion

	@Test
	public void canCreateVersionWithTag() {
		// Act:
		final NodeVersion version = new NodeVersion(2, 1, 12, "BETA");

		// Assert:
		MatcherAssert.assertThat(version.getMajorVersion(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(version.getMinorVersion(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(version.getBuildVersion(), IsEqual.equalTo(12));
		MatcherAssert.assertThat(version.getTag(), IsEqual.equalTo("BETA"));
	}

	@Test
	public void canCreateVersionWithoutTag() {
		// Act:
		final NodeVersion version = new NodeVersion(2, 1, 12);

		// Assert:
		MatcherAssert.assertThat(version.getMajorVersion(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(version.getMinorVersion(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(version.getBuildVersion(), IsEqual.equalTo(12));
		MatcherAssert.assertThat(version.getTag(), IsNull.nullValue());
	}

	@Test
	public void canParseVersionStringWithTag() {
		// Act:
		final NodeVersion version = NodeVersion.parse("22.1.123-BETA");

		// Assert:
		MatcherAssert.assertThat(version.getMajorVersion(), IsEqual.equalTo(22));
		MatcherAssert.assertThat(version.getMinorVersion(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(version.getBuildVersion(), IsEqual.equalTo(123));
		MatcherAssert.assertThat(version.getTag(), IsEqual.equalTo("BETA"));
	}

	@Test
	public void canParseVersionStringWithoutTag() {
		// Act:
		final NodeVersion version = NodeVersion.parse("22.1.123");

		// Assert:
		MatcherAssert.assertThat(version.getMajorVersion(), IsEqual.equalTo(22));
		MatcherAssert.assertThat(version.getMinorVersion(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(version.getBuildVersion(), IsEqual.equalTo(123));
		MatcherAssert.assertThat(version.getTag(), IsNull.nullValue());
	}

	@Test
	public void cannotParseInvalidVersionString() {
		// Assert:
		ExceptionAssert.assertThrows(v -> NodeVersion.parse("2.1A.12"), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> NodeVersion.parse("2.1.12.7"), IllegalArgumentException.class);
	}

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NodeVersion version = new NodeVersion(2, 1, 12, "ZETA");

		// Assert:
		MatcherAssert.assertThat(new NodeVersion(2, 1, 12, "ZETA"), IsEqual.equalTo(version));
		MatcherAssert.assertThat(new NodeVersion(3, 1, 12, "ZETA"), IsNot.not(IsEqual.equalTo(version)));
		MatcherAssert.assertThat(new NodeVersion(2, 0, 12, "ZETA"), IsNot.not(IsEqual.equalTo(version)));
		MatcherAssert.assertThat(new NodeVersion(2, 1, 123, "ZETA"), IsNot.not(IsEqual.equalTo(version)));
		MatcherAssert.assertThat(new NodeVersion(2, 1, 12, "BETA"), IsNot.not(IsEqual.equalTo(version)));
		MatcherAssert.assertThat(new NodeVersion(2, 1, 12), IsNot.not(IsEqual.equalTo(version)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(version)));
		MatcherAssert.assertThat("ZETA", IsNot.not(IsEqual.equalTo((Object) version)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final NodeVersion version = new NodeVersion(2, 1, 12, "ZETA");
		final int hashCode = version.hashCode();

		// Assert:
		MatcherAssert.assertThat(new NodeVersion(2, 1, 12, "ZETA").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(new NodeVersion(3, 1, 12, "ZETA").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(new NodeVersion(2, 0, 12, "ZETA").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(new NodeVersion(2, 1, 123, "ZETA").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(new NodeVersion(2, 1, 12, "BETA").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(new NodeVersion(2, 1, 12).hashCode(), IsEqual.equalTo(hashCode));
	}

	// endregion

	// region inline serialization

	@Test
	public void canWriteVersion() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final NodeVersion version = new NodeVersion(2, 1, 12, "BETA");

		// Act:
		NodeVersion.writeTo(serializer, "Version", version);

		// Assert:
		final JSONObject object = serializer.getObject();
		MatcherAssert.assertThat(object.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(object.get("Version"), IsEqual.equalTo("2.1.12-BETA"));
	}

	@Test
	public void canRoundtripVersion() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final NodeVersion originalVersion = new NodeVersion(2, 1, 12, "BETA");

		// Act:
		NodeVersion.writeTo(serializer, "Version", originalVersion);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final NodeVersion version = NodeVersion.readFrom(deserializer, "Version");

		// Assert:
		MatcherAssert.assertThat(version, IsEqual.equalTo(originalVersion));
	}

	// endregion

	// region toString

	@Test
	public void canCreateStringRepresentationForVersionWithTag() {
		// Arrange:
		final NodeVersion version = new NodeVersion(2, 1, 12, "BETA");

		// Assert:
		MatcherAssert.assertThat(version.toString(), IsEqual.equalTo("2.1.12-BETA"));
	}

	@Test
	public void canCreateStringRepresentationForVersionWithoutTag() {
		// Arrange:
		final NodeVersion version = new NodeVersion(2, 1, 12);

		// Assert:
		MatcherAssert.assertThat(version.toString(), IsEqual.equalTo("2.1.12"));
	}

	// endregion
}
