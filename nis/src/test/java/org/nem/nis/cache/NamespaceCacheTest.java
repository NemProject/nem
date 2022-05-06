package org.nem.nis.cache;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.NamespaceConstants;
import org.nem.nis.state.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public abstract class NamespaceCacheTest<T extends ExtendedNamespaceCache<T>> {
	private static final Account[] OWNERS = {
			Utils.generateRandomAccount(), Utils.generateRandomAccount()
	};
	private static final BlockHeight[] HEIGHTS = {
			new BlockHeight(123), new BlockHeight(10000234)
	};

	/**
	 * Creates a cache.
	 *
	 * @return The cache.
	 */
	protected T createCache() {
		return this.createImmutableCache().copy();
	}

	/**
	 * Creates a cache that has auto-caching disabled.
	 *
	 * @return The cache.
	 */
	protected abstract T createImmutableCache();

	// region constructor

	@Test
	public void namespaceCacheInitiallyHasSizeOne() {
		// Act:
		final NamespaceCache cache = this.createCache();

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1));
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
		MatcherAssert.assertThat(namespace, IsEqual.equalTo(original));
	}

	@Test
	public void getPreservesRootMosaicsAcrossCalls() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final Account owner = Utils.generateRandomAccount();
		cache.add(new Namespace(new NamespaceId("foo"), owner, new BlockHeight(123)));
		addMosaic(cache, new NamespaceId("foo"), 1, 7);

		// Act:
		final NamespaceEntry entry = cache.get(new NamespaceId("foo"));

		// Assert:
		MatcherAssert.assertThat(entry.getMosaics().size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(getSupply(cache, "foo", 1), IsEqual.equalTo(new Supply(7)));
	}

	@Test
	public void getPreservesChildMosaicsAcrossCalls() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final Account owner = Utils.generateRandomAccount();
		cache.add(new Namespace(new NamespaceId("foo"), owner, new BlockHeight(123)));
		cache.add(new Namespace(new NamespaceId("foo.bar"), owner, new BlockHeight(123)));
		addMosaic(cache, new NamespaceId("foo.bar"), 2, 9);

		// Act:
		final NamespaceEntry entry = cache.get(new NamespaceId("foo.bar"));

		// Assert:
		MatcherAssert.assertThat(entry.getMosaics().size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(getSupply(cache, "foo.bar", 2), IsEqual.equalTo(new Supply(9)));
	}

	// endregion

	// region getRootNamespaceIds

	@Test
	public void getRootNamespaceIdsReturnsEmptyCollectionIfCacheIsEmpty() {
		// Arrange:
		final NamespaceCache cache = this.createCache();

		// Act:
		final Collection<NamespaceId> rootIds = cache.getRootNamespaceIds();

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(rootIds.isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void getRootNamespaceIdsReturnsAllRootNamespaceIds_SameOwner() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final Account owner = Utils.generateRandomAccount();
		final Collection<NamespaceId> expectedNamespaceIds = new ArrayList<>();
		for (int i = 0; i < 10; ++i) {
			final Namespace original = new Namespace(new NamespaceId("foo" + (i + 1)), owner, new BlockHeight(123 + i));
			expectedNamespaceIds.add(original.getId());
			cache.add(original);
		}

		// Act:
		final Collection<NamespaceId> rootIds = cache.getRootNamespaceIds();

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(11));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(11));
		MatcherAssert.assertThat(rootIds.size(), IsEqual.equalTo(10));
		MatcherAssert.assertThat(rootIds, IsEquivalent.equivalentTo(expectedNamespaceIds));
	}

	@Test
	public void getRootNamespaceIdsReturnsAllRootNamespaceIds_DifferentOwners() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final Collection<NamespaceId> expectedNamespaceIds = new ArrayList<>();
		for (int i = 0; i < 10; ++i) {
			final Account owner = Utils.generateRandomAccount();
			final Namespace root = new Namespace(new NamespaceId("foo" + (i + 1)), owner, new BlockHeight(123 + i));
			expectedNamespaceIds.add(root.getId());
			cache.add(root);
		}

		// Act:
		final Collection<NamespaceId> rootIds = cache.getRootNamespaceIds();

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(11));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(11));
		MatcherAssert.assertThat(rootIds.size(), IsEqual.equalTo(10));
		MatcherAssert.assertThat(rootIds, IsEquivalent.equivalentTo(expectedNamespaceIds));
	}

	@Test
	public void getRootNamespaceIdsReturnsAllRootNamespaceIds_WithSubNamespacesPresent() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final Collection<NamespaceId> expectedNamespaceIds = new ArrayList<>();
		for (int i = 0; i < 10; ++i) {
			final Account owner = Utils.generateRandomAccount();
			final Namespace root = new Namespace(new NamespaceId("foo" + (i + 1)), owner, new BlockHeight(123 + i));
			expectedNamespaceIds.add(root.getId());
			cache.add(root);
			if (0 == i % 2) {
				final Namespace subNamespace = new Namespace(new NamespaceId("foo" + (i + 1) + ".bar"), owner, new BlockHeight(123 + i));
				cache.add(subNamespace);
			}
		}

		// Act:
		final Collection<NamespaceId> rootIds = cache.getRootNamespaceIds();

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(11 + 5));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(11 + 5));
		MatcherAssert.assertThat(rootIds.size(), IsEqual.equalTo(10));
		MatcherAssert.assertThat(rootIds, IsEquivalent.equivalentTo(expectedNamespaceIds));
	}

	// endregion

	// region getSubNamespaceIds

	@Test
	public void getSubNamespaceIdsReturnsEmptyCollectionIfNoSubNamespacesArePresent() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final Account owner = Utils.generateRandomAccount();
		final Namespace root = new Namespace(new NamespaceId("foo"), owner, new BlockHeight(123));
		cache.add(root);

		// Act:
		final Collection<NamespaceId> subNamespaceIds = cache.getSubNamespaceIds(root.getId());

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1 + 1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 1));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(true));
		MatcherAssert.assertThat(subNamespaceIds.isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void getSubNamespaceIdsReturnsAllSubNamespacesIds() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final Collection<NamespaceId> expectedNamespaceIds = new ArrayList<>();
		final Account owner = Utils.generateRandomAccount();
		final Namespace root1 = new Namespace(new NamespaceId("foo"), owner, new BlockHeight(123));
		final Namespace root2 = new Namespace(new NamespaceId("qux"), owner, new BlockHeight(123));
		cache.add(root1);
		cache.add(root2);
		for (int i = 0; i < 10; ++i) {
			final Namespace namespaceLevel1 = new Namespace(new NamespaceId("foo.bar" + (i + 1)), owner, new BlockHeight(123));
			cache.add(namespaceLevel1);
			expectedNamespaceIds.add(namespaceLevel1.getId());
			if (0 == i % 2) {
				final Namespace namespaceLevel2 = new Namespace(new NamespaceId("foo.bar" + (i + 1) + ".baz" + (i + 1)), owner,
						new BlockHeight(123));
				cache.add(namespaceLevel2);
				expectedNamespaceIds.add(namespaceLevel2.getId());
			}
		}

		// Act:
		final Collection<NamespaceId> subNamespaceIds = cache.getSubNamespaceIds(root1.getId());

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1 + 2 + 10 + 5));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 2 + 10 + 5));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("qux")), IsEqual.equalTo(true));
		MatcherAssert.assertThat(subNamespaceIds.size(), IsEqual.equalTo(15));
		MatcherAssert.assertThat(subNamespaceIds, IsEquivalent.equivalentTo(expectedNamespaceIds));
	}

	// endregion

	// region contains

	@Test
	public void containsReturnsTrueIfNamespaceExistsInCache() {
		// Arrange:
		final T cache = this.createCache();
		this.addToCache(cache, "foo", "foo.bar", "foo.baz", "foo.baz.qux");

		// Assert:
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo.baz.qux")), IsEqual.equalTo(true));
	}

	@Test
	public void containsReturnsFalseIfNamespaceDoesNotExistInCache() {
		// Arrange:
		final T cache = this.createCache();
		this.addToCache(cache, "foo", "foo.bar", "foo.baz", "foo.baz.qux");

		// Assert:
		MatcherAssert.assertThat(cache.contains(new NamespaceId("fo0o")), IsEqual.equalTo(false));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("bar.foo")), IsEqual.equalTo(false));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo.bar.qux")), IsEqual.equalTo(false));
	}

	// endregion

	// region isActive - basic

	@Test
	public void isActiveReturnsTrueIfRootNamespaceIsKnown() {
		// Assert:
		this.assertBasicIsActive(new String[]{
				"x", "foo", "y"
		}, "foo", true);
	}

	@Test
	public void isActiveReturnsFalseIfRootNamespaceIsUnknown() {
		// Assert:
		this.assertBasicIsActive(new String[]{
				"x", "foo", "y"
		}, "bar", false);
	}

	@Test
	public void isActiveReturnsTrueIfSubNamespaceIsKnown() {
		// Assert:
		this.assertBasicIsActive(new String[]{
				"x", "foo", "foo.bar", "y"
		}, "foo.bar", true);
	}

	@Test
	public void isActiveReturnsFalseIfSubNamespaceIsUnknown() {
		// Assert:
		this.assertBasicIsActive(new String[]{
				"x", "foo", "foo.bar", "y"
		}, "too.bar", false);
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
		MatcherAssert.assertThat(isActive, IsEqual.equalTo(expectedResult));
	}

	// endregion

	// region isActive - height-based inactivity

	@Test
	public void isActiveReturnsTrueIfAtLeastOneRootNamespacesForGivenRootIdIsActive() {
		// Assert:
		this.assertHeightBasedIsActive(new String[]{
				"x", "foo", "y",
		}, "foo", HEIGHTS[1], true);
	}

	@Test
	public void isActiveReturnsFalseIfAllRootNamespacesForGivenRootIdAreInactive() {
		// Assert:
		this.assertHeightBasedIsActive(new String[]{
				"x", "foo", "y",
		}, "foo", BlockHeight.ONE, false);
	}

	@Test
	public void isActiveReturnsTrueIfAtLeastOneRootNamespacesForGivenSubIdIsActive() {
		// Assert:
		this.assertHeightBasedIsActive(new String[]{
				"x", "foo", "foo.bar", "y",
		}, "foo.bar", HEIGHTS[1], true);
	}

	@Test
	public void isActiveReturnsFalseIfAllRootNamespacesForGivenSubIdAreInactive() {
		// Assert:
		this.assertHeightBasedIsActive(new String[]{
				"x", "foo", "foo.bar", "y",
		}, "foo.bar", BlockHeight.ONE, false);
	}

	private void assertHeightBasedIsActive(final String[] names, final String testName, final BlockHeight testHeight,
			final boolean expectedResult) {
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
		MatcherAssert.assertThat(isActive, IsEqual.equalTo(expectedResult));
	}

	// endregion

	// region isActive- owner-based inactivity

	@Test
	public void isActiveReturnsTrueIfAtLeastOneRootNamespaceForGivenIdIsActiveAndHasSameOwner() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo.bar", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo", OWNERS[0], new BlockHeight(1000000)));

		// Assert:
		// - at HEIGHTS[0] both have the same owner
		MatcherAssert.assertThat(cache.isActive(new NamespaceId("foo"), HEIGHTS[0]), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.isActive(new NamespaceId("foo.bar"), HEIGHTS[0]), IsEqual.equalTo(true));

		// - at 1000000 both have the same owner
		MatcherAssert.assertThat(cache.isActive(new NamespaceId("foo"), new BlockHeight(1000000)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.isActive(new NamespaceId("foo.bar"), new BlockHeight(1000000)), IsEqual.equalTo(true));
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
		MatcherAssert.assertThat(cache.isActive(new NamespaceId("foo"), HEIGHTS[0]), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.isActive(new NamespaceId("foo.bar"), HEIGHTS[0]), IsEqual.equalTo(true));

		// - at 1000000 root and sub have different owners
		MatcherAssert.assertThat(cache.isActive(new NamespaceId("foo"), new BlockHeight(1000000)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.isActive(new NamespaceId("foo.bar"), new BlockHeight(1000000)), IsEqual.equalTo(false));
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
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1 + 4));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 4));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo.bar.qux")), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("bar")), IsEqual.equalTo(true));
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
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1 + 2));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 4));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.isActive(new NamespaceId("foo.bar"), HEIGHTS[1]), IsEqual.equalTo(true));
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
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1 + 1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 2));
		MatcherAssert.assertThat(cache.contains(id), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.get(id).getNamespace().getOwner(), IsEqual.equalTo(OWNERS[1]));
		MatcherAssert.assertThat(cache.get(id).getNamespace().getHeight(), IsEqual.equalTo(HEIGHTS[1]));
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
		MatcherAssert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNERS[0]));
		MatcherAssert.assertThat(namespace.getHeight(), IsEqual.equalTo(HEIGHTS[0]));
	}

	@Test
	public void cannotAddSubNamespacesIfRootHasDifferentOwner() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[0]));
		cache.add(createNamespace("foo.baz", OWNERS[0], HEIGHTS[0]));

		// Assert:
		for (final String name : Arrays.asList("foo.bar", "foo.baz.bar")) {
			ExceptionAssert.assertThrows(v -> cache.add(createNamespace(name, OWNERS[1], HEIGHTS[1])), IllegalArgumentException.class);
		}
	}

	@Test
	public void addingSameRootNamespaceTwiceUpdatesHeightOfSubNamespacesIfRootHasSameOwner() {
		// Assert:
		this.assertSubNamespaceUpdateBehaviorForAdding(OWNERS[0], new Integer[]{
				0, 0, 0, 0, 0
		}, new Integer[]{
				1, 1, 1, 0, 0
		});
	}

	@Test
	public void addingSameRootNamespaceTwiceDoesNotUpdateSubNamespacesIfRootHasDifferentOwner() {
		// Assert:
		this.assertSubNamespaceUpdateBehaviorForAdding(OWNERS[1], new Integer[]{
				1, -1, -1, 0, 0
		}, new Integer[]{
				1, -1, -1, 0, 0
		});
	}

	private void assertSubNamespaceUpdateBehaviorForAdding(final Account newRootOwner, final Integer[] expectedOwnerIndices,
			final Integer[] expectedHeightIndices) {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final String[] ids = {
				"foo", "foo.bar", "foo.bar.baz", "bar", "bar.baz"
		};
		Arrays.stream(ids).forEach(s -> cache.add(createNamespace(s, OWNERS[0], HEIGHTS[0])));

		// Act:
		cache.add(createNamespace("foo", newRootOwner, HEIGHTS[1]));

		// Assert:
		IntStream.range(0, ids.length).forEach(i -> {
			final NamespaceEntry entry = cache.get(new NamespaceId(ids[i]));
			if (-1 == expectedOwnerIndices[i]) {
				MatcherAssert.assertThat(entry, IsNull.nullValue());
			} else {
				final Namespace namespace = entry.getNamespace();
				MatcherAssert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNERS[expectedOwnerIndices[i]]));
				MatcherAssert.assertThat(namespace.getHeight(), IsEqual.equalTo(HEIGHTS[expectedHeightIndices[i]]));
			}
		});
	}

	@Test
	public void canRemoveExistingSubNamespacesFromCache() {
		// Arrange:
		final T cache = this.createCache();
		this.addToCache(cache, "foo", "bar", "foo.baz", "foo.bar", "bar.baz", "bar.baz.qux");

		// Act:
		cache.remove(new NamespaceId("foo.baz"));
		cache.remove(new NamespaceId("bar.baz.qux"));

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1 + 4));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 4));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo.baz")), IsEqual.equalTo(false));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("bar.baz.qux")), IsEqual.equalTo(false));
	}

	@Test
	public void removeExistingRootNamespacesRemovesRootNamespaceFromCacheIfRootOnlyExistsOnceInRootMap() {
		// Arrange:
		final T cache = this.createCache();
		this.addToCache(cache, "foo", "bar");

		// Act:
		cache.remove(new NamespaceId("foo"));

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1 + 1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 1));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(false));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("bar")), IsEqual.equalTo(true));
	}

	@Test
	public void removalOfRootWithDescendantsShouldFail() {
		// Arrange:
		final T cache = this.createCache();
		this.addToCache(cache, "foo", "foo.bar");

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.remove(new NamespaceId("foo")), IllegalArgumentException.class);
	}

	@Test
	public void removalOfParentWithDescendantsShouldFail() {
		// Arrange:
		final T cache = this.createCache();
		this.addToCache(cache, "foo", "foo.bar", "foo.bar.qux");

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.remove(new NamespaceId("foo.bar")), IllegalArgumentException.class);
	}

	@Test
	public void removeExistingRootNamespacesUpdatesHeightOfSubNamespacesIfRootExistsMoreThanOnceInRootMapAndNewRootHasSameOwnerAsSubNamespace() {
		this.assertSubNamespaceUpdateBehaviorForRemoving(OWNERS[0], new Integer[]{
				0, 0, 0, 1, 1
		}, new Integer[]{
				0, 0, 0, 1, 1
		});
	}

	@Test
	public void removeExistingRootNamespacesDoesNotUpdateSubNamespacesIfRootExistsMoreThanOnceInRootMapAndNewRootHasDifferentOwnerThanSubNamespace() {
		this.assertSubNamespaceUpdateBehaviorForRemoving(OWNERS[1], new Integer[]{
				1, -1, -1, 1, 1
		}, new Integer[]{
				0, -1, -1, 1, 1
		});
	}

	private void assertSubNamespaceUpdateBehaviorForRemoving(final Account newRootOwner, final Integer[] expectedOwnerIndices,
			final Integer[] expectedHeightIndices) {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		final String[] ids = {
				"foo", "foo.bar", "foo.bar.baz", "bar", "bar.baz"
		};
		final Integer[] indices = {
				0, 0, 0, 1, 1
		};
		IntStream.range(0, ids.length).forEach(i -> cache.add(createNamespace(ids[i], OWNERS[indices[i]], HEIGHTS[indices[i]])));
		cache.add(createNamespace("foo", newRootOwner, HEIGHTS[0]));
		cache.add(createNamespace("foo", OWNERS[0], HEIGHTS[1])); // (this entry will be removed by the remove below)

		// Act:
		cache.remove(new NamespaceId("foo"));

		// Assert:
		// - the root namespace (foo) was not removed because it existed previously
		// - in the case of a -1 owner index, the corresponding namespace is considered inactive (because there was an owner change)
		final int adjustment = Long.valueOf(Arrays.stream(expectedOwnerIndices).filter(i -> -1 == i).count()).intValue();
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1 + 5 - adjustment));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 8 - adjustment));
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(true));

		// - all foo descendants have reverted to their original owners and heights
		IntStream.range(0, ids.length).forEach(i -> {
			final NamespaceEntry entry = cache.get(new NamespaceId(ids[i]));
			if (-1 == expectedOwnerIndices[i]) {
				MatcherAssert.assertThat(entry, IsNull.nullValue());
			} else {
				final Namespace namespace = entry.getNamespace();
				MatcherAssert.assertThat(namespace.getOwner(), IsEqual.equalTo(OWNERS[expectedOwnerIndices[i]]));
				MatcherAssert.assertThat(namespace.getHeight(), IsEqual.equalTo(HEIGHTS[expectedHeightIndices[i]]));
			}
		});
	}

	@Test
	public void cannotRemoveNonExistingNamespaceFromCache() {
		// Arrange:
		final T cache = this.createCache();
		this.addToCache(cache, "foo", "foo.bar", "foo.bar.qux");

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.remove(new NamespaceId("bar")), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> cache.remove(new NamespaceId("foo.qux")), IllegalArgumentException.class);
	}

	@Test
	public void cannotRemovePreviouslyExistingNonExistingNamespaceFromCache() {
		// Arrange:
		final T cache = this.createCache();
		this.addToCache(cache, "foo", "foo.bar", "foo.bar.qux", "bar");
		cache.remove(new NamespaceId("bar"));
		cache.remove(new NamespaceId("foo.bar.qux"));

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.remove(new NamespaceId("bar")), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> cache.remove(new NamespaceId("foo.bar.qux")), IllegalArgumentException.class);
	}

	// endregion

	// region mosaic propagation

	@Test
	public void namespaceRenewalWithSameOwnerPreservesMosaics() {
		// Arrange:
		final T cache = this.createCache();
		this.addToCache(cache, "foo", "bar", "bar.qux");
		addMosaic(cache, new NamespaceId("bar"), 1, 7);
		addMosaic(cache, new NamespaceId("bar.qux"), 2, 9);

		// Act:
		cache.add(createNamespace("bar", OWNERS[0], HEIGHTS[1]));

		// Assert:
		MatcherAssert.assertThat(getSupply(cache, "bar", 1), IsEqual.equalTo(new Supply(7)));
		MatcherAssert.assertThat(getSupply(cache, "bar.qux", 2), IsEqual.equalTo(new Supply(9)));
	}

	@Test
	public void namespaceRenewalWithDifferentOwnerDoesNotPreserveMosaics() {
		// Arrange:
		final T cache = this.createCache();
		this.addToCache(cache, "foo", "bar", "bar.qux");
		addMosaic(cache, new NamespaceId("bar"), 1, 7);
		addMosaic(cache, new NamespaceId("bar.qux"), 2, 9);

		// Act:
		cache.add(createNamespace("bar", OWNERS[1], HEIGHTS[1]));
		cache.add(createNamespace("bar.qux", OWNERS[1], HEIGHTS[1]));

		// Assert:
		MatcherAssert.assertThat(getMosaicEntry(cache, "bar", 1), IsNull.nullValue());
		MatcherAssert.assertThat(getMosaicEntry(cache, "bar.qux", 2), IsNull.nullValue());
	}

	@Test
	public void namespaceRollbackRestoresOriginalMosaics() {
		// Arrange:
		final T cache = this.createCache();
		this.addToCache(cache, "foo", "bar", "bar.qux");
		addMosaic(cache, new NamespaceId("bar"), 1, 7);
		addMosaic(cache, new NamespaceId("bar.qux"), 2, 9);

		// Act:
		cache.add(createNamespace("bar", OWNERS[1], HEIGHTS[1]));
		cache.remove(new NamespaceId("bar"));

		// Assert:
		MatcherAssert.assertThat(getSupply(cache, "bar", 1), IsEqual.equalTo(new Supply(7)));
		MatcherAssert.assertThat(getSupply(cache, "bar.qux", 2), IsEqual.equalTo(new Supply(9)));
	}

	// endregion

	// region prune

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
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1 + 12));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 12));

		// Act: prune at height 1002
		cache.prune(new BlockHeight(1002L));

		// Assert: only the items no less than the prune height (2, 3) remain
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1 + 6));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 6));
		for (final String rootName : Arrays.asList("2", "3")) {
			MatcherAssert.assertThat(cache.contains(new NamespaceId(rootName)), IsEqual.equalTo(true));
			MatcherAssert.assertThat(cache.contains(new NamespaceId(rootName + ".a")), IsEqual.equalTo(true));
			MatcherAssert.assertThat(cache.contains(new NamespaceId(rootName + ".a.a")), IsEqual.equalTo(true));
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
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1 + 2));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 5));

		// Act: prune at height 1002
		cache.prune(new BlockHeight(1002L));

		// Assert: no items were removed
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1 + 2));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 3));

		// Act: remove all items
		cache.remove(new NamespaceId("0.a"));
		cache.remove(new NamespaceId("0"));
		cache.remove(new NamespaceId("0"));

		// Assert: all items were removed (the root required two removals because it had two links)
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1));
	}

	// endregion

	// region shallowCopyTo / copy

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
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		MatcherAssert.assertThat(copy.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
	}

	@Test
	public void shallowCopyMosaicAdjustmentsAreLinked() {
		// Arrange:
		final T cache = this.createCacheForCopyTests();

		// Act:
		final T copy = this.createCache();
		cache.shallowCopyTo(copy);

		// Act: update mosaics quantities
		addMosaic(cache, new NamespaceId("bar"), 1, 7);
		addMosaic(cache, new NamespaceId("bar.qux"), 2, 11);

		// Assert: the supplies are updated in both the original and shallow copy
		MatcherAssert.assertThat(getSupply(cache, "bar", 1), IsEqual.equalTo(new Supply(14)));
		MatcherAssert.assertThat(getSupply(copy, "bar", 1), IsEqual.equalTo(new Supply(14)));
		MatcherAssert.assertThat(getSupply(cache, "bar.qux", 2), IsEqual.equalTo(new Supply(20)));
		MatcherAssert.assertThat(getSupply(copy, "bar.qux", 2), IsEqual.equalTo(new Supply(20)));
	}

	@Test
	@SuppressWarnings("rawtypes")
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
		copy.remove(new NamespaceId("foo"));

		// Assert: the namespace should be removed from the copy but not the original
		// (the first but not the second 'foo' namespace has a 'bar' subnamespace, so when the second
		// is removed (in the copy), the 'foo.bar' namespace is present)
		MatcherAssert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(false));
		MatcherAssert.assertThat(copy.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
	}

	@Test
	public void copyMosaicAdjustmentsAreUnlinked() {
		// Arrange:
		final T cache = this.createCacheForCopyTests();

		// Act:
		final T copy = cache.copy();

		// Act: update mosaics quantities in copy
		addMosaic(copy, new NamespaceId("bar"), 1, 7);
		addMosaic(copy, new NamespaceId("bar.qux"), 2, 11);

		// Assert: the supplies are only updated in the copy cache
		MatcherAssert.assertThat(getSupply(cache, "bar", 1), IsEqual.equalTo(new Supply(7)));
		MatcherAssert.assertThat(getSupply(copy, "bar", 1), IsEqual.equalTo(new Supply(14)));
		MatcherAssert.assertThat(getSupply(cache, "bar.qux", 2), IsEqual.equalTo(new Supply(9)));
		MatcherAssert.assertThat(getSupply(copy, "bar.qux", 2), IsEqual.equalTo(new Supply(20)));
	}

	private T createCacheForCopyTests() {
		// Arrange:
		final T cache = this.createImmutableCache();
		final T copy = cache.copy();
		this.addToCache(copy, "foo", "foo.bar", "foo.baz", "foo.baz.qux", "bar", "bar.qux");
		copy.add(createNamespace("foo", OWNERS[1], HEIGHTS[1]));
		addMosaic(copy, new NamespaceId("bar"), 1, 7);
		addMosaic(copy, new NamespaceId("bar.qux"), 2, 9);
		copy.commit();
		return cache;
	}

	private void assertBasicCopy(final Function<T, T> copyCache) {
		// Arrange:
		final T cache = this.createCacheForCopyTests();

		// Act:
		final T copy = copyCache.apply(cache);

		// Assert: initial copy
		MatcherAssert.assertThat(copy.size(), IsEqual.equalTo(1 + 3));
		MatcherAssert.assertThat(copy.deepSize(), IsEqual.equalTo(1 + 7));
		Arrays.asList("foo", "bar", "bar.qux").stream().map(NamespaceId::new)
				.forEach(id -> MatcherAssert.assertThat(id.toString(), copy.contains(id), IsEqual.equalTo(true)));
		MatcherAssert.assertThat(getSupply(copy, "bar", 1), IsEqual.equalTo(new Supply(7)));
		MatcherAssert.assertThat(getSupply(copy, "bar.qux", 2), IsEqual.equalTo(new Supply(9)));
	}

	// endregion

	// region special case of namespace nem

	@Test
	public void getReturnsExpectedNemNamespaceEntry() {
		// Arrange:
		final T cache = this.createCache();

		// Act:
		final NamespaceEntry namespaceEntry = cache.get(new NamespaceId("nem"));

		// Assert:
		MatcherAssert.assertThat(namespaceEntry, IsNull.notNullValue());
		MatcherAssert.assertThat(namespaceEntry, IsSame.sameInstance(NamespaceConstants.NAMESPACE_ENTRY_NEM));
	}

	@Test
	public void containsReturnsTrueForNemNamespace() {
		// Arrange:
		final T cache = this.createCache();

		// Assert:
		MatcherAssert.assertThat(cache.contains(new NamespaceId("nem")), IsEqual.equalTo(true));
	}

	@Test
	public void isActiveReturnsTrueForNemNamespaceAtAllBlockHeights() {
		// Arrange:
		final T cache = this.createCache();
		final NamespaceId id = new NamespaceId("nem");

		Arrays.asList(BlockHeight.ONE, new BlockHeight(10000), new BlockHeight(100000000), BlockHeight.MAX).stream().forEach(height -> {
			// Act:
			final boolean isActive = cache.isActive(id, height);

			// Assert:
			MatcherAssert.assertThat(height.toString(), isActive, IsEqual.equalTo(true));
		});
	}

	// endregion

	private void addToCache(final T cache, final String... ids) {
		Arrays.stream(ids).map(NamespaceCacheTest::createNamespace).forEach(cache::add);
		cache.commit();
	}

	private static void addMosaic(final NamespaceCache cache, final NamespaceId id, final int rawMosaicId, final int quantity) {
		final MosaicId mosaicId = Utils.createMosaicId(id, rawMosaicId);
		final Mosaics mosaics = cache.get(id).getMosaics();
		if (!mosaics.contains(mosaicId)) {
			mosaics.add(Utils.createMosaicDefinition(id, rawMosaicId));
		}

		mosaics.get(mosaicId).increaseSupply(new Supply(quantity));
	}

	private static MosaicEntry getMosaicEntry(final NamespaceCache cache, final String namespace, final int mosaicId) {
		final NamespaceId namespaceId = new NamespaceId(namespace);
		return cache.get(namespaceId).getMosaics().get(Utils.createMosaicId(namespaceId, mosaicId));
	}

	private static Supply getSupply(final NamespaceCache cache, final String namespace, final int mosaicId) {
		return getMosaicEntry(cache, namespace, mosaicId).getSupply();
	}

	private static Namespace createNamespace(final String id) {
		return new Namespace(new NamespaceId(id), OWNERS[0], BlockHeight.ONE);
	}

	private static Namespace createNamespace(final String id, final Account owner, final BlockHeight height) {
		return new Namespace(new NamespaceId(id), owner, height);
	}
}
