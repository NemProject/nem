package org.nem.nis.controller;

import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.ncc.MosaicDefinitionMetaDataPair;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.*;
import org.nem.nis.controller.requests.DefaultPageBuilder;
import org.nem.nis.dao.ReadOnlyMosaicDefinitionDao;
import org.nem.nis.dbmodel.DbMosaicDefinition;
import org.nem.nis.mappers.NisDbModelToModelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// TODO 20150709 J-J review

public class MosaicDefinitionControllerTest {

	//region getNamespaceMosaicDefinitions

	@Test
	public void getNamespaceMosaicDefinitionsDelegatesReturnsAllMosaicDefinisionsForNamespace() {
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
		final SerializableList<MosaicDefinitionMetaDataPair> mosaicDefinitions = context.controller.getMosaicDefinitions(builder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.only()).getMosaicDefinitions(444L, 12);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(DbMosaicDefinition.class));

		Assert.assertThat(
				projectMosaics(mosaicDefinitions, n -> n.getMetaData().getId()),
				IsEquivalent.equivalentTo(8L, 5L, 11L));
		Assert.assertThat(
				projectMosaics(mosaicDefinitions, n -> n.getEntity().getId().getName()),
				IsEquivalent.equivalentTo("a", "b", "c"));
		Assert.assertThat(
				projectMosaics(mosaicDefinitions, n -> n.getEntity().getId().getNamespaceId().toString()),
				IsEquivalent.equivalentTo("foo", "foo", "foo"));
	}

	private static <T> List<T> projectMosaics(final SerializableList<MosaicDefinitionMetaDataPair> mosaics, final Function<MosaicDefinitionMetaDataPair, T> map) {
		return mosaics.asCollection().stream().map(map).collect(Collectors.toList());
	}

	//endregion

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
					.then(invocationOnMock -> new MosaicDefinition(
							Utils.generateRandomAccount(),
							new MosaicId(
									new NamespaceId(((DbMosaicDefinition)invocationOnMock.getArguments()[0]).getNamespaceId()),
									((DbMosaicDefinition)invocationOnMock.getArguments()[0]).getName()),
							new MosaicDescriptor("a mosaic"),
							Utils.createMosaicProperties()));

			// create the controller
			this.controller = new MosaicDefinitionController(this.mosaicDefinitionDao, this.mapper);
		}
	}
}
