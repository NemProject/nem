package org.nem.nis.cache;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.nis.state.ReadOnlyMosaicEntry;

public class NamespaceCacheUtilsTest {

	//region getMosaic

	@Test
	public void getMosaicReturnsMosaicIfMosaicIsInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final Mosaic mosaic = NamespaceCacheUtils.getMosaic(cache, Utils.createMosaic("foo", "tokens").getId());

		// Assert:
		Assert.assertThat(mosaic, IsNull.notNullValue());
		Assert.assertThat(mosaic.getId(), IsEqual.equalTo(Utils.createMosaic("foo", "tokens").getId()));
	}

	@Test
	public void getMosaicReturnsNullIfMosaicNamespaceIsNotInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final Mosaic mosaic = NamespaceCacheUtils.getMosaic(cache, Utils.createMosaic("bar", "tokens").getId());

		// Assert:
		Assert.assertThat(mosaic, IsNull.nullValue());
	}

	@Test
	public void getMosaicReturnsNullIfMosaicIdIsNotInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final Mosaic mosaic = NamespaceCacheUtils.getMosaic(cache, Utils.createMosaic("foo", "coins").getId());

		// Assert:
		Assert.assertThat(mosaic, IsNull.nullValue());
	}

	//endregion

	//region getMosaicEntry

	@Test
	public void getMosaicEntryReturnsMosaicIfMosaicIsInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final ReadOnlyMosaicEntry entry = NamespaceCacheUtils.getMosaicEntry(cache, Utils.createMosaic("foo", "tokens").getId());

		// Assert:
		Assert.assertThat(entry, IsNull.notNullValue());
		Assert.assertThat(entry.getMosaic().getId(), IsEqual.equalTo(Utils.createMosaic("foo", "tokens").getId()));
	}

	@Test
	public void getMosaicEntryReturnsNullIfMosaicNamespaceIsNotInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final ReadOnlyMosaicEntry entry = NamespaceCacheUtils.getMosaicEntry(cache, Utils.createMosaic("bar", "tokens").getId());

		// Assert:
		Assert.assertThat(entry, IsNull.nullValue());
	}

	@Test
	public void getMosaicEntryReturnsNullIfMosaicIdIsNotInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final ReadOnlyMosaicEntry entry = NamespaceCacheUtils.getMosaicEntry(cache, Utils.createMosaic("foo", "coins").getId());

		// Assert:
		Assert.assertThat(entry, IsNull.nullValue());
	}

	//endregion

	private static NamespaceCache createCache() {
		final DefaultNamespaceCache cache = new DefaultNamespaceCache();
		cache.add(new Namespace(new NamespaceId("foo"), Utils.generateRandomAccount(), BlockHeight.ONE));
		cache.get(new NamespaceId("foo")).getMosaics().add(Utils.createMosaic("foo", "tokens"));
		return cache;
	}
}