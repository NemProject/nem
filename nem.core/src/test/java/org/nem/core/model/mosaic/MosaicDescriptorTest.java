package org.nem.core.model.mosaic;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.ExceptionAssert;
import wiremock.org.apache.commons.lang.StringUtils;

import java.util.*;

public class MosaicDescriptorTest {

	//region ctor

	@Test
	public void canCreateMosaicDescriptor() {
		// Act:
		final MosaicDescriptor descriptor = new MosaicDescriptor("Alice's vouchers");

		// Assert:
		Assert.assertThat(descriptor.toString(), IsEqual.equalTo("Alice's vouchers"));
	}

	@Test
	public void cannotCreateMosaicDescriptorFromInvalidString() {
		createInvalidIdList().stream()
				.forEach(s -> ExceptionAssert.assertThrows(v -> new MosaicDescriptor(s), IllegalArgumentException.class));
	}

	private static List<String> createInvalidIdList() {
		return Arrays.asList(null, "", "  ", "_foo", "-bar", StringUtils.repeat("too long", 65));
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteToSerializer() {
		// Arrange:
		final MosaicDescriptor descriptor = new MosaicDescriptor("Alice's vouchers");
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		MosaicDescriptor.writeTo(serializer, "description", descriptor);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("description"), IsEqual.equalTo("Alice's vouchers"));
	}

	@Test
	public void canReadFromDeserializer() {
		// Arrange:
		final MosaicDescriptor original = new MosaicDescriptor("Alice's vouchers");
		final JsonSerializer serializer = new JsonSerializer();
		MosaicDescriptor.writeTo(serializer, "description", original);
		final JsonDeserializer deserializer = new JsonDeserializer(serializer.getObject(), null);

		// Act:
		final MosaicDescriptor descriptor = MosaicDescriptor.readFrom(deserializer, "description");

		// Assert:
		Assert.assertThat(descriptor.toString(), IsEqual.equalTo("Alice's vouchers"));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsId() {
		// Arrange:
		final MosaicDescriptor descriptor = new MosaicDescriptor("Alice's vouchers");

		// Assert:
		Assert.assertThat(descriptor.toString(), IsEqual.equalTo("Alice's vouchers"));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final MosaicDescriptor descriptor = new MosaicDescriptor("bar");

		// Assert:
		Assert.assertThat(descriptor, IsEqual.equalTo(new MosaicDescriptor("bar")));
		Assert.assertThat(descriptor, IsNot.not(IsEqual.equalTo(new MosaicDescriptor("Bar"))));
		Assert.assertThat(descriptor, IsNot.not(IsEqual.equalTo(new MosaicDescriptor("bAr"))));
		Assert.assertThat(descriptor, IsNot.not(IsEqual.equalTo(new MosaicDescriptor("baR"))));
		Assert.assertThat(descriptor, IsNot.not(IsEqual.equalTo(new MosaicDescriptor("barr"))));
		Assert.assertThat(descriptor, IsNot.not(IsEqual.equalTo(new MosaicDescriptor("baz"))));
		Assert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(descriptor)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(descriptor)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = new MosaicDescriptor("bar").hashCode();

		// Assert:
		Assert.assertThat(hashCode, IsEqual.equalTo(new MosaicDescriptor("bar").hashCode()));
		Assert.assertThat(hashCode, IsNot.not(IsEqual.equalTo(new MosaicDescriptor("Bar").hashCode())));
		Assert.assertThat(hashCode, IsNot.not(IsEqual.equalTo(new MosaicDescriptor("bAr").hashCode())));
		Assert.assertThat(hashCode, IsNot.not(IsEqual.equalTo(new MosaicDescriptor("baR").hashCode())));
		Assert.assertThat(hashCode, IsNot.not(IsEqual.equalTo(new MosaicDescriptor("barr").hashCode())));
		Assert.assertThat(hashCode, IsNot.not(IsEqual.equalTo(new MosaicDescriptor("baz").hashCode())));
	}

	//endregion
}
