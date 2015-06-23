package org.nem.nis.cache;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;

public abstract class NamespaceCacheTest<T extends CopyableCache<T> & NamespaceCache> {
	private static final Account[] OWNERS = { Utils.generateRandomAccount(), Utils.generateRandomAccount() };
	private static final BlockHeight[] HEIGHTS = { new BlockHeight(123), new BlockHeight(234) };

	/**
	 * Creates a cache.
	 *
	 * @return The cache
	 */
	protected abstract T createCache();

	// region constructor

	@Test
	public void namespaceCacheIsInitiallyEmpty() {
		// Assert:
		Assert.assertThat(this.createCache().size(), IsEqual.equalTo(0));
	}

	// endregion

	// region get

	@Test
	public void getReturnsExpectedNamespace() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final Account owner = Utils.generateRandomAccount();
		final Namespace original = new Namespace(new NamespaceId("foo"), owner, new BlockHeight(123));
		cache.add(original);

		// Act:
		final Namespace namespace = cache.get(new NamespaceId("foo"));

		// Assert:
		Assert.assertThat(namespace, IsEqual.equalTo(original));
	}

	// endregion

	// region contains

	@Test
	public void containsReturnsTrueIfNamespaceExistsInCache() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		addToCache(cache, "foo", "foo.bar", "foo.baz", "foo.baz.qux");

		// Assert:
		Assert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new NamespaceId("foo.baz.qux")), IsEqual.equalTo(true));
	}

	@Test
	public void containsReturnsFalseIfNamespaceDoesNotExistInCache() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		addToCache(cache, "foo", "foo.bar", "foo.baz", "foo.baz.qux");

		// Assert:
		Assert.assertThat(cache.contains(new NamespaceId("fo0o")), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(new NamespaceId("bar.foo")), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(new NamespaceId("foo.bar.qux")), IsEqual.equalTo(false));
	}

	// endregion

	// region isActive

	@Test
	public void isActiveReturnsFalseIfNamespaceIsUnknown() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[0]));

		// Assert:
		Assert.assertThat(cache.isActive(new NamespaceId("foo"), HEIGHTS[0]), IsEqual.equalTo(true));
		Assert.assertThat(cache.isActive(new NamespaceId("foo.bar"), HEIGHTS[0]), IsEqual.equalTo(false));
	}

	@Test
	public void isActiveReturnsFalseIfAllRootNamespacesForGivenIdAreInactive() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo", OWNERS[1], HEIGHTS[1]));

		// Assert:
		Assert.assertThat(cache.isActive(new NamespaceId("foo"), BlockHeight.ONE), IsEqual.equalTo(false));
		Assert.assertThat(cache.isActive(new NamespaceId("foo.bar"), BlockHeight.ONE), IsEqual.equalTo(false));
	}

	@Test
	public void isActiveReturnsTrueIfAtLeastOneRootNamespaceForGivenIdIsActive() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo.bar", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo", OWNERS[1], new BlockHeight(10000)));

		// Assert:
		Assert.assertThat(cache.isActive(new NamespaceId("foo"), HEIGHTS[0]), IsEqual.equalTo(true));
		Assert.assertThat(cache.isActive(new NamespaceId("foo.bar"), new BlockHeight(10000)), IsEqual.equalTo(true));
	}

	// endregion

	// region add/remove

	@Test
	public void canAddDifferentNamespacesToCache() {
		// Arrange:
		final NamespaceCache cache = this.createCache();

		// Act:
		cache.add(createNamespace("foo"));
		cache.add(createNamespace("foo.bar"));
		cache.add(createNamespace("foo.bar.qux"));
		cache.add(createNamespace("bar"));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(4));
		Assert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new NamespaceId("foo.bar.qux")), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new NamespaceId("bar")), IsEqual.equalTo(true));
	}

	@Test
	public void cannotAddSameSubNamespaceTwice() {
		// Arrange:
		final NamespaceCache cache = this.createCache();

		// Act:
		cache.add(createNamespace("foo"));
		cache.add(createNamespace("foo.bar"));

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.add(createNamespace("foo.bar")), IllegalArgumentException.class);
	}

	@Test
	public void cannotAddNamespaceWithUnknownRoot() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("bar"));

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.add(createNamespace("foo.bar")), IllegalArgumentException.class);
	}

	@Test
	public void cannotAddNamespaceWithUnknownParent() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo"));

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.add(createNamespace("foo.bar.xyz")), IllegalArgumentException.class);
	}

	@Test
	public void canAddSameRootNamespaceTwice() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[0]));

		// Act:
		cache.add(createNamespace("foo", OWNERS[1], HEIGHTS[1]));

		// Assert:
		final NamespaceId id = new NamespaceId("foo");
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(id), IsEqual.equalTo(true));
		Assert.assertThat(cache.get(id).getOwner(), IsEqual.equalTo(OWNERS[1]));
		Assert.assertThat(cache.get(id).getHeight(), IsEqual.equalTo(HEIGHTS[1]));
	}

	@Test
	public void addedSubNamespacesInheritOwnerAndHeightFromRoot() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo.bar", OWNERS[1], HEIGHTS[1]));

		// Act:
		final Namespace namespace = cache.get(new NamespaceId("foo.bar"));

		// Assert:
		Assert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNERS[0]));
		Assert.assertThat(namespace.getHeight(), IsEqual.equalTo(HEIGHTS[0]));
	}

	@Test
	public void addingSameRootNamespaceTwiceUpdatesOwnerAndHeightOfSubNamespaces() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final String[] ids = { "foo", "foo.bar", "foo.bar.baz", "bar", "bar.baz" };
		final Integer[] indices = { 1, 1, 1, 0, 0 };
		Arrays.stream(ids).forEach(s -> cache.add(createNamespace(s, OWNERS[0], HEIGHTS[0])));

		// Act:
		cache.add(createNamespace("foo", OWNERS[1], HEIGHTS[1]));

		// Assert:
		IntStream.range(0, ids.length).forEach(i -> {
			final Namespace namespace = cache.get(new NamespaceId(ids[i]));
			Assert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNERS[indices[i]]));
			Assert.assertThat(namespace.getHeight(), IsEqual.equalTo(HEIGHTS[indices[i]]));
		});
	}

	@Test
	public void canRemoveExistingSubNamespacesFromCache() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		addToCache(cache, "foo", "bar", "foo.baz", "foo.bar", "bar.baz", "bar.baz.qux");

		// Act:
		cache.remove(new NamespaceId("foo.baz"));
		cache.remove(new NamespaceId("bar.baz.qux"));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(4));
		Assert.assertThat(cache.contains(new NamespaceId("foo.baz")), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new NamespaceId("bar.baz.qux")), IsEqual.equalTo(false));
	}

	@Test
	public void removeExistingRootNamespacesRemovesRootNamespaceFromCacheIfRootOnlyExistsOnceInRootMap() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		addToCache(cache, "foo", "bar");

		// Act:
		cache.remove(new NamespaceId("foo"));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(new NamespaceId("bar")), IsEqual.equalTo(true));
	}

	@Test
	public void removalOfRootWithDescendantsShouldFail() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		addToCache(cache, "foo", "foo.bar");

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.remove(new NamespaceId("foo")), IllegalArgumentException.class);
	}

	@Test
	public void removalOfParentWithDescendantsShouldFail() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		addToCache(cache, "foo", "foo.bar", "foo.bar.qux");

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.remove(new NamespaceId("foo.bar")), IllegalArgumentException.class);
	}

	@Test
	public void removeExistingRootNamespacesUpdatesOwnerAndHeightOfSubNamespacesIfRootOnlyExistsMoreThanOnceInRootMap() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final String[] ids = { "foo", "foo.bar", "foo.bar.baz", "bar", "bar.baz" };
		final Integer[] indices = { 0, 0, 0, 1, 1 };
		IntStream.range(0, ids.length).forEach(i -> cache.add(createNamespace(ids[i], OWNERS[indices[i]], HEIGHTS[indices[i]])));
		cache.add(createNamespace("foo", OWNERS[1], HEIGHTS[1]));

		// Assert the initial state:
		IntStream.range(0, ids.length).forEach(i -> {
			final Namespace namespace = cache.get(new NamespaceId(ids[i]));
			Assert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNERS[1]));
			Assert.assertThat(namespace.getHeight(), IsEqual.equalTo(HEIGHTS[1]));
		});

		// Act:
		cache.remove(new NamespaceId("foo"));

		// Assert:
		// - the root namespace (foo) was not removed because it existed previously
		Assert.assertThat(cache.size(), IsEqual.equalTo(5));
		Assert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(true));

		// - all foo descendants have reverted to their original owners and heights
		IntStream.range(0, ids.length).forEach(i -> {
			final Namespace namespace = cache.get(new NamespaceId(ids[i]));
			Assert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNERS[indices[i]]));
			Assert.assertThat(namespace.getHeight(), IsEqual.equalTo(HEIGHTS[indices[i]]));
		});
	}

	@Test
	public void cannotRemoveNonExistingNamespaceFromCache() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		addToCache(cache, "foo", "foo.bar", "foo.bar.qux");

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.remove(new NamespaceId("bar")), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> cache.remove(new NamespaceId("foo.qux")), IllegalArgumentException.class);
	}

	@Test
	public void cannotRemovePreviouslyExistingNonExistingNamespaceFromCache() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		addToCache(cache, "foo", "foo.bar", "foo.bar.qux", "bar");
		cache.remove(new NamespaceId("bar"));
		cache.remove(new NamespaceId("foo.bar.qux"));

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.remove(new NamespaceId("bar")), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> cache.remove(new NamespaceId("foo.bar.qux")), IllegalArgumentException.class);
	}

	// endregion

	//region shallowCopyTo / copy

	@Test
	public void shallowCopyToCopiesAllEntries() {
		// Assert:
		this.assertCopy(cache -> {
			final T copy = this.createCache();
			cache.shallowCopyTo(copy);
			return copy;
		});
	}

	@Test
	public void copyCopiesAllEntries() {
		// Assert:
		this.assertCopy(CopyableCache::copy);
	}

	private void assertCopy(final Function<T, T> copyCache) {
		// Arrange:
		final T cache = this.createCache();
		addToCache(cache, "foo", "foo.bar", "foo.baz", "foo.baz.qux", "bar");

		// Act:
		final T copy = copyCache.apply(cache);

		// Assert: initial copy
		Assert.assertThat(cache.size(), IsEqual.equalTo(5));
		Assert.assertThat(copy.contains(new NamespaceId("foo")), IsEqual.equalTo(true));
		Assert.assertThat(copy.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		Assert.assertThat(copy.contains(new NamespaceId("foo.baz")), IsEqual.equalTo(true));
		Assert.assertThat(copy.contains(new NamespaceId("foo.baz.qux")), IsEqual.equalTo(true));
		Assert.assertThat(copy.contains(new NamespaceId("bar")), IsEqual.equalTo(true));

		// Act: remove a root namespace
		cache.remove(new NamespaceId("bar"));

		// Assert: the namespace should only be removed from the original
		Assert.assertThat(cache.contains(new NamespaceId("bar")), IsEqual.equalTo(false));
		Assert.assertThat(copy.contains(new NamespaceId("bar")), IsEqual.equalTo(true));
	}

	// endregion

	private static void addToCache(final NamespaceCache cache, final String... ids) {
		Arrays.stream(ids).map(NamespaceCacheTest::createNamespace).forEach(cache::add);
	}

	private static Namespace createNamespace(final String id) {
		return new Namespace(new NamespaceId(id), Utils.generateRandomAccount(), BlockHeight.ONE);
	}

	private static Namespace createNamespace(final String id, final Account owner, final BlockHeight height) {
		return new Namespace(new NamespaceId(id), owner, height);
	}
}
