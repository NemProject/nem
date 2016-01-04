package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.*;

import java.util.*;

public class NemPropertyTest {

	//region constructor

	@Test
	public void canCreateNemProperty() {
		// Act:
		final NemProperty property = new NemProperty("foo", "bar");

		// Assert:
		Assert.assertThat(property.getName(), IsEqual.equalTo("foo"));
		Assert.assertThat(property.getValue(), IsEqual.equalTo("bar"));
	}

	@Test
	public void canCreateNemPropertyAroundMixedCaseStrings() {
		// Act:
		final NemProperty property = new NemProperty("FoO", "bAr");

		// Assert:
		Assert.assertThat(property.getName(), IsEqual.equalTo("FoO"));
		Assert.assertThat(property.getValue(), IsEqual.equalTo("bAr"));
	}

	//endregion

	//region serialization / deserialization

	@Test
	public void canSerializeNemProperty() {
		// Arrange:
		final NemProperty property = new NemProperty("FoO", "bAr");

		// Act:
		final JSONObject jsonObject = JsonSerializer.serializeToJson(property);

		// Assert:
		Assert.assertThat(jsonObject.size(), IsEqual.equalTo(2));
		Assert.assertThat(jsonObject.get("name"), IsEqual.equalTo("FoO"));
		Assert.assertThat(jsonObject.get("value"), IsEqual.equalTo("bAr"));
	}

	@Test
	public void canDeserializeNemProperty() {
		// Arrange:
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("name", "FoO");
		jsonObject.put("value", "bAr");

		// Act:
		final NemProperty property = new NemProperty(Utils.createDeserializer(jsonObject));

		// Assert:
		Assert.assertThat(property.getName(), IsEqual.equalTo("FoO"));
		Assert.assertThat(property.getValue(), IsEqual.equalTo("bAr"));
	}

	@Test
	public void canRoundTripNemProperty() {
		// Arrange:
		final NemProperty original = new NemProperty("FoO", "bAr");

		// Act:
		final NemProperty property = new NemProperty(Utils.roundtripSerializableEntity(original, new MockAccountLookup()));

		// Assert:
		Assert.assertThat(property.getName(), IsEqual.equalTo("FoO"));
		Assert.assertThat(property.getValue(), IsEqual.equalTo("bAr"));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsAppropriateStringRepresentation() {
		// Arrange:
		final NemProperty property = new NemProperty("Foo", "Bar");

		// Act:
		final String value = property.toString();

		// Assert:
		Assert.assertThat(value, IsEqual.equalTo("Foo -> Bar"));
	}

	//endregion

	//region equals / hashCode

	private static Map<String, NemProperty> createNemPropertiesForEqualityTests() {
		return new HashMap<String, NemProperty>() {
			{
				this.put("default", new NemProperty("foo", "bar"));
				this.put("diff-name-case", new NemProperty("FoO", "bar"));
				this.put("diff-name", new NemProperty("xyz", "bar"));
				this.put("diff-value-case", new NemProperty("foo", "zoo"));
				this.put("diff-value", new NemProperty("foo", "bAr"));
			}
		};
	}

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NemProperty property = new NemProperty("foo", "bar");

		// Assert:
		for (final Map.Entry<String, NemProperty> entry : createNemPropertiesForEqualityTests().entrySet()) {
			Assert.assertThat(
					entry.getValue(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(property)) : IsEqual.equalTo(property));
		}

		Assert.assertThat(property, IsNot.not(IsEqual.equalTo("foo")));
		Assert.assertThat(property, IsNot.not(IsEqual.equalTo(null)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = new NemProperty("foo", "bar").hashCode();

		// Assert:
		for (final Map.Entry<String, NemProperty> entry : createNemPropertiesForEqualityTests().entrySet()) {
			Assert.assertThat(
					entry.getValue().hashCode(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(hashCode)) : IsEqual.equalTo(hashCode));
		}
	}

	private static boolean isDiffExpected(final String propertyName) {
		return !propertyName.equals("default");
	}

	// endregion
}
