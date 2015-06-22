package org.nem.core.model.namespace;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;

import java.util.*;

public class NamespaceTest {
	private static final long BLOCKS_PER_YEAR = 1440 * 365;
	private static final Account OWNER = Utils.generateRandomAccount();

	// region ctor

	@Test
	public void canCreateNamespaceFromValidParameters() {
		// Act:
		final Namespace namespace = new Namespace(new NamespaceId("foo.bar"), OWNER, new BlockHeight(123));

		// Assert:
		Assert.assertThat(namespace.getId(), IsEqual.equalTo(new NamespaceId("foo.bar")));
		Assert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNER));
		Assert.assertThat(namespace.getHeight(), IsEqual.equalTo(new BlockHeight(123)));
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
		Assert.assertThat(namespace.isActive(new BlockHeight(123)), IsEqual.equalTo(true));
		Assert.assertThat(namespace.isActive(new BlockHeight(123 + BLOCKS_PER_YEAR - 1)), IsEqual.equalTo(true));
	}

	@Test
	public void isActiveReturnsFalseIfNamespaceOwnershipIsNotActive() {
		// Arrange:
		final Namespace namespace = new Namespace(new NamespaceId("foo"), OWNER, new BlockHeight(123));

		// Assert:
		Assert.assertThat(namespace.isActive(new BlockHeight(122)), IsEqual.equalTo(false));
		Assert.assertThat(namespace.isActive(new BlockHeight(123 + BLOCKS_PER_YEAR)), IsEqual.equalTo(false));
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Namespace namespace = new Namespace(new NamespaceId("foo.bar"), OWNER, new BlockHeight(123));
		final Map<String, Namespace> infoMap = createNamespacesForEqualityTests();

		// Assert:
		Assert.assertThat(infoMap.get("default"), IsEqual.equalTo(namespace));
		Assert.assertThat(infoMap.get("diff-id"), IsNot.not(IsEqual.equalTo(namespace)));
		Assert.assertThat(infoMap.get("diff-owner"), IsEqual.equalTo(namespace));
		Assert.assertThat(infoMap.get("diff-expiry"), IsEqual.equalTo(namespace));
		Assert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(namespace)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(namespace)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = new Namespace(new NamespaceId("foo.bar"), OWNER, new BlockHeight(123)).hashCode();
		final Map<String, Namespace> infoMap = createNamespacesForEqualityTests();

		// Assert:
		Assert.assertThat(infoMap.get("default").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(infoMap.get("diff-id").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(infoMap.get("diff-owner").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(infoMap.get("diff-expiry").hashCode(), IsEqual.equalTo(hashCode));
	}

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
}
