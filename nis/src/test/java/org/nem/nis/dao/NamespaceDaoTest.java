package org.nem.nis.dao;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.hibernate.*;
import org.hibernate.type.LongType;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Address;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.Utils;
import org.nem.nis.dao.retrievers.NamespaceRetriever;
import org.nem.nis.dbmodel.DbNamespace;

import java.util.*;

public class NamespaceDaoTest {

	@Test
	public void getNamespacesForAccountDelegatesToRetrieverForKnownAccount() {
		// Arrange:
		final Collection<DbNamespace> retrieverResult = new ArrayList<>();
		final TestContext context = new TestContext();
		Mockito.when(context.retriever.getNamespacesForAccount(Mockito.any(), Mockito.anyLong(), Mockito.any(), Mockito.anyInt()))
				.thenReturn(retrieverResult);

		// Act:
		final Collection<DbNamespace> result = context.namespaceDao.getNamespacesForAccount(context.address, new NamespaceId("foo"), 25);

		// Assert:
		MatcherAssert.assertThat(result, IsSame.sameInstance(retrieverResult));
		Mockito.verify(context.retriever, Mockito.only()).getNamespacesForAccount(context.session, 1L, new NamespaceId("foo"), 25);
	}

	@Test
	public void getNamespacesForAccountBypassesRetrieverForUnknownAccount() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final TestContext context = new TestContext();
		context.markUnknown(address);

		// Act:
		final Collection<DbNamespace> result = context.namespaceDao.getNamespacesForAccount(address, new NamespaceId("foo"), 25);

		// Assert:
		MatcherAssert.assertThat(result.isEmpty(), IsEqual.equalTo(true));
		Mockito.verify(context.retriever, Mockito.never()).getNamespacesForAccount(Mockito.any(), Mockito.anyLong(), Mockito.any(),
				Mockito.anyInt());
	}

	@Test
	public void getNamespaceDelegatesToRetriever() {
		// Arrange:
		final NamespaceId id = new NamespaceId("foo");
		final DbNamespace retrieverResult = new DbNamespace();
		final TestContext context = new TestContext();
		Mockito.when(context.retriever.getNamespace(Mockito.any(), Mockito.any())).thenReturn(retrieverResult);

		// Act:
		final DbNamespace result = context.namespaceDao.getNamespace(id);

		// Assert:
		MatcherAssert.assertThat(result, IsSame.sameInstance(retrieverResult));
		Mockito.verify(context.retriever, Mockito.only()).getNamespace(context.session, id);
	}

	@Test
	public void getRootNamespacesDelegatesToRetrieverWhenIdIsNull() {
		// Assert:
		assertGetRootNamespacesDelegation(null, Long.MAX_VALUE);
	}

	@Test
	public void getRootNamespacesDelegatesToRetrieverWhenIdIsNonNull() {
		// Assert:
		assertGetRootNamespacesDelegation(1234L, 1234L);
	}

	private static void assertGetRootNamespacesDelegation(final Long requestId, final long retrieverId) {
		// Arrange:
		final Collection<DbNamespace> retrieverResult = new ArrayList<>();
		final TestContext context = new TestContext();
		Mockito.when(context.retriever.getRootNamespaces(Mockito.any(), Mockito.anyLong(), Mockito.anyInt())).thenReturn(retrieverResult);

		// Act:
		final Collection<DbNamespace> result = context.namespaceDao.getRootNamespaces(requestId, 25);

		// Assert:
		MatcherAssert.assertThat(result, IsSame.sameInstance(retrieverResult));
		Mockito.verify(context.retriever, Mockito.only()).getRootNamespaces(context.session, retrieverId, 25);
	}

	private static class TestContext {
		private final Address address = Utils.generateRandomAddress();
		private final SessionFactory sessionFactory = Mockito.mock(SessionFactory.class);
		private final NamespaceRetriever retriever = Mockito.mock(NamespaceRetriever.class);
		private final Session session = Mockito.mock(Session.class);
		private final SQLQuery sqlQuery = Mockito.mock(SQLQuery.class);
		private final NamespaceDaoImpl namespaceDao = new NamespaceDaoImpl(this.sessionFactory, this.retriever);

		private TestContext() {
			Mockito.when(this.sessionFactory.getCurrentSession()).thenReturn(this.session);
			Mockito.when(this.session.createSQLQuery(Mockito.anyString())).thenReturn(this.sqlQuery);
			Mockito.when(this.sqlQuery.addScalar(Mockito.any(), Mockito.any())).thenReturn(this.sqlQuery);
			Mockito.when(this.sqlQuery.setParameter(Mockito.any(String.class), Mockito.any(LongType.class))).thenReturn(this.sqlQuery);
			Mockito.when(this.sqlQuery.uniqueResult()).thenReturn(1L);
		}

		private void markUnknown(final Address address) {
			final String encodedAddress = address.getEncoded();
			Mockito.when(this.sqlQuery.setParameter(Mockito.eq(encodedAddress), Mockito.any(LongType.class))).thenReturn(this.sqlQuery);
			Mockito.when(this.sqlQuery.uniqueResult()).thenReturn(null);
		}
	}
}
