package org.nem.core.model.mosaic;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.ExceptionAssert;
import wiremock.org.apache.commons.lang.StringUtils;

import java.util.*;

public class MosaicDescriptorTest {
	private static final String MAX_LENGTH_DESCRIPTION = StringUtils.repeat("abcd", 128);

	//region ctor

	@Test
	public void canCreateMosaicDescriptor() {
		// Act:
		final MosaicDescriptor descriptor = new MosaicDescriptor("Alice's vouchers");

		// Assert:
		Assert.assertThat(descriptor.toString(), IsEqual.equalTo("Alice's vouchers"));
	}

	@Test
	public void cannotCreateMosaicDescriptorWithInvalidDescription() {
		// Arrange:
		final List<String> invalidDescriptions = new ArrayList<>(createEmptyDescriptionList());
		invalidDescriptions.add(MAX_LENGTH_DESCRIPTION + "!");

		// Assert:
		invalidDescriptions.stream()
				.forEach(s -> ExceptionAssert.assertThrows(v -> new MosaicDescriptor(s), IllegalArgumentException.class));
	}

	private static List<String> createEmptyDescriptionList() {
		return Arrays.asList(null, "", "  ", "\t  \t");
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
		// Act:
		final MosaicDescriptor descriptor = readFrom("Alice's vouchers");

		// Assert:
		Assert.assertThat(descriptor, IsEqual.equalTo(new MosaicDescriptor("Alice's vouchers")));
	}

	@Test
	public void cannotReadFromDeserializerWithEmptyDescription() {
		// Act:
		createEmptyDescriptionList().stream()
				.forEach(s -> ExceptionAssert.assertThrows(v -> readFrom(s), MissingRequiredPropertyException.class));
	}

	@Test
	public void canReadTruncatedDescriptionFromDeserializerWithTooLongDescription() {
		// Act:
		final MosaicDescriptor descriptor = readFrom(MAX_LENGTH_DESCRIPTION + "!");

		// Assert:
		Assert.assertThat(descriptor, IsEqual.equalTo(new MosaicDescriptor(MAX_LENGTH_DESCRIPTION)));
	}

	private static MosaicDescriptor readFrom(final String description) {
		// Arrange:
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("description", description);
		final JsonDeserializer deserializer = new JsonDeserializer(jsonObject, null);

		// Act:
		return MosaicDescriptor.readFrom(deserializer, "description");
	}

	@Test
	public void canRoundTripMosaicDescription() {
		// Assert:
		assertCanRoundTripMosaicDescription("Alice's vouchers");
	}

	@Test
	public void canRoundTripMosaicDescriptionWithMaxLength() {
		// Assert:
		assertCanRoundTripMosaicDescription(MAX_LENGTH_DESCRIPTION);
	}

	private static void assertCanRoundTripMosaicDescription(final String description) {
		// Arrange:
		final MosaicDescriptor original = new MosaicDescriptor(description);
		final JsonSerializer serializer = new JsonSerializer();
		MosaicDescriptor.writeTo(serializer, "description", original);
		final JsonDeserializer deserializer = new JsonDeserializer(serializer.getObject(), null);

		// Act:
		final MosaicDescriptor descriptor = MosaicDescriptor.readFrom(deserializer, "description");

		// Assert:
		Assert.assertThat(descriptor, IsEqual.equalTo(new MosaicDescriptor(description)));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsDescription() {
		// Arrange:
		final MosaicDescriptor descriptor = new MosaicDescriptor("Bob's vouchers");

		// Assert:
		Assert.assertThat(descriptor.toString(), IsEqual.equalTo("Bob's vouchers"));
	}

	//endregion

	//region equals / hashCode

	private static Map<String, MosaicDescriptor> createMosaicDescriptorsForEqualityTests() {
		return new HashMap<String, MosaicDescriptor>() {
			{
				this.put("default", new MosaicDescriptor("bar"));
				this.put("diff-case-1", new MosaicDescriptor("Bar"));
				this.put("diff-case-2", new MosaicDescriptor("bAr"));
				this.put("diff-case-2", new MosaicDescriptor("baR"));
				this.put("diff-1", new MosaicDescriptor("barr"));
				this.put("diff-2", new MosaicDescriptor("baz"));
			}
		};
	}

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final MosaicDescriptor descriptor = new MosaicDescriptor("bar");

		// Assert:
		for (final Map.Entry<String, MosaicDescriptor> entry : createMosaicDescriptorsForEqualityTests().entrySet()) {
			Assert.assertThat(
					entry.getValue(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(descriptor)) : IsEqual.equalTo(descriptor));
		}

		Assert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(descriptor)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(descriptor)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = new MosaicDescriptor("bar").hashCode();

		// Assert:
		for (final Map.Entry<String, MosaicDescriptor> entry : createMosaicDescriptorsForEqualityTests().entrySet()) {
			Assert.assertThat(
					entry.getValue().hashCode(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(hashCode)) : IsEqual.equalTo(hashCode));
		}
	}

	private static boolean isDiffExpected(final String propertyName) {
		return !propertyName.equals("default");
	}

	//endregion
}
