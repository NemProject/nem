package org.nem.core.model.namespace;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

import java.util.*;

public class NamespaceIdPartTest {

	// region ctor

	@Test
	public void canCreateNamespaceIdPartFromValidString() {
		// Arrange:
		final String[] validNames = {
				"foo",
				"foo-bar",
				"f_"
		};

		// Act:
		Arrays.stream(validNames).forEach(name -> {
			final NamespaceIdPart part = new NamespaceIdPart(name);

			// Assert:
			Assert.assertThat(part.toString(), IsEqual.equalTo(name));
		});
	}

	@Test
	public void canCreateNamespaceIdPartWithUppercaseCharactersThatAreAutomaticallyLowercased() {
		// Act:
		final NamespaceIdPart part = new NamespaceIdPart("FoO");

		// Assert:
		Assert.assertThat(part.toString(), IsEqual.equalTo("foo"));
	}

	@Test
	public void cannotCreateNamespaceIdPartFromEmptyString() {
		// Assert:
		for (final String name : Arrays.asList(null, "", " \t ")) {
			ExceptionAssert.assertThrows(
					v -> new NamespaceIdPart(name),
					IllegalArgumentException.class);
		}
	}

	@Test
	public void cannotCreateNamespaceIdPartStartingWithSymbols() {
		// Assert:
		final String[] invalid = { "_", "-", "-foo", "_bar" };
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

	private static Map<String, NamespaceIdPart> createPartsForEqualityTests() {
		return new HashMap<String, NamespaceIdPart>() {
			{
				this.put("default", new NamespaceIdPart("foo"));
				this.put("diff-case", new NamespaceIdPart("FoO"));
				this.put("diff", new NamespaceIdPart("bar"));
			}
		};
	}

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NamespaceIdPart part = new NamespaceIdPart("foo");

		// Assert:
		for (final Map.Entry<String, NamespaceIdPart> entry : createPartsForEqualityTests().entrySet()) {
			Assert.assertThat(
					entry.getValue(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(part)) : IsEqual.equalTo(part));
		}

		Assert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(part)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(part)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = new NamespaceIdPart("foo").hashCode();

		// Assert:
		for (final Map.Entry<String, NamespaceIdPart> entry : createPartsForEqualityTests().entrySet()) {
			Assert.assertThat(
					entry.getValue().hashCode(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(hashCode)) : IsEqual.equalTo(hashCode));
		}
	}

	private static boolean isDiffExpected(final String propertyName) {
		return !propertyName.endsWith("-case") && !propertyName.equals("default");
	}

	// endregion
}

