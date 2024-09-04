package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.test.RemoteLinkFactory;

public class ExpiredMosaicEntryTest {

	@Test
	public void canCreateExpiredMosaicType() {
		MatcherAssert.assertThat(ExpiredMosaicType.Expired.value(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(ExpiredMosaicType.Restored.value(), IsEqual.equalTo(2));
	}

	@Test
	public void canCreateEntry() {
		// Arrange:
		final MosaicBalances balances = new MosaicBalances();

		// Act:
		final ExpiredMosaicEntry entry = new ExpiredMosaicEntry(Utils.createMosaicId(123), balances, ExpiredMosaicType.Restored);

		// Assert:
		MatcherAssert.assertThat(entry.getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(123)));
		MatcherAssert.assertThat(entry.getBalances(), IsSame.sameInstance(balances));
		MatcherAssert.assertThat(entry.getExpiredMosaicType(), IsEqual.equalTo(ExpiredMosaicType.Restored));
	}
}
