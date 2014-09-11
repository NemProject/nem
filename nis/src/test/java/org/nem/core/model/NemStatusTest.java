package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class NemStatusTest {

	@Test
	public void canCreateNemStatusFromValue() {
		// Assert:
		Assert.assertThat(NemStatus.fromValue(0), IsEqual.equalTo(NemStatus.UNKNOWN));
		Assert.assertThat(NemStatus.fromValue(1), IsEqual.equalTo(NemStatus.STOPPED));
		Assert.assertThat(NemStatus.fromValue(2), IsEqual.equalTo(NemStatus.STARTING));
		Assert.assertThat(NemStatus.fromValue(3), IsEqual.equalTo(NemStatus.RUNNING));
		Assert.assertThat(NemStatus.fromValue(4), IsEqual.equalTo(NemStatus.BOOTED));
		Assert.assertThat(NemStatus.fromValue(5), IsEqual.equalTo(NemStatus.SYNCHRONIZED));
	}

	@Test(expected = IllegalArgumentException.class)
	public void fromValueThrowsExceptionIfSuppliedValueIsUnknown() {
		NemStatus.fromValue(10);
	}
}
