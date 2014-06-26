package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.MockAccountLookup;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

import static org.mockito.Mockito.mock;

public class BlockMetaDataPairTest {

	@Test
	public void canCreateBlockMetaDataPair() {
		// Arrange:
		final Account forger = Utils.generateRandomAccount();
		final Block block = new Block(forger, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, BlockHeight.ONE);

		// Act:
		final BlockMetaDataPair blockMetaDataPair = new BlockMetaDataPair(block, new BlockMetaData(new Hash(new byte[]{1,2,3,4})));

		// Arrange:
		Assert.assertThat(blockMetaDataPair.getBlock(), IsEqual.equalTo(block));
		Assert.assertThat(blockMetaDataPair.getBlockMetaData().getHash(), IsEqual.equalTo(new Hash(new byte[]{1,2,3,4})));
	}

	@Test
	public void canRoundtripBlockMetaDataPair() {
		final Account forger = Utils.generateRandomAccount();
		final Block block = new Block(forger, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, BlockHeight.ONE);
		block.sign();

		// Act:
		final BlockMetaDataPair entity = new BlockMetaDataPair(block, new BlockMetaData(new Hash(new byte[]{1,2,3,4})));

		// Assert:
		final MockAccountLookup mockAccountLookup = new MockAccountLookup();
		mockAccountLookup.setMockAccount(forger);
		final Deserializer deserializer = Utils.roundtripSerializableEntity(entity, mockAccountLookup);
		final BlockMetaDataPair result = new BlockMetaDataPair(deserializer);

		Assert.assertThat(result.getBlock().getSigner(), IsEqual.equalTo(forger));
		Assert.assertThat(result.getBlock().getPreviousBlockHash(), IsEqual.equalTo(Hash.ZERO));
		// generation hash WON'T be set
		Assert.assertThat(result.getBlock().getTimeStamp(), IsEqual.equalTo(TimeInstant.ZERO));
		Assert.assertThat(result.getBlock().getHeight(), IsEqual.equalTo(BlockHeight.ONE));

		Assert.assertThat(result.getBlockMetaData().getHash(), IsEqual.equalTo(new Hash(new byte[]{1,2,3,4})));
	}

}
