package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockScorer;
import org.nem.nis.controller.viewmodels.BlockDebugInfo;
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
}
