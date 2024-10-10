package org.nem.nis.dao.retrievers;

import java.util.*;
import java.util.stream.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.model.*;
import org.nem.core.model.Transaction;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.test.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class MosaicSupplyRetrieverTest {
	// region auto wiring

	@Autowired
	private SessionFactory sessionFactory;

	private Session session;

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private MosaicIdCache mosaicIdCache;

	// endregion

	// region test setup

	@Before
	public void createDb() {
		this.session = this.sessionFactory.openSession();
		this.setupBlocks();
	}

	@After
	public void destroyDb() {
		if (null != this.session) {
			DbTestUtils.dbCleanup(this.session);
			this.session.close();
		}

		this.mosaicIdCache.clear();
		Utils.resetGlobals();
	}

	private Transaction createCreationTransaction(final Account account, final String namespaceId, final String name,
			final String description, final Long supply) {
		final MosaicId mosaicId = Utils.createMosaicId(namespaceId, name);
		final MosaicDefinition mosaicDefinition = new MosaicDefinition(account, mosaicId, new MosaicDescriptor(description),
				Utils.createMosaicPropertiesWithInitialSupply(supply), null);
		final Transaction transaction = new MosaicDefinitionCreationTransaction(new TimeInstant(1), account, mosaicDefinition,
				Utils.generateRandomAccount(), Amount.fromNem(50_000));
		transaction.sign();
		return transaction;
	}

	private Transaction createSupplyChangeTransaction(final Account account, final String namespaceId, final String name,
			final MosaicSupplyType supplyType, final Long supply) {
		final MosaicId mosaicId = Utils.createMosaicId(namespaceId, name);
		final Transaction transaction = new MosaicSupplyChangeTransaction(new TimeInstant(1), account, mosaicId, supplyType,
				Supply.fromValue(supply));
		transaction.sign();
		return transaction;
	}

	private Transaction createNamespaceTransaction(final Account account, final String namespaceId, final String parentNamespaceId) {
		final Transaction transaction = new ProvisionNamespaceTransaction(new TimeInstant(1), account, new NamespaceIdPart(namespaceId),
				null == parentNamespaceId ? null : new NamespaceId(parentNamespaceId));
		transaction.sign();
		return transaction;
	}

	private void saveDbBlock(final Long height, final Transaction transaction) {
		final List<Transaction> transactions = new ArrayList<>();
		transactions.add(transaction);
		this.saveDbBlock(height, transactions);
	}

	private void saveDbBlock(final Long height, final Collection<Transaction> transactions) {
		final Block block = NisUtils.createRandomBlockWithHeight(height);

		block.addTransactions(transactions);
		block.sign();

		final DbBlock dbBlock = MapperUtils.toDbModel(block, new AccountDaoLookupAdapter(this.accountDao), this.mosaicIdCache);
		this.blockDao.save(dbBlock);
	}

	private void setupBlocks() {
		final Account signer = Utils.generateRandomAccount();

		// root namespace should be used to calculate expiration time (until root namespace is recreated)
		// child namespace height should be ignored because it is tied to root namespace
		this.saveDbBlock(6L, this.createNamespaceTransaction(signer, "fox", null));
		this.saveDbBlock(8L, this.createNamespaceTransaction(signer, "fox", "kit"));

		this.saveDbBlock(10L, this.createCreationTransaction(signer, "fox.kit", "tokens", "cool", 1000L)); // supply change (create)

		this.saveDbBlock(20L, this.createCreationTransaction(signer, "fox.kit", "nuggets", "nice", 2000L)); // other token
		this.saveDbBlock(25L, this.createSupplyChangeTransaction(signer, "fox.kit", "nuggets", MosaicSupplyType.Create, 100L));

		this.saveDbBlock(26L, this.createNamespaceTransaction(signer, "fox", null)); // lifetime extended (within 30 blocks of previous)
		this.saveDbBlock(28L, this.createNamespaceTransaction(signer, "fox", "kit"));

		this.saveDbBlock(30L, this.createCreationTransaction(signer, "bobcat", "tokens", "nice bobcat tokens", 3000L)); // other token
		this.saveDbBlock(35L, this.createSupplyChangeTransaction(signer, "bobcat", "tokens", MosaicSupplyType.Create, 200L));

		// supply transactions are not allowed in same blocks as respective creation transactions
		// (last creation transaction in block is effective)
		final List<Transaction> block37Transactions = new ArrayList<>();
		block37Transactions.add(this.createCreationTransaction(signer, "fox.kit", "tokens", "cool", 6000L)); // supply change (reset)
		block37Transactions.add(this.createCreationTransaction(signer, "fox.kit", "tokens", "cool", 7000L)); // supply change (reset)
		this.saveDbBlock(37L, block37Transactions);

		this.saveDbBlock(40L, this.createCreationTransaction(signer, "fox.kit", "tokens", "cool", 4000L)); // supply change (reset)
		this.saveDbBlock(45L, this.createSupplyChangeTransaction(signer, "fox.kit", "tokens", MosaicSupplyType.Create, 900L));
		this.saveDbBlock(50L, this.createCreationTransaction(signer, "fox.kit", "tokens", "cool", 5000L)); // supply change (reset)

		this.saveDbBlock(52L, this.createNamespaceTransaction(signer, "fox", null)); // lifetime extended (within 30 blocks of previous)
		this.saveDbBlock(54L, this.createNamespaceTransaction(signer, "fox", "kit"));

		this.saveDbBlock(55L, this.createSupplyChangeTransaction(signer, "fox.kit", "tokens", MosaicSupplyType.Create, 800L));
		this.saveDbBlock(60L, this.createCreationTransaction(signer, "fox.kit", "tokens", "coolest", 5000L)); // desc only change (reuse)
		this.saveDbBlock(65L, this.createSupplyChangeTransaction(signer, "fox.kit", "tokens", MosaicSupplyType.Delete, 700L));

		this.saveDbBlock(140L, this.createNamespaceTransaction(signer, "fox", null)); // lifetime reset (outside 30 blocks of previous)
		this.saveDbBlock(145L, this.createNamespaceTransaction(signer, "fox", "kit"));

		this.saveDbBlock(150L, this.createCreationTransaction(signer, "fox.kit", "tokens", "coolest", 8000L)); // supply change (reset)
		this.saveDbBlock(151L, this.createSupplyChangeTransaction(signer, "fox.kit", "tokens", MosaicSupplyType.Create, 700L));
		this.saveDbBlock(152L, this.createSupplyChangeTransaction(signer, "fox.kit", "tokens", MosaicSupplyType.Delete, 500L));

		this.saveDbBlock(160L, this.createNamespaceTransaction(signer, "fox", null)); // lifetime extended (within 30 blocks of previous)
		this.saveDbBlock(165L, this.createNamespaceTransaction(signer, "fox", "kit"));
	}

	// endregion

	// region tests - getMosaicDefinitionWithSupply

	private Long getInitialBalancePropertyValue(final DbMosaicDefinition mosaicDefinition) {
		final DbMosaicProperty mosaicProperty = mosaicDefinition.getProperties().stream()
				.filter(property -> property.getName() == "initialSupply").findFirst().get();
		return Long.parseLong(mosaicProperty.getValue(), 10);
	}

	void assertFoxTokensSupplyAt(final Long height, final Long expectedInitialSupply, final Long expectedSupply,
			final Long expectedExpirationHeight) {
		// Arrange:
		final MosaicSupplyRetriever retriever = new MosaicSupplyRetriever(30);

		// Act:
		final DbMosaicDefinitionSupplyTuple tuple = retriever.getMosaicDefinitionWithSupply(this.session,
				Utils.createMosaicId("fox.kit", "tokens"), height);

		// Assert:
		MatcherAssert.assertThat(tuple, IsNot.not(IsEqual.equalTo(null)));
		MatcherAssert.assertThat(getInitialBalancePropertyValue(tuple.getMosaicDefinition()), IsEqual.equalTo(expectedInitialSupply));
		MatcherAssert.assertThat(tuple.getSupply(), IsEqual.equalTo(new Supply(expectedSupply)));
		MatcherAssert.assertThat(tuple.getExpirationHeight(), IsEqual.equalTo(new BlockHeight(expectedExpirationHeight)));
	}

	@Test
	public void getSupplyReturnsNullWhenNoMatchingMosaicDefinition() {
		// Arrange:
		final MosaicSupplyRetriever retriever = new MosaicSupplyRetriever(30);

		// Act:
		final DbMosaicDefinitionSupplyTuple tuple = retriever.getMosaicDefinitionWithSupply(this.session,
				Utils.createMosaicId("fox.kit", "tokens"), 9L);

		// Assert:
		MatcherAssert.assertThat(tuple, IsEqual.equalTo(null));
	}

	@Test
	public void getSupplyReturnsMostRecentSupplyWhenNoHeightConstraint() {
		this.assertFoxTokensSupplyAt(Long.MAX_VALUE, 8000L, 8200L, 190L);
	}

	@Test
	public void getSupplyReturnsHistoricalSupplyWhenHeightConstraint() {
		this.assertFoxTokensSupplyAt(10L, 1000L, 1000L, 82L); // creation = 1000
		this.assertFoxTokensSupplyAt(11L, 1000L, 1000L, 82L);
		this.assertFoxTokensSupplyAt(36L, 1000L, 1000L, 82L);

		this.assertFoxTokensSupplyAt(37L, 7000L, 7000L, 82L); // creation = 7000 ([creation = 6000] preempted)
		this.assertFoxTokensSupplyAt(38L, 7000L, 7000L, 82L);
		this.assertFoxTokensSupplyAt(39L, 7000L, 7000L, 82L);

		this.assertFoxTokensSupplyAt(40L, 4000L, 4000L, 82L); // creation = 4000
		this.assertFoxTokensSupplyAt(41L, 4000L, 4000L, 82L);
		this.assertFoxTokensSupplyAt(44L, 4000L, 4000L, 82L);

		this.assertFoxTokensSupplyAt(45L, 4000L, 4900L, 82L); // supply += 900
		this.assertFoxTokensSupplyAt(46L, 4000L, 4900L, 82L);
		this.assertFoxTokensSupplyAt(49L, 4000L, 4900L, 82L);

		this.assertFoxTokensSupplyAt(50L, 5000L, 5000L, 82L); // creation = 5000
		this.assertFoxTokensSupplyAt(51L, 5000L, 5000L, 82L);
		this.assertFoxTokensSupplyAt(54L, 5000L, 5000L, 82L);

		this.assertFoxTokensSupplyAt(55L, 5000L, 5800L, 82L); // supply += 800
		this.assertFoxTokensSupplyAt(56L, 5000L, 5800L, 82L);
		this.assertFoxTokensSupplyAt(59L, 5000L, 5800L, 82L);
		this.assertFoxTokensSupplyAt(60L, 5000L, 5800L, 82L); // creation [ignore]
		this.assertFoxTokensSupplyAt(61L, 5000L, 5800L, 82L);
		this.assertFoxTokensSupplyAt(64L, 5000L, 5800L, 82L);

		this.assertFoxTokensSupplyAt(65L, 5000L, 5100L, 82L); // supply -= 700
		this.assertFoxTokensSupplyAt(66L, 5000L, 5100L, 82L);

		this.assertFoxTokensSupplyAt(150L, 8000L, 8000L, 190L); // creation = 8000
		this.assertFoxTokensSupplyAt(151L, 8000L, 8700L, 190L); // supply += 700
		this.assertFoxTokensSupplyAt(152L, 8000L, 8200L, 190L); // supply -= 500
		this.assertFoxTokensSupplyAt(153L, 8000L, 8200L, 190L);
	}

	// endregion
}
