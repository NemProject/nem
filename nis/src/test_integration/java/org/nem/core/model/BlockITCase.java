package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;


public class BlockITCase {
	@Test
	public void multisigSignaturesShouldntAffectBlockHash() {
		// Assert:
		final Account harvester = Utils.generateRandomAccount();
		final Account sender = Utils.generateRandomAccount();
		final Account multisigAccount = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Account cosigner1 = Utils.generateRandomAccount();
		final Account cosigner2 = Utils.generateRandomAccount();

		final Transaction transaction = new TransferTransaction(TimeInstant.ZERO, multisigAccount, recipient, Amount.fromNem(123), null);
		final MultisigTransaction multisigTransaction = new MultisigTransaction(TimeInstant.ZERO, sender, transaction);
		multisigTransaction.sign();

		final MultisigSignatureTransaction signature1 = new MultisigSignatureTransaction(TimeInstant.ZERO, cosigner1, HashUtils.calculateHash(transaction));
		multisigTransaction.addSignature(signature1);
		signature1.sign();

		final Block block = new Block(harvester, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, BlockHeight.ONE);
		block.addTransaction(multisigTransaction);

		// Act:
		final Hash hash1 = HashUtils.calculateHash(block);

		final MultisigSignatureTransaction signature2 = new MultisigSignatureTransaction(TimeInstant.ZERO, cosigner2, HashUtils.calculateHash(transaction));
		multisigTransaction.addSignature(signature2);
		signature2.sign();

//		block.sign();

		final Hash hash2 = HashUtils.calculateHash(block);

		Assert.assertThat(hash2, IsEqual.equalTo(hash1));
	}
}
