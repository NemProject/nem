package org.nem.nis.dao;

import org.hamcrest.core.*;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.nis.dbmodel.DbBlock;
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
	AccountDao accountDao;

	@Autowired
	BlockDao blockDao;

	@Autowired
	SessionFactory sessionFactory;

	private Session session;

	@Before
	public void before() {
		this.session = this.sessionFactory.openSession();
	}

	@After
	public void after() {
		DbUtils.dbCleanup(this.session);
		this.session.close();
	}

	@Test
	public void getBlockByIdReturnsNullIfIdDoesNotExistInDatabase() {
		// Arrange:
		this.createAndSaveBlocks(10);

		// Act:
		final DbBlock dbBlock = this.createLoader().getBlockById(101L);

		// Assert:
		Assert.assertThat(dbBlock, IsNull.nullValue());
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
		Assert.assertThat(dbBlock.getId(), IsEqual.equalTo(original.getId()));
		Assert.assertThat(dbBlock.getHeight(), IsEqual.equalTo(original.getHeight()));
	}

	private BlockLoader createLoader() {
		return new BlockLoader(this.session);
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
}