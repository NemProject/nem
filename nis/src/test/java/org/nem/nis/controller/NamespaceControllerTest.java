package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.nis.controller.requests.NamespaceIdBuilder;
import org.nem.nis.dao.NamespaceDao;
import org.nem.nis.dbmodel.DbNamespace;
import org.nem.nis.mappers.*;

import java.util.*;
import java.util.stream.Collectors;

public class NamespaceControllerTest {

	//region getRoots

	@Test
	public void getRootsDelegatesReturnsAllRoots() {
		// Arrange:
		final TestContext context = new TestContext();
		final Collection<DbNamespace> dbNamespaces = Arrays.asList(createDbNamespace("a"), createDbNamespace("b"), createDbNamespace("c"));
		Mockito.when(context.namespaceDao.getRootNamespaces(Mockito.anyInt())).thenReturn(dbNamespaces);

		// Act:
		final SerializableList<Namespace> namespaces = context.controller.getRoots();

		// Assert:
		Mockito.verify(context.namespaceDao, Mockito.only()).getRootNamespaces(25);
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(), Mockito.eq(Namespace.class));

		Assert.assertThat(
				namespaces.asCollection().stream().map(n -> n.getId().toString()).collect(Collectors.toList()),
				IsEquivalent.equivalentTo("a", "b", "c"));
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
		Mockito.verify(context.mapper, Mockito.only()).map(Mockito.any(), Mockito.eq(Namespace.class));

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

	private static DbNamespace createDbNamespace(final String fqn) {
		final DbNamespace namespace = new DbNamespace();
		namespace.setFullName(fqn);
		return namespace;
	}

	private static NamespaceIdBuilder createIdBuilder(final NamespaceId id) {
		final NamespaceIdBuilder builder = new NamespaceIdBuilder();
		builder.setNamespace(id.toString());
		return builder;
	}

	public static class TestContext {
		private final NamespaceDao namespaceDao = Mockito.mock(NamespaceDao.class);
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final NamespaceController controller;

		public TestContext() {
			// set up the mock mapper
			Mockito.when(this.mapper.map(Mockito.any(), Mockito.eq(Namespace.class)))
					.then(invocationOnMock -> new Namespace(
							new NamespaceId(((DbNamespace)invocationOnMock.getArguments()[0]).getFullName()),
							Utils.generateRandomAccount(),
							BlockHeight.ONE));

			// create the controller
			final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
			final MapperFactory mapperFactory = Mockito.mock(MapperFactory.class);
			Mockito.when(mapperFactory.createDbModelToModelMapper(accountLookup)).thenReturn(this.mapper);
			this.controller = new NamespaceController(this.namespaceDao, mapperFactory, accountLookup);
		}
	}
}