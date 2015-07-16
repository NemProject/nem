package org.nem.nis.cache;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.state.*;

import java.util.Arrays;
import java.util.function.*;
import java.util.stream.IntStream;

public abstract class NamespaceCacheTest<T extends CopyableCache<T> & NamespaceCache> {
	private static final Account[] OWNERS = { Utils.generateRandomAccount(), Utils.generateRandomAccount() };
	private static final BlockHeight[] HEIGHTS = { new BlockHeight(123), new BlockHeight(10000234) };

	/**
	 * Creates a cache.
	 *
	 * @return The cache
	 */
	protected abstract T createCache();

	// region constructor

	@Test
	public void namespaceCacheInitiallyHasSizeOne() {
		// Act:
		final NamespaceCache cache = this.createCache();

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(1));
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
		final Namespace namespace = cache.get(new NamespaceId("foo")).getNamespace();

		// Assert:
		Assert.assertThat(namespace, IsEqual.equalTo(original));
	}

	@Test
	public void getPreservesRootSmartTilesAcrossCalls() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final Account owner = Utils.generateRandomAccount();
		cache.add(new Namespace(new NamespaceId("foo"), owner, new BlockHeight(123)));
		addSmartTile(cache, new NamespaceId("foo"), 1, 7);

		// Act:
		final NamespaceEntry entry = cache.get(new NamespaceId("foo"));

		// Assert:
		Assert.assertThat(entry.getMosaics().size(), IsEqual.equalTo(1));
		Assert.assertThat(getQuantity(cache, "foo", 1), IsEqual.equalTo(new Quantity(7)));
	}

	@Test
	public void getPreservesChildSmartTilesAcrossCalls() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final Account owner = Utils.generateRandomAccount();
		cache.add(new Namespace(new NamespaceId("foo"), owner, new BlockHeight(123)));
		cache.add(new Namespace(new NamespaceId("foo.bar"), owner, new BlockHeight(123)));
		addSmartTile(cache, new NamespaceId("foo.bar"), 2, 9);

		// Act:
		final NamespaceEntry entry = cache.get(new NamespaceId("foo.bar"));

		// Assert:
		Assert.assertThat(entry.getMosaics().size(), IsEqual.equalTo(1));
		Assert.assertThat(getQuantity(cache, "foo.bar", 2), IsEqual.equalTo(new Quantity(9)));
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

	// region basic

	@Test
	public void isActiveReturnsTrueIfRootNamespaceIsKnown() {
		// Assert:
		this.assertBasicIsActive(new String[] { "x", "foo", "y" }, "foo", true);
	}

	@Test
	public void isActiveReturnsFalseIfRootNamespaceIsUnknown() {
		// Assert:
		this.assertBasicIsActive(new String[] { "x", "foo", "y" }, "bar", false);
	}

	@Test
	public void isActiveReturnsTrueIfSubNamespaceIsKnown() {
		// Assert:
		this.assertBasicIsActive(new String[] { "x", "foo", "foo.bar", "y" }, "foo.bar", true);
	}

	@Test
	public void isActiveReturnsFalseIfSubNamespaceIsUnknown() {
		// Assert:
		this.assertBasicIsActive(new String[] { "x", "foo", "foo.bar", "y" }, "too.bar", false);
	}

	private void assertBasicIsActive(final String[] names, final String testName, final boolean expectedResult) {
		// Arrange:
		final BlockHeight height = HEIGHTS[0];
		final NamespaceCache cache = this.createCache();
		for (final String name : names) {
			cache.add(createNamespace(name, OWNERS[0], height));
		}

		// Act:
		final boolean isActive = cache.isActive(new NamespaceId(testName), height);

		// Assert:
		Assert.assertThat(isActive, IsEqual.equalTo(expectedResult));
	}

	// endregion

	// region height-based inactivity

	@Test
	public void isActiveReturnsTrueIfAtLeastOneRootNamespacesForGivenRootIdIsActive() {
		// Assert:
		this.assertHeightBasedIsActive(new String[] { "x", "foo", "y", }, "foo", HEIGHTS[1], true);
	}

	@Test
	public void isActiveReturnsFalseIfAllRootNamespacesForGivenRootIdAreInactive() {
		// Assert:
		this.assertHeightBasedIsActive(new String[] { "x", "foo", "y", }, "foo", BlockHeight.ONE, false);
	}

	@Test
	public void isActiveReturnsTrueIfAtLeastOneRootNamespacesForGivenSubIdIsActive() {
		// Assert:
		this.assertHeightBasedIsActive(new String[] { "x", "foo", "foo.bar", "y", }, "foo.bar", HEIGHTS[1], true);
	}

	@Test
	public void isActiveReturnsFalseIfAllRootNamespacesForGivenSubIdAreInactive() {
		// Assert:
		this.assertHeightBasedIsActive(new String[] { "x", "foo", "foo.bar", "y", }, "foo.bar", BlockHeight.ONE, false);
	}

	private void assertHeightBasedIsActive(final String[] names, final String testName, final BlockHeight testHeight, final boolean expectedResult) {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		for (final int i : Arrays.asList(0, 1)) {
			for (final String name : names) {
				cache.add(createNamespace(name, OWNERS[i], HEIGHTS[i]));
			}
		}

		// Act:
		final boolean isActive = cache.isActive(new NamespaceId(testName), testHeight);

		// Assert:
		Assert.assertThat(isActive, IsEqual.equalTo(expectedResult));
	}

	// endregion

	// region owner-based inactivity

	@Test
	public void isActiveReturnsTrueIfAtLeastOneRootNamespaceForGivenIdIsActiveAndHasSameOwner() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo.bar", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo", OWNERS[0], new BlockHeight(1000000)));

		// Assert:
		// - at HEIGHTS[0] both have the same owner
		Assert.assertThat(cache.isActive(new NamespaceId("foo"), HEIGHTS[0]), IsEqual.equalTo(true));
		Assert.assertThat(cache.isActive(new NamespaceId("foo.bar"), HEIGHTS[0]), IsEqual.equalTo(true));

		// - at 1000000 both have the same owner
		Assert.assertThat(cache.isActive(new NamespaceId("foo"), new BlockHeight(1000000)), IsEqual.equalTo(true));
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
		// - at HEIGHTS[0] both have the same owner
		Assert.assertThat(cache.isActive(new NamespaceId("foo"), HEIGHTS[0]), IsEqual.equalTo(true));
		Assert.assertThat(cache.isActive(new NamespaceId("foo.bar"), HEIGHTS[0]), IsEqual.equalTo(true));

		// - at 1000000 root and sub have different owners
		Assert.assertThat(cache.isActive(new NamespaceId("foo"), new BlockHeight(1000000)), IsEqual.equalTo(true));
		Assert.assertThat(cache.isActive(new NamespaceId("foo.bar"), new BlockHeight(1000000)), IsEqual.equalTo(false));
	}

	// endregion

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
		Assert.assertThat(cache.size(), IsEqual.equalTo(1 + 4));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 4));
		Assert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new NamespaceId("foo.bar.qux")), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new NamespaceId("bar")), IsEqual.equalTo(true));
	}

	@Test
	public void cannotAddSameSubNamespaceTwiceWithSameOwner() {
		// Arrange:
		final NamespaceCache cache = this.createCache();

		// Act:
		cache.add(createNamespace("foo"));
		cache.add(createNamespace("foo.bar"));

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.add(createNamespace("foo.bar")), IllegalArgumentException.class);
	}

	@Test
	public void canAddSameSubNamespaceTwiceWithDifferentOwner() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo.bar", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo", OWNERS[1], HEIGHTS[1]));

		// Act:
		cache.add(createNamespace("foo.bar", OWNERS[1], HEIGHTS[1]));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1 + 2));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 4));
		Assert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		Assert.assertThat(cache.isActive(new NamespaceId("foo.bar"), HEIGHTS[1]), IsEqual.equalTo(true));
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
		Assert.assertThat(cache.size(), IsEqual.equalTo(1 + 1));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 2));
		Assert.assertThat(cache.contains(id), IsEqual.equalTo(true));
		Assert.assertThat(cache.get(id).getNamespace().getOwner(), IsEqual.equalTo(OWNERS[1]));
		Assert.assertThat(cache.get(id).getNamespace().getHeight(), IsEqual.equalTo(HEIGHTS[1]));
	}

	@Test
	public void addedSubNamespacesInheritHeightFromRootIfRootHasSameOwner() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo.bar", OWNERS[0], HEIGHTS[1]));

		// Act:
		final Namespace namespace = cache.get(new NamespaceId("foo.bar")).getNamespace();

		// Assert:
		Assert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNERS[0]));
		Assert.assertThat(namespace.getHeight(), IsEqual.equalTo(HEIGHTS[0]));
	}

	@Test
	public void cannotAddSubNamespacesIfRootHasDifferentOwner() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo.baz", OWNERS[0], HEIGHTS[0]));

		// Assert:
		for (final String name : Arrays.asList("foo.bar", "foo.baz.bar")) {
			ExceptionAssert.assertThrows(
					v -> cache.add(createNamespace(name, OWNERS[1], HEIGHTS[1])),
					IllegalArgumentException.class);
		}
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
				new Integer[] { 1, -1, -1, 0, 0 },
				new Integer[] { 1, -1, -1, 0, 0 });
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
			final NamespaceEntry entry = cache.get(new NamespaceId(ids[i]));
			if (-1 == expectedOwnerIndices[i]) {
				Assert.assertThat(entry, IsNull.nullValue());
			} else {
				final Namespace namespace = entry.getNamespace();
				Assert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNERS[expectedOwnerIndices[i]]));
				Assert.assertThat(namespace.getHeight(), IsEqual.equalTo(HEIGHTS[expectedHeightIndices[i]]));
			}
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
		Assert.assertThat(cache.size(), IsEqual.equalTo(1 + 4));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 4));
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
		Assert.assertThat(cache.size(), IsEqual.equalTo(1 + 1));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 1));
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
	public void removeExistingRootNamespacesUpdatesHeightOfSubNamespacesIfRootExistsMoreThanOnceInRootMapAndNewRootHasSameOwnerAsSubNamespace() {
		this.assertSubNamespaceUpdateBehaviorForRemoving(
				OWNERS[0],
				new Integer[] { 0, 0, 0, 1, 1 },
				new Integer[] { 0, 0, 0, 1, 1 });
	}

	@Test
	public void removeExistingRootNamespacesDoesNotUpdateSubNamespacesIfRootExistsMoreThanOnceInRootMapAndNewRootHasDifferentOwnerThanSubNamespace() {
		this.assertSubNamespaceUpdateBehaviorForRemoving(
				OWNERS[1],
				new Integer[] { 1, -1, -1, 1, 1 },
				new Integer[] { 0, -1, -1, 1, 1 });
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
		cache.add(createNamespace("foo", newRootOwner, HEIGHTS[0]));
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[1])); // (this entry will be removed by the remove below)

		// Act:
		cache.remove(new NamespaceId("foo"));

		// Assert:
		// - the root namespace (foo) was not removed because it existed previously
		// - in the case of a -1 owner index, the corresponding namespace is considered inactive (because there was an owner change)
		final int adjustment = Long.valueOf(Arrays.stream(expectedOwnerIndices).filter(i -> -1 == i).count()).intValue();
		Assert.assertThat(cache.size(), IsEqual.equalTo(1 + 5 - adjustment));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 8 - adjustment));
		Assert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(true));

		// - all foo descendants have reverted to their original owners and heights
		IntStream.range(0, ids.length)
				.forEach(i -> {
					final NamespaceEntry entry = cache.get(new NamespaceId(ids[i]));
					if (-1 == expectedOwnerIndices[i]) {
						Assert.assertThat(entry, IsNull.nullValue());
					} else {
						final Namespace namespace = entry.getNamespace();
						Assert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNERS[expectedOwnerIndices[i]]));
						Assert.assertThat(namespace.getHeight(), IsEqual.equalTo(HEIGHTS[expectedHeightIndices[i]]));
					}
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

	// region smart tile propagation

	@Test
	public void namespaceRenewalWithSameOwnerPreservesSmartTiles() {
		// Arrange:
		final T cache = this.createCache();
		addToCache(cache, "foo", "bar", "bar.qux");
		addSmartTile(cache, new NamespaceId("bar"), 1, 7);
		addSmartTile(cache, new NamespaceId("bar.qux"), 2, 9);

		// Act:
		cache.add(createNamespace("bar", OWNERS[0], HEIGHTS[1]));

		// Assert:
		Assert.assertThat(getQuantity(cache, "bar", 1), IsEqual.equalTo(new Quantity(7)));
		Assert.assertThat(getQuantity(cache, "bar.qux", 2), IsEqual.equalTo(new Quantity(9)));
	}

	@Test
	public void namespaceRenewalWithDifferentOwnerDoesNotPreserveSmartTiles() {
		// Arrange:
		final T cache = this.createCache();
		addToCache(cache, "foo", "bar", "bar.qux");
		addSmartTile(cache, new NamespaceId("bar"), 1, 7);
		addSmartTile(cache, new NamespaceId("bar.qux"), 2, 9);

		// Act:
		cache.add(createNamespace("bar", OWNERS[1], HEIGHTS[1]));
		cache.add(createNamespace("bar.qux", OWNERS[1], HEIGHTS[1]));

		// Assert:
		Assert.assertThat(getMosaicEntry(cache, "bar", 1), IsNull.nullValue());
		Assert.assertThat(getMosaicEntry(cache, "bar.qux", 2), IsNull.nullValue());
	}

	@Test
	public void namespaceRollbackRestoresOriginalSmartTiles() {
		// Arrange:
		final T cache = this.createCache();
		addToCache(cache, "foo", "bar", "bar.qux");
		addSmartTile(cache, new NamespaceId("bar"), 1, 7);
		addSmartTile(cache, new NamespaceId("bar.qux"), 2, 9);

		// Act:
		cache.add(createNamespace("bar", OWNERS[1], HEIGHTS[1]));
		cache.remove(new NamespaceId("bar"));

		// Assert:
		Assert.assertThat(getQuantity(cache, "bar", 1), IsEqual.equalTo(new Quantity(7)));
		Assert.assertThat(getQuantity(cache, "bar.qux", 2), IsEqual.equalTo(new Quantity(9)));
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
		Assert.assertThat(cache.size(), IsEqual.equalTo(1 + 12));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 12));

		// Act: prune at height 1002
		cache.prune(new BlockHeight(1002L));

		// Assert: only the items no less than the prune height (2, 3) remain
		Assert.assertThat(cache.size(), IsEqual.equalTo(1 + 6));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 6));
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
		Assert.assertThat(cache.size(), IsEqual.equalTo(1 + 2));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 5));

		// Act: prune at height 1002
		cache.prune(new BlockHeight(1002L));

		// Assert: no items were removed
		Assert.assertThat(cache.size(), IsEqual.equalTo(1 + 2));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 3));

		// Act: remove all items
		cache.remove(new NamespaceId("0.a"));
		cache.remove(new NamespaceId("0"));
		cache.remove(new NamespaceId("0"));

		// Assert: all items were removed (the root required two removals because it had two links)
		Assert.assertThat(cache.size(), IsEqual.equalTo(1 + 0));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 0));
	}

	//endregion

	//region shallowCopyTo / copy

	@Test
	public void shallowCopyToCopiesAllEntries() {
		// Assert:
		this.assertBasicCopy(cache -> {
			final T copy = this.createCache();
			cache.shallowCopyTo(copy);
			return copy;
		});
	}

	@Test
	public void shallowCopyRemovalOfRootNamespaceIsLinked() {
		// Arrange:
		final T cache = this.createCacheForCopyTests();

		// Act:
		final T copy = this.createCache();
		cache.shallowCopyTo(copy);

		// Act: remove a root namespace
		cache.remove(new NamespaceId("foo"));

		// Assert: the namespace should be removed from the original and the shallow copy
		Assert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		Assert.assertThat(copy.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
	}

	@Test
	public void shallowCopySmartTileAdjustmentsAreLinked() {
		// Arrange:
		final T cache = this.createCacheForCopyTests();

		// Act:
		final T copy = this.createCache();
		cache.shallowCopyTo(copy);

		// Act: update smart tiles quantities
		addSmartTile(cache, new NamespaceId("bar"), 1, 7);
		addSmartTile(cache, new NamespaceId("bar.qux"), 2, 11);

		// Assert: the quantities are updated in both the original and shallow copy
		Assert.assertThat(getQuantity(cache, "bar", 1), IsEqual.equalTo(new Quantity(14)));
		Assert.assertThat(getQuantity(copy, "bar", 1), IsEqual.equalTo(new Quantity(14)));
		Assert.assertThat(getQuantity(cache, "bar.qux", 2), IsEqual.equalTo(new Quantity(20)));
		Assert.assertThat(getQuantity(copy, "bar.qux", 2), IsEqual.equalTo(new Quantity(20)));
	}

	@Test
	public void copyCopiesAllEntries() {
		// Assert:
		this.assertBasicCopy(CopyableCache::copy);
	}

	@Test
	public void copyRemovalOfRootNamespaceIsUnlinked() {
		// Arrange:
		final T cache = this.createCacheForCopyTests();

		// Act:
		final T copy = cache.copy();

		// Act: remove a root namespace
		cache.remove(new NamespaceId("foo"));

		// Assert: the namespace should be removed from the original but not the copy
		Assert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		Assert.assertThat(copy.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(false));
	}

	@Test
	public void copySmartTileAdjustmentsAreUnlinked() {
		// Arrange:
		final T cache = this.createCacheForCopyTests();

		// Act:
		final T copy = cache.copy();

		// Act: update smart tiles quantities
		addSmartTile(cache, new NamespaceId("bar"), 1, 7);
		addSmartTile(cache, new NamespaceId("bar.qux"), 2, 11);

		// Assert: the quantities are only updated in the original cache
		Assert.assertThat(getQuantity(cache, "bar", 1), IsEqual.equalTo(new Quantity(14)));
		Assert.assertThat(getQuantity(copy, "bar", 1), IsEqual.equalTo(new Quantity(7)));
		Assert.assertThat(getQuantity(cache, "bar.qux", 2), IsEqual.equalTo(new Quantity(20)));
		Assert.assertThat(getQuantity(copy, "bar.qux", 2), IsEqual.equalTo(new Quantity(9)));
	}

	private T createCacheForCopyTests() {
		// Arrange:
		final T cache = this.createCache();
		addToCache(cache, "foo", "foo.bar", "foo.baz", "foo.baz.qux", "bar", "bar.qux");
		cache.add(createNamespace("foo", OWNERS[1], HEIGHTS[1]));
		addSmartTile(cache, new NamespaceId("bar"), 1, 7);
		addSmartTile(cache, new NamespaceId("bar.qux"), 2, 9);
		return cache;
	}

	private void assertBasicCopy(final Function<T, T> copyCache) {
		// Arrange:
		final T cache = this.createCacheForCopyTests();

		// Act:
		final T copy = copyCache.apply(cache);

		// Assert: initial copy
		Assert.assertThat(copy.size(), IsEqual.equalTo(1 + 3));
		Assert.assertThat(copy.deepSize(), IsEqual.equalTo(1 + 7));
		Arrays.asList("foo", "bar", "bar.qux").stream()
				.map(NamespaceId::new)
				.forEach(id -> Assert.assertThat(id.toString(), copy.contains(id), IsEqual.equalTo(true)));
		Assert.assertThat(getQuantity(copy, "bar", 1), IsEqual.equalTo(new Quantity(7)));
		Assert.assertThat(getQuantity(copy, "bar.qux", 2), IsEqual.equalTo(new Quantity(9)));
	}

	// endregion

	// region special case of namespace nem

	@Test
	public void getReturnsExpectedNemNamespaceEntry() {
		// Arrange:
		final T cache = this.createCache();

		// Act:
		final NamespaceEntry namespaceEntry = cache.get(new NamespaceId("nem"));
		// TODO 20150715 J-B: (minor) since NamespaceEntry is mutable, this could be misused if someone adds mosaics to the entry

		// Assert:
		Assert.assertThat(namespaceEntry, IsNull.notNullValue());
		Assert.assertThat(namespaceEntry, IsSame.sameInstance(NamespaceConstants.NAMESPACE_ENTRY_NEM));
	}

	@Test
	public void containsReturnsTrueForNemNamespace() {
		// Arrange:
		final T cache = this.createCache();

		// Assert:
		Assert.assertThat(cache.contains(new NamespaceId("nem")), IsEqual.equalTo(true));
	}

	@Test
	public void isActiveReturnsTrueForNemNamespaceAtAllBlockHeights() {
		// Arrange:
		final T cache = this.createCache();
		final NamespaceId id = new NamespaceId("nem");

		Arrays.asList(BlockHeight.ONE, new BlockHeight(10000), new BlockHeight(100000000), BlockHeight.MAX).stream()
				.forEach(height -> {
					// Act:
					final boolean isActive = cache.isActive(id, height);

					// Assert:
					Assert.assertThat(height.toString(), isActive, IsEqual.equalTo(true));
				});
	}

	// endregion

	private static void addToCache(final NamespaceCache cache, final String... ids) {
		Arrays.stream(ids).map(NamespaceCacheTest::createNamespace).forEach(cache::add);
	}

	private static void addSmartTile(final NamespaceCache cache, final NamespaceId id, final int rawMosaicId, final int quantity) {
		final MosaicId mosaicId = Utils.createMosaicId(rawMosaicId);
		final Mosaics mosaics = cache.get(id).getMosaics();
		if (!mosaics.contains(mosaicId)) {
			mosaics.add(Utils.createMosaic(rawMosaicId));
		}

		mosaics.get(mosaicId).increaseSupply(new Quantity(quantity));
	}

	private static MosaicEntry getMosaicEntry(final NamespaceCache cache, final String namespace, final int mosaicId) {
		return cache.get(new NamespaceId(namespace)).getMosaics().get(Utils.createMosaicId(mosaicId));
	}

	private static Quantity getQuantity(final NamespaceCache cache, final String namespace, final int mosaicId) {
		return getMosaicEntry(cache, namespace, mosaicId).getSupply();
	}

	private static Namespace createNamespace(final String id) {
		return new Namespace(new NamespaceId(id), OWNERS[0], BlockHeight.ONE);
	}

	private static Namespace createNamespace(final String id, final Account owner, final BlockHeight height) {
		return new Namespace(new NamespaceId(id), owner, height);
	}
}
