package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.*;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.MockAccountLookup;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.controller.utils.RequiredBlockDaoAdapter;
import org.nem.nis.dbmodel.Transfer;
import org.nem.nis.test.MockAccountAnalyzer;
import org.nem.nis.test.MockBlockDao;

import java.util.ArrayList;
import java.util.List;

public class BlockControllerTest {

	@Test
	public void blockGetReturnsMappedBlockFromDao() {
		// Arrange:
		final Hash hash = new Hash(Utils.generateRandomBytes(64));
		final MockAccountLookup accountLookup = new MockAccountLookup();
		final MockBlockDao blockDao = new MockBlockDao(createBlockWithTimeStamp(27));
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);
		final BlockController controller = new BlockController(requiredBlockDao, accountLookup);

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
		final MockBlockDao blockDao = new MockBlockDao(createBlockWithTimeStamp(27));
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);
		final BlockController controller = new BlockController(requiredBlockDao, accountLookup);

		// Act:
		final org.nem.core.model.Block block = controller.blockAt(getHeightDeserializer(12));

		// Assert:
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(27)));
		Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
		Assert.assertThat(blockDao.getNumFindByHeightCalls(), IsEqual.equalTo(1));
		Assert.assertThat(blockDao.getLastFindByHeightHeight(), IsEqual.equalTo(12L));
	}

	private static org.nem.nis.dbmodel.Block createBlockWithTimeStamp(final int timeStamp) {
		final org.nem.nis.dbmodel.Account account = new org.nem.nis.dbmodel.Account();
		account.setPublicKey(Utils.generateRandomPublicKey());

		final org.nem.nis.dbmodel.Block block = new org.nem.nis.dbmodel.Block();
		block.setForgerId(account);
		block.setTimestamp(timeStamp);
		block.setHeight(10L);
		block.setForgerProof(Utils.generateRandomBytes(64));
		block.setBlockTransfers(new ArrayList<Transfer>());
		return block;
	}

	private static JsonDeserializer getHeightDeserializer(final long height) {
		final JsonSerializer serializer = new JsonSerializer();
		serializer.writeLong("height", height);
		return new JsonDeserializer(serializer.getObject(), new DeserializationContext(null));
	}
}
