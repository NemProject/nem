package org.nem.nis.poi;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.nis.test.MockAccount;

import java.util.*;

public class PoiContextTest {
	
	@Test
	public void vestedBalanceVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) both foraging-eligible and non-foraging-eligible accounts are represented
		// (2) vested balances are not normalized
		Assert.assertThat(
				context.getVestedBalanceVector(),
				IsEqual.equalTo(new ColumnVector(3000001, 2999999, 5, 1000000, 999999, 4000000)));
	}

	@Test
	public void outlinkScoreVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) both foraging-eligible and non-foraging-eligible accounts are represented
		// (2) calculation delegates to PoiAccountInfo
		Assert.assertThat(
				context.getOutlinkScoreVector().roundTo(5),
				IsEqual.equalTo(new ColumnVector(1e06, 0, 3e06, 0, 10e06, 7e06)));
	}

	@Test
	public void importanceVectorIsInitializedCorrectly() {
		// Act:
		// (0, 1, 8), (0, 2, 4)
		// (1, 0, 2), (1, 2, 6)
		// (3, 0, 3), (3, 2, 5)
		final PoiContext context = createTestPoiContextWithAccountLinks();

		// Assert:
		// (1) importance vector is initially set to row sum vector of the out-link matrix
		// (2) importance vector is normalized
		Assert.assertThat(
				context.getImportanceVector(),
				IsEqual.equalTo(new ColumnVector(0.375 / 3, 0.6 / 3, 2.025 / 3, 0)));
	}

	@Test
	public void teleportationVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) a uniform vector of 0.850
		final ColumnVector expectedVector = new ColumnVector(6);
		expectedVector.setAll(0.850);
		Assert.assertThat(context.getTeleportationVector(), IsEqual.equalTo(expectedVector));
	}

	@Test
	public void inverseTeleportationVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) a uniform vector of 0.0250
		final ColumnVector expectedVector = new ColumnVector(6);
		expectedVector.setAll(0.0250);
		Assert.assertThat(context.getInverseTeleportationVector().roundTo(5), IsEqual.equalTo(expectedVector));
	}

	@Test
	public void dangleIndexesAreInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) accounts without out-links are dangling
		Assert.assertThat(
				context.getDangleIndexes(),
				IsEquivalent.equivalentTo(new Integer[] { 1, 3 }));
	}

	@Test
	public void dangleVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		// (1) dangle vector is the 1-vector
		Assert.assertThat(
				context.getDangleVector(),
				IsEqual.equalTo(new ColumnVector(1, 1, 1, 1, 1, 1)));
	}

	@Test
	public void outlinkMatrixIsInitializedCorrectly() {
		// Act:
		// (0, 1, 8), (0, 2, 4)
		// (1, 0, 2), (1, 2, 6)
		// (3, 0, 3), (3, 2, 5)
		final PoiContext context = createTestPoiContextWithAccountLinks();

		// Assert:
		// (1) account link weights are normalized
		final Matrix expectedAccountLinks = new DenseMatrix(4, 4);
		expectedAccountLinks.setAt(1, 0, 0.6);
		expectedAccountLinks.setAt(2, 0, 0.4);
		expectedAccountLinks.setAt(2, 1, 1.0);
		expectedAccountLinks.setAt(0, 3, 0.375);
		expectedAccountLinks.setAt(2, 3, 0.625);

		Assert.assertThat(
				context.getOutlinkMatrix().roundTo(5),
				IsEqual.equalTo(expectedAccountLinks));
	}

	private static void addAccountLink(
			final BlockHeight height,
			final Account sender,
			final Account recipient,
			final int amount) {

		final AccountLink link = new AccountLink(height, Amount.fromNem(amount), recipient.getAddress());
		sender.getImportanceInfo().addOutlink(link);
	}

	private static List<Account> createTestPoiAccounts(
			final List<TestAccountInfo> accountInfos,
			final BlockHeight height) {
		final List<Account> accounts = new ArrayList<>();
		for (final TestAccountInfo info : accountInfos) {
			final MockAccount account = new MockAccount();
			account.incrementBalance(Amount.fromMicroNem(info.balance));
			account.setVestedBalanceAt(Amount.fromMicroNem(info.vestedBalance), height);

			for (final int amount : info.amounts) {
				final AccountLink link = new AccountLink(height, Amount.fromNem(amount), Utils.generateRandomAddress());
				account.getImportanceInfo().addOutlink(link);
			}

			accounts.add(account);
		}

		return accounts;
	}

	private static PoiContext createTestPoiContext() {
		final int umInNem = Amount.MICRONEMS_IN_NEM;
		final List<TestAccountInfo> accountInfos = Arrays.asList(
				new TestAccountInfo(3 * umInNem + 1,	umInNem - 1,		new int[] { 1 }), // 1
				new TestAccountInfo(3 * umInNem - 1,	4 * umInNem,		null),
				new TestAccountInfo(5, 					umInNem,			new int[] { 1, 2 }), // 3
				new TestAccountInfo(umInNem,			3 * umInNem - 1,	null),
				new TestAccountInfo(umInNem - 1,		3 * umInNem + 1,	new int[] { 1, 1, 4, 3, 1 }), // 10
				new TestAccountInfo(4 * umInNem,		5,					new int[] { 7 })); // 7

		final BlockHeight height = new BlockHeight(21);
		final List<Account> accounts = createTestPoiAccounts(accountInfos, height);
		return new PoiContext(accounts, accounts.size(), height);
	}

	private static PoiContext createTestPoiContextWithAccountLinks() {
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
		addAccountLink(height, accounts.get(0), accounts.get(1), 8);
		addAccountLink(height, accounts.get(0), accounts.get(2), 4);
		addAccountLink(height, accounts.get(1), accounts.get(0), 2);
		addAccountLink(height, accounts.get(1), accounts.get(2), 6);
		addAccountLink(height, accounts.get(3), accounts.get(0), 3);
		addAccountLink(height, accounts.get(3), accounts.get(2), 5);

		// Act:
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