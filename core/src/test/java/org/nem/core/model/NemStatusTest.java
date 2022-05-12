package org.nem.core.model;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class NemStatusTest {

	@Test
	public void canCreateNemStatusFromValue() {
		// Assert:
		MatcherAssert.assertThat(NemStatus.fromValue(0), IsEqual.equalTo(NemStatus.UNKNOWN));
		MatcherAssert.assertThat(NemStatus.fromValue(1), IsEqual.equalTo(NemStatus.STOPPED));
		MatcherAssert.assertThat(NemStatus.fromValue(2), IsEqual.equalTo(NemStatus.STARTING));
		MatcherAssert.assertThat(NemStatus.fromValue(3), IsEqual.equalTo(NemStatus.RUNNING));
		MatcherAssert.assertThat(NemStatus.fromValue(4), IsEqual.equalTo(NemStatus.BOOTING));
		MatcherAssert.assertThat(NemStatus.fromValue(5), IsEqual.equalTo(NemStatus.BOOTED));
		MatcherAssert.assertThat(NemStatus.fromValue(6), IsEqual.equalTo(NemStatus.SYNCHRONIZED));
		MatcherAssert.assertThat(NemStatus.fromValue(7), IsEqual.equalTo(NemStatus.NO_REMOTE_NIS_AVAILABLE));
	}

	@Test(expected = IllegalArgumentException.class)
	public void fromValueThrowsExceptionIfSuppliedValueIsUnknown() {
		NemStatus.fromValue(10);
	}
}
