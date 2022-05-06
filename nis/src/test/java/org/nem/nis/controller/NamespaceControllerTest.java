package org.nem.nis.controller;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Address;
import org.nem.core.model.namespace.*;
import org.nem.core.model.ncc.NamespaceMetaDataPair;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.ReadOnlyNamespaceDao;
import org.nem.nis.dbmodel.DbNamespace;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.service.MosaicInfoFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NamespaceControllerTest {

	// region getRoots

	@Test
	public void getRootsReturnsAllRoots() {
		// Arrange:
		final TestContext context = new TestContext();
		final Collection<DbNamespace> dbNamespaces = Arrays.asList(createDbNamespace(8L, "a"), createDbNamespace(5L, "b"),
				createDbNamespace(11L, "c"));
		Mockito.when(context.namespaceDao.getRootNamespaces(444L, 12)).thenReturn(dbNamespaces);
		Mockito.when(context.namespaceDao.getRootNamespaces(11L, 12)).thenReturn(Collections.emptyList());

		final DefaultPageBuilder builder = new DefaultPageBuilder();
		builder.setId("444");
		builder.setPageSize("12");

		// Act:
		final SerializableList<NamespaceMetaDataPair> pairs = context.controller.getRoots(builder);

		// Assert:
		Mockito.verify(context.namespaceDao, Mockito.times(1)).getRootNamespaces(444L, 12);
		Mockito.verify(context.namespaceDao, Mockito.times(1)).getRootNamespaces(11L, 12);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(DbNamespace.class), Mockito.eq(Namespace.class));

		MatcherAssert.assertThat(projectNamespaces(pairs, p -> p.getMetaData().getId()), IsEquivalent.equivalentTo(8L, 5L, 11L));
		MatcherAssert.assertThat(projectNamespaces(pairs, p -> p.getEntity().getId().toString()), IsEquivalent.equivalentTo("a", "b", "c"));
	}

	@Test
	public void getRootsFiltersExpiredRoots() {
		// Arrange:
		final TestContext context = new TestContext();
		final Collection<DbNamespace> dbNamespaces = Arrays.asList(createDbNamespace(8L, "a"), createDbNamespace(5L, "b"),
				createDbNamespace(11L, "c"));
		Mockito.when(context.namespaceDao.getRootNamespaces(444L, 12)).thenReturn(dbNamespaces);
		Mockito.when(context.namespaceDao.getRootNamespaces(11L, 12)).thenReturn(Collections.emptyList());
		Mockito.when(context.mosaicInfoFactory.isNamespaceActive(new NamespaceId("b"))).thenReturn(false);

		final DefaultPageBuilder builder = new DefaultPageBuilder();
		builder.setId("444");
		builder.setPageSize("12");

		// Act:
		final SerializableList<NamespaceMetaDataPair> pairs = context.controller.getRoots(builder);

		// Assert:
		Mockito.verify(context.namespaceDao, Mockito.times(1)).getRootNamespaces(444L, 12);
		Mockito.verify(context.namespaceDao, Mockito.times(1)).getRootNamespaces(11L, 12);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(DbNamespace.class), Mockito.eq(Namespace.class));

		MatcherAssert.assertThat(projectNamespaces(pairs, p -> p.getMetaData().getId()), IsEquivalent.equivalentTo(8L, 11L));
		MatcherAssert.assertThat(projectNamespaces(pairs, p -> p.getEntity().getId().toString()), IsEquivalent.equivalentTo("a", "c"));
	}

	// endregion

	// region get

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
		Mockito.verify(context.mapper, Mockito.only()).map(Mockito.any(DbNamespace.class), Mockito.eq(Namespace.class));

		MatcherAssert.assertThat(namespace.getId(), IsEqual.equalTo(id));
	}

	@Test
	public void getOfUnknownNamespaceFails() {
		// Arrange:
		final NamespaceId id = new NamespaceId("a");
		final TestContext context = new TestContext();
		Mockito.when(context.namespaceDao.getNamespace(id)).thenReturn(null);

		// Act:
		ExceptionAssert.assertThrows(v -> context.controller.get(createIdBuilder(id)), MissingResourceException.class);
	}

	@Test
	public void getOfKnownExpiredNamespaceFails() {
		// Arrange:
		final NamespaceId id = new NamespaceId("a");
		final TestContext context = new TestContext();
		Mockito.when(context.namespaceDao.getNamespace(id)).thenReturn(createDbNamespace("a"));
		Mockito.when(context.mosaicInfoFactory.isNamespaceActive(Mockito.any())).thenReturn(false);

		// Act:
		ExceptionAssert.assertThrows(v -> context.controller.get(createIdBuilder(id)), MissingResourceException.class);
	}

	// endregion

	// region accountNamespaces

	@Test
	public void accountNamespacesDelegatesToNamespaceDao() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final TestContext context = new TestContext();
		final Collection<DbNamespace> dbNamespaces = Arrays.asList(createDbNamespace(8L, "a"), createDbNamespace(5L, "b"),
				createDbNamespace(11L, "c"));
		Mockito.when(context.namespaceDao.getNamespacesForAccount(address, new NamespaceId("foo"), 12)).thenReturn(dbNamespaces);

		final AccountNamespaceBuilder idBuilder = new AccountNamespaceBuilder();
		idBuilder.setAddress(address.getEncoded());
		idBuilder.setParent("foo");

		final DefaultPageBuilder pageBuilder = new DefaultPageBuilder();
		pageBuilder.setPageSize("12");

		// Act:
		final SerializableList<Namespace> namespaces = context.controller.accountNamespaces(idBuilder, pageBuilder);

		// Assert:
		Mockito.verify(context.namespaceDao, Mockito.times(1)).getNamespacesForAccount(address, new NamespaceId("foo"), 12);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(DbNamespace.class), Mockito.eq(Namespace.class));

		MatcherAssert.assertThat(namespaces.asCollection().stream().map(n -> n.getId().toString()).collect(Collectors.toList()),
				IsEquivalent.equivalentTo("a", "b", "c"));
	}

	@Test
	public void accountNamespacesFiltersExpiredNamespaces() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final TestContext context = new TestContext();
		final Collection<DbNamespace> dbNamespaces = Arrays.asList(createDbNamespace(8L, "a"), createDbNamespace(5L, "b"),
				createDbNamespace(11L, "c"));
		Mockito.when(context.namespaceDao.getNamespacesForAccount(address, new NamespaceId("foo"), 12)).thenReturn(dbNamespaces);
		Mockito.when(context.mosaicInfoFactory.isNamespaceActive(new NamespaceId("b"))).thenReturn(false);

		final AccountNamespaceBuilder idBuilder = new AccountNamespaceBuilder();
		idBuilder.setAddress(address.getEncoded());
		idBuilder.setParent("foo");

		final DefaultPageBuilder pageBuilder = new DefaultPageBuilder();
		pageBuilder.setPageSize("12");

		// Act:
		final SerializableList<Namespace> namespaces = context.controller.accountNamespaces(idBuilder, pageBuilder);

		// Assert:
		Mockito.verify(context.namespaceDao, Mockito.times(1)).getNamespacesForAccount(address, new NamespaceId("foo"), 12);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(DbNamespace.class), Mockito.eq(Namespace.class));

		MatcherAssert.assertThat(namespaces.asCollection().stream().map(n -> n.getId().toString()).collect(Collectors.toList()),
				IsEquivalent.equivalentTo("a", "c"));
	}

	// endregion

	private static <T> List<T> projectNamespaces(final SerializableList<NamespaceMetaDataPair> namespaces,
			final Function<NamespaceMetaDataPair, T> map) {
		return namespaces.asCollection().stream().map(map).collect(Collectors.toList());
	}

	private static DbNamespace createDbNamespace(final Long id, final String fqn) {
		final DbNamespace namespace = new DbNamespace();
		namespace.setId(id);
		namespace.setFullName(fqn);
		return namespace;
	}

	private static DbNamespace createDbNamespace(final String fqn) {
		return createDbNamespace(null, fqn);
	}

	private static NamespaceIdBuilder createIdBuilder(final NamespaceId id) {
		final NamespaceIdBuilder builder = new NamespaceIdBuilder();
		builder.setNamespace(id.toString());
		return builder;
	}

	public static class TestContext {
		private final ReadOnlyNamespaceDao namespaceDao = Mockito.mock(ReadOnlyNamespaceDao.class);
		private final NisDbModelToModelMapper mapper = Mockito.mock(NisDbModelToModelMapper.class);
		private final MosaicInfoFactory mosaicInfoFactory = Mockito.mock(MosaicInfoFactory.class);
		private final NamespaceController controller;

		public TestContext() {
			// set up the mock mapper
			Mockito.when(this.mapper.map(Mockito.any(DbNamespace.class), Mockito.eq(Namespace.class)))
					.then(invocationOnMock -> new Namespace(
							new NamespaceId(((DbNamespace) invocationOnMock.getArguments()[0]).getFullName()),
							Utils.generateRandomAccount(), BlockHeight.ONE));

			Mockito.when(this.mosaicInfoFactory.isNamespaceActive(Mockito.any())).thenReturn(true);

			// create the controller
			this.controller = new NamespaceController(this.namespaceDao, this.mapper, this.mosaicInfoFactory);
		}
	}
}
