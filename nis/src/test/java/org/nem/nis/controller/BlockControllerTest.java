package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.controller.utils.DbBlockIoAdapter;
import org.nem.nis.controller.utils.RequiredBlockDaoAdapter;
import org.nem.nis.test.*;

public class BlockControllerTest {

	@Test
	public void blockGetReturnsMappedBlockFromDao() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final MockAccountLookup accountLookup = new MockAccountLookup();
		final MockBlockDao blockDao = new MockBlockDao(NisUtils.createBlockWithTimeStamp(27));
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);
		final DbBlockIoAdapter dbBlockIoAdapter = new DbBlockIoAdapter(requiredBlockDao, accountLookup);
		final BlockController controller = new BlockController(dbBlockIoAdapter);

		// Act:
		final org.nem.core.model.Block block = controller.blockGet(hash.toString());

		// Assert:
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(27)));
		Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
		Assert.assertThat(blockDao.getNumFindByHashCalls(), IsEqual.equalTo(1));
		Assert.assertThat(blockDao.getLastFindByHashHash(), IsEqual.equalTo(hash));
	}

	@Test
	public void blockAtReturnsMappedBlockFromDao() {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();
		final MockBlockDao blockDao = new MockBlockDao(NisUtils.createBlockWithTimeStamp(27));
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);
		final DbBlockIoAdapter dbBlockIoAdapter = new DbBlockIoAdapter(requiredBlockDao, accountLookup);
		final BlockController controller = new BlockController(dbBlockIoAdapter);

		// Act:
		final org.nem.core.model.Block block = controller.blockAt(new BlockHeight(12));

		// Assert:
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(27)));
		Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
		Assert.assertThat(blockDao.getNumFindByHeightCalls(), IsEqual.equalTo(1));
		Assert.assertThat(blockDao.getLastFindByHeightHeight(), IsEqual.equalTo(new BlockHeight(12)));
	}
}
