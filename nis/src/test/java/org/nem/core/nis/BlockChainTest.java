package org.nem.core.nis;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.Account;
import org.nem.core.test.Utils;
import org.nem.core.transactions.TransferTransaction;
import org.nem.nis.BlockChain;

public class BlockChainTest {
	@Test
	public void canProcessTransaction() {
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final BlockChain blockChain = new BlockChain();

		// Act:
		TransferTransaction transaction = new TransferTransaction(signer, recipient, 123, null);
		transaction.sign();
		boolean result = blockChain.processTransaction(transaction);

		// Assert:
		Assert.assertThat(transaction.verify(), IsEqual.equalTo(true));
		Assert.assertThat(result, IsEqual.equalTo(true));
	}

	@Test
	public void cannotProcessSameTransaction() {
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final BlockChain blockChain = new BlockChain();

		// Act:
		TransferTransaction transaction = new TransferTransaction(signer, recipient, 123, null);
		transaction.sign();

		boolean result1 = blockChain.processTransaction(transaction);
		boolean result2 = blockChain.processTransaction(transaction);

		// Assert:
		Assert.assertThat(transaction.verify(), IsEqual.equalTo(true));
		Assert.assertThat(result1, IsEqual.equalTo(true));
		Assert.assertThat(result2, IsEqual.equalTo(false));
	}
}
