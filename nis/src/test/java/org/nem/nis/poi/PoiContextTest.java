


package org.nem.nis.poi;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.Account;
import org.nem.core.model.AccountLink;
import org.nem.core.model.Amount;
import org.nem.core.model.BlockHeight;
import org.nem.core.test.IsEquivalent;
import org.nem.core.test.MockAccount;
import org.nem.core.test.MockAccountLookup;
import org.nem.core.test.Utils;

import java.util.*;
import java.util.stream.Collectors;

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
				IsEquivalent.equivalentTo(new Integer[] { 1, 3 }));
	}

	@Test
	public void dangleSumIsCalculatedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContext();

		// Assert:
		Assert.assertThat(0.5450 / 6, IsEqual.equalTo(context.calculateDangleSum()));
	}

	public static PoiContext createTestPoiContext() {
		final int umInNem = Amount.MICRONEMS_IN_NEM;
		final List<TestAccountInfo> accountInfos = Arrays.asList(
				new TestAccountInfo(3 * umInNem + 1,	umInNem - 1,		true),
				new TestAccountInfo(3 * umInNem - 1,	4 * umInNem,		false),
				new TestAccountInfo(5, 					umInNem,			true),
				new TestAccountInfo(umInNem,			3 * umInNem - 1,	false),
				new TestAccountInfo(umInNem - 1,		3 * umInNem + 1,	true),
				new TestAccountInfo(4 * umInNem,		5,					true));

		final List<Account> accounts = new ArrayList<>();
		final BlockHeight height = new BlockHeight(21);
		for (final TestAccountInfo info : accountInfos) {
			final MockAccount account = new MockAccount();
			account.incrementBalance(Amount.fromMicroNem(info.balance));
			account.setCoinDaysAt(Amount.fromMicroNem(info.coinDays), height);

			if (info.hasOutLinks) {
				// TODO: addOutLinks probably makes more sense
				final List<AccountLink> outLinks = new ArrayList<>();
				outLinks.add(new AccountLink());
				account.setOutlinks(outLinks);
			}

			accounts.add(account);
		}

		return new PoiContext(accounts, accounts.size(), height);
	}

	private static class TestAccountInfo {

		public final int coinDays;
		public final int balance;
		public final boolean hasOutLinks;

		public TestAccountInfo(int coinDays, int balance, boolean hasOutLinks) {
			this.coinDays = coinDays;
			this.balance = balance;
			this.hasOutLinks = hasOutLinks;
		}
	}

	// TODO: we need a better way to set coin days
	private static class MockAccount extends Account {

		private final Map<BlockHeight, Amount> heightToCoinDaysMap;

		public MockAccount(){
			super(Utils.generateRandomAddress());
			this.heightToCoinDaysMap = new HashMap<>();
		}

		private void setCoinDaysAt(final Amount coinDays, final BlockHeight blockHeight) {
			this.heightToCoinDaysMap.put(blockHeight, coinDays);
		}

		@Override
		public Amount getCoinDayWeightedBalance(final BlockHeight blockHeight) {
			return this.heightToCoinDaysMap.getOrDefault(blockHeight, null);
		}
	}
//
//	// TODO: move to somewhere more general, maybe ColumnVector
//	private static ColumnVector round(final ColumnVector vector, int numPlaces) {
//		final double multiplier = Math.pow(10, numPlaces);
//		final ColumnVector roundedVector = new ColumnVector(vector.getSize());
//		for (int i = 0; i < vector.getSize(); ++i)
//			roundedVector.setAt(i, Math.round(vector.getAt(i) * multiplier) / multiplier);
//
//		return roundedVector;
//	}
}