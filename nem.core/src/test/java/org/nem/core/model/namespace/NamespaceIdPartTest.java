package org.nem.core.model.namespace;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.*;

import java.util.Arrays;

public class NamespaceIdPartTest {

	// region ctor

	@Test
	public void canCreateNamespaceIdPartFromValidString() {
		// Assert:
		assertIsValid("foo");
	}

	@Test
	public void canCreateNamespaceIdPartFromValidStringThatContainsAllowedSpecialCharacters() {
		// Assert:
		assertIsValid("foo-bar");
	}

	@Test
	public void canCreateNamespaceIdPartFromStringEndingWithSpecialCharacters() {
		// Assert:
		assertIsValid("f_");
	}


	private static void assertIsValid(final String s) {
		// Act:
		final NamespaceIdPart part = new NamespaceIdPart(s);

		// Assert:
		Assert.assertThat(part.toString(), IsEqual.equalTo(s));
	}

	@Test
	public void cannotCreateNamespaceIdPartFromEmptyString() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new NamespaceIdPart(""), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateNamespaceIdPartStartingWithSymbols() {
		// Assert:
		final String[] invalid = { "_", "-", "-foo", "_bar"};
		Arrays.stream(invalid).forEach(s -> ExceptionAssert.assertThrows(v -> new NamespaceIdPart(s), IllegalArgumentException.class));
	}

	@Test
	public void cannotCreateNamespaceIdPartFromStringContainingDisallowedCharacters() {
		// Assert:
		final String[] invalid = { ".", "foo.", "fooä", "foo ", "foo bar" };
		Arrays.stream(invalid).forEach(s -> ExceptionAssert.assertThrows(v -> new NamespaceIdPart(s), IllegalArgumentException.class));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsExpectedString() {
		// Arrange:
		final NamespaceIdPart part = new NamespaceIdPart("foo");

		// Act:
		final String name = part.toString();

		// Assert:
		Assert.assertThat(name, IsEqual.equalTo("foo"));
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NamespaceIdPart part = new NamespaceIdPart("foo");

		// Assert:
		Assert.assertThat(part, IsEqual.equalTo(new NamespaceIdPart("foo")));
		Assert.assertThat(part, IsNot.not(IsEqual.equalTo(new NamespaceIdPart("bar"))));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = new NamespaceIdPart("foo").hashCode();

		// Assert:
		Assert.assertThat(hashCode, IsEqual.equalTo(new NamespaceIdPart("foo").hashCode()));
		Assert.assertThat(hashCode, IsNot.not(IsEqual.equalTo(new NamespaceIdPart("bar").hashCode())));
	}

	// endregion
}

