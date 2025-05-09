package org.nem.core.model.namespace;

import java.util.*;
import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.*;

public class NamespaceTest {
	private static final long BLOCKS_PER_YEAR = 1440 * 365;
	private static final Account OWNER = Utils.generateRandomAccount();

	// region ctor

	@Test
	public void canCreateNamespaceFromValidParameters() {
		// Act:
		final Namespace namespace = new Namespace(new NamespaceId("foo.bar"), OWNER, new BlockHeight(123));

		// Assert:
		MatcherAssert.assertThat(namespace.getId(), IsEqual.equalTo(new NamespaceId("foo.bar")));
		MatcherAssert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNER));
		MatcherAssert.assertThat(namespace.getHeight(), IsEqual.equalTo(new BlockHeight(123)));
	}

	// endregion

	// region expiration

	@Test
	public void isActiveIsOnlyAllowedToBeCalledOnRootNamespaces() {
		// Arrange:
		final Namespace namespace = new Namespace(new NamespaceId("foo.bar"), OWNER, new BlockHeight(123));

		// Assert:
		ExceptionAssert.assertThrows(v -> namespace.isActive(BlockHeight.ONE), UnsupportedOperationException.class);
	}

	@Test
	public void isActiveReturnsTrueIfNamespaceOwnershipIsActive() {
		// Arrange:
		final Namespace namespace = new Namespace(new NamespaceId("foo"), OWNER, new BlockHeight(123));

		// Assert:
		for (final Long delta : Arrays.asList(0L, BLOCKS_PER_YEAR - 10000, BLOCKS_PER_YEAR - 1)) {
			MatcherAssert.assertThat(delta.toString(), namespace.isActive(new BlockHeight(123 + delta)), IsEqual.equalTo(true));
		}
	}

	@Test
	public void isActiveReturnsFalseIfNamespaceOwnershipIsNotActive() {
		// Arrange:
		final Namespace namespace = new Namespace(new NamespaceId("foo"), OWNER, new BlockHeight(123));

		// Assert:
		for (final Long delta : Arrays.asList(-1L, BLOCKS_PER_YEAR, BLOCKS_PER_YEAR + 10000)) {
			MatcherAssert.assertThat(delta.toString(), namespace.isActive(new BlockHeight(123 + delta)), IsEqual.equalTo(false));
		}
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Namespace namespace = new Namespace(new NamespaceId("foo.bar"), OWNER, new BlockHeight(123));
		final Map<String, Namespace> infoMap = createNamespacesForEqualityTests();

		// Assert:
		for (final Map.Entry<String, Namespace> entry : createNamespacesForEqualityTests().entrySet()) {
			final String key = entry.getKey();
			MatcherAssert.assertThat(key, infoMap.get(key),
					"diff-id".equals(key) ? IsNot.not(IsEqual.equalTo(namespace)) : IsEqual.equalTo(namespace));
		}

		MatcherAssert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(namespace)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(namespace)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = new Namespace(new NamespaceId("foo.bar"), OWNER, new BlockHeight(123)).hashCode();
		final Map<String, Namespace> infoMap = createNamespacesForEqualityTests();

		// Assert:
		for (final Map.Entry<String, Namespace> entry : createNamespacesForEqualityTests().entrySet()) {
			final String key = entry.getKey();
			MatcherAssert.assertThat(key, infoMap.get(key).hashCode(),
					"diff-id".equals(key) ? IsNot.not(IsEqual.equalTo(hashCode)) : IsEqual.equalTo(hashCode));
		}
	}

	@SuppressWarnings("serial")
	private static Map<String, Namespace> createNamespacesForEqualityTests() {
		return new HashMap<String, Namespace>() {
			{
				this.put("default", new Namespace(new NamespaceId("foo.bar"), OWNER, new BlockHeight(123)));
				this.put("diff-id", new Namespace(new NamespaceId("foo.baz"), OWNER, new BlockHeight(123)));
				this.put("diff-owner", new Namespace(new NamespaceId("foo.bar"), Utils.generateRandomAccount(), new BlockHeight(123)));
				this.put("diff-expiry", new Namespace(new NamespaceId("foo.bar"), OWNER, new BlockHeight(321)));
			}
		};
	}

	// endregion

	// region serialization

	@Test
	public void canRoundTripNamespace() {
		// Arrange:
		final Namespace original = new Namespace(new NamespaceId("abc.def"), OWNER, new BlockHeight(737));

		// Act:
		final Namespace namespace = new Namespace(Utils.roundtripSerializableEntity(original, new MockAccountLookup()));

		// Assert:
		MatcherAssert.assertThat(namespace.getId(), IsEqual.equalTo(new NamespaceId("abc.def")));
		MatcherAssert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNER));
		MatcherAssert.assertThat(namespace.getHeight(), IsEqual.equalTo(new BlockHeight(737)));
	}

	@Test
	public void canSerializeNamespace() {
		// Arrange:
		final Namespace namespace = new Namespace(new NamespaceId("abc.def"), new Account(Address.fromEncoded("XYZ")),
				new BlockHeight(737));

		// Act:
		final JSONObject jsonObject = JsonSerializer.serializeToJson(namespace);

		// Assert:
		MatcherAssert.assertThat(jsonObject.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(jsonObject.get("fqn"), IsEqual.equalTo("abc.def"));
		MatcherAssert.assertThat(jsonObject.get("owner"), IsEqual.equalTo("XYZ"));
		MatcherAssert.assertThat(jsonObject.get("height"), IsEqual.equalTo(737L));
	}

	// endregion
}
