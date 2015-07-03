package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.*;

// TODO 20150702 J-J review these tests in more detail
public class NemPropertyTest {

	@Test
	public void canCreateNemProperty() {
		// Act:
		final NemProperty property = new NemProperty("foo", "bar");

		// Assert:
		Assert.assertThat(property.getName(), IsEqual.equalTo("foo"));
		Assert.assertThat(property.getValue(), IsEqual.equalTo("bar"));
	}

	// serialization / deserialization

	@Test
	public void canRoundTripNemProperty() {
		// Arrange:
		final NemProperty original = new NemProperty("foo", "bar");

		// Act:
		final NemProperty property = new NemProperty(Utils.roundtripSerializableEntity(original, new MockAccountLookup()));

		// Assert:
		Assert.assertThat(property.getName(), IsEqual.equalTo("foo"));
		Assert.assertThat(property.getValue(), IsEqual.equalTo("bar"));
	}

	// endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NemProperty original = new NemProperty("foo", "bar");

		// Assert:
		Assert.assertThat(original, IsEqual.equalTo(new NemProperty("foo", "bar")));
		Assert.assertThat(original, IsNot.not(IsEqual.equalTo(new NemProperty("fooo", "bar"))));
		Assert.assertThat(original, IsNot.not(IsEqual.equalTo(new NemProperty("foo", "baz"))));
		Assert.assertThat(original, IsNot.not(IsEqual.equalTo(null)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = new NemProperty("foo", "bar").hashCode();

		// Assert:
		Assert.assertThat(hashCode, IsEqual.equalTo(new NemProperty("foo", "bar").hashCode()));
		Assert.assertThat(hashCode, IsNot.not(IsEqual.equalTo(new NemProperty("fooo", "bar").hashCode())));
		Assert.assertThat(hashCode, IsNot.not(IsEqual.equalTo(new NemProperty("foo", "baz").hashCode())));
	}

	// endregion
}
