package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockScorer;
import org.nem.nis.service.BlockIo;
import org.nem.nis.test.*;

import java.math.BigInteger;

public class BlockControllerTest {

	@Test
	public void blockGetDelegatesToBlockIo() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final Block blockIoBlock = NisUtils.createRandomBlockWithTimeStamp(27);
		final BlockIo blockIo = Mockito.mock(BlockIo.class);
		Mockito.when(blockIo.getBlock(hash)).thenReturn(blockIoBlock);
		final BlockController controller = new BlockController(blockIo, Mockito.mock(BlockScorer.class));

		// Act:
		final Block block = controller.blockGet(hash.toString());

		// Assert:
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(27)));
		Mockito.verify(blockIo, Mockito.times(1)).getBlock(hash);
		Mockito.verify(blockIo, Mockito.times(1)).getBlock(Mockito.any());
	}

	@Test
	public void blockAtGetDelegatesToBlockIo() {
		// Arrange:
		final Block blockIoBlock = NisUtils.createRandomBlockWithTimeStamp(27);
		final BlockIo blockIo = Mockito.mock(BlockIo.class);
		Mockito.when(blockIo.getBlockAt(new BlockHeight(12))).thenReturn(blockIoBlock);
		final BlockController controller = new BlockController(blockIo, Mockito.mock(BlockScorer.class));

		// Act:
		final Block block = controller.blockAt(new BlockHeight(12));

		// Assert:
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(27)));
		Mockito.verify(blockIo, Mockito.times(1)).getBlockAt(new BlockHeight(12));
		Mockito.verify(blockIo, Mockito.times(1)).getBlockAt(Mockito.any());
	}

	@Test
	public void blockDebugInfoDelegatesToBlockIoAndBlockScorer() {
		// Arrange:
		final BlockHeight height = new BlockHeight(10);
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final TimeInstant timestamp = new TimeInstant(1000);
		final BlockDifficulty difficulty = new BlockDifficulty(123_000_000_000_000L);
		final BigInteger hit = BigInteger.valueOf(1234);
		final Block blockIoBlock = new Block(
				new Account(address),
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				timestamp,
				height);
		blockIoBlock.setDifficulty(difficulty);

		final BlockIo blockIo = Mockito.mock(BlockIo.class);
		Mockito.when(blockIo.getBlockAt(new BlockHeight(43))).thenReturn(blockIoBlock);

		final BlockScorer blockScorer = Mockito.mock(BlockScorer.class);
		Mockito.when(blockScorer.calculateHit(blockIoBlock)).thenReturn(hit);

		final BlockController controller = new BlockController(blockIo, blockScorer);

		// Act:
		final BlockDebugInfo blockDebugInfo = controller.blockDebugInfo("43");

		// Assert:
		Assert.assertThat(blockDebugInfo.getHeight(), IsEqual.equalTo(height));
		Assert.assertThat(blockDebugInfo.getForagerAddress(), IsEqual.equalTo(address));
		Assert.assertThat(blockDebugInfo.getTimeInstant(), IsEqual.equalTo(timestamp));
		Assert.assertThat(blockDebugInfo.getDifficulty(), IsEqual.equalTo(difficulty));
		Assert.assertThat(blockDebugInfo.getHit(), IsEqual.equalTo(hit));

		Mockito.verify(blockIo, Mockito.times(1)).getBlockAt(new BlockHeight(43));
		Mockito.verify(blockIo, Mockito.times(1)).getBlockAt(Mockito.any());

		Mockito.verify(blockScorer, Mockito.times(1)).calculateHit(blockIoBlock);
		Mockito.verify(blockScorer, Mockito.times(1)).calculateHit(Mockito.any());
	}
}
