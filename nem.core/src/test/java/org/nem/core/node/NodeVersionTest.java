package org.nem.core.node;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.Arrays;

public class NodeVersionTest {

	//region constants

	@Test
	public void zeroConstantIsCorrect() {
		// Assert:
		Assert.assertThat(NodeVersion.ZERO, IsEqual.equalTo(new NodeVersion(0, 0, 0)));
	}

	//endregion

	@Test
	public void canCreateVersionWithTag() {
		// Act:
		final NodeVersion version = new NodeVersion(2, 1, 12, "BETA");

		// Assert:
		Assert.assertThat(version.getMajorVersion(), IsEqual.equalTo(2));
		Assert.assertThat(version.getMinorVersion(), IsEqual.equalTo(1));
		Assert.assertThat(version.getBuildVersion(), IsEqual.equalTo(12));
		Assert.assertThat(version.getTag(), IsEqual.equalTo("BETA"));
	}

	@Test
	public void canCreateVersionWithoutTag() {
		// Act:
		final NodeVersion version = new NodeVersion(2, 1, 12);

		// Assert:
		Assert.assertThat(version.getMajorVersion(), IsEqual.equalTo(2));
		Assert.assertThat(version.getMinorVersion(), IsEqual.equalTo(1));
		Assert.assertThat(version.getBuildVersion(), IsEqual.equalTo(12));
		Assert.assertThat(version.getTag(), IsNull.nullValue());
	}

	@Test
	public void canParseVersionStringWithTag() {
		// Act:
		final NodeVersion version = NodeVersion.parse("22.1.123-BETA");

		// Assert:
		Assert.assertThat(version.getMajorVersion(), IsEqual.equalTo(22));
		Assert.assertThat(version.getMinorVersion(), IsEqual.equalTo(1));
		Assert.assertThat(version.getBuildVersion(), IsEqual.equalTo(123));
		Assert.assertThat(version.getTag(), IsEqual.equalTo("BETA"));
	}

	@Test
	public void canParseVersionStringWithoutTag() {
		// Act:
		final NodeVersion version = NodeVersion.parse("22.1.123");

		// Assert:
		Assert.assertThat(version.getMajorVersion(), IsEqual.equalTo(22));
		Assert.assertThat(version.getMinorVersion(), IsEqual.equalTo(1));
		Assert.assertThat(version.getBuildVersion(), IsEqual.equalTo(123));
		Assert.assertThat(version.getTag(), IsNull.nullValue());
	}

	@Test
	public void cannotParseInvalidVersionString() {
		// Assert:
		ExceptionAssert.assertThrows(v -> NodeVersion.parse("2.1A.12"), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> NodeVersion.parse("2.1.12.7"), IllegalArgumentException.class);
	}

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NodeVersion version = new NodeVersion(2, 1, 12, "ZETA");

		// Assert:
		Assert.assertThat(new NodeVersion(2, 1, 12, "ZETA"), IsEqual.equalTo(version));
		Assert.assertThat(new NodeVersion(3, 1, 12, "ZETA"), IsNot.not(IsEqual.equalTo(version)));
		Assert.assertThat(new NodeVersion(2, 0, 12, "ZETA"), IsNot.not(IsEqual.equalTo(version)));
		Assert.assertThat(new NodeVersion(2, 1, 123, "ZETA"), IsNot.not(IsEqual.equalTo(version)));
		Assert.assertThat(new NodeVersion(2, 1, 12, "BETA"), IsNot.not(IsEqual.equalTo(version)));
		Assert.assertThat(new NodeVersion(2, 1, 12), IsNot.not(IsEqual.equalTo(version)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(version)));
		Assert.assertThat("ZETA", IsNot.not(IsEqual.equalTo((Object)version)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final NodeVersion version = new NodeVersion(2, 1, 12, "ZETA");
		final int hashCode = version.hashCode();

		// Assert:
		Assert.assertThat(new NodeVersion(2, 1, 12, "ZETA").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new NodeVersion(3, 1, 12, "ZETA").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeVersion(2, 0, 12, "ZETA").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeVersion(2, 1, 123, "ZETA").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeVersion(2, 1, 12, "BETA").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new NodeVersion(2, 1, 12).hashCode(), IsEqual.equalTo(hashCode));
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteVersion() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final NodeVersion version = new NodeVersion(2, 1, 12, "BETA");

		// Act:
		NodeVersion.writeTo(serializer, "Version", version);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("Version"), IsEqual.equalTo("2.1.12-BETA"));
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
		Assert.assertThat(version, IsEqual.equalTo(originalVersion));
	}

	//endregion

	//region serialization

	@Test
	public void canRoundTripVersion() {
		// Arrange:
		final NodeVersion originalVersion = new NodeVersion(2, 1, 12, "BETA");

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalVersion, null);
		final NodeVersion version = new NodeVersion(deserializer);

		// Assert:
		Assert.assertThat(version, IsEqual.equalTo(originalVersion));
	}

	@Test
	public void canRoundTripVersionWithNullTag() {
		// Arrange:
		final NodeVersion originalVersion = new NodeVersion(2, 1, 12, null);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalVersion, null);
		final NodeVersion version = new NodeVersion(deserializer);

		// Assert:
		Assert.assertThat(version, IsEqual.equalTo(originalVersion));
	}

	@Test
	public void cannotDeserializeWithMissingRequiredProperty() {
		Arrays.asList("majorVersion", "minorVersion", "buildVersion").stream()
				.forEach(NodeVersionTest::assertCannotDeserialize);
	}

	private static void assertCannotDeserialize(final String key) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final NodeVersion originalVersion = new NodeVersion(2, 1, 12, "BETA");
		originalVersion.serialize(serializer);
		final JSONObject jsonObject = serializer.getObject();
		jsonObject.remove(key);
		final JsonDeserializer deserializer = new JsonDeserializer(jsonObject, null);

		// Assert:
		ExceptionAssert.assertThrows(v -> new NodeVersion(deserializer), MissingRequiredPropertyException.class);
	}

	//endregion

	//region toString

	@Test
	public void canCreateStringRepresentationForVersionWithTag() {
		// Arrange:
		final NodeVersion version = new NodeVersion(2, 1, 12, "BETA");

		// Assert:
		Assert.assertThat(version.toString(), IsEqual.equalTo("2.1.12-BETA"));
	}

	@Test
	public void canCreateStringRepresentationForVersionWithoutTag() {
		// Arrange:
		final NodeVersion version = new NodeVersion(2, 1, 12);

		// Assert:
		Assert.assertThat(version.toString(), IsEqual.equalTo("2.1.12"));
	}

	//endregion
}