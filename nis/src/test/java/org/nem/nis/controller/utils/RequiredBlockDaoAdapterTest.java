package org.nem.nis.controller.utils;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsSame;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.Hash;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.Block;
import org.nem.nis.test.MockBlockDao;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

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
		final Block block = requiredBlockDao.findByHeight(124);

		// Assert:
		Assert.assertThat(block, IsSame.sameInstance(originalBlock));
		Assert.assertThat(blockDao.getNumFindByHeightCalls(), IsEqual.equalTo(1));
		Assert.assertThat(blockDao.getLastFindByHeightHeight(), IsEqual.equalTo(124L));
	}

	@Test(expected = MissingResourceException.class)
	public void findByHeightThrowsExceptionIfBlockCannotBeFound() {
		// Arrange:
		final MockBlockDao blockDao = new MockBlockDao(null);
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);

		// Act:
		requiredBlockDao.findByHeight(124);
	}

	@Test
	public void getHashesFromDelegatesToBlockDao() {
		// Arrange:
		final List<byte[]> originalHashes = new ArrayList<>();
		final MockBlockDao blockDao = new MockBlockDao(new Block(), originalHashes);
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);

		// Act:
		final List<byte[]> hashes = requiredBlockDao.getHashesFrom(11, 14);

		// Assert:
		Assert.assertThat(hashes, IsSame.sameInstance(originalHashes));
		Assert.assertThat(blockDao.getNumGetHashesFromCalls(), IsEqual.equalTo(1));
		Assert.assertThat(blockDao.getLastGetHashesFromHeight(), IsEqual.equalTo(11L));
		Assert.assertThat(blockDao.getLastGetHashesFromLimit(), IsEqual.equalTo(14));
	}
}
