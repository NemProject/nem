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
	public void orderOfAddingOfMultisigSignaturesShouldntAffectBlockHash() {
		// Assert:
		final Account harvester = Utils.generateRandomAccount();
		final Account sender = Utils.generateRandomAccount();
		final Account multisigAccount = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Account cosigner1 = Utils.generateRandomAccount();
		final Account cosigner2 = Utils.generateRandomAccount();

		final Transaction transaction = new TransferTransaction(TimeInstant.ZERO, multisigAccount, recipient, Amount.fromNem(123), null);
		final MultisigTransaction multisigTransaction1 = new MultisigTransaction(TimeInstant.ZERO, sender, transaction);
		multisigTransaction1.sign();
		final MultisigTransaction multisigTransaction2 = new MultisigTransaction(TimeInstant.ZERO, sender, transaction);
		multisigTransaction2.sign();

		final MultisigSignatureTransaction signature1 = new MultisigSignatureTransaction(TimeInstant.ZERO, cosigner1, HashUtils.calculateHash(transaction));
		signature1.sign();

		final MultisigSignatureTransaction signature2 = new MultisigSignatureTransaction(TimeInstant.ZERO, cosigner2, HashUtils.calculateHash(transaction));
		signature2.sign();

		multisigTransaction1.addSignature(signature1);
		multisigTransaction1.addSignature(signature2);

		multisigTransaction2.addSignature(signature2);
		multisigTransaction2.addSignature(signature1);

		final Block block1 = new Block(harvester, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, BlockHeight.ONE);
		block1.addTransaction(multisigTransaction1);

		final Block block2 = new Block(harvester, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, BlockHeight.ONE);
		block2.addTransaction(multisigTransaction2);

		// Act:
		final Hash hash1 = HashUtils.calculateHash(block1);
		final Hash hash2 = HashUtils.calculateHash(block2);

		// Assert:
		Assert.assertThat(hash2, IsEqual.equalTo(hash1));
	}
}
