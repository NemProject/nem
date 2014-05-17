package org.nem.nis.poi;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.math.DenseMatrix;
import org.nem.core.math.Matrix;
import org.nem.core.model.*;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.test.MockAccount;

import java.util.*;

public class PoiContextTest {

	private static final double TELEPORTATION_PROB = .85;
	
	@Test
	public void vestedBalanceVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) both foraging-eligible and non-foraging-eligible accounts are represented
		// (2) coin days are not normalized
		Assert.assertThat(
				context.getVestedBalanceVector(),
				IsEqual.equalTo(new ColumnVector(3000001, 2999999, 5, 1000000, 999999, 4000000)));
	}

	@Test
	public void outLinkScoreVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) both foraging-eligible and non-foraging-eligible accounts are represented
		// (2) calculation delegates to PoiAccountInfo
		Assert.assertThat(
				context.getOutLinkScoreVector().roundTo(5),
				IsEqual.equalTo(new ColumnVector(1e06, 0, 6e06, 0, 5e06, 10e06)));
	}

	@Test
	public void importanceVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// Outlink matrix should look like:
		// (1 0 0 0 0 0)
		// (0 0 0 0 0 0)
		// (0 0 1 0 0 0)
		// (0 0 0 0 0 0)
		// (0 0 0 0 1 0)
		// (0 0 0 0 0 1)
		// (1) both foraging-eligible and non-foraging-eligible accounts are represented
		// (2) importance vector is initially set to row sum vector of the outlink matrix
		// (3) importance vector is normalized
		Assert.assertThat(
				context.getImportanceVector(),
				IsEqual.equalTo(new ColumnVector(0.25, 0.0, 0.25, 0.0, 0.25, 0.25)));
	}

	@Test
	public void teleportationVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) accounts without out-links are dangling
		Assert.assertThat(
				context.getTeleportationVector(),
				IsEqual.equalTo(new ColumnVector(TELEPORTATION_PROB, TELEPORTATION_PROB, TELEPORTATION_PROB, TELEPORTATION_PROB, TELEPORTATION_PROB, TELEPORTATION_PROB)));
	}

	@Test
	public void inverseTeleportationVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) accounts without out-links are dangling
		Assert.assertThat(
				context.getInverseTeleportationVector().roundTo(5),
				IsEqual.equalTo(new ColumnVector(0.0250, 0.0250, 0.0250, 0.0250, 0.0250, 0.0250)));
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

	@Test
	public void dangleVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) accounts without out-links are dangling
		Assert.assertThat(
				context.getDangleVector(),
				IsEqual.equalTo(new ColumnVector(1, 0, 1, 0, 1, 1)));
	}

	@Test
	public void outLinkMatrixIsInitializedCorrectly() {

		// Arrange: create 4 accounts
		final int umInNem = Amount.MICRONEMS_IN_NEM;
		final List<TestAccountInfo> accountInfos = Arrays.asList(
				new TestAccountInfo(umInNem, umInNem, null),
				new TestAccountInfo(umInNem, umInNem, null),
				new TestAccountInfo(umInNem, umInNem, null),
				new TestAccountInfo(umInNem, umInNem, null));

		final BlockHeight height = new BlockHeight(21);
		final List<Account> accounts = createTestPoiAccounts(accountInfos, height);

		// set up account links
		addAccountLink(height, accounts.get(0), accounts.get(1), 6);
		addAccountLink(height, accounts.get(0), accounts.get(2), 4);
		addAccountLink(height, accounts.get(1), accounts.get(0), 2);
		addAccountLink(height, accounts.get(3), accounts.get(0), 3);
		addAccountLink(height, accounts.get(3), accounts.get(2), 5);

		// Act:
		final PoiContext context = new PoiContext(accounts, accounts.size(), height);

		// Assert:
		// (1) account link weights are normalized
		final Matrix expectedAccountLinks = new DenseMatrix(4, 4);
		expectedAccountLinks.setAt(1, 0, 0.6);
		expectedAccountLinks.setAt(2, 0, 0.4);
		expectedAccountLinks.setAt(0, 1, 1.0);
		expectedAccountLinks.setAt(0, 3, 0.375);
		expectedAccountLinks.setAt(2, 3, 0.625);

		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) {
				Assert.assertTrue(expectedAccountLinks.getAt(i, j) == context.getOutLinkMatrix().getAt(i, j));
			}
		}
	}

	private static void addAccountLink(
			final BlockHeight height,
			final Account sender,
			final Account recipient,
			final int amount) {

		List<AccountLink> accountLinks = sender.getOutlinks();
		if (null == accountLinks) {
			accountLinks = new ArrayList<>();
			sender.setOutlinks(accountLinks);
		}

		final AccountLink link = new AccountLink(height, Amount.fromNem(amount), recipient);
		sender.getOutlinks().add(link);
	}

	private static List<Account> createTestPoiAccounts(
			final List<TestAccountInfo> accountInfos,
			final BlockHeight height) {
		final List<Account> accounts = new ArrayList<>();
		for (final TestAccountInfo info : accountInfos) {
			final MockAccount account = new MockAccount();
			account.incrementBalance(Amount.fromMicroNem(info.balance));
			account.setVestedBalanceAt(Amount.fromMicroNem(info.vestedBalance), height);

			final List<AccountLink> outLinks = new ArrayList<>();
			for (final int amount : info.amounts) {
				// TODO: addOutLinks probably makes more sense
				final AccountLink link = new AccountLink(height, Amount.fromNem(amount), account);
				outLinks.add(link);
			}

			account.setOutlinks(outLinks);
			accounts.add(account);
		}

		return accounts;
	}

	private static PoiContext createTestPoiContext() {
		final int umInNem = Amount.MICRONEMS_IN_NEM;
		final List<TestAccountInfo> accountInfos = Arrays.asList(
				new TestAccountInfo(3 * umInNem + 1,	umInNem - 1,		new int[] { 1 }), // 1 * 1
				new TestAccountInfo(3 * umInNem - 1,	4 * umInNem,		null),
				new TestAccountInfo(5, 					umInNem,			new int[] { 1, 2, 7 }), // 2 * 3
				new TestAccountInfo(umInNem,			3 * umInNem - 1,	null),
				new TestAccountInfo(umInNem - 1,		3 * umInNem + 1,	new int[] { 1, 1, 4, 3, 1 }), // 1 * 5
				new TestAccountInfo(4 * umInNem,		5,					new int[] { 7, 3 })); // 5 * 2

		final BlockHeight height = new BlockHeight(21);
		final List<Account> accounts = createTestPoiAccounts(accountInfos, height);
		return new PoiContext(accounts, accounts.size(), height);
	}

	private static class TestAccountInfo {

		public final int vestedBalance;
		public final int balance;
		public final int[] amounts;

		public TestAccountInfo(int vestedBalance, int balance, int[] amounts) {
			this.vestedBalance = vestedBalance;
			this.balance = balance;
			this.amounts = null == amounts ? new int[] { } : amounts;
		}
	}
}