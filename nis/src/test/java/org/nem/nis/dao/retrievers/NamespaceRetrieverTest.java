package org.nem.nis.dao.retrievers;

import org.hamcrest.core.IsEqual;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.model.namespace.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.DbNamespace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;

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

	@Test
	public void getNamespacesForAccountCanRetrieveAllNamespacesForSpecificAccount() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		final Collection<DbNamespace> dbNamespaces = retriever.getNamespacesForAccount(this.session, 1, null, 300);

		// Assert:
		Assert.assertThat(dbNamespaces.size(), IsEqual.equalTo(200));
	}

	@Test
	public void getNamespacesForAccountCanRetrieveAllNamespacesForSpecificAccountAndParent() {
		// Arrange:
		final NamespaceRetriever retriever = new NamespaceRetriever();

		// Act:
		final Collection<DbNamespace> dbNamespaces = retriever.getNamespacesForAccount(this.session, 1, new NamespaceId("aa"), 300);

		// Assert:
		Assert.assertThat(dbNamespaces.size(), IsEqual.equalTo(100));
		dbNamespaces.stream().forEach(n -> Assert.assertThat(new NamespaceId(n.getFullName()).getRoot(), IsEqual.equalTo(new NamespaceId("aa"))));
	}

	private void setupNamespaces() {
		String[] levels = { "", "", "" };
		for (int i = 0; i < 10; i++) {
			levels[0] += "a";
			levels[1] = "";
			for (int j = 0; j < 10; j++) {
				levels[1] += "b";
				levels[2] = "";
				for (int k = 0; k < 10; k++) {
					levels[2] += "c";
					final String fullName = levels[0] + "." + levels[1] + "." + levels[2];
					final long expiryHeight = i * 100 + j * 10 + k;
					final String statement = createSQLStatement(fullName, i / 2 + 1, expiryHeight + 1);
					this.session.createSQLQuery(statement).executeUpdate();
				}
			}
		}
	}

	private String createSQLStatement(final String fullName, final long ownerId, final long expiryHeight) {
		return String.format("Insert into namespaces (fullName, ownerId, expiryHeight) values('%s', %d, %d)",
				fullName,
				ownerId,
				expiryHeight);
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
