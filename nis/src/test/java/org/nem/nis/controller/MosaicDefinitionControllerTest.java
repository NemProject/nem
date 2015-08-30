package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.ncc.MosaicDefinitionMetaDataPair;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.ReadOnlyMosaicDefinitionDao;
import org.nem.nis.dbmodel.DbMosaicDefinition;
import org.nem.nis.mappers.NisDbModelToModelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MosaicDefinitionControllerTest {

	//region getMosaicDefinitions

	@Test
	public void getMosaicDefinitionReturnsXemMosaicDefinitionForXemMosaicIdBypassingDao() {
		// Arrange:
		final TestContext context = new TestContext();

		final MosaicIdBuilder builder = new MosaicIdBuilder();
		builder.setMosaicId("nem * xem");

		// Act:
		final MosaicDefinition mosaicDefinition = context.controller.getMosaicDefinition(builder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.never()).getMosaicDefinition(Mockito.any());
		Mockito.verify(context.mapper, Mockito.never()).map(Mockito.any(DbMosaicDefinition.class));

		Assert.assertThat(mosaicDefinition, IsEqual.equalTo(MosaicConstants.MOSAIC_DEFINITION_XEM));
	}

	@Test
	public void getMosaicDefinitionReturnsMosaicDefinitionIfMosaicIdIsKnown() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMosaicDefinition dbMosaicDefinition = createDbMosaicDefinition(8L, "alice.vouchers", "foo");
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinition(Mockito.any())).thenReturn(dbMosaicDefinition);

		final MosaicIdBuilder builder = new MosaicIdBuilder();
		builder.setMosaicId("alice.vouchers * foo");

		// Act:
		final MosaicDefinition mosaicDefinition = context.controller.getMosaicDefinition(builder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.only()).getMosaicDefinition(builder.build());
		Mockito.verify(context.mapper, Mockito.only()).map(Mockito.any(DbMosaicDefinition.class));

		Assert.assertThat(mosaicDefinition.toString(), IsEqual.equalTo("alice.vouchers * foo"));
	}

	@Test
	public void getMosaicDefinitionThrowsIfMosaicIdIsUnknown() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinition(Mockito.any())).thenReturn(null);

		final MosaicIdBuilder builder = new MosaicIdBuilder();
		builder.setMosaicId("alice.vouchers * foo");

		// Act:
		ExceptionAssert.assertThrows(v -> context.controller.getMosaicDefinition(builder), IllegalArgumentException.class);
	}

	//endregion

	//region getMosaicDefinitions

	@Test
	public void getMosaicDefinitionsReturnsAllMosaicDefinitions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Collection<DbMosaicDefinition> dbMosaicDefinitions = Arrays.asList(
				createDbMosaicDefinition(8L, "foo", "a"),
				createDbMosaicDefinition(5L, "foo", "b"),
				createDbMosaicDefinition(11L, "foo", "c"));
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinitions(Mockito.anyLong(), Mockito.anyInt())).thenReturn(dbMosaicDefinitions);

		final DefaultPageBuilder builder = new DefaultPageBuilder();
		builder.setId("444");
		builder.setPageSize("12");

		// Act:
		final SerializableList<MosaicDefinitionMetaDataPair> pairs = context.controller.getMosaicDefinitions(builder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.only()).getMosaicDefinitions(444L, 12);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(DbMosaicDefinition.class));

		Assert.assertThat(
				projectMosaics(pairs, n -> n.getMetaData().getId()),
				IsEquivalent.equivalentTo(8L, 5L, 11L));
		Assert.assertThat(
				projectMosaics(pairs, n -> n.getEntity().getId()),
				IsEquivalent.equivalentTo(Utils.createMosaicId("foo", "a"), Utils.createMosaicId("foo", "b"), Utils.createMosaicId("foo", "c")));
	}

	//endregion

	//region getNamespaceMosaicDefinitions

	@Test
	public void getNamespaceMosaicDefinitionsDelegatesReturnsAllMosaicsForNamespace() {
		// Arrange:
		final TestContext context = new TestContext();
		final Collection<DbMosaicDefinition> dbMosaicDefinitions = Arrays.asList(
				createDbMosaicDefinition(8L, "foo", "a"),
				createDbMosaicDefinition(5L, "foo", "b"),
				createDbMosaicDefinition(11L, "foo", "c"));
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinitionsForNamespace(Mockito.any(), Mockito.anyLong(), Mockito.anyInt()))
				.thenReturn(dbMosaicDefinitions);

		final NamespaceIdMaxIdPageBuilder builder = new NamespaceIdMaxIdPageBuilder();
		builder.setId("444");
		builder.setPageSize("12");
		builder.setNamespace("foo");

		// Act:
		final SerializableList<MosaicDefinitionMetaDataPair> pairs = context.controller.getNamespaceMosaicDefinitions(builder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.only()).getMosaicDefinitionsForNamespace(new NamespaceId("foo"), 444L, 12);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(DbMosaicDefinition.class));

		Assert.assertThat(
				projectMosaics(pairs, n -> n.getMetaData().getId()),
				IsEquivalent.equivalentTo(8L, 5L, 11L));
		Assert.assertThat(
				projectMosaics(pairs, n -> n.getEntity().getId()),
				IsEquivalent.equivalentTo(Utils.createMosaicId("foo", "a"), Utils.createMosaicId("foo", "b"), Utils.createMosaicId("foo", "c")));
	}

	//endregion

	private static <T> List<T> projectMosaics(final SerializableList<MosaicDefinitionMetaDataPair> pairs, final Function<MosaicDefinitionMetaDataPair, T> map) {
		return pairs.asCollection().stream().map(map).collect(Collectors.toList());
	}

	private static DbMosaicDefinition createDbMosaicDefinition(final Long id, final String namespaceId, final String name) {
		final DbMosaicDefinition mosaic = new DbMosaicDefinition();
		mosaic.setId(id);
		mosaic.setName(name);
		mosaic.setNamespaceId(namespaceId);
		return mosaic;
	}

	public static class TestContext {
		private final ReadOnlyMosaicDefinitionDao mosaicDefinitionDao = Mockito.mock(ReadOnlyMosaicDefinitionDao.class);
		private final NisDbModelToModelMapper mapper = Mockito.mock(NisDbModelToModelMapper.class);
		private final MosaicDefinitionController controller;

		public TestContext() {
			// set up the mock mapper
			Mockito.when(this.mapper.map(Mockito.any(DbMosaicDefinition.class)))
					.then(invocationOnMock -> {
						final DbMosaicDefinition mosaicDefinition = ((DbMosaicDefinition)invocationOnMock.getArguments()[0]);
						return new MosaicDefinition(
								Utils.generateRandomAccount(),
								new MosaicId(
										new NamespaceId(mosaicDefinition.getNamespaceId()),
										mosaicDefinition.getName()),
								new MosaicDescriptor("a mosaic"),
								Utils.createMosaicProperties(),
								null);
					});

			// create the controller
			this.controller = new MosaicDefinitionController(this.mosaicDefinitionDao, this.mapper);
		}
	}
}
