package org.nem.nis.controller.utils;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.Block;
import org.nem.nis.test.*;

import java.util.*;

public class RequiredBlockDaoAdapterTest {

	@Test
	public void findByIdDelegatesToBlockDao() {
		// Arrange:
		final Block originalBlock = new Block();
		final MockBlockDao blockDao = new MockBlockDao(originalBlock);
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);

		// Act:
		final Block block = requiredBlockDao.findById(56);

		// Assert:
		Assert.assertThat(block, IsSame.sameInstance(originalBlock));
		Assert.assertThat(blockDao.getNumFindByIdCalls(), IsEqual.equalTo(1));
		Assert.assertThat(blockDao.getLastFindByIdId(), IsEqual.equalTo(56L));
	}

	@Test(expected = MissingResourceException.class)
	public void findByIdThrowsExceptionIfBlockCannotBeFound() {
		// Arrange:
		final MockBlockDao blockDao = new MockBlockDao(null);
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);

		// Act:
		requiredBlockDao.findById(56);
	}

	@Test
	public void findByHashDelegatesToBlockDao() {
		// Arrange:
		final Block originalBlock = new Block();
		final Hash hash = new Hash(Utils.generateRandomBytes(64));
		final MockBlockDao blockDao = new MockBlockDao(originalBlock);
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);

		// Act:
		final Block block = requiredBlockDao.findByHash(hash);

		// Assert:
		Assert.assertThat(block, IsSame.sameInstance(originalBlock));
		Assert.assertThat(blockDao.getNumFindByHashCalls(), IsEqual.equalTo(1));
		Assert.assertThat(blockDao.getLastFindByHashHash(), IsEqual.equalTo(hash));
	}

	@Test(expected = MissingResourceException.class)
	public void findByHashThrowsExceptionIfBlockCannotBeFound() {
		// Arrange:
		final Hash hash = new Hash(Utils.generateRandomBytes(64));
		final MockBlockDao blockDao = new MockBlockDao(null);
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);

		// Act:
		requiredBlockDao.findByHash(hash);
	}

	@Test
	public void findByHeightDelegatesToBlockDao() {
		// Arrange:
		final Block originalBlock = new Block();
		final MockBlockDao blockDao = new MockBlockDao(originalBlock);
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);

		// Act:
		final Block block = requiredBlockDao.findByHeight(new BlockHeight(124));

		// Assert:
		Assert.assertThat(block, IsSame.sameInstance(originalBlock));
		Assert.assertThat(blockDao.getNumFindByHeightCalls(), IsEqual.equalTo(1));
		Assert.assertThat(blockDao.getLastFindByHeightHeight(), IsEqual.equalTo(new BlockHeight(124)));
	}

	@Test(expected = MissingResourceException.class)
	public void findByHeightThrowsExceptionIfBlockCannotBeFound() {
		// Arrange:
		final MockBlockDao blockDao = new MockBlockDao(null);
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);

		// Act:
		requiredBlockDao.findByHeight(new BlockHeight(124));
	}

	@Test
	public void getHashesFromDelegatesToBlockDao() {
		// Arrange:
		final HashChain originalHashes = new HashChain(NisUtils.createRawHashesList(3));
		final MockBlockDao blockDao = new MockBlockDao(new Block(), originalHashes);
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);

		// Act:
		final HashChain hashes = requiredBlockDao.getHashesFrom(new BlockHeight(11), 14);

		// Assert:
		Assert.assertThat(hashes.findFirstDifferent(originalHashes), IsEqual.equalTo(3));
		Assert.assertThat(blockDao.getNumGetHashesFromCalls(), IsEqual.equalTo(1));
		Assert.assertThat(blockDao.getLastGetHashesFromHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(blockDao.getLastGetHashesFromLimit(), IsEqual.equalTo(14));
	}
}
