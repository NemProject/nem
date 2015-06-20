package org.nem.nis.cache;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;

import java.util.Arrays;
import java.util.function.Function;

public abstract class NamespaceCacheTest<T extends CopyableCache<T> & NamespaceCache> {

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

	// region add/remove

	@Test
	public void canAddDifferentNamespacesToCache() {
		// Arrange:
		final NamespaceCache cache = this.createCache();

		// Act:
		cache.add(createNamespace("foo"));
		cache.add(createNamespace("foo.bar"));
		cache.add(createNamespace("bar.baz.qux"));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(3));
		Assert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new NamespaceId("bar.baz.qux")), IsEqual.equalTo(true));
	}

	@Test
	public void cannotAddSameNamespaceTwice() {
		// Arrange:
		final NamespaceCache cache = this.createCache();

		// Act:
		cache.add(createNamespace("foo"));

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.add(createNamespace("foo")), IllegalArgumentException.class);
	}

	@Test
	public void canRemoveExistingNamespacesFromCache() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		addToCache(cache, "foo", "foo.bar", "bar.baz.qux");

		// Act:
		cache.remove(new NamespaceId("foo"));
		cache.remove(new NamespaceId("bar.baz.qux"));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new NamespaceId("bar.baz.qux")), IsEqual.equalTo(false));
	}

	@Test
	public void cannotRemoveNonExistingNamespaceFromCache() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		addToCache(cache, "foo", "foo.bar", "bar.baz.qux");

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.remove(new NamespaceId("bar")), IllegalArgumentException.class);
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
		addToCache(cache, "foo", "foo.bar", "bar.baz.qux");

		// Assert:
		Assert.assertThat(cache.contains(new NamespaceId("foo")), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new NamespaceId("bar.baz.qux")), IsEqual.equalTo(true));
	}

	@Test
	public void containsReturnsFalseIfNamespaceDoesNotExistInCache() {
		// Arrange:
		final NamespaceCache cache = this.createCache();
		addToCache(cache, "foo", "foo.bar", "bar.baz.qux");

		// Assert:
		Assert.assertThat(cache.contains(new NamespaceId("fo0o")), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(new NamespaceId("bar.foo")), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(new NamespaceId("baz.bar.qux")), IsEqual.equalTo(false));
	}

	// endregion

	// region shallowCopyTo

	@Test
	public void shallowCopyToCopiesAllEntries() {
		// Assert:
		this.assertCopy(cache -> {
			final T copy = this.createCache();
			cache.shallowCopyTo(copy);
			return copy;
		});
	}

	// endregion

	// region copy

	@Test
	public void copyCopiesAllEntries() {
		// Assert:
		this.assertCopy(CopyableCache::copy);
	}

	// endregion

	private void assertCopy(final Function<T, T> copyCache) {
		// Arrange:
		final T cache = this.createCache();
		addToCache(cache, "foo", "foo.bar", "bar.baz.qux");

		// Act:
		final T copy = copyCache.apply(cache);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(3));
		Assert.assertThat(copy.contains(new NamespaceId("foo")), IsEqual.equalTo(true));
		Assert.assertThat(copy.contains(new NamespaceId("foo.bar")), IsEqual.equalTo(true));
		Assert.assertThat(copy.contains(new NamespaceId("bar.baz.qux")), IsEqual.equalTo(true));
	}

	private static void addToCache(final NamespaceCache cache, final String... ids) {
		Arrays.stream(ids).map(NamespaceCacheTest::createNamespace).forEach(cache::add);
	}

	private static Namespace createNamespace(final String id) {
		return new Namespace(new NamespaceId(id), Utils.generateRandomAccount(), BlockHeight.ONE);
	}
}
