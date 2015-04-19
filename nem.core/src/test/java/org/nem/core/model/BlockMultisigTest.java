package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

public class BlockMultisigTest {
	@Test
	public void orderOfAddingOfMultisigSignaturesShouldNotAffectBlockHash() {
		// Assert:
		final Account harvester = Utils.generateRandomAccount();
		final Account sender = Utils.generateRandomAccount();
		final Account multisigAccount = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Account cosigner1 = Utils.generateRandomAccount();
		final Account cosigner2 = Utils.generateRandomAccount();

		final Transaction transaction = new TransferTransaction(TimeInstant.ZERO, multisigAccount, recipient, Amount.fromNem(123), null);
		final SimpleMultisigContext context = new SimpleMultisigContext(transaction);
		final MultisigTransaction multisigTransaction1 = context.createMultisig(sender);
		multisigTransaction1.sign();
		final MultisigTransaction multisigTransaction2 = context.createMultisig(sender);
		multisigTransaction2.sign();

		final MultisigSignatureTransaction signature1 = context.createSignature(cosigner1);
		signature1.sign();

		final MultisigSignatureTransaction signature2 = context.createSignature(cosigner2);
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
