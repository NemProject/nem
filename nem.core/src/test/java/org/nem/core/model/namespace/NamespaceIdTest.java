package org.nem.core.model.namespace;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.*;

import java.util.*;

public class NamespaceIdTest {

	// region ctor

	@Test
	public void canCreateNamespaceIdFromValidString() {
		// Assert:
		assertIsValid("foo.bar.baz");
	}

	@Test
	public void canCreateNamespaceIdFromValidStringThatContainsAllowedSpecialCharacters() {
		// Assert:
		assertIsValid("foo-bar.baz_qux");
	}

	@Test
	public void canCreateNamespaceIdWithRootBeingSixteenCharsLong() {
		// Assert:
		assertIsValid("0123456789abcdef.bar");
	}

	@Test
	public void canCreateNamespaceIdWithSublevelBeingSixtyFourCharsLong() {
		// Assert:
		assertIsValid("foo.0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
	}

	private static void assertIsValid(final String s) {
		// Act:
		final NamespaceId namespaceId = new NamespaceId(s);

		// Assert:
		Assert.assertThat(namespaceId.toString(), IsEqual.equalTo(s));
	}

	@Test
	public void cannotCreateNamespaceIdFromInvalidString() {
		// Assert:
		final String[] invalid = {
				"",
				".",
				"..",
				"foo.",
				".foo",
				"foo..foo",
				"fooä",
				"foo bar",
				"foo. .bar",
				"0123456789abcdefg",
				"foo.0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0"
		};
		Arrays.stream(invalid).forEach(s -> ExceptionAssert.assertThrows(v -> new NamespaceId(s), IllegalArgumentException.class));
	}

	// endregion

	// region getRoot / getParent

	// region isRoot

	@Test
	public void isRootReturnsTrueForRootNamespaceIds() {
		// Assert
		Assert.assertThat(new NamespaceId("foo").isRoot(), IsEqual.equalTo(true));
		Assert.assertThat(new NamespaceId("bar").isRoot(), IsEqual.equalTo(true));
		Assert.assertThat(new NamespaceId("baz").isRoot(), IsEqual.equalTo(true));
	}

	@Test
	public void isRootReturnsFalseForNonRootNamespaceIds() {
		// Assert
		Assert.assertThat(new NamespaceId("foo.bar").isRoot(), IsEqual.equalTo(false));
		Assert.assertThat(new NamespaceId("bar.baz").isRoot(), IsEqual.equalTo(false));
		Assert.assertThat(new NamespaceId("baz.foo.bar").isRoot(), IsEqual.equalTo(false));
	}

	// endregion

	@Test
	public void getRootReturnsExpectedRoot() {
		// Arrange:
		final NamespaceId namespaceId = new NamespaceId("foo.bar.baz");

		// Act:
		final NamespaceId root = namespaceId.getRoot();

		// Assert:
		Assert.assertThat(root, IsEqual.equalTo(new NamespaceId("foo")));
	}

	@Test
	public void getParentReturnsNullForRootNamespaceId() {
		// Arrange:
		final NamespaceId namespaceId = new NamespaceId("foo");

		// Act:
		final NamespaceId parent = namespaceId.getParent();

		// Assert:
		Assert.assertThat(parent, IsNull.nullValue());
	}

	@Test
	public void getParentReturnsExpectedParentForNonRootNamespaceId() {
		// Arrange:
		final NamespaceId namespaceId = new NamespaceId("foo.bar.baz");

		// Act:
		final NamespaceId parent = namespaceId.getParent();

		// Assert:
		Assert.assertThat(parent, IsEqual.equalTo(new NamespaceId("foo.bar")));
	}

	// endregion

	// region getLastPart

	@Test
	public void getLastPartReturnsExpectedNamespaceIdPart() {
		// Arrange:
		final NamespaceId namespaceId = new NamespaceId("foo.bar.baz");

		// Act:
		final NamespaceIdPart part = namespaceId.getLastPart();

		// Assert:
		Assert.assertThat(part, IsEqual.equalTo(new NamespaceIdPart("baz")));
	}

	// endregion

	// region getLevel

	@Test
	public void getLevelReturnsExpectedLevel() {
		// Arrange:
		final NamespaceId rootId = new NamespaceId("foo");
		final NamespaceId sublevelId = new NamespaceId("foo.bar.baz");

		// Assert:
		Assert.assertThat(rootId.getLevel(), IsEqual.equalTo(0));
		Assert.assertThat(sublevelId.getLevel(), IsEqual.equalTo(2));
	}

	// endregion

	// region concat

	@Test
	public void concatReturnsExpectedNamespaceId() {
		// Arrange:
		final NamespaceId namespaceId = new NamespaceId("foo.bar");
		final NamespaceIdPart part = new NamespaceIdPart("baz");

		// Act:
		final NamespaceId result = namespaceId.concat(part);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new NamespaceId("foo.bar.baz")));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsExpectedString() {
		// Arrange:
		final NamespaceId namespaceId = new NamespaceId("foo.bar.baz");

		// Act:
		final String name = namespaceId.toString();

		// Assert:
		Assert.assertThat(name, IsEqual.equalTo("foo.bar.baz"));
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NamespaceId namespaceId = new NamespaceId("foo.bar.baz");
		final Map<String, NamespaceId> infoMap = createNamespaceIdsForEqualityTests();

		// Assert:
		Assert.assertThat(infoMap.get("default"), IsEqual.equalTo(namespaceId));
		Assert.assertThat(infoMap.get("default2"), IsEqual.equalTo(namespaceId));
		Assert.assertThat(infoMap.get("diff-root"), IsNot.not(IsEqual.equalTo(namespaceId)));
		Assert.assertThat(infoMap.get("diff-sublevel1"), IsNot.not(IsEqual.equalTo(namespaceId)));
		Assert.assertThat(infoMap.get("diff-sublevel2"), IsNot.not(IsEqual.equalTo(namespaceId)));
		Assert.assertThat(infoMap.get("missing-sublevel1"), IsNot.not(IsEqual.equalTo(namespaceId)));
		Assert.assertThat(infoMap.get("missing-sublevel2"), IsNot.not(IsEqual.equalTo(namespaceId)));
		Assert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(namespaceId)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(namespaceId)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = new NamespaceId("foo.bar.baz").hashCode();
		final Map<String, NamespaceId> infoMap = createNamespaceIdsForEqualityTests();

		// Assert:
		Assert.assertThat(infoMap.get("default").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(infoMap.get("default2").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(infoMap.get("diff-root").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(infoMap.get("diff-sublevel1").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(infoMap.get("diff-sublevel2").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(infoMap.get("missing-sublevel1").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(infoMap.get("missing-sublevel2").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	private static Map<String, NamespaceId> createNamespaceIdsForEqualityTests() {
		return new HashMap<String, NamespaceId>() {
			{
				this.put("default", new NamespaceId("foo.bar.baz"));
				this.put("default2", new NamespaceId("FoO.bAr.BaZ"));
				this.put("diff-root", new NamespaceId("fooo.bar.baz"));
				this.put("diff-sublevel1", new NamespaceId("foo.baar.baz"));
				this.put("diff-sublevel2", new NamespaceId("foo.bar.bazz"));
				this.put("missing-sublevel1", new NamespaceId("foo.baz"));
				this.put("missing-sublevel2", new NamespaceId("foo.bar"));
			}
		};
	}

	// endregion

	// region serialization / deserialization

	@Test
	public void namespaceIdCanBeRoundTripped() {
		// Arrange:
		final NamespaceId original = new NamespaceId("foo.bar.baz");

		// Act:
		final NamespaceId namespaceId = createRoundTrippedEntity(original);

		// Assert:
		Assert.assertThat(namespaceId, IsEqual.equalTo(new NamespaceId("foo.bar.baz")));
	}

	private static NamespaceId createRoundTrippedEntity(final NamespaceId original) {
		// Act:
		return new NamespaceId(Utils.roundtripSerializableEntity(original, null));
	}

	// endregion
}
