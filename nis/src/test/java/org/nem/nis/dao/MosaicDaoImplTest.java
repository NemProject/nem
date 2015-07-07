package org.nem.nis.dao;

import org.hamcrest.core.*;
import org.hibernate.*;
import org.hibernate.type.LongType;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.Utils;
import org.nem.nis.dao.retrievers.MosaicRetriever;
import org.nem.nis.dbmodel.*;

import java.util.*;

public class MosaicDaoImplTest {

	// region getMosaicsForAccount

	@Test
	public void getMosaicsForAccountDelegatesToRetrieverForKnownAccount() {
		// Arrange:
		final Collection<DbMosaic> retrieverResult = new ArrayList<>();
		final TestContext context = new TestContext();
		Mockito.when(context.retriever.getMosaicsForAccount(
				Mockito.any(),
				Mockito.anyLong(),
				Mockito.any(),
				Mockito.anyLong(),
				Mockito.anyInt()))
				.thenReturn(retrieverResult);

		// Act:
		final Collection<DbMosaic> result = context.mosaicDao.getMosaicsForAccount(
				context.account,
				new NamespaceId("foo"),
				Long.MAX_VALUE,
				25);

		// Assert:
		Assert.assertThat(result, IsSame.sameInstance(retrieverResult));
		Mockito.verify(context.retriever, Mockito.only())
				.getMosaicsForAccount(context.session, 1L, new NamespaceId("foo"), Long.MAX_VALUE, 25);
	}

	@Test
	public void getMosaicsForAccountBypassesRetrieverForUnknownAccount() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final TestContext context = new TestContext();
		context.markUnknown(account);

		// Act:
		final Collection<DbMosaic> result = context.mosaicDao.getMosaicsForAccount(
				account,
				new NamespaceId("foo"),
				Long.MAX_VALUE,
				25);

		// Assert:
		Assert.assertThat(result.isEmpty(), IsEqual.equalTo(true));
		Mockito.verify(context.retriever, Mockito.never()).getMosaicsForAccount(
				Mockito.any(),
				Mockito.anyLong(),
				Mockito.any(),
				Mockito.anyLong(),
				Mockito.anyInt());
	}

	@Test
	public void getMosaicsForAccountDelegatesToRetrieverWhenIdIsNull() {
		// Assert:
		assertGetMosaicsForAccountDelegation(null, Long.MAX_VALUE);
	}

	@Test
	public void getMosaicsForAccountDelegatesToRetrieverWhenIdIsNonNull() {
		// Assert:
		assertGetMosaicsForAccountDelegation(12345L, 12345L);
	}

	private static void assertGetMosaicsForAccountDelegation(final Long requestId, final long retrieverId) {
		// Arrange:
		final Collection<DbMosaic> retrieverResult = new ArrayList<>();
		final TestContext context = new TestContext();
		Mockito.when(context.retriever.getMosaicsForAccount(Mockito.any(),
				Mockito.anyLong(),
				Mockito.any(),
				Mockito.anyLong(),
				Mockito.anyInt()))
				.thenReturn(retrieverResult);


		// Act:
		final Collection<DbMosaic> result = context.mosaicDao.getMosaicsForAccount(
				context.account,
				new NamespaceId("foo"),
				requestId,
				25);

		// Assert:
		Assert.assertThat(result, IsSame.sameInstance(retrieverResult));
		Mockito.verify(context.retriever, Mockito.only())
				.getMosaicsForAccount(context.session, 1L, new NamespaceId("foo"), retrieverId, 25);
	}

	// endregion

	// region getMosaicsForAccount

	@Test
	public void getMosaicsForNamespaceDelegatesToRetriever() {
		// Arrange:
		final Collection<DbMosaic> retrieverResult = new ArrayList<>();
		final TestContext context = new TestContext();
		Mockito.when(context.retriever.getMosaicsForNamespace(
				Mockito.any(),
				Mockito.any(),
				Mockito.anyLong(),
				Mockito.anyInt()))
				.thenReturn(retrieverResult);

		// Act:
		final Collection<DbMosaic> result = context.mosaicDao.getMosaicsForNamespace(
				new NamespaceId("foo"),
				Long.MAX_VALUE,
				25);

		// Assert:
		Assert.assertThat(result, IsSame.sameInstance(retrieverResult));
		Mockito.verify(context.retriever, Mockito.only())
				.getMosaicsForNamespace(context.session, new NamespaceId("foo"), Long.MAX_VALUE, 25);
	}

	@Test
	public void getMosaicsForNamespaceDelegatesToRetrieverWhenIdIsNull() {
		// Assert:
		assertGetMosaicsForNamespaceDelegation(null, Long.MAX_VALUE);
	}

	@Test
	public void getMosaicsForNamespaceDelegatesToRetrieverWhenIdIsNonNull() {
		// Assert:
		assertGetMosaicsForNamespaceDelegation(12345L, 12345L);
	}

	private static void assertGetMosaicsForNamespaceDelegation(final Long requestId, final long retrieverId) {
		// Arrange:
		final Collection<DbMosaic> retrieverResult = new ArrayList<>();
		final TestContext context = new TestContext();
		Mockito.when(context.retriever.getMosaicsForNamespace(
				Mockito.any(),
				Mockito.any(),
				Mockito.anyLong(),
				Mockito.anyInt()))
				.thenReturn(retrieverResult);


		// Act:
		final Collection<DbMosaic> result = context.mosaicDao.getMosaicsForNamespace(
				new NamespaceId("foo"),
				requestId,
				25);

		// Assert:
		Assert.assertThat(result, IsSame.sameInstance(retrieverResult));
		Mockito.verify(context.retriever, Mockito.only())
				.getMosaicsForNamespace(context.session, new NamespaceId("foo"), retrieverId, 25);
	}

	// endregion

	// region getMosaicsForAccount

	@Test
	public void getMosaicsDelegatesToRetrieverWhenIdIsNull() {
		// Assert:
		assertGetMosaicsDelegation(null, Long.MAX_VALUE);
	}

	@Test
	public void getMosaicsDelegatesToRetrieverWhenIdIsNonNull() {
		// Assert:
		assertGetMosaicsDelegation(12345L, 12345L);
	}

	private static void assertGetMosaicsDelegation(final Long requestId, final long retrieverId) {
		// Arrange:
		final Collection<DbMosaic> retrieverResult = new ArrayList<>();
		final TestContext context = new TestContext();
		Mockito.when(context.retriever.getMosaics(
				Mockito.any(),
				Mockito.anyLong(),
				Mockito.anyInt()))
				.thenReturn(retrieverResult);


		// Act:
		final Collection<DbMosaic> result = context.mosaicDao.getMosaics(requestId, 25);

		// Assert:
		Assert.assertThat(result, IsSame.sameInstance(retrieverResult));
		Mockito.verify(context.retriever, Mockito.only())
				.getMosaics(context.session, retrieverId, 25);
	}

	// endregion

	private static class TestContext {
		private final Account account = Utils.generateRandomAccount();
		private final SessionFactory sessionFactory = Mockito.mock(SessionFactory.class);
		private final MosaicRetriever retriever = Mockito.mock(MosaicRetriever.class);
		private final Session session = Mockito.mock(Session.class);
		private final SQLQuery sqlQuery = Mockito.mock(SQLQuery.class);
		private final MosaicDaoImpl mosaicDao = new MosaicDaoImpl(this.sessionFactory, this.retriever);

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
	}
}
