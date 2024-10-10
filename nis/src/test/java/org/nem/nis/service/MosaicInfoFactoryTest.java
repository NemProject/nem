package org.nem.nis.service;

import java.util.List;
import java.util.function.Supplier;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.Utils;
import org.nem.nis.ForkConfiguration;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.cache.ReadOnlyNamespaceCache;
import org.nem.nis.dao.ReadOnlyNamespaceDao;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.state.*;

public class MosaicInfoFactoryTest {

	@Test
	public void factoryFiltersMosaicsThatAreNotFoundInNamespaceCache() {
		// Arrange: account info contains mosaic ids foo.bar and baz.qux
		final TestContext context = new TestContext();

		// Act:
		final List<Mosaic> mosaics = context.factory.getAccountOwnedMosaics(context.address);

		// Assert: two mosaics were returned (nem.xem, foo.bar), mosaic baz.qux was filtered
		MatcherAssert.assertThat(mosaics.size(), IsEqual.equalTo(2));
		final Mosaic xemMosaic = mosaics.get(0);
		MatcherAssert.assertThat(xemMosaic.getMosaicId(), IsEqual.equalTo(new MosaicId(new NamespaceId("nem"), "xem")));
		MatcherAssert.assertThat(xemMosaic.getQuantity(), IsEqual.equalTo(Quantity.ZERO));
		final Mosaic barMosaic = mosaics.get(1);
		MatcherAssert.assertThat(barMosaic.getMosaicId(), IsEqual.equalTo(new MosaicId(new NamespaceId("foo"), "bar")));
		MatcherAssert.assertThat(barMosaic.getQuantity(), IsEqual.equalTo(new Quantity(1234)));
	}

	private static class TestContext {
		private final Address address = Utils.generateRandomAddressWithPublicKey();
		private final AccountState accountState = new AccountState(this.address);

		private final ReadOnlyAccountStateCache accountStateCache = Mockito.mock(ReadOnlyAccountStateCache.class);
		private final ReadOnlyNamespaceCache namespaceCache = Mockito.mock(ReadOnlyNamespaceCache.class);
		private final ReadOnlyNamespaceDao namespaceDao = Mockito.mock(ReadOnlyNamespaceDao.class);
		private final NisDbModelToModelMapper mapper = Mockito.mock(NisDbModelToModelMapper.class);
		private final Supplier<BlockHeight> heightSupplier = () -> new BlockHeight(123);
		private final MosaicInfoFactory factory = new MosaicInfoFactory(this.accountStateCache, this.namespaceCache, this.namespaceDao,
				this.mapper, this.heightSupplier);

		public TestContext() {
			final NamespaceId namespaceId1 = new NamespaceId("foo");
			final NamespaceId namespaceId2 = new NamespaceId("baz");
			final NamespaceId namespaceIdNoMosaicId = new NamespaceId("nomosaic");
			final MosaicId mosaicId1 = new MosaicId(namespaceId1, "bar");
			final MosaicId mosaicId2 = new MosaicId(namespaceId2, "qux");
			final MosaicId mosaicIdNotInNamespace = new MosaicId(namespaceIdNoMosaicId, "crash");

			this.accountState.getAccountInfo().addMosaicId(mosaicId1);
			this.accountState.getAccountInfo().addMosaicId(mosaicId2);
			this.accountState.getAccountInfo().addMosaicId(mosaicIdNotInNamespace);
			Mockito.when(this.accountStateCache.findStateByAddress(this.address)).thenReturn(this.accountState);

			final ForkConfiguration forkConfiguration = new ForkConfiguration.Builder().build();
			final Namespace namespace = new Namespace(namespaceId1, Utils.generateRandomAccount(), BlockHeight.ONE);
			final Mosaics mosaics = new Mosaics(namespace.getId(), forkConfiguration.getMosaicRedefinitionForkHeight());
			mosaics.add(Utils.createMosaicDefinition(namespaceId1.toString(), mosaicId1.getName()));
			final MosaicEntry mosaicEntry = mosaics.get(mosaicId1);
			mosaicEntry.getBalances().incrementBalance(this.address, new Quantity(1234));
			final NamespaceEntry namespaceEntry = new NamespaceEntry(namespace, mosaics);
			Mockito.when(this.namespaceCache.get(namespaceId1)).thenReturn(namespaceEntry);
			Mockito.when(this.namespaceCache.get(namespaceId2)).thenReturn(null);

			// Add regression crash for when namespace is valid but mosaic entry is not present
			final Namespace namespaceNoMosaicIdEntry = new Namespace(namespaceId1, Utils.generateRandomAccount(), BlockHeight.ONE);
			final Mosaics mosaicsEmpty = new Mosaics(namespaceNoMosaicIdEntry.getId(), forkConfiguration.getMosaicRedefinitionForkHeight());
			final NamespaceEntry namespaceEntryNoMosaic = new NamespaceEntry(namespaceNoMosaicIdEntry, mosaicsEmpty);
			Mockito.when(this.namespaceCache.get(namespaceIdNoMosaicId)).thenReturn(namespaceEntryNoMosaic);
		}
	}
}
