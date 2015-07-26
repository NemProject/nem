package org.nem.nis.controller;

import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.ncc.MosaicMetaDataPair;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.DbMosaic;
import org.nem.nis.mappers.NisDbModelToModelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// TODO 20150709 J-J review

public class MosaicControllerTest {

	//region getNamespaceMosaics

	@Test
	public void getNamespaceMosaicsDelegatesReturnsAllMosaicsForNamespace() {
		// Arrange:
		final TestContext context = new TestContext();
		final Collection<DbMosaic> dbMosaics = Arrays.asList(
				createDbMosaic(8L, "foo", "a"),
				createDbMosaic(5L, "foo", "b"),
				createDbMosaic(11L, "foo", "c"));
		Mockito.when(context.mosaicDao.getMosaics(Mockito.anyLong(), Mockito.anyInt())).thenReturn(dbMosaics);

		final DefaultPageBuilder builder = new DefaultPageBuilder();
		builder.setId("444");
		builder.setPageSize("12");

		// Act:
		final SerializableList<MosaicMetaDataPair> mosaics = context.controller.getMosaics(builder);

		// Assert:
		Mockito.verify(context.mosaicDao, Mockito.only()).getMosaics(444L, 12);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(DbMosaic.class));

		Assert.assertThat(
				projectMosaics(mosaics, n -> n.getMetaData().getId()),
				IsEquivalent.equivalentTo(8L, 5L, 11L));
		Assert.assertThat(
				projectMosaics(mosaics, n -> n.getEntity().getId().getName()),
				IsEquivalent.equivalentTo("a", "b", "c"));
		Assert.assertThat(
				projectMosaics(mosaics, n -> n.getEntity().getId().getNamespaceId().toString()),
				IsEquivalent.equivalentTo("foo", "foo", "foo"));
	}

	private static <T> List<T> projectMosaics(final SerializableList<MosaicMetaDataPair> mosaics, final Function<MosaicMetaDataPair, T> map) {
		return mosaics.asCollection().stream().map(map).collect(Collectors.toList());
	}

	//endregion

	private static DbMosaic createDbMosaic(final Long id, final String namespaceId, final String name) {
		final DbMosaic mosaic = new DbMosaic();
		mosaic.setId(id);
		mosaic.setName(name);
		mosaic.setNamespaceId(namespaceId);
		return mosaic;
	}

	public static class TestContext {
		private final ReadOnlyMosaicDao mosaicDao = Mockito.mock(ReadOnlyMosaicDao.class);
		private final NisDbModelToModelMapper mapper = Mockito.mock(NisDbModelToModelMapper.class);
		private final MosaicController controller;

		public TestContext() {
			// set up the mock mapper
			Mockito.when(this.mapper.map(Mockito.any(DbMosaic.class)))
					.then(invocationOnMock -> new Mosaic(
							Utils.generateRandomAccount(),
							new MosaicId(
									new NamespaceId(((DbMosaic)invocationOnMock.getArguments()[0]).getNamespaceId()),
									((DbMosaic)invocationOnMock.getArguments()[0]).getName()),
							new MosaicDescriptor("a mosaic"),
							Utils.createMosaicProperties()));

			// create the controller
			this.controller = new MosaicController(this.mosaicDao, this.mapper);
		}
	}
}
