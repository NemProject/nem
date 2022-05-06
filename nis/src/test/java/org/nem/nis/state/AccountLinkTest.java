package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.test.NisUtils;

public class AccountLinkTest {

	@Test
	public void accountLinkExposesConstructorParameters() {
		// Arrange:
		final BlockHeight height = new BlockHeight(7);
		final Amount amount = Amount.fromNem(12);
		final Address address = Utils.generateRandomAddress();
		final AccountLink link = new AccountLink(height, amount, address);

		// Assert:
		MatcherAssert.assertThat(link.getHeight(), IsSame.sameInstance(height));
		MatcherAssert.assertThat(link.getAmount(), IsSame.sameInstance(amount));
		MatcherAssert.assertThat(link.getOtherAccountAddress(), IsSame.sameInstance(address));
	}

	@Test
	public void canCompareEquivalentLinks() {
		// Arrange:
		final AccountLink link1 = NisUtils.createLink(7, 12, "AAA");
		final AccountLink link2 = NisUtils.createLink(7, 12, "AAA");

		// Assert:
		MatcherAssert.assertThat(link1.compareTo(link2), IsEqual.equalTo(0));
		MatcherAssert.assertThat(link2.compareTo(link1), IsEqual.equalTo(0));
	}

	@Test
	public void linksAreComparedFirstByHeights() {
		// Arrange:
		final AccountLink link1 = NisUtils.createLink(8, 1, "AAA");
		final AccountLink link2 = NisUtils.createLink(7, 12, "ZZZ");

		// Assert:
		MatcherAssert.assertThat(link1.compareTo(link2) >= 1, IsEqual.equalTo(true));
		MatcherAssert.assertThat(link2.compareTo(link1) <= -1, IsEqual.equalTo(true));
	}

	@Test
	public void linksAreComparedSecondByAmounts() {
		// Arrange:
		final AccountLink link1 = NisUtils.createLink(8, 12, "AAA");
		final AccountLink link2 = NisUtils.createLink(8, 1, "ZZZ");

		// Assert:
		MatcherAssert.assertThat(link1.compareTo(link2) >= 1, IsEqual.equalTo(true));
		MatcherAssert.assertThat(link2.compareTo(link1) <= -1, IsEqual.equalTo(true));
	}

	@Test
	public void linksAreComparedLastByAccounts() {
		// Arrange:
		final AccountLink link1 = NisUtils.createLink(8, 1, "ZZZ");
		final AccountLink link2 = NisUtils.createLink(8, 1, "AAA");

		// Assert:
		MatcherAssert.assertThat(link1.compareTo(link2) >= 1, IsEqual.equalTo(true));
		MatcherAssert.assertThat(link2.compareTo(link1) <= -1, IsEqual.equalTo(true));
	}

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final AccountLink link = NisUtils.createLink(8, 1, "ZZZ");

		// Assert:
		MatcherAssert.assertThat(NisUtils.createLink(8, 1, "ZZZ"), IsEqual.equalTo(link));
		MatcherAssert.assertThat(NisUtils.createLink(9, 1, "ZZZ"), IsNot.not(IsEqual.equalTo(link)));
		MatcherAssert.assertThat(NisUtils.createLink(8, 2, "ZZZ"), IsNot.not(IsEqual.equalTo(link)));
		MatcherAssert.assertThat(NisUtils.createLink(8, 1, "ZZA"), IsNot.not(IsEqual.equalTo(link)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(link)));
		MatcherAssert.assertThat(8, IsNot.not(IsEqual.equalTo((Object) link)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final AccountLink link = NisUtils.createLink(8, 1, "ZZZ");
		final int hashCode = link.hashCode();

		// Assert:
		MatcherAssert.assertThat(NisUtils.createLink(8, 1, "ZZZ").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(NisUtils.createLink(9, 1, "ZZZ").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(NisUtils.createLink(8, 2, "ZZZ").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(NisUtils.createLink(8, 1, "ZZA").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsAppropriateRepresentation() {
		// Arrange:
		final AccountLink link = NisUtils.createLink(8, 1, "ZZZ");

		// Assert:
		MatcherAssert.assertThat(link.toString(), IsEqual.equalTo("1000000 -> ZZZ @ 8"));
	}

	// endregion
}
