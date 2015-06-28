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
	public void isActiveReturnsTrueIfAtLeastOneRootNamespaceForGivenIdIsActiveAndHasSameOwner() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo.bar", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo", OWNERS[0], new BlockHeight(1000000)));

		// Assert:
		Assert.assertThat(cache.isActive(new NamespaceId("foo"), HEIGHTS[0]), IsEqual.equalTo(true));
		Assert.assertThat(cache.isActive(new NamespaceId("foo.bar"), new BlockHeight(1000000)), IsEqual.equalTo(true));
	}

	@Test
	public void isActiveReturnsFalseIfRootNamespaceForGivenIdIsActiveButHasDifferentOwner() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo.bar", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo", OWNERS[1], new BlockHeight(1000000)));

		// Assert:
		Assert.assertThat(cache.isActive(new NamespaceId("foo.bar"), new BlockHeight(1000000)), IsEqual.equalTo(false));
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
	public void addedSubNamespacesInheritHeightFromRootIfRootHasSameOwner() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo.bar", OWNERS[0], HEIGHTS[1]));

		// Act:
		final Namespace namespace = cache.get(new NamespaceId("foo.bar"));

		// Assert:
		Assert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNERS[0]));
		Assert.assertThat(namespace.getHeight(), IsEqual.equalTo(HEIGHTS[0]));
	}

	@Test
	public void cannotAddSubNamespacesIfRootHasDifferentOwner() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[0]));

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.add(createNamespace("foo.bar", OWNERS[1], HEIGHTS[1])), IllegalArgumentException.class);
	}

	@Test
	public void addingSameRootNamespaceTwiceUpdatesHeightOfSubNamespacesIfRootHasSameOwner() {
		// Assert:
		this.assertSubNamespaceUpdateBehaviorForAdding(
				OWNERS[0],
				new Integer[] { 0, 0, 0, 0, 0 },
				new Integer[] { 1, 1, 1, 0, 0 });
	}

	@Test
	public void addingSameRootNamespaceTwiceDoesNotUpdateSubNamespacesIfRootHasDifferentOwner() {
		// Assert:
		this.assertSubNamespaceUpdateBehaviorForAdding(
				OWNERS[1],
				new Integer[] { 1, 0, 0, 0, 0 },
				new Integer[] { 1, 0, 0, 0, 0 });
	}

	private void assertSubNamespaceUpdateBehaviorForAdding(
			final Account newRootOwner,
			final Integer[] expectedOwnerIndices,
			final Integer[] expectedHeightIndices) {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final String[] ids = { "foo", "foo.bar", "foo.bar.baz", "bar", "bar.baz" };
		Arrays.stream(ids).forEach(s -> cache.add(createNamespace(s, OWNERS[0], HEIGHTS[0])));

		// Act:
		cache.add(createNamespace("foo", newRootOwner, HEIGHTS[1]));

		// Assert:
		IntStream.range(0, ids.length).forEach(i -> {
			final Namespace namespace = cache.get(new NamespaceId(ids[i]));
			Assert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNERS[expectedOwnerIndices[i]]));
			Assert.assertThat(namespace.getHeight(), IsEqual.equalTo(HEIGHTS[expectedHeightIndices[i]]));
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
	public void removeExistingRootNamespacesUpdatesHeightOfSubNamespacesIfRootOnlyExistsMoreThanOnceInRootMapAndNewRootHasSameOwnerAsSubNamespace() {
		assertSubNamespaceUpdateBehaviorForRemoving(
				OWNERS[0],
				new Integer[] { 0, 0, 0, 1, 1 },
				new Integer[] { 0, 0, 0, 1, 1 });
	}

	@Test
	public void removeExistingRootNamespacesDoesNotUpdateSubNamespacesIfRootOnlyExistsMoreThanOnceInRootMapAndNewRootHasDifferentOwnerAsSubNamespace() {
		assertSubNamespaceUpdateBehaviorForRemoving(
				OWNERS[1],
				new Integer[] { 1, 0, 0, 1, 1 },
				new Integer[] { 0, 1, 1, 1, 1 });
	}

	private void assertSubNamespaceUpdateBehaviorForRemoving(
			final Account newRootOwner,
			final Integer[] expectedOwnerIndices,
			final Integer[] expectedHeightIndices) {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final String[] ids = { "foo", "foo.bar", "foo.bar.baz", "bar", "bar.baz" };
		final Integer[] indices = { 0, 0, 0, 1, 1 };
		IntStream.range(0, ids.length).forEach(i -> cache.add(createNamespace(ids[i], OWNERS[indices[i]], HEIGHTS[indices[i]])));
		if (!newRootOwner.equals(OWNERS[0])) {
			cache.add(createNamespace("foo", newRootOwner, HEIGHTS[0]));
		}

		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[1]));

		// Assert the initial state:
		IntStream.range(0, ids.length).forEach(i -> {
			final Namespace namespace = cache.get(new NamespaceId(ids[i]));
			Assert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNERS[indices[i]]));
			Assert.assertThat(namespace.getHeight(), IsEqual.equalTo(HEIGHTS[1]));
		});

		// Act:
		cache.remove(new NamespaceId("foo"));

		// Assert:
		// - the root namespace (foo) was not removed because it existed previously
		Assert.assertThat(cache.size(), IsEqual.equalTo(5));
		Assert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(true));

		// - all foo descendants have reverted to their original owners and heights
		IntStream.range(0, ids.length)
				.forEach(i -> {
					final Namespace namespace = cache.get(new NamespaceId(ids[i]));
					Assert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNERS[expectedOwnerIndices[i]]));
					Assert.assertThat(namespace.getHeight(), IsEqual.equalTo(HEIGHTS[expectedHeightIndices[i]]));
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

	//region prune

	@Test
	public void pruneRemovesAllExpiredRoots() {
		// Arrange: create 4 3-level roots (0, 1, 2, 3)
		final NamespaceCache cache = this.createCache();
		for (int i = 0; i < 4; ++i) {
			final Account account = Utils.generateRandomAccount();
			final String rootName = Integer.toString(i);
			cache.add(new Namespace(new NamespaceId(rootName), account, new BlockHeight(1000L + i)));
			cache.add(new Namespace(new NamespaceId(rootName + ".a"), account, BlockHeight.ONE));
			cache.add(new Namespace(new NamespaceId(rootName + ".a.a"), account, BlockHeight.ONE));
		}

		// Sanity: precondition 12 items were added to the cache
		Assert.assertThat(cache.size(), IsEqual.equalTo(12));

		// Act: prune at height 1002
		cache.prune(new BlockHeight(1002L));

		// Assert: only the items no less than the prune height (2, 3) remain
		Assert.assertThat(cache.size(), IsEqual.equalTo(6));
		for (final String rootName : Arrays.asList("2", "3")) {
			Assert.assertThat(cache.contains(new NamespaceId(rootName)), IsEqual.equalTo(true));
			Assert.assertThat(cache.contains(new NamespaceId(rootName + ".a")), IsEqual.equalTo(true));
			Assert.assertThat(cache.contains(new NamespaceId(rootName + ".a.a")), IsEqual.equalTo(true));
		}
	}

	@Test
	public void pruneRemovesLockedInRootInformation() {
		// Arrange: create four root entries for 0 and a single sub-entry
		final NamespaceCache cache = this.createCache();
		final Account account = Utils.generateRandomAccount();
		for (int i = 0; i < 4; ++i) {
			cache.add(new Namespace(new NamespaceId("0"), account, new BlockHeight(1000L + i)));
		}

		cache.add(new Namespace(new NamespaceId("0.a"), account, BlockHeight.ONE));

		// Sanity: precondition 2 items were added to the cache
		Assert.assertThat(cache.size(), IsEqual.equalTo(2));

		// Act: prune at height 1002
		cache.prune(new BlockHeight(1002L));

		// Assert: no items were removed
		Assert.assertThat(cache.size(), IsEqual.equalTo(2));

		// Act: remove all items
		cache.remove(new NamespaceId("0.a"));
		cache.remove(new NamespaceId("0"));
		cache.remove(new NamespaceId("0"));

		// Assert: all items were removed (the root required two removals because it had two links)
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
	}

	//endregion

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
		return new Namespace(new NamespaceId(id), OWNERS[0], BlockHeight.ONE);
	}

	private static Namespace createNamespace(final String id, final Account owner, final BlockHeight height) {
		return new Namespace(new NamespaceId(id), owner, height);
	}
}
