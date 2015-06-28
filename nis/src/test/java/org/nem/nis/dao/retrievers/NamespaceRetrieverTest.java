package org.nem.nis.dao.retrievers;

import org.hamcrest.core.*;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.DbNamespace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.stream.Collectors;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class NamespaceRetrieverTest {

	@Autowired
	AccountDao accountDao;

	@Autowired
	SessionFactory sessionFactory;

	protected Session session;

	@Before
	public void createDb() {
		this.session = this.sessionFactory.openSession();
		this.createAccounts(5);
		this.setupNamespaces();
	}

	@After
	public void destroyDb() {
		DbUtils.dbCleanup(this.session);
		this.session.close();
	}

	//region getNamespacesForAccount

	@Test
	public void getNamespacesForAccountRetrievesAllNamespacesForSpecificAccount() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		final Collection<DbNamespace> dbNamespaces = retriever.getNamespacesForAccount(this.session, 1, null, 300);

		// Assert:
		Assert.assertThat(dbNamespaces.size(), IsEqual.equalTo(222));
	}

	@Test
	public void getNamespacesForAccountRetrievesAllNamespacesForSpecificAccountAndParent() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		final Collection<DbNamespace> dbNamespaces = retriever.getNamespacesForAccount(this.session, 1, new NamespaceId("aa"), 300);

		// Assert:
		Assert.assertThat(dbNamespaces.size(), IsEqual.equalTo(110));
		dbNamespaces.stream()
				.map(n -> new NamespaceId(n.getFullName()).getRoot())
				.forEach(root -> Assert.assertThat(root, IsEqual.equalTo(new NamespaceId("aa"))));
	}

	@Test
	public void getNamespacesForAccountRetrievesNoNamespacesForSpecificAccountAndParentOwnedByOtherAccount() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		final Collection<DbNamespace> dbNamespaces = retriever.getNamespacesForAccount(this.session, 2, new NamespaceId("aa"), 300);

		// Assert:
		Assert.assertThat(dbNamespaces.size(), IsEqual.equalTo(0));
	}

	@Test
	public void getNamespacesForAccountRetrievesUpdatedOwnerAndHeightForAllNamespacesForSpecificAccountAndParent() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		// originally "aaa" is owned by account 2 who provisioned at height 101, at height 2000 account 3 has provisioned the namespace
		// only namespace "aaa.b" should be found
		final List<DbNamespace> dbNamespaces = new ArrayList<>(retriever.getNamespacesForAccount(this.session, 3, new NamespaceId("aaa"), 300));

		// Assert:
		Assert.assertThat(dbNamespaces.size(), IsEqual.equalTo(1));
		Assert.assertThat(dbNamespaces.get(0).getOwner().getId(), IsEqual.equalTo(3L));
		Assert.assertThat(dbNamespaces.get(0).getHeight(), IsEqual.equalTo(2000L));
	}

	@Test
	public void getNamespacesForAccountRetrievesUpdatedHeightForRootNamespaceIfOwnerDoesNotChange() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		// originally "a" is is provisioned by account 1 at height 1, at height 5000 account 1 renews the provision of the namespace
		final List<DbNamespace> dbNamespaces = new ArrayList<>(retriever.getNamespacesForAccount(this.session, 1, new NamespaceId("a"), 300));

		// Assert:
		dbNamespaces.stream().forEach(n -> {
			Assert.assertThat(n.getOwner().getId(), IsEqual.equalTo(1L));
			Assert.assertThat(n.getHeight(), IsEqual.equalTo(5000L));
		});
	}

	//endregion

	//region getNamespace

	@Test
	public void getNamespaceRetrievesSpecifiedNamespace() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		final DbNamespace dbNamespace = retriever.getNamespace(this.session, new NamespaceId("aa.bbb.cccc"));

		// Assert:
		Assert.assertThat(dbNamespace.getFullName(), IsEqual.equalTo("aa.bbb.cccc"));
	}

	@Test
	public void getNamespaceReturnsNullWhenSpecifiedNamespaceRootIsUnknown() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		final DbNamespace dbNamespace = retriever.getNamespace(this.session, new NamespaceId("aa.bbb.abcd"));

		// Assert:
		Assert.assertThat(dbNamespace, IsNull.nullValue());
	}

	@Test
	public void getNamespaceReturnsNullWhenSpecifiedNamespaceIsUnknown() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		final DbNamespace dbNamespace = retriever.getNamespace(this.session, new NamespaceId("zz.bbb.cccc"));

		// Assert:
		Assert.assertThat(dbNamespace, IsNull.nullValue());
	}

	@Test
	public void getNamespaceRetrievesUpdatedOwnerAndHeightForRootNamespace() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		// originally "aaa" is owned by account 2 who provisioned at height 101, at height 2000 account 3 has provisioned the namespace
		final DbNamespace dbNamespace = retriever.getNamespace(this.session, new NamespaceId("aaa"));

		// Assert:
		Assert.assertThat(dbNamespace.getOwner().getId(), IsEqual.equalTo(3L));
		Assert.assertThat(dbNamespace.getHeight(), IsEqual.equalTo(2000L));
	}

	@Test
	public void getNamespaceRetrievesUpdatedOwnerAndHeightForSubNamespace() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		// originally "aaa" is owned by account 2 who provisioned at height 101, at height 2000 account 3 has provisioned the namespace
		final DbNamespace dbNamespace = retriever.getNamespace(this.session, new NamespaceId("aaa.bbb.ccc"));

		// Assert:
		Assert.assertThat(dbNamespace.getOwner().getId(), IsEqual.equalTo(3L));
		Assert.assertThat(dbNamespace.getHeight(), IsEqual.equalTo(2000L));
	}

	@Test
	public void getNamespaceRetrievesUpdatedHeightForRootNamespaceIfOwnerDoesNotChange() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		// originally "a" is is provisioned by account 1 at height 1, at height 5000 account 1 renews the provision of the namespace
		final DbNamespace dbNamespace = retriever.getNamespace(this.session, new NamespaceId("a"));

		// Assert:
		Assert.assertThat(dbNamespace.getOwner().getId(), IsEqual.equalTo(1L));
		Assert.assertThat(dbNamespace.getHeight(), IsEqual.equalTo(5000L));
	}

	@Test
	public void getNamespaceRetrievesUpdatedHeightForSubNamespaceIfOwnerDoesNotChange() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		// originally "a" is is provisioned by account 1 at height 1, at height 5000 account 1 renews the provision of the namespace
		final DbNamespace dbNamespace = retriever.getNamespace(this.session, new NamespaceId("a.b.c"));

		// Assert:
		Assert.assertThat(dbNamespace.getOwner().getId(), IsEqual.equalTo(1L));
		Assert.assertThat(dbNamespace.getHeight(), IsEqual.equalTo(5000L));
	}

	//endregion

	//region getRootNamespaces

	@Test
	public void getRootNamespacesRetrievesAllRootNamespaces() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		final Collection<String> dbFullNames = getNames(retriever.getRootNamespaces(this.session, Long.MAX_VALUE, 100));
		final Collection<String> expectedFullNames = Arrays.asList(
				"a",
				"aa",
				"aaa",
				"aaaa",
				"aaaaa",
				"aaaaaa",
				"aaaaaaa",
				"aaaaaaaa",
				"aaaaaaaaa",
				"aaaaaaaaaa");

		// Assert:
		Assert.assertThat(dbFullNames.size(), IsEqual.equalTo(10));
		Assert.assertThat(dbFullNames, IsEquivalent.equivalentTo(expectedFullNames));
	}

	@Test
	public void getRootNamespacesRetrievesAllRootNamespacesWhenPaging() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		final Collection<DbNamespace> dbNamespacesPage1 = retriever.getRootNamespaces(this.session, Long.MAX_VALUE, 4);
		final Collection<DbNamespace> dbNamespacesPage2 = retriever.getRootNamespaces(this.session, getLastId(dbNamespacesPage1), 4);
		final Collection<DbNamespace> dbNamespacesPage3 = retriever.getRootNamespaces(this.session, getLastId(dbNamespacesPage2), 4);
		final Collection<String> dbFullNamesPage1 = getNames(dbNamespacesPage1);
		final Collection<String> dbFullNamesPage2 = getNames(dbNamespacesPage2);
		final Collection<String> dbFullNamesPage3 = getNames(dbNamespacesPage3);

		// Assert:
		// - the first page should show "a" and "aaa" because they are the most recent
		// - the last page should not show "a" and "aaa" because they are not the most recent
		final Collection<String> expectedFullNamesPage1 = Arrays.asList("aaa", "a", "aaaaaaaaaa", "aaaaaaaaa");
		final Collection<String> expectedFullNamesPage2 = Arrays.asList("aaaaaaaa", "aaaaaaa", "aaaaaa", "aaaaa");
		final Collection<String> expectedFullNamesPage3 = Arrays.asList("aaaa", "aa");

		Assert.assertThat(dbFullNamesPage1, IsEquivalent.equivalentTo(expectedFullNamesPage1));
		Assert.assertThat(dbFullNamesPage2, IsEquivalent.equivalentTo(expectedFullNamesPage2));
		Assert.assertThat(dbFullNamesPage3, IsEquivalent.equivalentTo(expectedFullNamesPage3));
	}

	private static Long getLastId(final Collection<DbNamespace> dbNamespaces) {
		Long id = null;
		for (final DbNamespace dbNamespace : dbNamespaces) {
			id = dbNamespace.getId();
		}

		return id;
	}

	private static Collection<String> getNames(final Collection<DbNamespace> dbNamespaces) {
		return dbNamespaces.stream()
				.map(DbNamespace::getFullName)
				.collect(Collectors.toList());
	}

	@Test
	public void getRootNamespacesRetrievesUpdatedHeightForRootNamespaces() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		final Collection<DbNamespace> dbNamespaces = retriever.getRootNamespaces(this.session, Long.MAX_VALUE, 100);

		// originally "a" is is provisioned by account 1 at height 1, at height 5000 account 1 renews the provision of the namespace
		final DbNamespace dbNamespace = filter(dbNamespaces, "a");

		// Assert:
		Assert.assertThat(dbNamespace.getFullName(), IsEqual.equalTo("a"));
		Assert.assertThat(dbNamespace.getOwner().getId(), IsEqual.equalTo(1L));
		Assert.assertThat(dbNamespace.getHeight(), IsEqual.equalTo(5000L));
	}

	@Test
	public void getRootNamespacesRetrievesUpdatedOwnerAndHeightForRootNamespace() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		final Collection<DbNamespace> dbNamespaces = retriever.getRootNamespaces(this.session, Long.MAX_VALUE, 100);

		// originally "aaa" is owned by account 2 who provisioned at height 101, at height 2000 account 3 has provisioned the namespace
		final DbNamespace dbNamespace = filter(dbNamespaces, "aaa");

		// Assert:
		Assert.assertThat(dbNamespace.getOwner().getId(), IsEqual.equalTo(3L));
		Assert.assertThat(dbNamespace.getHeight(), IsEqual.equalTo(2000L));
	}

	private static DbNamespace filter(final Collection<DbNamespace> dbNamespaces, final String name) {
		return dbNamespaces.stream().filter(ns -> name.equals(ns.getFullName())).findFirst().get();
	}

	//endregion

	private void setupNamespaces() {
		// Adds the following namespaces to the namespace table:
		// a.b.c, a.b.cc, ..., a.b.cccccccccc
		// a.bb.c, ..., a.bb.cccccccccc
		// ...
		// a.bbbbbbbbbb.c, ..., a.bbbbbbbbbb.cccccccccc
		// aa.b.c, ..., aa.b.cccccccccc
		// ...
		// aaaaaaaaaa.bbbbbbbbbb.cccccccccc
		//
		// The 10 root namespaces (and all sub-namespaces) are owned by 5 accounts (account 1 owns a and aa, account 2 owns aaa and aaaa,...)
		// Expiry heights for the root namespaces are 1, 101, 201, ...
		// The namespace "a" has 2 entries, the first one has height 1, the second one height 5000
		// The namespace "aaa" has 2 entries, the first one has account 2 as owner and height 201, the second one account 3 as owner and height 2000
		// The namespace "aaa.b" has 2 entries, the first one has account 2 as owner and height 201, the second one account 3 as owner and height 2000
		final String[] levels = { "", "", "" };
		String fullName;
		String statement;
		long expiryHeight;
		for (int i = 0; i < 10; i++) {
			levels[0] += "a";
			levels[1] = "";
			levels[2] = "";
			fullName = levels[0];
			statement = createSQLStatement(fullName, i / 2 + 1, i * 100 + 1, 0);
			this.session.createSQLQuery(statement).executeUpdate();
			for (int j = 0; j < 10; j++) {
				levels[1] += "b";
				levels[2] = "";
				fullName = levels[0] + "." + levels[1];
				expiryHeight = i * 100 + j * 10;
				statement = createSQLStatement(fullName, i / 2 + 1, expiryHeight + 1, 1);
				this.session.createSQLQuery(statement).executeUpdate();
				for (int k = 0; k < 10; k++) {
					levels[2] += "c";
					fullName = levels[0] + "." + levels[1] + "." + levels[2];
					expiryHeight = i * 100 + j * 10 + k;
					statement = createSQLStatement(fullName, i / 2 + 1, expiryHeight + 1, 2);
					this.session.createSQLQuery(statement).executeUpdate();
				}
			}
		}

		// Arrange: in order for paging to work correctly, transactions must be added in temporal order
		// - add a "renewal" to "aaa" AFTER the original entry
		statement = createSQLStatement("aaa", 3, 2000, 0);
		this.session.createSQLQuery(statement).executeUpdate();
		statement = createSQLStatement("aaa.b", 3, 2000, 1);
		this.session.createSQLQuery(statement).executeUpdate();

		// - add a "renewal" to "a" BEFORE the original entry
		statement = createSQLStatement("a", 1, 5000, 0);
		this.session.createSQLQuery(statement).executeUpdate();
	}

	private static String createSQLStatement(final String fullName, final long ownerId, final long height, final int level) {
		return String.format("Insert into namespaces (fullName, ownerId, height, level) values('%s', %d, %d, %d)",
				fullName,
				ownerId,
				height,
				level);
	}

	private void createAccounts(final int count) {
		for (int i = 0; i < count; i++) {
			final PublicKey publicKey = new KeyPair().getPublicKey();
			final Address address = Address.fromPublicKey(publicKey);
			final String statement = String.format("Insert into accounts (printableKey, publicKey) values('%s', '%s')",
					address.toString(),
					publicKey.toString());
			this.session.createSQLQuery(statement).executeUpdate();
		}
	}
}
