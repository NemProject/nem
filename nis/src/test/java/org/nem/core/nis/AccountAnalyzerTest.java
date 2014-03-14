package org.nem.core.nis;

import org.hamcrest.core.IsSame;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.dbmodel.Account;
import org.nem.core.dbmodel.Block;
import org.nem.core.dbmodel.Transfer;
import org.nem.core.model.Address;
import org.nem.core.model.TransactionTypes;
import org.nem.core.test.MockAccount;
import org.nem.core.test.MockAccountAnalyzer;
import org.nem.core.test.Utils;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.Genesis;

import java.util.*;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;

public class AccountAnalyzerTest {
	public static final long RECIPIENT1_AMOUNT = 3 * 1000000L;
	public static final long RECIPIENT2_AMOUNT = 5 * 1000000L;
	private static org.nem.core.model.Account sender = new MockAccount(Address.fromEncoded(Genesis.CREATOR_ACCOUNT_ID));
	private static org.nem.core.model.Account recipient1 = new org.nem.core.model.Account(Utils.generateRandomAddress());
	private static org.nem.core.model.Account recipient2 = new org.nem.core.model.Account(Utils.generateRandomAddress());
	private static Account dbSender = new Account(sender.getAddress().getEncoded(), sender.getKeyPair().getPublicKey());
	private static Account dbRecipient1 = new Account(recipient1.getAddress().getEncoded(), null);
	private static Account dbRecipient2 = new Account(recipient2.getAddress().getEncoded(), null);

	@Test
	public void aaAnalyzeDoesntCacheDummyResults() {
		// Arrange:
		Block b = prepareTestBlock(dbSender, dbRecipient1, dbRecipient2);
		MockAccountAnalyzer aa = new MockAccountAnalyzer();

		// Act:
		org.nem.core.model.Account t1 = aa.findByAddress(recipient1.getAddress());
		org.nem.core.model.Account t2 = aa.findByAddress(recipient2.getAddress());

		// Assert:
		Assert.assertThat(t1, not(sameInstance(t2)));
	}

	@Test
	public void aaAnalyzeCachesAccounts() {
		// Arrange:
		Block b = prepareTestBlock(dbSender, dbRecipient1, dbRecipient2);
		MockAccountAnalyzer aa = new MockAccountAnalyzer();

		// Act:
		aa.analyze(b);
		org.nem.core.model.Account t1a = aa.findByNemAddress(recipient1);
		org.nem.core.model.Account t1b = aa.findByNemAddress(recipient1);
		org.nem.core.model.Account t2 = aa.findByNemAddress(recipient2);

		// Assert:
		Assert.assertThat(t1a, equalTo(recipient1));
		Assert.assertThat(t1a, equalTo(t1b));
		Assert.assertThat(t1a, sameInstance(t1b));
		Assert.assertThat(t1a, not(sameInstance(t2)));
		Assert.assertThat(t2, equalTo(recipient2));
	}

	@Test
	public void aaAnalyzeSearchingByPublicKeyWorks() {
		// Arrange:
		Block b = prepareTestBlock(dbSender, dbRecipient1, dbRecipient2);
		MockAccountAnalyzer aa = new MockAccountAnalyzer();

		// Act:
		aa.analyze(b);
		// this should search by nem address
		org.nem.core.model.Account t3a = aa.findByNemAddress(sender);
		// this should search by public key
		org.nem.core.model.Account t3b = aa.findByPublicKey(sender);

		// Assert:
		Assert.assertThat(t3a, sameInstance(t3b));
	}

	@Test
	public void aaAnalyzeChangesBalances() {
		// Arrange:
		Block b = prepareTestBlock(dbSender, dbRecipient1, dbRecipient2);
		MockAccountAnalyzer aa = new MockAccountAnalyzer();

		// Act:
		aa.analyze(b);
		org.nem.core.model.Account t1 = aa.findByNemAddress(recipient1);
		org.nem.core.model.Account t2 = aa.findByNemAddress(recipient2);
		org.nem.core.model.Account t3 = aa.findByNemAddress(sender);

		// Assert:
		Assert.assertThat(t1.getBalance(), equalTo(RECIPIENT1_AMOUNT));
		Assert.assertThat(t2.getBalance(), equalTo(RECIPIENT2_AMOUNT));
		Assert.assertThat(t3.getBalance(), equalTo(-RECIPIENT1_AMOUNT - RECIPIENT2_AMOUNT));
	}

	private Block prepareTestBlock(Account sender, Account recipient1, Account recipient2) {
		Transfer t1 = prepareTransfer(sender, recipient1, RECIPIENT1_AMOUNT, 1);
		Transfer t2 = prepareTransfer(sender, recipient2, RECIPIENT2_AMOUNT, 2);

		Block b = new Block(
				1L, 1, new byte[32], new byte[32], 0, sender, new byte[64], new byte[32], 1L, 8*1000000L, 0L
		);

		b.setBlockTransfers(Arrays.asList(t1, t2));

		return b;
	}

	private Transfer prepareTransfer(Account sender, Account recipient, long amount, int idInBlock) {
		return new Transfer(1L, new byte[32], 1, TransactionTypes.TRANSFER,
				0L, // fee
				0, 0,
				sender,
				new byte[64], // sig
				recipient,
				idInBlock,
				amount,
				0L
		);
	}
}