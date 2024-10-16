package org.nem.nis.controller;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.controller.viewmodels.*;
import org.nem.nis.dao.ReadOnlyMosaicDefinitionDao;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.service.MosaicInfoFactory;

public class MosaicDefinitionControllerTest {

	// region getMosaicDefinition

	private void assertMosaicDefinitionReturnsXemMosaicDefinitionForXemMosaicIdBypassingDao(
			final BiFunction<MosaicDefinitionController, MosaicIdBuilder, MosaicDefinition> getter) {
		// Arrange:
		final TestContext context = new TestContext();

		final MosaicIdBuilder builder = new MosaicIdBuilder();
		builder.setMosaicId("nem:xem");

		// Act:
		final MosaicDefinition mosaicDefinition = getter.apply(context.controller, builder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.never()).getMosaicDefinition(Mockito.any());
		Mockito.verify(context.mapper, Mockito.never()).map(Mockito.any(), Mockito.eq(MosaicDefinition.class));

		MatcherAssert.assertThat(mosaicDefinition, IsEqual.equalTo(MosaicConstants.MOSAIC_DEFINITION_XEM));
	}

	private void assertMosaicDefinitionReturnsMosaicDefinitionIfMosaicIdIsKnown(
			final BiFunction<MosaicDefinitionController, MosaicIdBuilder, MosaicDefinition> getter) {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMosaicDefinition dbMosaicDefinition = createDbMosaicDefinition(8L, "alice.vouchers", "foo");
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinition(Mockito.any())).thenReturn(dbMosaicDefinition);

		final MosaicIdBuilder builder = new MosaicIdBuilder();
		builder.setMosaicId("alice.vouchers:foo");

		// Act:
		final MosaicDefinition mosaicDefinition = getter.apply(context.controller, builder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.only()).getMosaicDefinition(builder.build());
		Mockito.verify(context.mapper, Mockito.only()).map(dbMosaicDefinition, MosaicDefinition.class);

		MatcherAssert.assertThat(mosaicDefinition.toString(), IsEqual.equalTo("alice.vouchers:foo"));
	}

	private void assertMosaicDefinitionThrowsIfMosaicIdIsUnknown(
			final BiFunction<MosaicDefinitionController, MosaicIdBuilder, MosaicDefinition> getter) {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinition(Mockito.any())).thenReturn(null);

		final MosaicIdBuilder builder = new MosaicIdBuilder();
		builder.setMosaicId("alice.vouchers:foo");

		// Act:
		ExceptionAssert.assertThrows(v -> getter.apply(context.controller, builder), MissingResourceException.class);
	}

	@Test
	public void getMosaicDefinitionReturnsXemMosaicDefinitionForXemMosaicIdBypassingDao() {
		this.assertMosaicDefinitionReturnsXemMosaicDefinitionForXemMosaicIdBypassingDao(
				(controller, builder) -> controller.getMosaicDefinition(builder));
	}

	@Test
	public void getMosaicDefinitionReturnsMosaicDefinitionIfMosaicIdIsKnown() {
		this.assertMosaicDefinitionReturnsMosaicDefinitionIfMosaicIdIsKnown(
				(controller, builder) -> controller.getMosaicDefinition(builder));
	}

	@Test
	public void getMosaicDefinitionThrowsIfMosaicIdIsUnknown() {
		this.assertMosaicDefinitionThrowsIfMosaicIdIsUnknown((controller, builder) -> controller.getMosaicDefinition(builder));
	}

	@Test
	public void getMosaicDefinitionThrowsIfParentNamespaceIsInactive() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMosaicDefinition dbMosaicDefinition = createDbMosaicDefinition(8L, "alice.vouchers", "foo");
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinition(Mockito.any())).thenReturn(dbMosaicDefinition);
		Mockito.when(context.mosaicInfoFactory.isNamespaceActive(Mockito.any(NamespaceId.class))).thenReturn(false);

		final MosaicIdBuilder builder = new MosaicIdBuilder();
		builder.setMosaicId("alice.vouchers:foo");

		// Act:
		ExceptionAssert.assertThrows(v -> context.controller.getMosaicDefinition(builder), MissingResourceException.class);
	}

	// endregion

	// region getLastMosaicDefinition

	@Test
	public void getLastMosaicDefinitionReturnsXemMosaicDefinitionForXemMosaicIdBypassingDao() {
		this.assertMosaicDefinitionReturnsXemMosaicDefinitionForXemMosaicIdBypassingDao(
				(controller, builder) -> controller.getLastMosaicDefinition(builder));
	}

	@Test
	public void getLastMosaicDefinitionReturnsMosaicDefinitionIfMosaicIdIsKnown() {
		this.assertMosaicDefinitionReturnsMosaicDefinitionIfMosaicIdIsKnown(
				(controller, builder) -> controller.getLastMosaicDefinition(builder));
	}

	@Test
	public void getLastMosaicDefinitionThrowsIfMosaicIdIsUnknown() {
		this.assertMosaicDefinitionThrowsIfMosaicIdIsUnknown((controller, builder) -> controller.getLastMosaicDefinition(builder));
	}

	@Test
	public void getMosaicDefinitionReturnsMosaicDefinitionIfParentNamespaceIsInactive() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMosaicDefinition dbMosaicDefinition = createDbMosaicDefinition(8L, "alice.vouchers", "foo");
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinition(Mockito.any())).thenReturn(dbMosaicDefinition);
		Mockito.when(context.mosaicInfoFactory.isNamespaceActive(Mockito.any(NamespaceId.class))).thenReturn(false);

		final MosaicIdBuilder builder = new MosaicIdBuilder();
		builder.setMosaicId("alice.vouchers:foo");

		// Act:
		final MosaicDefinition mosaicDefinition = context.controller.getLastMosaicDefinition(builder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.only()).getMosaicDefinition(builder.build());
		Mockito.verify(context.mapper, Mockito.only()).map(dbMosaicDefinition, MosaicDefinition.class);

		MatcherAssert.assertThat(mosaicDefinition.toString(), IsEqual.equalTo("alice.vouchers:foo"));
	}

	// endregion

	// region getMosaicDefinitionWithSupply

	@Test
	public void getMosaicDefinitionWithSupplyReturnsEmptyListWhenNoMatch() {
		// Arrange:
		final TestContext context = new TestContext();

		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinitionWithSupply(Mockito.any(), Mockito.anyLong())).thenReturn(null);

		final MosaicIdBuilder builder = new MosaicIdBuilder();
		builder.setMosaicId("alice.vouchers:foo");

		final BlockHeightBuilder heightBuilder = new BlockHeightBuilder();
		heightBuilder.setHeight("1234");

		// Act:
		final SerializableList<MosaicDefinitionSupplyTuple> tuples = context.controller.getMosaicDefinitionWithSupply(builder,
				heightBuilder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.never()).getMosaicDefinition(Mockito.any());
		Mockito.verify(context.mapper, Mockito.never()).map(Mockito.any(), Mockito.eq(MosaicDefinition.class));

		MatcherAssert.assertThat(tuples.size(), IsEqual.equalTo(0));
	}

	@Test
	public void getMosaicDefinitionWithSupplyReturnsSingleItemListWhenMatch() {
		// Arrange:
		final TestContext context = new TestContext();

		final DbMosaicDefinition dbMosaicDefinition = createDbMosaicDefinition(8L, "alice.vouchers", "foo");
		final DbMosaicDefinitionSupplyTuple dbMosaicDefinitionSupplyTuple = new DbMosaicDefinitionSupplyTuple(dbMosaicDefinition,
				new Supply(111), new BlockHeight(234));
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinitionWithSupply(Mockito.any(), Mockito.anyLong()))
				.thenReturn(dbMosaicDefinitionSupplyTuple);

		final MosaicIdBuilder builder = new MosaicIdBuilder();
		builder.setMosaicId("alice.vouchers:foo");

		final BlockHeightBuilder heightBuilder = new BlockHeightBuilder();
		heightBuilder.setHeight("1234");

		// Act:
		final SerializableList<MosaicDefinitionSupplyTuple> tuples = context.controller.getMosaicDefinitionWithSupply(builder,
				heightBuilder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.only()).getMosaicDefinitionWithSupply(builder.build(), 1234L);
		Mockito.verify(context.mapper, Mockito.only()).map(dbMosaicDefinition, MosaicDefinition.class);

		MatcherAssert.assertThat(tuples.size(), IsEqual.equalTo(1));

		final MosaicDefinitionSupplyTuple tuple = tuples.get(0);
		MatcherAssert.assertThat(tuple.getMosaicDefinition().toString(), IsEqual.equalTo("alice.vouchers:foo"));
		MatcherAssert.assertThat(tuple.getSupply(), IsEqual.equalTo(new Supply(111)));
		MatcherAssert.assertThat(tuple.getExpirationHeight(), IsEqual.equalTo(new BlockHeight(234)));
	}

	// endregion

	// region getMosaicDefinitions

	@Test
	public void getMosaicDefinitionsReturnsAllMosaicDefinitions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Collection<DbMosaicDefinition> dbMosaicDefinitions = Arrays.asList(createDbMosaicDefinition(8L, "foo", "a"),
				createDbMosaicDefinition(5L, "foo", "b"), createDbMosaicDefinition(11L, "foo", "c"));
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinitions(444L, 12)).thenReturn(dbMosaicDefinitions);
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinitions(11L, 12)).thenReturn(Collections.emptyList());

		final DefaultPageBuilder builder = new DefaultPageBuilder();
		builder.setId("444");
		builder.setPageSize("12");

		// Act:
		final SerializableList<MosaicDefinitionMetaDataPair> pairs = context.controller.getMosaicDefinitions(builder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.times(1)).getMosaicDefinitions(444L, 12);
		Mockito.verify(context.mosaicDefinitionDao, Mockito.times(1)).getMosaicDefinitions(11L, 12);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(DbMosaicDefinition.class), Mockito.eq(MosaicDefinition.class));

		MatcherAssert.assertThat(projectMosaics(pairs, p -> p.getMetaData().getId()), IsEquivalent.equivalentTo(8L, 5L, 11L));
		MatcherAssert.assertThat(projectMosaics(pairs, p -> p.getEntity().getId()), IsEquivalent
				.equivalentTo(Utils.createMosaicId("foo", "a"), Utils.createMosaicId("foo", "b"), Utils.createMosaicId("foo", "c")));
	}

	@Test
	public void getMosaicDefinitionsFiltersMosaicDefinitionsWithInactiveParentNamespace() {
		// Arrange:
		final TestContext context = new TestContext();
		final Collection<DbMosaicDefinition> dbMosaicDefinitions = Arrays.asList(createDbMosaicDefinition(8L, "foo", "a"),
				createDbMosaicDefinition(5L, "bar", "b"), createDbMosaicDefinition(11L, "foo", "c"));
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinitions(444L, 12)).thenReturn(dbMosaicDefinitions);
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinitions(11L, 12)).thenReturn(Collections.emptyList());
		Mockito.when(context.mosaicInfoFactory.isNamespaceActive(new NamespaceId("bar"))).thenReturn(false);

		final DefaultPageBuilder builder = new DefaultPageBuilder();
		builder.setId("444");
		builder.setPageSize("12");

		// Act:
		final SerializableList<MosaicDefinitionMetaDataPair> pairs = context.controller.getMosaicDefinitions(builder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.times(1)).getMosaicDefinitions(444L, 12);
		Mockito.verify(context.mosaicDefinitionDao, Mockito.times(1)).getMosaicDefinitions(11L, 12);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(DbMosaicDefinition.class), Mockito.eq(MosaicDefinition.class));

		MatcherAssert.assertThat(projectMosaics(pairs, p -> p.getMetaData().getId()), IsEquivalent.equivalentTo(8L, 11L));
		MatcherAssert.assertThat(projectMosaics(pairs, p -> p.getEntity().getId()),
				IsEquivalent.equivalentTo(Utils.createMosaicId("foo", "a"), Utils.createMosaicId("foo", "c")));
	}

	// endregion

	// region getNamespaceMosaicDefinitions

	@Test
	public void getNamespaceMosaicDefinitionsReturnsAllMosaicsForNamespace() {
		// Arrange:
		final TestContext context = new TestContext();
		final Collection<DbMosaicDefinition> dbMosaicDefinitions = Arrays.asList(createDbMosaicDefinition(8L, "foo", "a"),
				createDbMosaicDefinition(5L, "foo", "b"), createDbMosaicDefinition(11L, "foo", "c"));
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinitionsForNamespace(Mockito.any(), Mockito.anyLong(), Mockito.anyInt()))
				.thenReturn(dbMosaicDefinitions);

		final DefaultPageBuilder pageBuilder = new DefaultPageBuilder();
		pageBuilder.setId("444");
		pageBuilder.setPageSize("12");

		final NamespaceIdBuilder idBuilder = new NamespaceIdBuilder();
		idBuilder.setNamespace("foo");

		// Act:
		final SerializableList<MosaicDefinitionMetaDataPair> pairs = context.controller.getNamespaceMosaicDefinitions(idBuilder,
				pageBuilder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.only()).getMosaicDefinitionsForNamespace(new NamespaceId("foo"), 444L, 12);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(DbMosaicDefinition.class), Mockito.eq(MosaicDefinition.class));

		MatcherAssert.assertThat(projectMosaics(pairs, p -> p.getMetaData().getId()), IsEquivalent.equivalentTo(8L, 5L, 11L));
		MatcherAssert.assertThat(projectMosaics(pairs, p -> p.getEntity().getId()), IsEquivalent
				.equivalentTo(Utils.createMosaicId("foo", "a"), Utils.createMosaicId("foo", "b"), Utils.createMosaicId("foo", "c")));
	}

	@Test
	public void getNamespaceMosaicDefinitionsReturnsEmptyListForInactiveNamespace() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.mosaicInfoFactory.isNamespaceActive(Mockito.any())).thenReturn(false);

		final DefaultPageBuilder pageBuilder = new DefaultPageBuilder();
		pageBuilder.setId("444");
		pageBuilder.setPageSize("12");

		final NamespaceIdBuilder idBuilder = new NamespaceIdBuilder();
		idBuilder.setNamespace("foo");

		// Act:
		final SerializableList<MosaicDefinitionMetaDataPair> pairs = context.controller.getNamespaceMosaicDefinitions(idBuilder,
				pageBuilder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.never()).getMosaicDefinitionsForNamespace(Mockito.any(), Mockito.anyLong(),
				Mockito.anyInt());
		Mockito.verify(context.mapper, Mockito.never()).map(Mockito.any(DbMosaicDefinition.class), Mockito.eq(MosaicDefinition.class));

		MatcherAssert.assertThat(pairs.size(), IsEqual.equalTo(0));
	}

	// endregion

	// region accountMosaicDefinitions

	@Test
	public void accountMosaicDefinitionsDelegatesToMosaicDefinitionDao() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final TestContext context = new TestContext();
		final Collection<DbMosaicDefinition> dbMosaicDefinitions = Arrays.asList(createDbMosaicDefinition(8L, "foo", "a"),
				createDbMosaicDefinition(5L, "foo", "b"), createDbMosaicDefinition(11L, "foo", "c"));
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinitionsForAccount(address, new NamespaceId("bazz"), 7L, 12))
				.thenReturn(dbMosaicDefinitions);
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinitionsForAccount(address, new NamespaceId("bazz"), 11L, 12))
				.thenReturn(Collections.emptyList());

		final AccountNamespaceBuilder idBuilder = new AccountNamespaceBuilder();
		idBuilder.setAddress(address.getEncoded());
		idBuilder.setParent("bazz");

		final DefaultPageBuilder pageBuilder = new DefaultPageBuilder();
		pageBuilder.setId("7");
		pageBuilder.setPageSize("12");

		// Act:
		final SerializableList<MosaicDefinition> mosaicDefinitions = context.controller.accountMosaicDefinitions(idBuilder, pageBuilder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.times(1)).getMosaicDefinitionsForAccount(address, new NamespaceId("bazz"), 7L,
				12);
		Mockito.verify(context.mosaicDefinitionDao, Mockito.times(1)).getMosaicDefinitionsForAccount(address, new NamespaceId("bazz"), 11L,
				12);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(DbMosaicDefinition.class), Mockito.eq(MosaicDefinition.class));

		MatcherAssert.assertThat(mosaicDefinitions.asCollection().stream().map(MosaicDefinition::getId).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(Utils.createMosaicId("foo", "a"), Utils.createMosaicId("foo", "b"),
						Utils.createMosaicId("foo", "c")));
	}

	@Test
	public void accountMosaicDefinitionsFiltersMosaicDefinitionsWithInactiveParentNamespace() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final TestContext context = new TestContext();
		final Collection<DbMosaicDefinition> dbMosaicDefinitions = Arrays.asList(createDbMosaicDefinition(8L, "foo", "a"),
				createDbMosaicDefinition(5L, "bar", "b"), createDbMosaicDefinition(11L, "foo", "c"));
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinitionsForAccount(address, new NamespaceId("bazz"), 7L, 12))
				.thenReturn(dbMosaicDefinitions);
		Mockito.when(context.mosaicDefinitionDao.getMosaicDefinitionsForAccount(address, new NamespaceId("bazz"), 11L, 12))
				.thenReturn(Collections.emptyList());
		Mockito.when(context.mosaicInfoFactory.isNamespaceActive(new NamespaceId("bar"))).thenReturn(false);

		final AccountNamespaceBuilder idBuilder = new AccountNamespaceBuilder();
		idBuilder.setAddress(address.getEncoded());
		idBuilder.setParent("bazz");

		final DefaultPageBuilder pageBuilder = new DefaultPageBuilder();
		pageBuilder.setId("7");
		pageBuilder.setPageSize("12");

		// Act:
		final SerializableList<MosaicDefinition> mosaicDefinitions = context.controller.accountMosaicDefinitions(idBuilder, pageBuilder);

		// Assert:
		Mockito.verify(context.mosaicDefinitionDao, Mockito.times(1)).getMosaicDefinitionsForAccount(address, new NamespaceId("bazz"), 7L,
				12);
		Mockito.verify(context.mosaicDefinitionDao, Mockito.times(1)).getMosaicDefinitionsForAccount(address, new NamespaceId("bazz"), 11L,
				12);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(DbMosaicDefinition.class), Mockito.eq(MosaicDefinition.class));

		MatcherAssert.assertThat(
				mosaicDefinitions.asCollection().stream().filter(m -> m.getId().getNamespaceId().toString().equals("foo"))
						.map(MosaicDefinition::getId).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(Utils.createMosaicId("foo", "a"), Utils.createMosaicId("foo", "c")));
	}

	// endregion

	private static <T> List<T> projectMosaics(final SerializableList<MosaicDefinitionMetaDataPair> pairs,
			final Function<MosaicDefinitionMetaDataPair, T> map) {
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
		private final MosaicInfoFactory mosaicInfoFactory = Mockito.mock(MosaicInfoFactory.class);
		private final MosaicDefinitionController controller;

		public TestContext() {
			// set up the mock mapper
			Mockito.when(this.mapper.map(Mockito.any(DbMosaicDefinition.class), Mockito.eq(MosaicDefinition.class)))
					.then(invocationOnMock -> {
						final DbMosaicDefinition mosaicDefinition = ((DbMosaicDefinition) invocationOnMock.getArguments()[0]);
						return new MosaicDefinition(Utils.generateRandomAccount(),
								new MosaicId(new NamespaceId(mosaicDefinition.getNamespaceId()), mosaicDefinition.getName()),
								new MosaicDescriptor("a mosaic"), Utils.createMosaicProperties(), null);
					});

			Mockito.when(this.mosaicInfoFactory.isNamespaceActive(Mockito.any())).thenReturn(true);

			// create the controller
			this.controller = new MosaicDefinitionController(this.mosaicDefinitionDao, this.mapper, this.mosaicInfoFactory);
		}
	}
}
