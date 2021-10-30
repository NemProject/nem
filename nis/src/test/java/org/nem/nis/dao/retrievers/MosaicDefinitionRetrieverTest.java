package org.nem.nis.dao.retrievers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.*;
import org.nem.nis.dao.TestConf;
import org.nem.nis.dbmodel.DbMosaicDefinition;
import org.nem.nis.test.DbTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.stream.*;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class MosaicDefinitionRetrieverTest {

	@Autowired
	private SessionFactory sessionFactory;

	private Session session;

	@Before
	public void createDb() {
		this.session = this.sessionFactory.openSession();
		this.createAccounts(3);
		this.setupMosaicDefinitions();
	}

	@After
	public void destroyDb() {
		DbTestUtils.dbCleanup(this.session);
		this.session.close();
	}

	// region getMosaicDefinition

	@Test
	public void canRetrieveMosaicDefinitionForExistingMosaicId() {
		// Arrange:
		final MosaicDefinitionRetriever retriever = new MosaicDefinitionRetriever();
		final NamespaceId namespaceId = new NamespaceId("alice.drinks");
		final MosaicId mosaicId = new MosaicId(namespaceId, "cola");

		// Act:
		final DbMosaicDefinition dbMosaicDefinition = retriever.getMosaicDefinition(this.session, mosaicId);

		// Assert:
		MatcherAssert.assertThat(dbMosaicDefinition, IsNull.notNullValue());
		MatcherAssert.assertThat(dbMosaicDefinition.getDescription(), IsEqual.equalTo("sugary"));
	}

	@Test
	public void canRetrieveNullMosaicDefinitionForNonExistingMosaicId() {
		// Arrange:
		final MosaicDefinitionRetriever retriever = new MosaicDefinitionRetriever();
		final NamespaceId namespaceId = new NamespaceId("alice.drinks");
		final MosaicId mosaicId = new MosaicId(namespaceId, "tequila");

		// Act:
		final DbMosaicDefinition dbMosaicDefinition = retriever.getMosaicDefinition(this.session, mosaicId);

		// Assert:
		MatcherAssert.assertThat(dbMosaicDefinition, IsNull.nullValue());
	}

	@Test
	public void cannotRetrieveMosaicDefinitionForNullMosaicId() {
		// Arrange:
		final MosaicDefinitionRetriever retriever = new MosaicDefinitionRetriever();

		// Act:
		ExceptionAssert.assertThrows(v -> retriever.getMosaicDefinition(this.session, null), IllegalArgumentException.class);
	}

	// endregion

	// region getMosaicDefinitionsForAccount

	@Test
	public void canRetrieveAllMosaicDefinitionsForAccount() {
		// Arrange:
		final MosaicDefinitionRetriever retriever = new MosaicDefinitionRetriever();

		// Act:
		final Collection<String> names = retriever.getMosaicDefinitionsForAccount(this.session, 2L, null, Long.MAX_VALUE, 25).stream()
				.map(DbMosaicDefinition::getName).collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(names.size(), IsEqual.equalTo(5));
		MatcherAssert.assertThat(names, IsEquivalent.equivalentTo("orange", "butter", "honey", "cola", "beer"));
	}

	@Test
	public void canRetrieveAllMosaicDefinitionsForAccountAndNamespace() {
		// Arrange:
		final MosaicDefinitionRetriever retriever = new MosaicDefinitionRetriever();

		// Act:
		final Collection<String> names = retriever
				.getMosaicDefinitionsForAccount(this.session, 2L, new NamespaceId("alice.drinks"), Long.MAX_VALUE, 25).stream()
				.map(DbMosaicDefinition::getName).collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(names.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(names, IsEquivalent.equivalentTo("cola", "beer"));
	}

	@Test
	public void cannotRetrieveAllMosaicDefinitionsForNullAccount() {
		// Arrange:
		final MosaicDefinitionRetriever retriever = new MosaicDefinitionRetriever();

		// Act:
		ExceptionAssert.assertThrows(v -> retriever.getMosaicDefinitionsForAccount(this.session, null, null, Long.MAX_VALUE, 25),
				IllegalArgumentException.class);
	}

	// endregion

	// region getMosaicDefinitionsForNamespace

	@Test
	public void canRetrieveAllMosaicDefinitionsForNamespace() {
		// Arrange:
		final MosaicDefinitionRetriever retriever = new MosaicDefinitionRetriever();

		// Act:
		final Collection<DbMosaicDefinition> dbMosaicDefinitions = retriever.getMosaicDefinitionsForNamespace(this.session,
				new NamespaceId("makoto.metals"), Long.MAX_VALUE, 25);
		final Collection<String> names = dbMosaicDefinitions.stream().map(DbMosaicDefinition::getName).collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(names.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(names, IsEquivalent.equivalentTo("silver", "gold", "platinum"));
	}

	@Test
	public void cannotRetrieveAllMosaicDefinitionsForNullNamespace() {
		// Arrange:
		final MosaicDefinitionRetriever retriever = new MosaicDefinitionRetriever();

		// Act:
		ExceptionAssert.assertThrows(v -> retriever.getMosaicDefinitionsForNamespace(this.session, null, Long.MAX_VALUE, 25),
				IllegalArgumentException.class);
	}

	// endregion

	// region getMosaicDefinitions

	@Test
	public void canRetrieveAllMosaicDefinitionsOrderedDescendingById() {
		// Arrange:
		final MosaicDefinitionRetriever retriever = new MosaicDefinitionRetriever();

		// Act:
		final Collection<DbMosaicDefinition> dbMosaicDefinitions = retriever.getMosaicDefinitions(this.session, Long.MAX_VALUE, 25);
		final Collection<Long> ids = dbMosaicDefinitions.stream().map(DbMosaicDefinition::getId).collect(Collectors.toList());

		// Assert (database id 11 points to the old mosaic with description "alcoholic"):
		MatcherAssert.assertThat(dbMosaicDefinitions.size(), IsEqual.equalTo(11));
		MatcherAssert.assertThat(ids, IsEqual.equalTo(Arrays.asList(12L, 10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L)));
	}

	@Test
	public void canRetrieveLimitedNumberOfMosaicDefinitions() {
		// Arrange:
		final MosaicDefinitionRetriever retriever = new MosaicDefinitionRetriever();

		// Act:
		final Collection<Long> ids = retriever.getMosaicDefinitions(this.session, Long.MAX_VALUE, 5).stream().map(DbMosaicDefinition::getId)
				.collect(Collectors.toList());

		// Assert (database id 11 points to the old mosaic with description "alcoholic"):
		MatcherAssert.assertThat(ids.size(), IsEqual.equalTo(5));
		MatcherAssert.assertThat(ids, IsEqual.equalTo(Arrays.asList(12L, 10L, 9L, 8L, 7L)));
	}

	@Test
	public void canRetrievePageOfMosaicDefinitions() {
		// Arrange:
		final MosaicDefinitionRetriever retriever = new MosaicDefinitionRetriever();

		// Act:
		final Collection<Long> ids = retriever.getMosaicDefinitions(this.session, 7, 4).stream().map(DbMosaicDefinition::getId)
				.collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(ids.size(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(ids, IsEqual.equalTo(Arrays.asList(6L, 5L, 4L, 3L)));
	}

	// endregion

	private void setupMosaicDefinitions() {
		final String[] namespaceIds = {
				"makoto.metals", "alice.food", "bob.lectures"
		};
		final String[] names = {
				"silver", "gold", "platinum", "orange", "butter", "honey", "math", "physics", "biology"
		};
		final String[] descriptions = {
				"valuable", "very valuable", "highest value", "tasty", "high calories", "very sweet", "geometry", "mechanics", "mammals"
		};
		IntStream.range(0, 3).forEach(i -> IntStream.range(0, 3)
				.forEach(j -> this.addMosaicDefinitionToSession(i + 1, names[3 * i + j], namespaceIds[i], descriptions[3 * i + j])));

		this.addMosaicDefinitionToSession(2, "cola", "alice.drinks", "sugary");
		this.addMosaicDefinitionToSession(2, "beer", "alice.drinks", "alcoholic");
		this.addMosaicDefinitionToSession(2, "beer", "alice.drinks", "low alcohol"); // takes precedence over the previous
	}

	private void addMosaicDefinitionToSession(final long creatorId, final String name, final String namespaceId, final String description) {
		final String statement = createMosaicSQLStatement(creatorId, name, namespaceId, description);
		this.session.createSQLQuery(statement).executeUpdate();
		this.setupPropertiesForMosaicDefinition(creatorId);
	}

	private void setupPropertiesForMosaicDefinition(final long mosaicId) {
		final String[] names = {
				"divisibility", "quantity", "mutablequantity", "transferable"
		};
		final String[] values = {
				"3", "1234", "true", "false"
		};
		IntStream.range(0, 4).forEach(i -> {
			final String statement = createMosaicPropertiesSQLStatement(mosaicId, names[i], values[i]);
			this.session.createSQLQuery(statement).executeUpdate();
		});
	}

	private static String createMosaicSQLStatement(final long creatorId, final String name, final String namespaceId,
			final String description) {
		return String.format("Insert into mosaicdefinitions (creatorId, name, namespaceId, description) values(%d, '%s', '%s', '%s')",
				creatorId, name, namespaceId, description);
	}

	private static String createMosaicPropertiesSQLStatement(final long mosaicId, final String name, final String value) {
		return String.format("Insert into mosaicProperties (mosaicDefinitionId, name, value) values(%d, '%s', '%s')", mosaicId, name,
				value);
	}

	private void createAccounts(final int count) {
		for (int i = 0; i < count; i++) {
			final Address address = Utils.generateRandomAddressWithPublicKey();
			final String statement = String.format("Insert into accounts (printableKey, publicKey) values('%s', '%s')", address.toString(),
					address.getPublicKey().toString());
			this.session.createSQLQuery(statement).executeUpdate();
		}
	}
}
