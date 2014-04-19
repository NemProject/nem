package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.nis.dbmodel.Block;
import org.nem.nis.dbmodel.Transfer;
import org.nem.core.model.*;
import org.nem.core.test.MockAccount;
import org.nem.nis.test.MockBlockChain;
import org.nem.core.test.Utils;
import org.nem.core.time.SystemTimeProvider;
import org.nem.core.model.TransferTransaction;

import java.util.Arrays;

public class BlockChainTest {
	public static final long RECIPIENT1_AMOUNT = 3 * 1000000L;
	public static final long RECIPIENT2_AMOUNT = 5 * 1000000L;
	private static org.nem.core.model.Account SENDER = new MockAccount(Address.fromEncoded(GenesisBlock.ACCOUNT.getAddress().getEncoded()));
	private static org.nem.core.model.Account RECIPIENT1 = new org.nem.core.model.Account(Utils.generateRandomAddress());
	private static org.nem.core.model.Account RECIPIENT2 = new org.nem.core.model.Account(Utils.generateRandomAddress());
	private static org.nem.nis.dbmodel.Account DB_SENDER = new org.nem.nis.dbmodel.Account(SENDER.getAddress().getEncoded(), SENDER.getKeyPair().getPublicKey());
	private static org.nem.nis.dbmodel.Account DB_RECIPIENT1 = new org.nem.nis.dbmodel.Account(RECIPIENT1.getAddress().getEncoded(), null);
	private static org.nem.nis.dbmodel.Account DB_RECIPIENT2 = new org.nem.nis.dbmodel.Account(RECIPIENT2.getAddress().getEncoded(), null);
	private static final SystemTimeProvider time = new SystemTimeProvider();

	@Test
	public void analyzeSavesResults() {
		// Arrange:
		Block block = createDummyDbBlock();
		BlockChain blockChain = new MockBlockChain();
		blockChain.bootup();

		// Act:
		blockChain.analyzeLastBlock(block);

		// Assert:
		Assert.assertThat(blockChain.getLastDbBlock(), IsSame.sameInstance(block));
	}

	private Transaction dummyTransaction(org.nem.core.model.Account recipient, long amount) {
		return new TransferTransaction((new SystemTimeProvider()).getCurrentTime(), SENDER, recipient, new Amount(amount), null);
	}

	private Block createDummyDbBlock() {
		Transaction tx1 = dummyTransaction(RECIPIENT1, RECIPIENT1_AMOUNT);
		Transaction tx2 = dummyTransaction(RECIPIENT2, RECIPIENT2_AMOUNT);
		tx1.sign();
		tx2.sign();

		org.nem.core.model.Block b = new org.nem.core.model.Block(
				SENDER,
				Hash.ZERO,
				time.getCurrentTime(),
				BlockHeight.ONE);

		b.addTransaction(tx1);
		b.addTransaction(tx2);

		b.sign();

		Block dbBlock = new Block(
				HashUtils.calculateHash(b),
				1,
				// generation hash
				Hash.ZERO,
				// prev hash
				Hash.ZERO,
				0, // timestamp
				DB_SENDER,
				// proof
				b.getSignature().getBytes(),
				b.getHeight().getRaw(), // height
				RECIPIENT1_AMOUNT + RECIPIENT2_AMOUNT,
				0L,
				123L
		);

		Transfer dbTransaction1 = new Transfer(
				HashUtils.calculateHash(tx1),
				tx1.getVersion(),
				tx1.getType(),
				0L,
				0, // timestamp
				0, // deadline
				DB_SENDER,
				// proof
				tx1.getSignature().getBytes(),
				DB_RECIPIENT1,
				0, // index
				RECIPIENT1_AMOUNT,
				0L // referenced tx
		);

		Transfer dbTransaction2 = new Transfer(
				HashUtils.calculateHash(tx2),
				tx2.getVersion(),
				tx2.getType(),
				0L,
				0, // timestamp
				0, // deadline
				DB_SENDER,
				// proof
				tx1.getSignature().getBytes(),
				DB_RECIPIENT2,
				0, // index
				RECIPIENT2_AMOUNT,
				0L // referenced tx
		);
		dbBlock.setBlockTransfers(Arrays.asList(dbTransaction1, dbTransaction2));

		return dbBlock;
	}
}
