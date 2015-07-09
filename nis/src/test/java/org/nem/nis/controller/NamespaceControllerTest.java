package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.NisDbModelToModelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// TODO 20150709 J-J review

public class NamespaceControllerTest {

	//region getRoots

	@Test
	public void getRootsDelegatesReturnsAllRoots() {
		// Arrange:
		final TestContext context = new TestContext();
		final Collection<DbNamespace> dbNamespaces = Arrays.asList(
				createDbNamespace(8L, "a"),
				createDbNamespace(5L, "b"),
				createDbNamespace(11L, "c"));
		Mockito.when(context.namespaceDao.getRootNamespaces(Mockito.anyLong(), Mockito.anyInt())).thenReturn(dbNamespaces);

		final DefaultPageBuilder builder = new DefaultPageBuilder();
		builder.setId("444");
		builder.setPageSize("12");

		// Act:
		final SerializableList<NamespaceMetaDataPair> namespaces = context.controller.getRoots(builder);

		// Assert:
		Mockito.verify(context.namespaceDao, Mockito.only()).getRootNamespaces(444L, 12);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(DbNamespace.class));

		Assert.assertThat(
				projectNamespaces(namespaces, n -> n.getMetaData().getId()),
				IsEquivalent.equivalentTo(8L, 5L, 11L));
		Assert.assertThat(
				projectNamespaces(namespaces, n -> n.getNamespace().getId().toString()),
				IsEquivalent.equivalentTo("a", "b", "c"));
	}

	private static <T> List<T> projectNamespaces(final SerializableList<NamespaceMetaDataPair> namespaces, final Function<NamespaceMetaDataPair, T> map) {
		return namespaces.asCollection().stream().map(map).collect(Collectors.toList());
	}

	//endregion

	//region getNamespaceMosaics

	@Test
	public void getNamespaceMosaicsDelegatesReturnsAllMosaicsForNamespace() {
		// Arrange:
		final TestContext context = new TestContext();
		final Collection<DbMosaic> dbMosaics = Arrays.asList(
				createDbMosaic(8L, "foo", "a"),
				createDbMosaic(5L, "foo", "b"),
				createDbMosaic(11L, "foo", "c"));
		Mockito.when(context.mosaicDao.getMosaicsForNamespace(Mockito.any(), Mockito.anyLong(), Mockito.anyInt())).thenReturn(dbMosaics);

		final NamespaceIdMaxIdPageBuilder builder = new NamespaceIdMaxIdPageBuilder();
		builder.setId("444");
		builder.setPageSize("12");
		builder.setNamespace("foo");

		// Act:
		final SerializableList<MosaicMetaDataPair> mosaics = context.controller.getNamespaceMosaics(builder);

		// Assert:
		Mockito.verify(context.mosaicDao, Mockito.only()).getMosaicsForNamespace(new NamespaceId("foo"), 444L, 12);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(DbMosaic.class));

		Assert.assertThat(
				projectMosaics(mosaics, n -> n.getMetaData().getId()),
				IsEquivalent.equivalentTo(8L, 5L, 11L));
		Assert.assertThat(
				projectMosaics(mosaics, n -> n.getMosaic().getId().getName()),
				IsEquivalent.equivalentTo("a", "b", "c"));
		Assert.assertThat(
				projectMosaics(mosaics, n -> n.getMosaic().getId().getNamespaceId().toString()),
				IsEquivalent.equivalentTo("foo", "foo", "foo"));
	}

	private static <T> List<T> projectMosaics(final SerializableList<MosaicMetaDataPair> mosaics, final Function<MosaicMetaDataPair, T> map) {
		return mosaics.asCollection().stream().map(map).collect(Collectors.toList());
	}

	//endregion

	//region get

	@Test
	public void getOfKnownNamespaceReturnsKnownNamespace() {
		// Arrange:
		final NamespaceId id = new NamespaceId("a");
		final TestContext context = new TestContext();
		Mockito.when(context.namespaceDao.getNamespace(id)).thenReturn(createDbNamespace("a"));

		// Act:
		final Namespace namespace = context.controller.get(createIdBuilder(id));

		// Assert:
		Mockito.verify(context.namespaceDao, Mockito.only()).getNamespace(id);
		Mockito.verify(context.mapper, Mockito.only()).map(Mockito.any(DbNamespace.class));

		Assert.assertThat(namespace.getId(), IsEqual.equalTo(id));
	}

	@Test
	public void getOfUnknownNamespaceFails() {
		// Arrange:
		final NamespaceId id = new NamespaceId("a");
		final TestContext context = new TestContext();
		Mockito.when(context.namespaceDao.getNamespace(id)).thenReturn(null);

		// Act:
		ExceptionAssert.assertThrows(
				v -> context.controller.get(createIdBuilder(id)),
				MissingResourceException.class);
	}

	//endregion

	private static DbNamespace createDbNamespace(final Long id, final String fqn) {
		final DbNamespace namespace = new DbNamespace();
		namespace.setId(id);
		namespace.setFullName(fqn);
		return namespace;
	}

	private static DbNamespace createDbNamespace(final String fqn) {
		return createDbNamespace(null, fqn);
	}

	private static DbMosaic createDbMosaic(final Long id, final String namespaceId, final String name) {
		final DbMosaic mosaic = new DbMosaic();
		mosaic.setId(id);
		mosaic.setName(name);
		mosaic.setNamespaceId(namespaceId);
		return mosaic;
	}

	private static NamespaceIdBuilder createIdBuilder(final NamespaceId id) {
		final NamespaceIdBuilder builder = new NamespaceIdBuilder();
		builder.setNamespace(id.toString());
		return builder;
	}

	public static class TestContext {
		private final NamespaceDao namespaceDao = Mockito.mock(NamespaceDao.class);
		private final ReadOnlyMosaicDao mosaicDao = Mockito.mock(ReadOnlyMosaicDao.class);
		private final NisDbModelToModelMapper mapper = Mockito.mock(NisDbModelToModelMapper.class);
		private final NamespaceController controller;

		public TestContext() {
			// set up the mock mapper
			Mockito.when(this.mapper.map(Mockito.any(DbNamespace.class)))
					.then(invocationOnMock -> new Namespace(
							new NamespaceId(((DbNamespace)invocationOnMock.getArguments()[0]).getFullName()),
							Utils.generateRandomAccount(),
							BlockHeight.ONE));

			Mockito.when(this.mapper.map(Mockito.any(DbMosaic.class)))
					.then(invocationOnMock -> new Mosaic(
							Utils.generateRandomAccount(),
							new MosaicId(
									new NamespaceId(((DbMosaic)invocationOnMock.getArguments()[0]).getNamespaceId()),
									((DbMosaic)invocationOnMock.getArguments()[0]).getName()),
							new MosaicDescriptor("a mosaic"),
							new MosaicPropertiesImpl(new Properties())));

			// create the controller
			this.controller = new NamespaceController(this.namespaceDao, this.mosaicDao, this.mapper);
		}
	}
}