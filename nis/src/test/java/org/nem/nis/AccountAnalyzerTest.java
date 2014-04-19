package org.nem.nis;

import org.junit.*;
import org.nem.core.model.*;
import org.nem.nis.dbmodel.Account;
import org.nem.nis.dbmodel.Transfer;
import org.nem.core.test.MockAccount;
import org.nem.nis.test.MockAccountAnalyzer;
import org.nem.core.test.Utils;

import java.util.*;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;

public class AccountAnalyzerTest {
	public static final Amount SENDER_AMOUNT = Amount.fromNem(10);
	public static final Amount RECIPIENT1_AMOUNT = Amount.fromNem(3);
	public static final Amount RECIPIENT2_AMOUNT = Amount.fromNem(5);
	public static final Amount RECIPIENT1_FEE = Amount.fromMicroNem(6000);
	public static final Amount RECIPIENT2_FEE = Amount.fromMicroNem(10000);
	private static org.nem.core.model.Account sender = new MockAccount(Address.fromEncoded(GenesisBlock.ACCOUNT.getAddress().getEncoded()));
	private static org.nem.core.model.Account recipient1 = new org.nem.core.model.Account(Utils.generateRandomAddress());
	private static org.nem.core.model.Account recipient2 = new org.nem.core.model.Account(Utils.generateRandomAddress());
	private static Account dbSender = new Account(sender.getAddress().getEncoded(), sender.getKeyPair().getPublicKey());
	private static Account dbRecipient1 = new Account(recipient1.getAddress().getEncoded(), null);
	private static Account dbRecipient2 = new Account(recipient2.getAddress().getEncoded(), null);

	@Test
	public void aaAnalyzeDoesntCacheDummyResults() {
		// Arrange:
		MockAccountAnalyzer aa = new MockAccountAnalyzer();
		aa.initializeGenesisAccount(sender).incrementBalance(SENDER_AMOUNT);

		// Act:
		org.nem.core.model.Account t1 = aa.findByAddress(recipient1.getAddress());
		org.nem.core.model.Account t2 = aa.findByAddress(recipient2.getAddress());

		// Assert:
		Assert.assertThat(t1, not(sameInstance(t2)));
	}

	@Test
	public void aaAnalyzeCachesAccounts() {
		// Arrange:
		org.nem.nis.dbmodel.Block b = prepareTestBlock(dbSender, dbRecipient1, dbRecipient2);
		MockAccountAnalyzer aa = new MockAccountAnalyzer();
		aa.initializeGenesisAccount(sender).incrementBalance(SENDER_AMOUNT);

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
		org.nem.nis.dbmodel.Block b = prepareTestBlock(dbSender, dbRecipient1, dbRecipient2);
		MockAccountAnalyzer aa = new MockAccountAnalyzer();
		aa.initializeGenesisAccount(sender).incrementBalance(SENDER_AMOUNT);

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
		org.nem.nis.dbmodel.Block b = prepareTestBlock(dbSender, dbRecipient1, dbRecipient2);
		MockAccountAnalyzer aa = new MockAccountAnalyzer();
		aa.initializeGenesisAccount(sender).incrementBalance(SENDER_AMOUNT);

		// Act:
		aa.analyze(b);
		org.nem.core.model.Account t1 = aa.findByNemAddress(recipient1);
		org.nem.core.model.Account t2 = aa.findByNemAddress(recipient2);
		org.nem.core.model.Account t3 = aa.findByNemAddress(sender);

		// Assert:
		Assert.assertThat(t1.getBalance(), equalTo(RECIPIENT1_AMOUNT));
		Assert.assertThat(t2.getBalance(), equalTo(RECIPIENT2_AMOUNT));
		// zero fees
		final Amount rest = SENDER_AMOUNT
				.subtract(RECIPIENT1_AMOUNT).subtract(RECIPIENT1_FEE)
				.subtract(RECIPIENT2_AMOUNT).subtract(RECIPIENT2_FEE);
		Assert.assertThat(t3.getBalance(), equalTo(rest));
	}

	private org.nem.nis.dbmodel.Block prepareTestBlock(Account sender, Account recipient1, Account recipient2) {
		Transfer t1 = prepareTransfer(sender, recipient1, RECIPIENT1_AMOUNT, RECIPIENT1_FEE, 0);
		Transfer t2 = prepareTransfer(sender, recipient2, RECIPIENT2_AMOUNT, RECIPIENT2_FEE, 1);

		org.nem.nis.dbmodel.Block b = new org.nem.nis.dbmodel.Block(
				Hash.ZERO, 1, Hash.ZERO, Hash.ZERO, 0, sender, new byte[64], 1L, 8 * 1000000L, 0L, 123L
		);

		b.setBlockTransfers(Arrays.asList(t1, t2));

		return b;
	}

	private Transfer prepareTransfer(Account sender, Account recipient, Amount amount, Amount fee, int idInBlock) {
		return new Transfer(Hash.ZERO, 1, TransactionTypes.TRANSFER,
				fee.getNumMicroNem(),
				0, 0,
				sender,
				new byte[64], // sig
				recipient,
				idInBlock,
				amount.getNumMicroNem(),
				0L
		);
	}
}
