package org.nem.nis.pox.poi;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.test.Utils;

public class WeightedLinkTest {

	@Test
	public void linkExposesConstructorParameters() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final double amount = 12.34;
		final WeightedLink link = new WeightedLink(address, amount);

		// Assert:
		MatcherAssert.assertThat(link.getOtherAccountAddress(), IsSame.sameInstance(address));
		MatcherAssert.assertThat(link.getWeight(), IsEqual.equalTo(amount));
	}

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final WeightedLink link = createLink(8, "ZZZ");

		// Assert:
		MatcherAssert.assertThat(createLink(8, "ZZZ"), IsEqual.equalTo(link));
		MatcherAssert.assertThat(createLink(9, "ZZZ"), IsNot.not(IsEqual.equalTo(link)));
		MatcherAssert.assertThat(createLink(8, "ZZA"), IsNot.not(IsEqual.equalTo(link)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(link)));
		MatcherAssert.assertThat(8, IsNot.not(IsEqual.equalTo((Object) link)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final WeightedLink link = createLink(8, "ZZZ");
		final int hashCode = link.hashCode();

		// Assert:
		MatcherAssert.assertThat(createLink(8, "ZZZ").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(createLink(9, "ZZZ").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(createLink(8, "ZZA").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsAppropriateRepresentation() {
		// Arrange:
		final WeightedLink link = createLink(8, "ZZZ");

		// Assert:
		MatcherAssert.assertThat(link.toString(), IsEqual.equalTo("8.0 -> ZZZ"));
	}

	// endregion

	private static WeightedLink createLink(final double weight, final String address) {
		return new WeightedLink(Address.fromEncoded(address), weight);
	}
}
