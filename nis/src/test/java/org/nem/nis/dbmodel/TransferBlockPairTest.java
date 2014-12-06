package org.nem.nis.dbmodel;

import org.hamcrest.core.IsSame;
import org.junit.*;
import org.mockito.Mockito;

public class TransferBlockPairTest {

	@Test
	public void canCreatePair() {
		// Arrange:
		final Transfer transfer = Mockito.mock(Transfer.class);
		final Block block = Mockito.mock(Block.class);

		// Act:
		final TransferBlockPair pair = new TransferBlockPair(transfer, block);

		// Assert:
		Assert.assertThat(pair.getTransfer(), IsSame.sameInstance(transfer));
		Assert.assertThat(pair.getBlock(), IsSame.sameInstance(block));
	}
}