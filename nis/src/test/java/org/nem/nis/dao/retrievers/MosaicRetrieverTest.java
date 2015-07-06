package org.nem.nis.dao.retrievers;

import org.hamcrest.core.IsEqual;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.DbMosaic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.stream.*;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class MosaicRetrieverTest {

	@Autowired
	SessionFactory sessionFactory;

	protected Session session;

	@Before
	public void createDb() {
		this.session = this.sessionFactory.openSession();
		this.createAccounts(3);
		this.setupMosaics();
	}

	@After
	public void destroyDb() {
		DbUtils.dbCleanup(this.session);
		this.session.close();
	}

	@Test
	public void canRetrieveAllMosaicsForAccount() {
		// Arrange:
		final MosaicRetriever retriever = new MosaicRetriever();

		// Act:
		final Collection<DbMosaic> dbMosaics = retriever.getMosaicsForAccount(this.session, 2L, null, Long.MAX_VALUE, 25);

		// Assert:
		Assert.assertThat(dbMosaics.size(), IsEqual.equalTo(3));
		Assert.assertThat(
				dbMosaics.stream().map(DbMosaic::getName).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(Arrays.asList("orange", "butter", "honey")));
	}

	@Test
	public void canRetrieveAllMosaicsForNamespace() {
		// Arrange:
		final MosaicRetriever retriever = new MosaicRetriever();

		// Act:
		final Collection<DbMosaic> dbMosaics = retriever.getMosaicsForNamespace(
				this.session,
				new NamespaceId("makoto.metals"),
				Long.MAX_VALUE, 25);

		// Assert:
		Assert.assertThat(dbMosaics.size(), IsEqual.equalTo(3));
		dbMosaics.forEach(m -> Assert.assertThat(m.getNamespaceId(), IsEqual.equalTo("makoto.metals")));
	}

	@Test
	public void canRetrieveAllMosaics() {
		// Arrange:
		final MosaicRetriever retriever = new MosaicRetriever();

		// Act:
		final Collection<DbMosaic> dbMosaics = retriever.getMosaics(this.session, Long.MAX_VALUE, 25);

		// Assert:
		Assert.assertThat(dbMosaics.size(), IsEqual.equalTo(9));
	}

	@Test
	public void canRetrieveLimitedNumberOfMosaics() {
		// Arrange:
		final MosaicRetriever retriever = new MosaicRetriever();

		// Act:
		final Collection<DbMosaic> dbMosaics = retriever.getMosaics(this.session, Long.MAX_VALUE, 5);

		// Assert:
		Assert.assertThat(dbMosaics.size(), IsEqual.equalTo(5));
	}

	@Test
	public void retrieverReturnsMosaicsOrderedDescendingById() {
		// Arrange:
		final MosaicRetriever retriever = new MosaicRetriever();

		// Act:
		final Collection<DbMosaic> dbMosaics = retriever.getMosaics(this.session, Long.MAX_VALUE, 9);
		long[] id = new long[1];
		id[0] = 9;

		// Assert:
		Assert.assertThat(dbMosaics.size(), IsEqual.equalTo(9));
		dbMosaics.forEach(m -> Assert.assertThat(m.getId(), IsEqual.equalTo(id[0]--)));
	}

	private void setupMosaics() {
		final String[] namespaceIds = { "makoto.metals", "alice.food", "bob.lectures" };
		final String[] names = { "silver", "gold", "platinum", "orange", "butter", "honey", "math", "physics", "biology" };
		final String[] descriptions = { "valuable", "very valuable", "highest value", "tasty", "high calories", "very sweet", "geometry", "mechanics", "mammals" };
		IntStream.range(0, 3).forEach(i -> {
			IntStream.range(0, 3).forEach(j -> {
				String statement = createMosaicSQLStatement(i + 1, names[3 * i + j], namespaceIds[i], descriptions[3 * i + j]);
				this.session.createSQLQuery(statement).executeUpdate();
				this.setupPropertiesForMosaic(i + 1);
			});
		});

	}

	private void setupPropertiesForMosaic(final long mosaicId) {
		final String[] names = { "divisibilty", "quantity", "mutablequantity", "transferable" };
		final String[] values = { "3", "1234", "true", "false" };
		IntStream.range(0, 4).forEach(i -> {
			final String statement = createMosaicPropertiesSQLStatement(mosaicId, names[i], values[i]);
			this.session.createSQLQuery(statement).executeUpdate();
		});
	}

	private static String createMosaicSQLStatement(final long creatorId, final String name, final String namespaceId, final String description) {
		return String.format("Insert into mosaics (creatorId, name, namespaceId, description) values(%d, '%s', '%s', '%s')",
				creatorId,
				name,
				namespaceId,
				description);
	}

	private static String createMosaicPropertiesSQLStatement(final long mosaicId, final String name, final String value) {
		return String.format("Insert into mosaicProperties (mosaicId, name, value) values(%d, '%s', '%s')",
				mosaicId,
				name,
				value);
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
