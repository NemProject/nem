package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.messages.PlainMessage;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.core.time.TimeProvider;
import org.nem.core.transactions.TransferTransaction;

public class EntityFactoryTest {

	@Test
	public void currentTimeIsInjectedIntoBlock() {
		// Arrange:
		final EntityFactory factory = new EntityFactory(new MockTimeProvider(11890));
		final Account signer = Utils.generateRandomAccount();
		final Hash previousHash = new Hash(Utils.generateRandomBytes());

		// Act:
		final Block block = factory.createBlock(signer, previousHash, 12);

		// Assert:
		Assert.assertThat(block.getType(), IsEqual.equalTo(1));
		Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(12L));
		Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(previousHash));
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(11890)));
	}

	@Test
	public void currentTimeIsInjectedIntoTransfer() {
		// Arrange:
		final EntityFactory factory = new EntityFactory(new MockTimeProvider(11891));
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Message message = new PlainMessage(new byte[] { 12, 50, 21 });

		// Act:
		TransferTransaction transaction = factory.createTransfer(signer, recipient, new Amount(123), message);

		// Assert:
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(new Amount(123L)));
		Assert.assertThat(transaction.getMessage().getDecodedPayload(), IsEqual.equalTo(new byte[] { 12, 50, 21 }));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(new TimeInstant(11891)));
	}

	private static class MockTimeProvider implements TimeProvider {

		private final int currentTime;

		public MockTimeProvider(final int currentTime) {
			this.currentTime = currentTime;
		}

		@Override
		public TimeInstant getEpochTime() {
			return TimeInstant.ZERO;
		}

		@Override
		public TimeInstant getCurrentTime() {
			return new TimeInstant(this.currentTime);
		}
	}
}
