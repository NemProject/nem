package org.nem.nis.cache;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.nis.state.ReadOnlyMosaicEntry;

public class NamespaceCacheUtilsTest {

	//region getMosaicDefinition

	@Test
	public void getMosaicDefinitionReturnsMosaicDefinitionIfMosaicDefinitionIsInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final MosaicDefinition mosaicDefinition = NamespaceCacheUtils.getMosaicDefinition(cache, Utils.createMosaicDefinition("foo", "tokens").getId());

		// Assert:
		Assert.assertThat(mosaicDefinition, IsNull.notNullValue());
		Assert.assertThat(mosaicDefinition.getId(), IsEqual.equalTo(Utils.createMosaicDefinition("foo", "tokens").getId()));
	}

	@Test
	public void getMosaicDefinitionReturnsNullIfMosaicNamespaceIsNotInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final MosaicDefinition mosaicDefinition = NamespaceCacheUtils.getMosaicDefinition(cache, Utils.createMosaicDefinition("bar", "tokens").getId());

		// Assert:
		Assert.assertThat(mosaicDefinition, IsNull.nullValue());
	}

	@Test
	public void getMosaicDefinitionReturnsNullIfMosaicIdIsNotInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final MosaicDefinition mosaicDefinition = NamespaceCacheUtils.getMosaicDefinition(cache, Utils.createMosaicDefinition("foo", "coins").getId());

		// Assert:
		Assert.assertThat(mosaicDefinition, IsNull.nullValue());
	}

	//endregion

	//region getMosaicEntry

	@Test
	public void getMosaicEntryReturnsMosaicEntryIfMosaicEntryIsInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final ReadOnlyMosaicEntry entry = NamespaceCacheUtils.getMosaicEntry(cache, Utils.createMosaicDefinition("foo", "tokens").getId());

		// Assert:
		Assert.assertThat(entry, IsNull.notNullValue());
		Assert.assertThat(entry.getMosaicDefinition().getId(), IsEqual.equalTo(Utils.createMosaicDefinition("foo", "tokens").getId()));
	}

	@Test
	public void getMosaicEntryReturnsNullIfMosaicNamespaceIsNotInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final ReadOnlyMosaicEntry entry = NamespaceCacheUtils.getMosaicEntry(cache, Utils.createMosaicDefinition("bar", "tokens").getId());

		// Assert:
		Assert.assertThat(entry, IsNull.nullValue());
	}

	@Test
	public void getMosaicEntryReturnsNullIfMosaicIdIsNotInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final ReadOnlyMosaicEntry entry = NamespaceCacheUtils.getMosaicEntry(cache, Utils.createMosaicDefinition("foo", "coins").getId());

		// Assert:
		Assert.assertThat(entry, IsNull.nullValue());
	}

	//endregion

	private static NamespaceCache createCache() {
		final DefaultNamespaceCache cache = new DefaultNamespaceCache();
		cache.add(new Namespace(new NamespaceId("foo"), Utils.generateRandomAccount(), BlockHeight.ONE));
		cache.get(new NamespaceId("foo")).getMosaics().add(Utils.createMosaicDefinition("foo", "tokens"));
		return cache;
	}
}