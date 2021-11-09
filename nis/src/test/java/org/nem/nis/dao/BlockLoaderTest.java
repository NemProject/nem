package org.nem.nis.dao;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.model.Transaction;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.RandomTransactionFactory;
import org.nem.nis.cache.MosaicIdCache;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.test.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.stream.IntStream;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class BlockLoaderTest {
	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private MosaicIdCache mosaicIdCache;

	@Autowired
	private SessionFactory sessionFactory;

	private Session session;

	@Before
	public void before() {
		this.session = this.sessionFactory.openSession();
	}

	@After
	public void after() {
		DbTestUtils.dbCleanup(this.session);
		this.mosaicIdCache.clear();
		this.session.close();
	}

	@Test
	public void getBlockByIdReturnsNullIfIdDoesNotExistInDatabase() {
		// Arrange:
		this.createAndSaveBlocks(10);

		// Act:
		final DbBlock dbBlock = this.createLoader().getBlockById(101L);

		// Assert:
		MatcherAssert.assertThat(dbBlock, IsNull.nullValue());
	}

	@Test
	public void getBlockByIdReturnsExpectedBlockIfIdExistsInDatabase() {
		// Arrange:
		final List<DbBlock> dbBlocks = this.createAndSaveBlocks(10);

		// Act + Assert:
		IntStream.range(0, dbBlocks.size()).forEach(i -> this.assertDbBlock(dbBlocks.get(i)));
	}

	private void assertDbBlock(final DbBlock original) {
		// Act:
		final DbBlock dbBlock = this.createLoader().getBlockById(original.getId());

		// Assert:
		MatcherAssert.assertThat(dbBlock.getId(), IsEqual.equalTo(original.getId()));
		MatcherAssert.assertThat(dbBlock.getHeight(), IsEqual.equalTo(original.getHeight()));
	}

	@Test
	public void loadBlocksAssuresProvisionNamespaceTransactionsWithSameSenderAndNamespaceOwner() {
		// Act:
		final DbProvisionNamespaceTransaction t = this.createRoundTrippedDbProvisionNamespaceTransaction();

		// Assert:
		MatcherAssert.assertThat(t.getSender(), IsSame.sameInstance(t.getNamespace().getOwner()));
	}

	@Test
	public void loadBlocksAssuresProvisionNamespaceTransactionsWithCorrectNamespaceHeight() {
		// Act:
		final DbProvisionNamespaceTransaction t = this.createRoundTrippedDbProvisionNamespaceTransaction();

		// Assert:
		MatcherAssert.assertThat(t.getNamespace().getHeight(), IsEqual.equalTo(123L));
	}

	@Test
	public void loadBlocksAssuresMosaicDefinitionCreationTransactionsWithSameSenderAndMosaicCreator() {
		// Act:
		final DbMosaicDefinitionCreationTransaction t = this.createRoundTrippedDbMosaicDefinitionCreationTransaction();

		// Assert:
		MatcherAssert.assertThat(t.getSender(), IsSame.sameInstance(t.getMosaicDefinition().getCreator()));
	}

	private BlockLoader createLoader() {
		return new BlockLoader(this.session);
	}

	private DbProvisionNamespaceTransaction createRoundTrippedDbProvisionNamespaceTransaction() {
		// Act:
		final DbBlock dbBlock = this
				.createAndSaveAndReloadBlockWithTransaction(RandomTransactionFactory.createProvisionNamespaceTransaction());
		return dbBlock.getBlockProvisionNamespaceTransactions().get(0);
	}

	private DbMosaicDefinitionCreationTransaction createRoundTrippedDbMosaicDefinitionCreationTransaction() {
		// Act:
		final DbBlock dbBlock = this
				.createAndSaveAndReloadBlockWithTransaction(RandomTransactionFactory.createMosaicDefinitionCreationTransaction());
		return dbBlock.getBlockMosaicDefinitionCreationTransactions().get(0);
	}

	private List<DbBlock> createAndSaveBlocks(final int count) {
		final List<DbBlock> dbBlocks = new ArrayList<>();
		IntStream.range(0, count).forEach(i -> {
			final org.nem.core.model.Block block = NisUtils.createRandomBlockWithHeight(i + 1);
			block.sign();
			final DbBlock dbBlock = MapperUtils.toDbModel(block, new AccountDaoLookupAdapter(this.accountDao));
			this.blockDao.save(dbBlock);
			dbBlocks.add(dbBlock);
		});
		return dbBlocks;
	}

	private void createAndSaveBlockWithTransaction(final Transaction t) {
		final org.nem.core.model.Block block = NisUtils.createRandomBlockWithHeight(123);
		t.sign();
		block.addTransaction(t);
		block.sign();
		final DbBlock dbBlock = MapperUtils.toDbModel(block, new AccountDaoLookupAdapter(this.accountDao));
		this.blockDao.save(dbBlock);
	}

	private DbBlock createAndSaveAndReloadBlockWithTransaction(final Transaction t) {
		this.createAndSaveBlockWithTransaction(t);

		// Act:
		final BlockHeight height = new BlockHeight(123);
		return this.createLoader().loadBlocks(height, height).get(0);
	}
}
