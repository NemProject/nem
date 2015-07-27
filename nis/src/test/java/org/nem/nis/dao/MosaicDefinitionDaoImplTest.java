package org.nem.nis.dao;

import org.hamcrest.core.*;
import org.hibernate.*;
import org.hibernate.type.LongType;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.Utils;
import org.nem.nis.dao.retrievers.MosaicDefinitionRetriever;
import org.nem.nis.dbmodel.DbMosaicDefinition;

import java.util.*;

public class MosaicDefinitionDaoImplTest {

	// region getMosaicDefinitionsForAccount

	@Test
	public void getMosaicDefinitionsForAccountDelegatesToRetrieverForKnownAccount() {
		// Arrange:
		final Collection<DbMosaicDefinition> retrieverResult = new ArrayList<>();
		final TestContext context = new TestContext();
		Mockito.when(context.getMosaicDefinitionsForAccountMocked()).thenReturn(retrieverResult);

		// Act:
		final Collection<DbMosaicDefinition> result = context.mosaicDefinitionDao.getMosaicDefinitionsForAccount(
				context.account,
				new NamespaceId("foo"),
				Long.MAX_VALUE,
				25);

		// Assert:
		Assert.assertThat(result, IsSame.sameInstance(retrieverResult));
		Mockito.verify(context.retriever, Mockito.only())
				.getMosaicDefinitionsForAccount(context.session, 1L, new NamespaceId("foo"), Long.MAX_VALUE, 25);
	}

	@Test
	public void getMosaicDefinitionsForAccountBypassesRetrieverForUnknownAccount() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final TestContext context = new TestContext();
		context.markUnknown(account);

		// Act:
		final Collection<DbMosaicDefinition> result = context.mosaicDefinitionDao.getMosaicDefinitionsForAccount(
				account,
				new NamespaceId("foo"),
				Long.MAX_VALUE,
				25);

		// Assert:
		Assert.assertThat(result.isEmpty(), IsEqual.equalTo(true));
		Mockito.verify(context.retriever, Mockito.never()).getMosaicDefinitionsForAccount(
				Mockito.any(),
				Mockito.anyLong(),
				Mockito.any(),
				Mockito.anyLong(),
				Mockito.anyInt());
	}

	@Test
	public void getMosaicDefinitionsForAccountDelegatesToRetrieverWhenIdIsNull() {
		// Assert:
		assertGetMosaicDefinitionsForAccountDelegation(null, Long.MAX_VALUE);
	}

	@Test
	public void getMosaicDefinitionsForAccountDelegatesToRetrieverWhenIdIsNonNull() {
		// Assert:
		assertGetMosaicDefinitionsForAccountDelegation(12345L, 12345L);
	}

	private static void assertGetMosaicDefinitionsForAccountDelegation(final Long requestId, final long retrieverId) {
		// Arrange:
		final Collection<DbMosaicDefinition> retrieverResult = new ArrayList<>();
		final TestContext context = new TestContext();
		Mockito.when(context.getMosaicDefinitionsForAccountMocked()).thenReturn(retrieverResult);

		// Act:
		final Collection<DbMosaicDefinition> result = context.mosaicDefinitionDao.getMosaicDefinitionsForAccount(
				context.account,
				new NamespaceId("foo"),
				requestId,
				25);

		// Assert:
		Assert.assertThat(result, IsSame.sameInstance(retrieverResult));
		Mockito.verify(context.retriever, Mockito.only())
				.getMosaicDefinitionsForAccount(context.session, 1L, new NamespaceId("foo"), retrieverId, 25);
	}

	// endregion

	// region getMosaicDefinitionsForNamespace

	@Test
	public void getMosaicsForNamespaceDelegatesToRetriever() {
		// Arrange:
		final Collection<DbMosaicDefinition> retrieverResult = new ArrayList<>();
		final TestContext context = new TestContext();
		Mockito.when(context.getMosaicDefinitionsForNamespaceMocked())
				.thenReturn(retrieverResult);

		// Act:
		final Collection<DbMosaicDefinition> result = context.mosaicDefinitionDao.getMosaicDefinitionsForNamespace(
				new NamespaceId("foo"),
				Long.MAX_VALUE,
				25);

		// Assert:
		Assert.assertThat(result, IsSame.sameInstance(retrieverResult));
		Mockito.verify(context.retriever, Mockito.only())
				.getMosaicDefinitionsForNamespace(context.session, new NamespaceId("foo"), Long.MAX_VALUE, 25);
	}

	@Test
	public void getMosaicDefinitionsForNamespaceDelegatesToRetrieverWhenIdIsNull() {
		// Assert:
		assertGetMosaicDefinitionsForNamespaceDelegation(null, Long.MAX_VALUE);
	}

	@Test
	public void getMosaicDefinitionsForNamespaceDelegatesToRetrieverWhenIdIsNonNull() {
		// Assert:
		assertGetMosaicDefinitionsForNamespaceDelegation(12345L, 12345L);
	}

	private static void assertGetMosaicDefinitionsForNamespaceDelegation(final Long requestId, final long retrieverId) {
		// Arrange:
		final Collection<DbMosaicDefinition> retrieverResult = new ArrayList<>();
		final TestContext context = new TestContext();
		Mockito.when(context.getMosaicDefinitionsForNamespaceMocked()).thenReturn(retrieverResult);

		// Act:
		final Collection<DbMosaicDefinition> result = context.mosaicDefinitionDao.getMosaicDefinitionsForNamespace(
				new NamespaceId("foo"),
				requestId,
				25);

		// Assert:
		Assert.assertThat(result, IsSame.sameInstance(retrieverResult));
		Mockito.verify(context.retriever, Mockito.only())
				.getMosaicDefinitionsForNamespace(context.session, new NamespaceId("foo"), retrieverId, 25);
	}

	// endregion

	// region getMosaicDefinitions

	@Test
	public void getMosaicDefinitionsDelegatesToRetrieverWhenIdIsNull() {
		// Assert:
		assertGetMosaicDefinitionsDelegation(null, Long.MAX_VALUE);
	}

	@Test
	public void getMosaicsDelegatesToRetrieverWhenIdIsNonNull() {
		// Assert:
		assertGetMosaicDefinitionsDelegation(12345L, 12345L);
	}

	private static void assertGetMosaicDefinitionsDelegation(final Long requestId, final long retrieverId) {
		// Arrange:
		final Collection<DbMosaicDefinition> retrieverResult = new ArrayList<>();
		final TestContext context = new TestContext();
		Mockito.when(context.getMosaicDefinitionsMocked()).thenReturn(retrieverResult);

		// Act:
		final Collection<DbMosaicDefinition> result = context.mosaicDefinitionDao.getMosaicDefinitions(requestId, 25);

		// Assert:
		Assert.assertThat(result, IsSame.sameInstance(retrieverResult));
		Mockito.verify(context.retriever, Mockito.only())
				.getMosaicDefinitions(context.session, retrieverId, 25);
	}

	// endregion

	private static class TestContext {
		private final Account account = Utils.generateRandomAccount();
		private final SessionFactory sessionFactory = Mockito.mock(SessionFactory.class);
		private final MosaicDefinitionRetriever retriever = Mockito.mock(MosaicDefinitionRetriever.class);
		private final Session session = Mockito.mock(Session.class);
		private final SQLQuery sqlQuery = Mockito.mock(SQLQuery.class);
		private final MosaicDefinitionDaoImpl mosaicDefinitionDao = new MosaicDefinitionDaoImpl(this.sessionFactory, this.retriever);

		private TestContext() {
			Mockito.when(this.sessionFactory.getCurrentSession()).thenReturn(this.session);
			Mockito.when(this.session.createSQLQuery(Mockito.anyString())).thenReturn(this.sqlQuery);
			Mockito.when(this.sqlQuery.addScalar(Mockito.any(), Mockito.any())).thenReturn(this.sqlQuery);
			Mockito.when(this.sqlQuery.setParameter(Mockito.any(String.class), Mockito.any(LongType.class))).thenReturn(this.sqlQuery);
			Mockito.when(this.sqlQuery.uniqueResult()).thenReturn(1L);
		}

		private void markUnknown(final Account account) {
			final String encodedAddress = account.getAddress().getEncoded();
			Mockito.when(this.sqlQuery.setParameter(Mockito.eq(encodedAddress), Mockito.any(LongType.class))).thenReturn(this.sqlQuery);
			Mockito.when(this.sqlQuery.uniqueResult()).thenReturn(null);
		}

		private Collection<DbMosaicDefinition> getMosaicDefinitionsForAccountMocked() {
			return this.retriever.getMosaicDefinitionsForAccount(
					Mockito.any(),
					Mockito.anyLong(),
					Mockito.any(),
					Mockito.anyLong(),
					Mockito.anyInt());
		}

		private Collection<DbMosaicDefinition> getMosaicDefinitionsForNamespaceMocked() {
			return this.retriever.getMosaicDefinitionsForNamespace(
					Mockito.any(),
					Mockito.any(),
					Mockito.anyLong(),
					Mockito.anyInt());
		}

		private Collection<DbMosaicDefinition> getMosaicDefinitionsMocked() {
			return this.retriever.getMosaicDefinitions(
					Mockito.any(),
					Mockito.anyLong(),
					Mockito.anyInt());
		}
	}
}
