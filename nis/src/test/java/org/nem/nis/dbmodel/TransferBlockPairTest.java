package org.nem.nis.dbmodel;

import org.hamcrest.core.IsSame;
import org.junit.*;
import org.mockito.Mockito;

public class TransferBlockPairTest {

	@Test
	public void canCreatePair() {
		// Arrange:
		final AbstractBlockTransfer transfer = Mockito.mock(AbstractBlockTransfer.class);
		final DbBlock block = Mockito.mock(DbBlock.class);

		// Act:
		final TransferBlockPair pair = new TransferBlockPair(transfer, block);

		// Assert:
		Assert.assertThat(pair.getTransfer(), IsSame.sameInstance(transfer));
		Assert.assertThat(pair.getDbBlock(), IsSame.sameInstance(block));
	}
}