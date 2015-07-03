package org.nem.core.model.mosaic;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.ExceptionAssert;
import wiremock.org.apache.commons.lang.StringUtils;

import java.util.*;

public class MosaicIdTest {

	//region ctor

	@Test
	public void canCreateMosaicId() {
		// Act:
		final MosaicId mosaicId = new MosaicId("foo");

		// Assert:
		Assert.assertThat(mosaicId, IsEqual.equalTo(new MosaicId("foo")));
	}

	@Test
	public void cannotCreateMosaicIdFromInvalidString() {
		createInvalidIdList().stream()
				.forEach(s -> ExceptionAssert.assertThrows(v -> new MosaicId(s), IllegalArgumentException.class));
	}

	private static List<String> createInvalidIdList() {
		return Arrays.asList("", "-id", "_id", " id", StringUtils.repeat("too long", 5));
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteToSerializer() {
		// Arrange:
		final MosaicId mosaicId = new MosaicId("foo");
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		MosaicId.writeTo(serializer, "id", mosaicId);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("id"), IsEqual.equalTo("foo"));
	}

	@Test
	public void canReadFromDeserializer() {
		// Arrange:
		final MosaicId original = new MosaicId("foo");
		final JsonSerializer serializer = new JsonSerializer();
		MosaicId.writeTo(serializer, "id", original);
		final JsonDeserializer deserializer = new JsonDeserializer(serializer.getObject(), null);

		// Act:
		final MosaicId mosaicId = MosaicId.readFrom(deserializer, "id");

		// Assert:
		Assert.assertThat(mosaicId, IsEqual.equalTo(new MosaicId("foo")));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsId() {
		// Arrange:
		final MosaicId mosaicId = new MosaicId("foo");

		// Assert:
		Assert.assertThat(mosaicId.toString(), IsEqual.equalTo("foo"));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final MosaicId mosaicId = new MosaicId("bar");

		// Assert:
		Assert.assertThat(mosaicId, IsEqual.equalTo(new MosaicId("bar")));
		Assert.assertThat(mosaicId, IsEqual.equalTo(new MosaicId("Bar")));
		Assert.assertThat(mosaicId, IsEqual.equalTo(new MosaicId("bAr")));
		Assert.assertThat(mosaicId, IsEqual.equalTo(new MosaicId("baR")));
		Assert.assertThat(mosaicId, IsNot.not(IsEqual.equalTo(new MosaicId("barr"))));
		Assert.assertThat(mosaicId, IsNot.not(IsEqual.equalTo(new MosaicId("baz"))));
		Assert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(mosaicId)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(mosaicId)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = new MosaicId("bar").hashCode();

		// Assert:
		Assert.assertThat(hashCode, IsEqual.equalTo(new MosaicId("bar").hashCode()));
		Assert.assertThat(hashCode, IsEqual.equalTo(new MosaicId("Bar").hashCode()));
		Assert.assertThat(hashCode, IsEqual.equalTo(new MosaicId("bAr").hashCode()));
		Assert.assertThat(hashCode, IsEqual.equalTo(new MosaicId("baR").hashCode()));
		Assert.assertThat(hashCode, IsNot.not(IsEqual.equalTo(new MosaicId("barr").hashCode())));
		Assert.assertThat(hashCode, IsNot.not(IsEqual.equalTo(new MosaicId("baz").hashCode())));
	}

	//endregion
}
