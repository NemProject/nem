package org.nem.nis.poi;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.test.MockAccount;

import java.util.*;

public class PoiContextTest {

	@Test
	public void coinDaysVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) both foraging-eligible and non-foraging-eligible accounts are represented
		// (2) coin days are not normalized
		Assert.assertThat(
				context.getCoinDaysVector(),
				IsEqual.equalTo(new ColumnVector(3, 2, 0, 1, 0, 4)));
	}

	@Test
	public void outLinkScoreVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) both foraging-eligible and non-foraging-eligible accounts are represented
		// (2) calculation delegates to PoiAccountInfo
		Assert.assertThat(
				context.getOutLinkScoreVector(),
				IsEqual.equalTo(new ColumnVector(1, 0, 3, 0, 5, 2)));
	}

	@Test
	public void importanceVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) both foraging-eligible and non-foraging-eligible accounts are represented
		// (2) importance is initially set to balance
		// (3) importance are normalized
		Assert.assertThat(
				context.getImportanceVector(),
				IsEqual.equalTo(new ColumnVector(0, 0.4, 0.1, 0.2, 0.3, 0)));
	}

	@Test
	public void teleportationVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) accounts without out-links are dangling
		Assert.assertThat(
				context.getTeleportationVector(),
				IsEqual.equalTo(new ColumnVector(0.7000, 0.9500, 0.7625, 0.8250, 0.8875, 0.7000)));
	}

	@Test
	public void dangleIndexesAreInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) accounts without out-links are dangling
		Assert.assertThat(
				context.getDangleIndexes(),
				IsEquivalent.equivalentTo(new Integer[]{ 1, 3 }));
	}

	private static PoiContext createTestPoiContext() {
		final int umInNem = Amount.MICRONEMS_IN_NEM;
		final List<TestAccountInfo> accountInfos = Arrays.asList(
				new TestAccountInfo(3 * umInNem + 1,	umInNem - 1,		1),
				new TestAccountInfo(3 * umInNem - 1,	4 * umInNem,		0),
				new TestAccountInfo(5, 					umInNem,			3),
				new TestAccountInfo(umInNem,			3 * umInNem - 1,	0),
				new TestAccountInfo(umInNem - 1,		3 * umInNem + 1,	5),
				new TestAccountInfo(4 * umInNem,		5,					2));

		final List<Account> accounts = new ArrayList<>();
		final BlockHeight height = new BlockHeight(21);
		for (final TestAccountInfo info : accountInfos) {
			final MockAccount account = new MockAccount();
			account.incrementBalance(Amount.fromMicroNem(info.balance));
			account.setCoinDaysAt(Amount.fromMicroNem(info.coinDays), height);

			if (0 != info.outLinkStrength) {
				// TODO: addOutLinks probably makes more sense
				final List<AccountLink> outLinks = new ArrayList<>();
				final AccountLink link = new AccountLink();
				link.setStrength(info.outLinkStrength);
				outLinks.add(link);
				account.setOutlinks(outLinks);
			}

			accounts.add(account);
		}

		return new PoiContext(accounts, accounts.size(), height);
	}

	private static class TestAccountInfo {

		public final int coinDays;
		public final int balance;
		public final int outLinkStrength;

		public TestAccountInfo(int coinDays, int balance, int outLinkStrength) {
			this.coinDays = coinDays;
			this.balance = balance;
			this.outLinkStrength = outLinkStrength;
		}
	}
}