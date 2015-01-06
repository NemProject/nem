package org.nem.nis.dbmodel;

import org.hamcrest.core.IsSame;
import org.junit.*;
import org.mockito.Mockito;

public class TransferBlockPairTest {

	@Test
	public void canCreatePair() {
		// Arrange:
		final DbTransferTransaction dbTransferTransaction = Mockito.mock(DbTransferTransaction.class);
		final DbBlock block = Mockito.mock(DbBlock.class);

		// Act:
		final TransferBlockPair pair = new TransferBlockPair(dbTransferTransaction, block);

		// Assert:
		Assert.assertThat(pair.getDbTransferTransaction(), IsSame.sameInstance(dbTransferTransaction));
		Assert.assertThat(pair.getDbBlock(), IsSame.sameInstance(block));
	}
}