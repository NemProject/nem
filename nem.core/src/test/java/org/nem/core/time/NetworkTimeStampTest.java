package org.nem.core.time;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

public class NetworkTimeStampTest {

	// region constructor

	@Test
	public void canBeCreatedFromLongValue() {
		// Act:
		final NetworkTimeStamp networkTimeStamp = new NetworkTimeStamp(1234);

		// Assert:
		MatcherAssert.assertThat(networkTimeStamp.getRaw(), IsEqual.equalTo(1234L));
	}

	// endregion

	// region subtraction

	@Test
	public void networkTimeStampsCanBeSubtracted() {
		// Arrange:
		final NetworkTimeStamp networkTimeStamp1 = new NetworkTimeStamp(1234);
		final NetworkTimeStamp networkTimeStamp2 = new NetworkTimeStamp(576);

		// Act + Assert:
		MatcherAssert.assertThat(networkTimeStamp1.subtract(networkTimeStamp2), IsEqual.equalTo(658L));
		MatcherAssert.assertThat(networkTimeStamp2.subtract(networkTimeStamp1), IsEqual.equalTo(-658L));
	}

	// endregion

	// region inline serialization

	@Test
	public void canWriteNetworkTimeStamp() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final NetworkTimeStamp networkTimeStamp = new NetworkTimeStamp(1234);

		// Act:
		NetworkTimeStamp.writeTo(serializer, "timeStamp", networkTimeStamp);

		// Assert:
		final JSONObject object = serializer.getObject();
		MatcherAssert.assertThat(object.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(object.get("timeStamp"), IsEqual.equalTo(1234L));
	}

	@Test
	public void canRoundtripNetworkTimeStamp() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final NetworkTimeStamp originalNetworkTimeStamp = new NetworkTimeStamp(1234);

		// Act:
		NetworkTimeStamp.writeTo(serializer, "timeStamp", originalNetworkTimeStamp);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final NetworkTimeStamp networkTimeStamp = NetworkTimeStamp.readFrom(deserializer, "timeStamp");

		// Assert:
		MatcherAssert.assertThat(networkTimeStamp, IsEqual.equalTo(originalNetworkTimeStamp));
	}

	// endregion
}
