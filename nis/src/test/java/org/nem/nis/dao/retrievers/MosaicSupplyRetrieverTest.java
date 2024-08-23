package org.nem.nis.dao.retrievers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
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
import org.nem.nis.state.AccountState;
import org.nem.nis.test.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.*;

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

	private Transaction createCreationTransaction(final Account account, final String namespaceId, final String name, final String description, final Long supply) {
		final MosaicId mosaicId = Utils.createMosaicId(namespaceId, name);
		final MosaicDefinition mosaicDefinition = new MosaicDefinition(account, mosaicId, new MosaicDescriptor(description),
				Utils.createMosaicPropertiesWithInitialSupply(supply), null);
		final Transaction transaction = new MosaicDefinitionCreationTransaction(new TimeInstant(1), account, mosaicDefinition,
				Utils.generateRandomAccount(), Amount.fromNem(50_000));
		transaction.sign();
		return transaction;
	}

	private Transaction createSupplyChangeTransaction(final Account account, final String namespaceId, final String name, final MosaicSupplyType supplyType, final Long supply) {
		final MosaicId mosaicId = Utils.createMosaicId(namespaceId, name);
		final Transaction transaction = new MosaicSupplyChangeTransaction(new TimeInstant(1), account, mosaicId, supplyType, Supply.fromValue(supply));
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
		this.saveDbBlock(10L, this.createCreationTransaction(signer, "fox", "tokens", "cool fox tokens", 1000L)); // supply change (create)

		this.saveDbBlock(20L, this.createCreationTransaction(signer, "fox", "nuggets", "nice fox tokens", 2000L)); // other token
		this.saveDbBlock(25L, this.createSupplyChangeTransaction(signer, "fox", "nuggets", MosaicSupplyType.Create, 100L));

		this.saveDbBlock(30L, this.createCreationTransaction(signer, "bobcat", "tokens", "nice bobcat tokens", 3000L)); // other token
		this.saveDbBlock(35L, this.createSupplyChangeTransaction(signer, "bobcat", "tokens", MosaicSupplyType.Create, 200L));

		this.saveDbBlock(40L, this.createCreationTransaction(signer, "fox", "tokens", "cool fox tokens", 4000L)); // supply change (reset)
		this.saveDbBlock(45L, this.createSupplyChangeTransaction(signer, "fox", "tokens", MosaicSupplyType.Create, 900L));
		this.saveDbBlock(50L, this.createCreationTransaction(signer, "fox", "tokens", "cool fox tokens", 5000L)); // supply change (reset)
		this.saveDbBlock(55L, this.createSupplyChangeTransaction(signer, "fox", "tokens", MosaicSupplyType.Create, 800L));
		this.saveDbBlock(60L, this.createCreationTransaction(signer, "fox", "tokens", "coolest fox tokens", 5000L)); // description only change (reuse)
		this.saveDbBlock(65L, this.createSupplyChangeTransaction(signer, "fox", "tokens", MosaicSupplyType.Delete, 700L));
	}

	// endregion

	// region tests - getMosaicDefinitionWithSupply

	private Long getInitialBalancePropertyValue(final DbMosaicDefinition mosaicDefinition) {
		final DbMosaicProperty mosaicProperty = mosaicDefinition.getProperties().stream()
				.filter(property -> property.getName() == "initialSupply")
				.findFirst()
				.get();
		return Long.parseLong(mosaicProperty.getValue(), 10);
	}

	void assertFoxTokensSupplyAt(final Long height, final Long expectedInitialSupply, final Long expectedSupply) {
		// Arrange:
		final MosaicSupplyRetriever retriever = new MosaicSupplyRetriever();

		// Act:
		final DbMosaicDefinitionSupplyPair pair = retriever.getMosaicDefinitionWithSupply(this.session, Utils.createMosaicId("fox", "tokens"), height);

		// Assert:
		MatcherAssert.assertThat(pair, IsNot.not(IsEqual.equalTo(null)));
		MatcherAssert.assertThat(getInitialBalancePropertyValue(pair.getMosaicDefinition()), IsEqual.equalTo(expectedInitialSupply));
		MatcherAssert.assertThat(pair.getSupply(), IsEqual.equalTo(new Supply(expectedSupply)));
	}

	@Test
	public void getSupplyReturnsNullWhenNoMatchingMosaicDefinition() {
		// Arrange:
		final MosaicSupplyRetriever retriever = new MosaicSupplyRetriever();

		// Act:
		final DbMosaicDefinitionSupplyPair pair = retriever.getMosaicDefinitionWithSupply(this.session, Utils.createMosaicId("fox", "tokens"), 9L);

		// Assert:
		MatcherAssert.assertThat(pair, IsEqual.equalTo(null));
	}

	@Test
	public void getSupplyReturnsMostRecentSupplyWhenNoHeightConstraint() {
		this.assertFoxTokensSupplyAt(Long.MAX_VALUE, 5000L, 5100L);
	}

	@Test
	public void getSupplyReturnsHistoricalSupplyWhenHeightConstraint() {
		this.assertFoxTokensSupplyAt(10L, 1000L, 1000L); // creation = 1000
		this.assertFoxTokensSupplyAt(11L, 1000L, 1000L);
		this.assertFoxTokensSupplyAt(39L, 1000L, 1000L);

		this.assertFoxTokensSupplyAt(40L, 4000L, 4000L); // creation = 4000
		this.assertFoxTokensSupplyAt(41L, 4000L, 4000L);
		this.assertFoxTokensSupplyAt(44L, 4000L, 4000L);

		this.assertFoxTokensSupplyAt(45L, 4000L, 4900L); // supply += 900
		this.assertFoxTokensSupplyAt(46L, 4000L, 4900L);
		this.assertFoxTokensSupplyAt(49L, 4000L, 4900L);

		this.assertFoxTokensSupplyAt(50L, 5000L, 5000L); // creation = 5000
		this.assertFoxTokensSupplyAt(51L, 5000L, 5000L);
		this.assertFoxTokensSupplyAt(54L, 5000L, 5000L);

		this.assertFoxTokensSupplyAt(55L, 5000L, 5800L); // supply += 800
		this.assertFoxTokensSupplyAt(56L, 5000L, 5800L);
		this.assertFoxTokensSupplyAt(59L, 5000L, 5800L);
		this.assertFoxTokensSupplyAt(60L, 5000L, 5800L); // creation [ignore]
		this.assertFoxTokensSupplyAt(61L, 5000L, 5800L);
		this.assertFoxTokensSupplyAt(64L, 5000L, 5800L);

		this.assertFoxTokensSupplyAt(65L, 5000L, 5100L); // supply -= 700
		this.assertFoxTokensSupplyAt(66L, 5000L, 5100L);
	}

	// endregion
}
