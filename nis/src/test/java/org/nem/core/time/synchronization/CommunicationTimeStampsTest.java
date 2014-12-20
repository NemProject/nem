package org.nem.core.time.synchronization;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.core.time.NetworkTimeStamp;

import java.util.HashMap;

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
	public void communicationTimeStampsCanBeRoundTripped() {
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
		final CommunicationTimeStamps timeStamps = new CommunicationTimeStamps(new NetworkTimeStamp(5), new NetworkTimeStamp(17));
		final HashMap<String, CommunicationTimeStamps> timeStampsMap = this.createTestCommunicationTimeStampsForEqualityTests();

		// Assert:
		Assert.assertThat(timeStampsMap.get("default"), IsEqual.equalTo(timeStamps));
		Assert.assertThat(timeStampsMap.get("diff-sendTimeStamp"), IsNot.not(IsEqual.equalTo(timeStamps)));
		Assert.assertThat(timeStampsMap.get("diff-receiveTimeStamp"), IsNot.not(IsEqual.equalTo(timeStamps)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(timeStamps)));
		Assert.assertThat("foo", IsNot.not(IsEqual.equalTo((Object)timeStamps)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final CommunicationTimeStamps timeStamps = new CommunicationTimeStamps(new NetworkTimeStamp(5), new NetworkTimeStamp(17));
		final HashMap<String, CommunicationTimeStamps> timeStampsMap = this.createTestCommunicationTimeStampsForEqualityTests();

		// Assert:
		Assert.assertThat(timeStampsMap.get("default").hashCode(), IsEqual.equalTo(timeStamps.hashCode()));
		Assert.assertThat(timeStampsMap.get("diff-sendTimeStamp").hashCode(), IsNot.not(IsEqual.equalTo(timeStamps.hashCode())));
		Assert.assertThat(timeStampsMap.get("diff-receiveTimeStamp").hashCode(), IsNot.not(IsEqual.equalTo(timeStamps.hashCode())));
	}

	//endregion

	private HashMap<String, CommunicationTimeStamps> createTestCommunicationTimeStampsForEqualityTests() {
		return new HashMap<String, CommunicationTimeStamps>() {
			{
				this.put("default", new CommunicationTimeStamps(new NetworkTimeStamp(5), new NetworkTimeStamp(17)));
				this.put("diff-sendTimeStamp", new CommunicationTimeStamps(new NetworkTimeStamp(6), new NetworkTimeStamp(17)));
				this.put("diff-receiveTimeStamp", new CommunicationTimeStamps(new NetworkTimeStamp(5), new NetworkTimeStamp(18)));
			}
		};
	}
}
