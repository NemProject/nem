package org.nem.nis.dao.retrievers;

import org.hamcrest.core.IsEqual;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.model.Address;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.DbMosaic;
import org.nem.nis.test.DbTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.stream.*;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class MosaicRetrieverTest {

	@Autowired
	private SessionFactory sessionFactory;

	private Session session;

	@Before
	public void createDb() {
		this.session = this.sessionFactory.openSession();
		this.createAccounts(3);
		this.setupMosaics();
	}

	@After
	public void destroyDb() {
		DbTestUtils.dbCleanup(this.session);
		this.session.close();
	}

	//region getMosaicsForAccount

	@Test
	public void canRetrieveAllMosaicsForAccount() {
		// Arrange:
		final MosaicRetriever retriever = new MosaicRetriever();

		// Act:
		final Collection<String> names = retriever.getMosaicsForAccount(this.session, 2L, null, Long.MAX_VALUE, 25).stream()
				.map(DbMosaic::getName)
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(names.size(), IsEqual.equalTo(5));
		Assert.assertThat(
				names,
				IsEquivalent.equivalentTo("orange", "butter", "honey", "cola", "beer"));
	}

	@Test
	public void canRetrieveAllMosaicsForAccountAndNamespace() {
		// Arrange:
		final MosaicRetriever retriever = new MosaicRetriever();

		// Act:
		final Collection<String> names = retriever.getMosaicsForAccount(this.session, 2L, new NamespaceId("alice.drinks"), Long.MAX_VALUE, 25).stream()
				.map(DbMosaic::getName)
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(names.size(), IsEqual.equalTo(2));
		Assert.assertThat(
				names,
				IsEquivalent.equivalentTo("cola", "beer"));
	}

	@Test
	public void cannotRetrieveAllMosaicsForNullAccount() {
		// Arrange:
		final MosaicRetriever retriever = new MosaicRetriever();

		// Act:
		ExceptionAssert.assertThrows(
				v -> retriever.getMosaicsForAccount(this.session, null, null, Long.MAX_VALUE, 25),
				IllegalArgumentException.class);
	}

	//endregion

	//region getMosaicsForNamespace

	@Test
	public void canRetrieveAllMosaicsForNamespace() {
		// Arrange:
		final MosaicRetriever retriever = new MosaicRetriever();

		// Act:
		final Collection<DbMosaic> dbMosaics = retriever.getMosaicsForNamespace(
				this.session,
				new NamespaceId("makoto.metals"),
				Long.MAX_VALUE,
				25);
		final Collection<String> names = dbMosaics.stream().map(DbMosaic::getName).collect(Collectors.toList());

		// Assert:
		Assert.assertThat(names.size(), IsEqual.equalTo(3));
		Assert.assertThat(
				names,
				IsEquivalent.equivalentTo("silver", "gold", "platinum"));
	}

	@Test
	public void cannotRetrieveAllMosaicsForNullNamespace() {
		// Arrange:
		final MosaicRetriever retriever = new MosaicRetriever();

		// Act:
		ExceptionAssert.assertThrows(
				v -> retriever.getMosaicsForNamespace(this.session, null, Long.MAX_VALUE, 25),
				IllegalArgumentException.class);
	}

	//endregion

	//region getMosaics

	@Test
	public void canRetrieveAllMosaicsOrderedDescendingById() {
		// Arrange:
		final MosaicRetriever retriever = new MosaicRetriever();

		// Act:
		final Collection<DbMosaic> dbMosaics = retriever.getMosaics(this.session, Long.MAX_VALUE, 25);
		final Collection<Long> ids = dbMosaics.stream().map(DbMosaic::getId).collect(Collectors.toList());

		// Assert:
		Assert.assertThat(dbMosaics.size(), IsEqual.equalTo(11));
		Assert.assertThat(ids, IsEqual.equalTo(Arrays.asList(11L, 10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L)));
	}

	@Test
	public void canRetrieveLimitedNumberOfMosaics() {
		// Arrange:
		final MosaicRetriever retriever = new MosaicRetriever();

		// Act:
		final Collection<Long> ids = retriever.getMosaics(this.session, Long.MAX_VALUE, 5).stream()
				.map(DbMosaic::getId)
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(ids.size(), IsEqual.equalTo(5));
		Assert.assertThat(ids, IsEqual.equalTo(Arrays.asList(11L, 10L, 9L, 8L, 7L)));
	}

	@Test
	public void canRetrievePageOfMosaics() {
		// Arrange:
		final MosaicRetriever retriever = new MosaicRetriever();

		// Act:
		final Collection<Long> ids = retriever.getMosaics(this.session, 7, 4).stream()
				.map(DbMosaic::getId)
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(ids.size(), IsEqual.equalTo(4));
		Assert.assertThat(ids, IsEqual.equalTo(Arrays.asList(7L, 6L, 5L, 4L)));
	}

	//endregion

	private void setupMosaics() {
		final String[] namespaceIds = { "makoto.metals", "alice.food", "bob.lectures" };
		final String[] names = { "silver", "gold", "platinum", "orange", "butter", "honey", "math", "physics", "biology" };
		final String[] descriptions = { "valuable", "very valuable", "highest value", "tasty", "high calories", "very sweet", "geometry", "mechanics", "mammals" };
		IntStream.range(0, 3).forEach(i ->
				IntStream.range(0, 3).forEach(j ->
						this.addMosaicToSession(i + 1, names[3 * i + j], namespaceIds[i], descriptions[3 * i + j])));

		this.addMosaicToSession(2, "cola", "alice.drinks", "sugary");
		this.addMosaicToSession(2, "beer", "alice.drinks", "alcoholic");
	}

	private void addMosaicToSession(final long creatorId, final String name, final String namespaceId, final String description) {
		final String statement = createMosaicSQLStatement(creatorId, name, namespaceId, description);
		this.session.createSQLQuery(statement).executeUpdate();
		this.setupPropertiesForMosaic(creatorId);
	}

	private void setupPropertiesForMosaic(final long mosaicId) {
		final String[] names = { "divisibility", "quantity", "mutablequantity", "transferable" };
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
			final Address address = Utils.generateRandomAddressWithPublicKey();
			final String statement = String.format("Insert into accounts (printableKey, publicKey) values('%s', '%s')",
					address.toString(),
					address.getPublicKey().toString());
			this.session.createSQLQuery(statement).executeUpdate();
		}
	}
}
