package org.nem.nis.cache;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.state.ReadOnlyMosaicEntry;

import java.util.*;

public class NamespaceCacheUtilsTest {

	// region getMosaicDefinition

	@Test
	public void getMosaicDefinitionReturnsMosaicDefinitionIfMosaicDefinitionIsInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final MosaicDefinition mosaicDefinition = NamespaceCacheUtils.getMosaicDefinition(cache, createMosaicId("foo", "tokens"));

		// Assert:
		MatcherAssert.assertThat(mosaicDefinition, IsNull.notNullValue());
		MatcherAssert.assertThat(mosaicDefinition.getId(), IsEqual.equalTo(Utils.createMosaicDefinition("foo", "tokens").getId()));
	}

	@Test
	public void getMosaicDefinitionReturnsNullIfMosaicNamespaceIsNotInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final MosaicDefinition mosaicDefinition = NamespaceCacheUtils.getMosaicDefinition(cache, createMosaicId("bar", "tokens"));

		// Assert:
		MatcherAssert.assertThat(mosaicDefinition, IsNull.nullValue());
	}

	@Test
	public void getMosaicDefinitionReturnsNullIfMosaicIdIsNotInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final MosaicDefinition mosaicDefinition = NamespaceCacheUtils.getMosaicDefinition(cache, createMosaicId("foo", "coins"));

		// Assert:
		MatcherAssert.assertThat(mosaicDefinition, IsNull.nullValue());
	}

	// endregion

	// region getMosaicEntry

	@Test
	public void getMosaicEntryReturnsMosaicEntryIfMosaicEntryIsInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final ReadOnlyMosaicEntry entry = NamespaceCacheUtils.getMosaicEntry(cache, createMosaicId("foo", "tokens"));

		// Assert:
		MatcherAssert.assertThat(entry, IsNull.notNullValue());
		MatcherAssert.assertThat(entry.getMosaicDefinition().getId(), IsEqual.equalTo(createMosaicId("foo", "tokens")));
	}

	@Test
	public void getMosaicEntryReturnsNullIfMosaicNamespaceIsNotInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final ReadOnlyMosaicEntry entry = NamespaceCacheUtils.getMosaicEntry(cache, createMosaicId("bar", "tokens"));

		// Assert:
		MatcherAssert.assertThat(entry, IsNull.nullValue());
	}

	@Test
	public void getMosaicEntryReturnsNullIfMosaicIdIsNotInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final ReadOnlyMosaicEntry entry = NamespaceCacheUtils.getMosaicEntry(cache, createMosaicId("foo", "coins"));

		// Assert:
		MatcherAssert.assertThat(entry, IsNull.nullValue());
	}

	// endregion

	// region getMosaicOwners

	@Test
	public void getMosaicOwnersReturnsEmptyCollectionIfMosaicIdIsNotInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final Collection<Address> addresses = NamespaceCacheUtils.getMosaicOwners(cache, createMosaicId("foo", "coins"));

		// Assert:
		MatcherAssert.assertThat(addresses.isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void getMosaicOwnersReturnsEmptyCollectionIfNamespaceIdIsNotInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final Collection<Address> addresses = NamespaceCacheUtils.getMosaicOwners(cache, createMosaicId("bar", "tokens"));

		// Assert:
		MatcherAssert.assertThat(addresses.isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void getMosaicOwnersReturnsEmptyCollectionIfNamespaceHasNoMosaics() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final Collection<Address> addresses = NamespaceCacheUtils.getMosaicOwners(cache, createMosaicId("foo", "tokens"));

		// Assert:
		MatcherAssert.assertThat(addresses.isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void getMosaicOwnersReturnsSingleOwnerIfNamespaceHasMosaicWithSingleOwner() {
		// Arrange:
		final NamespaceCache cache = createCache();
		Collection<Address> owners = Collections.singletonList(Utils.generateRandomAddress());
		addOwners(cache, createMosaicId("foo", "tokens"), owners);

		// Act:
		final Collection<Address> addresses = NamespaceCacheUtils.getMosaicOwners(cache, createMosaicId("foo", "tokens"));

		// Assert:
		MatcherAssert.assertThat(addresses, IsEquivalent.equivalentTo(owners));
	}

	@Test
	public void getMosaicOwnersReturnsAllOwnersIfNamespaceHasMosaicWithMultipleOwners() {
		// Arrange:
		final NamespaceCache cache = createCache();
		Collection<Address> owners = Arrays.asList(Utils.generateRandomAddress(), Utils.generateRandomAddress(),
				Utils.generateRandomAddress());
		addOwners(cache, createMosaicId("foo", "tokens"), owners);

		// Act:
		final Collection<Address> addresses = NamespaceCacheUtils.getMosaicOwners(cache, createMosaicId("foo", "tokens"));

		// Assert:
		MatcherAssert.assertThat(addresses, IsEquivalent.equivalentTo(owners));
	}

	// endregion

	// region getMosaicIds

	@Test
	public void getMosaicIdsReturnsEmptyCollectionIfNamespaceIdIsNotInCache() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final Collection<MosaicId> mosaicIds = NamespaceCacheUtils.getMosaicIds(cache, new NamespaceId("bar"));

		// Assert:
		MatcherAssert.assertThat(mosaicIds.isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void getMosaicIdsReturnsCollectionWithSingleMosaicIdIfNamespaceHasSingleMosaic() {
		// Act:
		final ReadOnlyNamespaceCache cache = createCache();
		final Collection<MosaicId> mosaicIds = NamespaceCacheUtils.getMosaicIds(cache, new NamespaceId("foo"));

		// Assert:
		MatcherAssert.assertThat(mosaicIds, IsEquivalent.equivalentTo(Collections.singletonList(createMosaicId("foo", "tokens"))));
	}

	@Test
	public void getMosaicIdsReturnsAllMosaicIdsIfNamespaceHasMultipleMosaics() {
		// Arrange:
		final NamespaceCache cache = createCache();
		addMosaic(cache, "coins");
		addMosaic(cache, "paddles");

		// Act:
		final Collection<MosaicId> mosaicIds = NamespaceCacheUtils.getMosaicIds(cache, new NamespaceId("foo"));

		// Assert:
		MatcherAssert.assertThat(mosaicIds, IsEquivalent.equivalentTo(
				Arrays.asList(createMosaicId("foo", "tokens"), createMosaicId("foo", "coins"), createMosaicId("foo", "paddles"))));
	}

	// endregion

	private static NamespaceCache createCache() {
		final DefaultNamespaceCache cache = new DefaultNamespaceCache().copy();
		cache.add(new Namespace(new NamespaceId("foo"), Utils.generateRandomAccount(), BlockHeight.ONE));
		cache.get(new NamespaceId("foo")).getMosaics().add(Utils.createMosaicDefinition("foo", "tokens"));
		cache.commit();
		return cache;
	}

	private static void addMosaic(final NamespaceCache cache, final String mosaicName) {
		cache.get(new NamespaceId("foo")).getMosaics().add(Utils.createMosaicDefinition("foo", mosaicName));
	}

	private static void addOwners(final NamespaceCache cache, final MosaicId mosaicId, final Collection<Address> owners) {
		owners.forEach(owner -> cache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId).getBalances().incrementBalance(owner,
				Quantity.fromValue(1)));
	}

	private static MosaicId createMosaicId(final String namespaceName, final String mosaicName) {
		return new MosaicId(new NamespaceId(namespaceName), mosaicName);
	}
}
