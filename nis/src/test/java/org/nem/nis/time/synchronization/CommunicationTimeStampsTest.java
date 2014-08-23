package org.nem.nis.time.synchronization;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.NetworkTimeStamp;
import org.nem.core.test.Utils;

import java.util.*;

public class CommunicationTimeStampsTest {

	//region constructor

	@Test
	public void canCreateCommunicationTimeStamps() {
		// Act:
		final CommunicationTimeStamps timeStamps = new CommunicationTimeStamps(new NetworkTimeStamp(5), new NetworkTimeStamp(17));

		// Assert:
		Assert.assertThat(timeStamps.getSendTimeStamp(), IsEqual.equalTo(new NetworkTimeStamp(5)));
		Assert.assertThat(timeStamps.getReceiveTimeStamp(), IsEqual.equalTo(new NetworkTimeStamp(17)));
	}

	//endregion

	//region serialization

	@Test
	public void communicationTimeStampsCanBeRoundTripped() throws Exception {
		// Arrange:
		final CommunicationTimeStamps originalTimeStamps = new CommunicationTimeStamps(new NetworkTimeStamp(5), new NetworkTimeStamp(17));

		// Act:
		final CommunicationTimeStamps timeStamps = new CommunicationTimeStamps(Utils.roundtripSerializableEntity(originalTimeStamps, null));

		// Assert:
		Assert.assertThat(timeStamps.getSendTimeStamp(), IsEqual.equalTo(new NetworkTimeStamp(5)));
		Assert.assertThat(timeStamps.getReceiveTimeStamp(), IsEqual.equalTo(new NetworkTimeStamp(17)));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final List<CommunicationTimeStamps> timeStampsList = createTestCommunicationTimeStampsList();

		// Assert:
		Assert.assertThat(timeStampsList.get(0), IsEqual.equalTo(timeStampsList.get(1)));
		Assert.assertThat(timeStampsList.get(0), IsNot.not(IsEqual.equalTo(timeStampsList.get(2))));
		Assert.assertThat(timeStampsList.get(0), IsNot.not(IsEqual.equalTo(timeStampsList.get(3))));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final List<CommunicationTimeStamps> timeStampsList = createTestCommunicationTimeStampsList();

		// Assert:
		Assert.assertThat(timeStampsList.get(0).hashCode(), IsEqual.equalTo(timeStampsList.get(1).hashCode()));
		Assert.assertThat(timeStampsList.get(0).hashCode(), IsNot.not(IsEqual.equalTo(timeStampsList.get(2).hashCode())));
		Assert.assertThat(timeStampsList.get(0).hashCode(), IsNot.not(IsEqual.equalTo(timeStampsList.get(3).hashCode())));
	}

	//endregion

	private List<CommunicationTimeStamps> createTestCommunicationTimeStampsList() {
		return Arrays.asList(
				new CommunicationTimeStamps(new NetworkTimeStamp(5), new NetworkTimeStamp(17)),
				new CommunicationTimeStamps(new NetworkTimeStamp(5), new NetworkTimeStamp(17)),
				new CommunicationTimeStamps(new NetworkTimeStamp(6), new NetworkTimeStamp(17)),
				new CommunicationTimeStamps(new NetworkTimeStamp(5), new NetworkTimeStamp(18)));
	}
}
